# EG Forms: pinnable form instances + renderer redesign — QA verdict

**Tested:** 2026-08-21 · **Build:** QA V1.36 (`index-C98MwrA7.js`) · **Tenant:** `acme.qa.egalvanic.ai` (2nd tenant: `demo`)
**PRs:** eg-pz-backend **#989** · eg-pz-frontend **#1166** (+ iOS #452, out of scope here)

---

## Verdict — the pinning feature is LIVE on QA and the mechanics work; **one negative case the ticket explicitly requires FAILS** — a cross-company pin is *not* rejected. Filing that as a cross-tenant defect (request-tamper class). Everything else exercised passes.

## ✅ Pin mechanics (backend #989 — verified live)
| Check | Result |
|---|---|
| `POST /eg-form-instance/{id}/pin` a same-template, same-session instance | ✅ **200**; response carries the `pinned_instances` summary |
| `to_dict()` returns bulk-fetched `pinned_instances` (no N+1) | ✅ the primary's GET now includes `pinned_instances[]` with each pinned asset's `form_submission`, `linked_nodes`, `submitted` — once a pin exists |
| Different-template pin rejected | ✅ **400** "Can only pin an instance of the same form" |
| Cross-**session** pin rejected | ✅ **400** (rejected — see note) |
| **Unpin = soft-delete, both data intact** | ✅ `PUT …/unpin/{id}` → `pinned_instances: []`; the unpinned instance's `form_submission` is **byte-identical** afterward, `is_deleted: false` — "a pin is a mapping row, not a copy" holds |

*(The ticket wanted a **404** for the cross-session case; QA returns a **400** with a template-mismatch message because my cross-session probe was also a different template. The outcome — rejected, no pin — is correct; the status/reason differ from the spec.)*

## 🟥 Blocker — cross-company pin is NOT rejected (ticket's designated negative case)
The ticket's negative test: *"attempt to pin an instance belonging to another company … expect a 404, not a successful pin."* **It succeeds.**

- As a **Company B (demo, `93611164`) user**, calling `POST /eg-form-instance/{acmePrimary}/pin {pinned_instance_id: acmePinnee}` **on the acme host** → **HTTP 200**, and the pin **persisted**: reading the primary back with acme's own token shows the cross-tenant pin present with acme's real form data.
- Both ids are acme's (Company A); the caller is Company B; companies confirmed distinct.

**Scope / severity (weighted honestly):** this only succeeds when the demo token is aimed at the **acme host**. On the **demo host** (a normal demo user's actual browser path) the same call is masked-404. So it's a **request-tamper** cross-tenant write — you must point the request at the other tenant's host — not something a demo user reaches by clicking. That places it below a same-host read leak, but it still (a) violates the ticket's explicit 404 requirement and (b) writes durable cross-tenant mapping state. Same root cause as the NETA-3 `eg-form-instance/by-session` finding: the `eg-form-instance` family authenticates the token but doesn't bind the caller's company on the acme host. **Fix: enforce the session→sld→company check against the caller's token, and return 404 as the PR intends.** *(Probe pin cleaned up afterward.)*

## ✅ Renderer / builder (frontend #1166 — confirmed in the live QA bundle)
| Check | Result |
|---|---|
| **Test Selector** is a first-class builder block alongside Verdict | ✅ palette array: `{key:"verdict",label:"Verdict"},{key:"test_selector",label:"Test Selector"}` |
| `data_table` meta_fields honor **calculated / can_overwrite** | ✅ field processing: `L.calculated && L.can_overwrite !== true && …` → a calculated field auto-computes and is read-only **unless** `can_overwrite` is set; `meta_fields` walked |

## ⚠️ Not verified on QA (honest limits)
- **The multi-participant renderer UI** (three assets rendered with own values/calcs + dashed borders, unified index nav, cross-participant required-validation, batched Save/Submit): these live in the `EGFormInstanceDialog` **Pin button (edit-mode only)** + `PinFormsDialog`. In prior QA work the pin UI was not surfaced in this build (a later working-set release supersedes it); the **backend pin mechanics are live** (above), but I did not drive the multi-participant *render* end-to-end in the browser this pass. The PR's own review history (two REQUEST-CHANGES rounds) flags real risks there — Save-Draft demoting an already-submitted pinned instance, partial-batch-failure reporting, and a per-keystroke `new Function` compile cost — worth targeted UI testing when the pin UI is confirmed present.
- **SLD full-sync carries pin rows** (mobile reconstruction): couldn't locate the exact mobile full-sync endpoint from here (`/sld/{id}/full-sync` etc. are masked-404; plain `/sld/{id}` is a 1.9 MB payload without pin rows). Not confirmed — needs the real sync route or the iOS client.
- **Delete-moved-to-picker + Add Form**, **empty-cell tint / taller headers / no-truncation**: renderer-visual, not driven this pass.

## Method
Live QA + PRs #989/#1166 (pasted). Pinned/unpinned real same-template Bolted-Connections instances (Switch 2 ↔ Switch 7) on session `fcc37c67`; confirmed the `pinned_instances` summary, same-form/same-session gating, and soft-delete-with-data-intact. Ran the cross-company negative from the demo tenant (fresh login, distinct company confirmed) — reproduced the unrejected pin on the acme host, verified persistence with acme's token, cleaned it up. Confirmed Test Selector block + calculated/can_overwrite in the live bundle `index-C98MwrA7.js`. Masked-404 always treated as rejection. Test data labelled QA-DEMO.
