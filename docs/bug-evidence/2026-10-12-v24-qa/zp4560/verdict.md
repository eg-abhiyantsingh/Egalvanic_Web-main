# ZP-4560: Lock EG Form record-sink keys + blueprint-owned FSA/torque eligibility (task #67)

**Verdict:** PARTIAL: every web-visible row passes (no bug found). Two rows are not web-testable (AI-agent draft, typed-record projection), and one is changed by design (class torque toggles are retired).
Build `index-C14cwR0U.js` (QA, checked 17:33 and 19:12 IST, 13 Oct 2026). Frontend PR #1605 is in the bundle (`EGFormBuilderV2-kgrwB9vv.js`: `record_sink_keys_locked`, lock note text).

Test data (mine): EG form "QA-DEMO ZP-4560 sink lock (delete me)" `61cc16bb-e648-4615-bb2a-67c9e78163bf` (Builder › Forms).
- Created with *Create Form*.
- It holds the canonical Torque Log block from `GET /torque-records/form-block`.
- Its "Sign-off" sink block was later deleted as one of the tests.
No company form, class or subtype was changed.

## Checked
| Check | How | Result |
|---|---|---|
| Block dialog lock | Builder › Block View › Data Table (inside Torque Log) › edit | Note "Part of a Fluid Sample / Torque Log block — saved keys are locked because they map onto the asset's records. Labels stay editable." Block key `connections` and every column key are disabled. Type (Column/Row/Hybrid/Key-Value) is greyed out (frozen). Labels editable. PASS (shots -1, -2) |
| Label-only edit | API save with a new column label | 200; key still `connection`; label shows in the builder. PASS (shot -3) |
| Rename a key in Form Code | Form Code › change `"key": "connection"` → `connection_renamed_by_qa` › back to Form › Save | Refused in the browser with "Fluid Sample and Torque Log blocks keep their keys … (Torque Log block "connection_torque_log": key "…[col:connection]" was renamed or removed)". **No PUT was sent**; the server copy is unchanged. PASS (shots -4, -5) |
| Server lock (API, same form) | PUT with: column key renamed / block key renamed / `record_sink` tag removed / sink kind torque→fsa / a column removed | All 400 `record_sink_keys_locked` with a precise detail each. PASS |
| Allowed changes (API) | Add a new heading `qa_new_col`; delete the whole "Sign-off" sink block | 200 and 200. PASS |
| Edge: remove a heading right after saving it | Delete `qa_new_col` after it was saved | 400, because once saved it is locked. Consistent with the rule. See suggestion |
| Edge: option keys inside a sink column | Change Y option key `yes`→`si` on `torqued_to_check` | 200 (allowed; then restored). Option keys are not in the ticket's lock list. See suggestion |
| Migration `tqlock_a1` | `GET /lookup/node-classes` | Cable, Fuse, DC Cable, DC Fuse and their subtypes have `torque_eligible=false`. Their properties no longer include "Torque values" (Circuit Breaker and Panelboard still do). PASS |
| Override class/subtype toggles | Settings › Classes; bundle | The Fluid Samples / Torque Records toggles no longer exist in this build: the class flags are retired, and eligibility now comes from `GET /node/<id>/record-sections` (e.g. a Panelboard with `torque_eligible=true` returns `torque:false` when no service has a Torque Log). The 403 on flipping a global/override flag was not probed: that would write to shared Acme classes |
| Roles | PM, CP, FM, EE open `/eg-forms` | "Access Denied" for all four (no `forms.manage`). PASS |
| Flag `feature-eg-forms` | Live evalx | ON for Acme. The ticket adds no flag; nothing to flip |

## Bugs found
None in scope.

## Enhancement suggestions
- Let users remove a heading they added by mistake while no records use it yet. Today, once saved, the only way out is deleting the whole Torque Log block.
- Consider locking option keys (`yes`/`no`/`na`) of sink columns too, or confirm that the projection reads labels, not keys. Renaming one is accepted today.
- In the block dialog, show a small lock icon on each locked key field (today they are only greyed out).

## Mobile part for Nency
The iOS/Android form renderer is not touched. Nency can skip this one, unless the app also saves form definitions (it does not, as far as we know).

## Not covered / NOT WEB-TESTABLE (dev to confirm)
- **AI agent draft that breaks sink keys disables Accept:** needs a paid AI build request crafted to rename a key. Not run.
- **"The typed record projection still fills afterward":** needs a work order with a Torque Verification service. QA has no Torque Verification seed (see torque memory), so no torque_records row can be produced from a form on QA.
- **Staff/MCP form save and company service-version apply paths:** server-side only. Dev to confirm that both return `record_sink_keys_locked` (the PUT path is proven above).
- **403 when flipping `fsa_eligible`/`torque_eligible` on a global/override class or subtype:** not probed, because it writes to shared classes. Dev to confirm, or give QA a throwaway company.

## Proposed Jira comment
h3. QA: ZP-4560 web part passes, 4 rows need data or a dev check (QA build index-C14cwR0U.js, 13 Oct)

Tested on my own form "QA-DEMO ZP-4560 sink lock (delete me)" with the standard Torque Log block:
# The Torque Log table dialog shows the lock note; the block key and every column key are greyed out, and the table type is frozen. Labels can still be changed.
# A label-only change saves (200) and the key stays the same.
# Renaming a key in *Form Code* and pressing *Save* shows the lock message, and nothing is sent to the server.
# The server also refuses: renamed key, renamed block, removed sink tag, changed sink kind, removed column (all 400 {{record_sink_keys_locked}}). Adding a heading and deleting the whole sink block are allowed (200).
# Cable and Fuse (and their DC versions and subtypes) are no longer torque-eligible, and the "Torque values" property is gone from them.

!QA-13Oct-ZP-4560-1-torque-log-block-dialog-keys-locked-note-PASS.png|width=720!
!QA-13Oct-ZP-4560-5-save-refused-with-lock-message-no-request-PASS.png|width=720!

Not testable from the web (please confirm in code): AI-agent draft disables Accept; staff/MCP save and service-version apply return the same 400; the typed torque records still fill (QA has no Torque Verification service). The class Fluid/Torque toggles are gone in this build (flags retired), so the "403 on override" row no longer applies.

To close it, QA needs either a Torque Verification service on QA (to prove the torque records still fill after a label edit) or a dev confirmation of the four rows above.

*Status:* stays In QA

## Recommended status
Stay In QA. Every web-visible row passes and no bug was found. But four QA Review rows can't be shown from the web:
- typed records still fill (needs a Torque Verification service on QA);
- AI-agent Accept disabled;
- staff/MCP and service-version apply;
- the 403 on override flags.

Move to READY TO RELEASE once the dev confirms these, or once QA gets the seed data.
