# EG Forms: preload previous values as blue ghosts (neta-2) — QA verdict

**Tested:** 2026-08-20 · **Build:** QA V1.36 · **Tenant:** `acme.qa.egalvanic.ai` (2nd tenant: `demo`)
**PRs:** eg-pz-backend **#999** (harden + wire the previous-submission lookup) · eg-pz-frontend **#1180** (blue ghosts)

---

## Verdict — both halves are live on QA (contra the "dev only" note) and the core works, including the security fix.

The lookup that was previously **unscoped** (any authenticated user could read another company's submission) is now tenant-gated, and the frontend renders the prior submission as blue ghosts.

## ✅ Backend #999 — verified on QA (the security fix)

| # | Check | Result |
|---|---|---|
| tenancy | acme token → `GET /eg-form-instance/previous/{DEMO form}/{DEMO node}` → must NOT return demo's submission | ✅ **masked-404, no `form_submission` returned** |
| tenancy (reverse) | demo token → previous/{ACME form}/{ACME node} | ✅ masked-404 |
| positive | acme → previous/{own form}/{own node} | ✅ returns the **slim payload** with `form_submission` (not full `to_dict`) |
| `?exclude=` | malformed exclude | ✅ **HTTP 400 "exclude must be a UUID"** |

*(Note: the tenancy reject is the platform's 200+SPA masked-404, not a clean JSON 404 — the security intent is met, no foreign data leaks; the status-code hygiene is the usual masked-404 pattern.)*

## ✅ Frontend #1180 — blue ghosts verified on QA

Opened a **new draft** instance on an asset (Switch 7) that already has a **submitted** instance of the same form.

![Real UI on QA — blue ghosts from the prior submission: "As Left" table cell = 88 (blue), Test Equipment "qwerty (897)" (blue select), Overall Result "Pass w/ Conditions" (blue, painted not solid-selected), Comments "Did" (blue). Save button reads "Save 1 as Draft".](../bug-evidence/eg-forms-ghosts/ghost-blue-value.jpg)

| Check | Result |
|---|---|
| Frontend calls the wired-up lookup | ✅ `GET …/previous/66d01aa2/{node}?exclude={thisInstance}` — with `?exclude=self` so it doesn't ghost itself |
| Previous values render as blue placeholders | ✅ blue ghosts across field types (screenshot) |
| data_table cell ghosts | ✅ "As Left" = **88** in blue |
| select/dropdown ghost | ✅ Test Equipment "qwerty (897)" in blue |
| choice paints but is not solid-selected | ✅ "Pass w/ Conditions" shown blue/outlined (painted), not a filled selection |
| text field ghost | ✅ Comments "Did" in blue |
| no prior → no ghost | ✅ (implied — the ghosts only appeared once a prior submitted instance existed; the top fields with no prior value showed plain grey placeholders) |

## ⚠️ Not exercised (honest — these are deep interactive/behavioural flows)
- Type into a ghost → replaced outright; **Submit untouched → ghost materialized** into the real record; **Save Draft → ghosts NOT baked in** (re-derived on reopen).
- Previous **0 / false** ghosts rather than treated as blank (couldn't isolate a 0/unchecked prior value).
- **Required field showing only a ghost must not block Submit.**
- **Calculated fields compute on a no-keystroke Submit** (the `applyCalculatedFields` change).
- **Signature / photo never ghost** (couldn't confirm the prior submission had both to test the exclusion).
- **Pinned participants each ghost from their OWN history** (needs the pin UI, which isn't surfaced on QA per the pinning ticket).
- `?exclude={newest}` → returns next-newest (needs a node with ≥2 submissions).

## Method
Backend: `previous/{form}/{node}` across acme+demo (tenancy both directions), positive own-tenant, malformed `?exclude=`. Frontend: created a new draft instance on Switch 7 (which has a submitted prior), opened it with a `fetch` recorder (confirmed the `previous?exclude=` call), measured blue-italic ghosts via computed style, and screenshotted the Verdict/data_table section. Test draft deleted after.
