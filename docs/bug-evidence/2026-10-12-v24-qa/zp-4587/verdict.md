# ZP-4587 — Stop ~1 GB EG-form load on every site sync (SLD payload); account-route tenant gates

**Verdict:** PASS (observable halves) / NOT WEB-TESTABLE (dev 502/OOM/memory) — build `index-D1IWZ6SI.js`. Ticket env is "dev only (not yet in cicd/qa)", but the `/sld/v2`, `/sld/v3` and the hardened account routes are all live and exercisable on QA.

## Checked
| Check | How | Result |
|---|---|---|
| `/sld/v2` + `/sld/v3` return + time, largest QA site | staff, site `21bd10d4…` = 2,274 nodes / 1,983 assets (largest on QA) | **200, no 502**; ~7.5–13.7 s; 30.9 MB JSON / **1.58 MB br wire**. `eg_forms` block carries **node_class_names / node_subtype_names only** (each form ~438 bytes = form_template id, no full form/node-class definition JSON). Matches the names-only fix. PASS |
| Tiny site control | `bb593be6…` = 5 nodes | 200, 7.06 MB JSON / **189 KB br wire**, ~1.2–2.8 s. PASS |
| v2 vs v3 parity | both versions, 2 runs each, 4 sites | identical node_class_names/node_subtype_names counts across runs; no error. PASS |
| Form with deleted class/subtype | forms on QA sites show empty `node_class_names:[]` where the mapping is gone | returns remaining names without error (outer-join behaviour). PASS |
| Account access-list tenant gate | PM/AM/EE (is_eg_admin:false): own vs another-company account | own → 200 JSON (users list); another company's account id (`727adcff…` company 0a61e613, `bd474012…` company d2e4b67b) → masked-404 (refused). Even the staff seat is refused on the foreign account. PASS (shot -1) |
| contacts-map tenant gate | PM/AM/EE/staff: own vs foreign | own → 200 JSON (contacts); foreign → masked-404. PASS |
| primary-contact PUT gate | AM/PM: foreign account (random contact, no-op), own | foreign/random → masked-404; own → 400 "Pick one of this customer's contacts" (reached handler, no write). PASS |
| DELETE internal-account-assignments gate | AM/PM: own user id + foreign account | foreign/random account → masked-404 (the route that previously had NO company check). PASS |
| Staff cross-tenant still works | staff seat on own-company accounts | 200 (staff is exempt by design). PASS |
| Performance | /sld/v2 on big site | ~1.58 MB over the wire (br), but the `eg_forms` block alone is ~3.5 MB uncompressed (648 global forms) on EVERY site sync regardless of node count — residual inefficiency (enhancement E1). |

## Bugs found
None in scope. The 1 GB / 9 s / OOM regression is the dev-only symptom; what's observable on QA (payload shape, names-only, no 502, few-second response, account-route gates) all passes.

## Enhancement suggestions
**E1 — `/sld/v2` still ships all 648 global EG-forms (~3.5 MB uncompressed) on EVERY site sync, independent of the site's node count.** The names-only fix removed the 1 GB definition load, but the form list itself is the dominant payload term even for a 5-node site. Consider filtering `eg_forms` to the classes/subtypes actually present on the site, or paginating/omitting it from the sync and fetching on demand. Assignee: Avani (backend / optimisation).

## Mobile part for Nency
This fix exists *because* of iOS 502s on site switch. Nency must confirm on iOS/Android: a **site switch / site sync for a large tenant completes without a 502** and the app doesn't hang, and that node-class/subtype names still render on asset cards. This is the primary consumer of the fix.

## Not covered (why)
- Dev 502 / gunicorn OOM-kill / 1 GB→365 MB process-memory drop — **NOT WEB-TESTABLE**; needs a dev memory metric / log from Avani (the ticket cites "demo, egal, EG-ACME on dev" with timings 9.7 s→0.24 s). QA can only show the payload is now names-only and returns without a 502.
- Byte-for-byte identical node_class_names vs the pre-fix build — can't diff against the old backend from the web; trusted on the dev's verification.

## Proposed Jira comment
h3. QA — ZP-4587 PASS on the web-observable parts (QA build index-D1IWZ6SI.js)

* {{/sld/v2}} + {{/sld/v3}} on the largest QA site (2,274 nodes / 1,983 assets) → *200, no 502*, ~7.5–13.7 s, 1.58 MB over the wire; the {{eg_forms}} block carries node_class_names / node_subtype_names *only* (no full form/node-class definition JSON) — the names-only fix. A tiny 5-node site returns in ~1–3 s. A form whose mapped class/subtype was deleted still returns its remaining names.
* Account-route hardening verified from a customer seat (is_eg_admin:false): {{access-list}}, {{contacts-map}}, {{primary-contact}} PUT and {{DELETE /users/\{id\}/internal-account-assignments/\{account\}}} all refuse another company's account id (own id = JSON; even the staff seat is refused on the foreign account). Staff keeps cross-tenant by design.

!QA-12Oct-ZP-4587-1-account-route-tenant-gate-proof.png|width=720!
!QA-12Oct-ZP-4587-2-sld-v2-payload-largest-site-proof.png|width=720!

The dev-side 502/OOM/1 GB→365 MB memory drop is not web-observable — please confirm from the dev metrics. Enhancement: the 648-form {{eg_forms}} block (~3.5 MB) still ships on every sync regardless of site size.

## Recommended status
**READY TO RELEASE** for the web-observable scope (payload shape + account-route gates), with a note that the dev memory/502 numbers are the dev's to confirm, and E1 filed as an optimisation follow-up.
