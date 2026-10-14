# ZP-4562: Hide "Fill from Photos" on work orders (task #46)

**Verdict: PASS** on QA. Build `index-D1IWZ6SI.js` (19:15 IST); this ticket's code is the same as in `index-BdXGlAq5.js`.
Source check of the live build (source map): `SessionDetail.jsx` has `const FILL_FROM_PHOTOS_ENABLED = false` and `canFillForms = FILL_FROM_PHOTOS_ENABLED && canManageWorkOrders`. That constant gates both entry points: the work-order Actions menu (line ~6513) and the Forms tab action (`onFillFromPhotos`, line ~6705).
It is a code constant, not a LaunchDarkly flag, so there is no ON/OFF state to test. `FillFormsFromPhotosDialog` is still mounted with `open=false` and makes no request while closed (its effects return early when `open` is false).

Test work order (has 7 EG form instances): **QA-DEMO ZP-4614 link adds no services (delete me)**
https://acme.qa.egalvanic.ai/sessions/3f682025-6e77-4176-8461-68d8c27d36fc, signed in as Super Admin (has `workorders.manage`, so it would have seen the item before).

## Checked

| Check | How | Result |
|---|---|---|
| Work-order menu | Assets tab › Actions: Upload IR Photos, Add New Asset, Add Existing by Service, New Quick Count, Copy Quick Count, **EG FORMS: Manage Forms, Export to Excel, Import from Excel**, Add Issues, Generate Report | PASS: no "Fill from Photos" |
| Forms tab action | Forms tab › Actions: Manage Forms, Add Assets, Export to Excel, Import from Excel | PASS: no "Fill from Photos" |
| Rest of the EG Forms menu | Items above are still present, and the Forms tab lists the 7 forms by type | PASS |
| Console | No error from the removed path. Only the existing Beamer font CSP warnings and one 429 from a third-party resource | PASS |
| Other work-order actions | Close Work Order, Bulk Ops, tabs, menus unchanged | PASS |
| Backend left in place (by design) | `GET /api/form-fill/jobs/active?session_id=…` → 200 `{job:null}` | as described |
| Roles | The item was already gated on `workorders.manage`, so roles without it never saw it. Hiding it for the highest seat proves it is hidden for all | PASS |
| Flags | None: the gate is a code constant | n/a |
| Performance | Work-order page makes no form-fill request on load | PASS |

## Bugs found
None.

## Enhancement suggestions
1. Turn off job creation on the server too. Today `POST /api/form-fill/jobs` is still live, so any API client (or an old open tab) can still start paid AI jobs for a feature that has been withdrawn. A company flag or a kill switch on the route would stop that.
2. Work orders that already have a finished but unapplied fill job have no way to review or apply it now. Either apply or discard those jobs, or show a read-only note on the Forms tab.

## Mobile part for Nency
None for this ticket. The change is web-only (SessionDetail.jsx). If the iOS/Android apps expose a "Fill from Photos" entry, confirm whether it should also be hidden, for parity.

## Not covered
- Positive control of the old state (the item visible): the constant cannot be flipped without a rebuild. The previous web release had the item; the code shows the single gate.

## Proposed Jira comment
```
h3. QA result: PASS (QA, build index-COeCBn0z.js, 13 Oct; first passed on index-D1IWZ6SI.js, 12 Oct)

# Open https://acme.qa.egalvanic.ai/sessions/3f682025-6e77-4176-8461-68d8c27d36fc (a work order with 7 EG forms), signed in as a Project Manager (has workorders.manage, so the item would have shown before).
# Assets tab > Actions: the EG FORMS group shows Manage Forms, Export to Excel, Import from Excel. *No Fill from Photos.*
# Forms tab > Actions: Manage Forms, Add Assets, Export to Excel, Import from Excel. *No Fill from Photos.*
# The page makes no form-fill request on load. The other actions still work and the console shows no new errors.

!QA-13Oct-ZP-4562-1-wo-actions-menu-no-fill-from-photos-PASS.png|width=720!
!QA-13Oct-ZP-4562-2-forms-tab-actions-no-fill-from-photos-PASS.png|width=720!

Note (suggestion, not a blocker): POST /api/form-fill/jobs is still live, so jobs can still be started by API. Consider a server-side switch so the withdrawn feature cannot be used through the API.
```

## Recommended status
**READY TO RELEASE**

## Re-check 13 Oct on index-COeCBn0z.js
**Result: still PASS. Nothing changed.** Checked at 13:37 IST, signed in through the real login form as the **Project Manager** seat (`is_eg_admin:false`, has `workorders.manage`). This is a customer seat, unlike the Super Admin used on 12 Oct.

| Check | Result |
|---|---|
| Code | The `SessionDetail` chunk of the new build (`SessionDetail-CS7xxypd.js`) is **identical** to 12 Oct's `SessionDetail-5xDhhgTN.js` once chunk hashes are ignored, so the constant gate is unchanged |
| Assets tab › Actions | Upload IR Photos, Add New Asset, Add Existing by Service, New Quick Count, Copy Quick Count, Manage Forms, Export to Excel, Import from Excel, Add Issues, Generate Report. **No Fill from Photos** |
| Forms tab › Actions | Manage Forms, Add Assets, Export to Excel, Import from Excel. **No Fill from Photos** |
| Whole page text | No "Fill from Photos" anywhere |
| Network | No `form-fill` request while the work order loads. `GET /api/form-fill/jobs/active?session_id=…` → 200 `{job:null}` (backend left in place, as described) |
| Console | Only the known Beamer font CSP warnings, the DevRev plug integrity warning and one third-party 429 |

New screenshots: `QA-13Oct-ZP-4562-1-wo-actions-menu-no-fill-from-photos-PASS.png`, `QA-13Oct-ZP-4562-2-forms-tab-actions-no-fill-from-photos-PASS.png`.
Recommended status unchanged: **READY TO RELEASE**.
