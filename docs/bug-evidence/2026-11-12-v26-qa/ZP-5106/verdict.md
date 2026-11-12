# ZP-5106 — Task #218: Forking a global service repoints its open work orders and matches Set Up Forms

**Verdict:** PARTLY on QA, build `index-C1ZRIunF.js` (checked 16:02 and 17:24 IST, 12 Nov 2026). Super Admin seat.
Set Up Forms matching and the Build Quote warning pass. The ticket's headline row (fork a global service → its open work orders move
onto the fork) was **not run**: it is one-way and acts on shared QA data.

**Recommended status:** stay **In QA**. Needs one of these:
(a) the owner's go-ahead to fork a real global on QA (e.g. UPS Maintenance, about 26 open EG-ACME work orders on 2 Nov; all of them
would move for good);
(b) a QA-only global service with 1–2 QA-DEMO open work orders (a developer can seed it); or
(c) a developer run on dev.

Test data (created today, QA-DEMO): work order "QA-DEMO ZP-5108 12 Nov AF WO"
(https://acme.qa.egalvanic.ai/sessions/c3791ea4-f421-465e-9c16-61b37eecdd6d). I registered the **global** Infrared Thermography
(3b732d14…) on asset PNL-1 with `POST /ir_session/<id>/add-assets`, so it looks like a work order made before EG-ACME forked IR. Also
quote "QA-DEMO ZP-5106 12 Nov build quote" (https://acme.qa.egalvanic.ai/plans/5fbf0dfb-7a2f-4fd8-8d6c-609725a6f9fb). Both are on site
"QA-DEMO ZP-5108 ZP-5109 12 Nov".

## Checked
| Check | How | Result |
|---|---|---|
| Fork a global service with open WOs → open WOs move onto the fork | — | **NOT RUN** (one-way, shared data; see status). Background: EG-ACME's existing forks (Infrared Thermography 4d5ea9cb ← global 3b732d14, NETA Testing 133be545 ← 0d914f81, DGA) were made before this fix, so their old WOs still point at the globals. Expected, and not a re-test of the fix |
| Set Up Forms matches a forked service to its global | Same WO › Assets › Actions › Add New Asset › Panelboard "QA-DEMO ZP-5106 PNL-3 12 Nov" → Set Up Forms opens | PASS: it lists the WO's (global) "Infrared Thermography" as applying ("On 1 of 8 assets already carrying services", pre-ticked, "Create Forms (1)"). `available-for-node` returns only the fork (4d5ea9cb, `forked_from_id` 3b732d14), so the dialog matched global → fork. Without the match it would read "This service's rules don't match this asset" (shot 1). Closed with "Not Now", no forms created |
| Build Quote: no false "no implementation" warning on forked services | Quote › Build Quote › Service = Infrared Thermography (fork) and NETA Testing (fork), All assets (~9) | PASS: 0 warnings (shot 2) |
| Positive control for that warning | Same dialog, add UPS Maintenance (global, no implementation for these classes) | Warning shown: "⚠ 9 matched assets have no UPS Maintenance implementation (Circuit Breaker, Panelboard, Switchboard) — they would add zero work to the quote." Also Panel Schedule Updates → "5 matched assets … (Circuit Breaker)" (shot 3). Dialog cancelled, nothing generated |
| Negative: a fork with nothing to repoint errors nowhere | Can only be run by forking a global | NOT RUN. Related negative: `POST /procedures-v2/services/<own QA-DEMO service>/fork` → 400 "only global services can be forked" (nothing written) |
| Already-forked global → 409 with `existing_service_id` (bundle contract) | Not probed: a re-fork of IR/NETA might touch 76/30 open work orders if the server repoints on that path | Not run, on purpose |
| Roles | — | Not walked (Set Up Forms / Build Quote need WO/quote edit rights; PM/EE have `procedures.manage`, CP does not) |
| Flags | No ticket flag | n/a |

## Bugs found
None.

## Enhancement suggestions
- The fork confirmation should say how many open work orders will move ("Forking moves 26 open work orders onto your copy — this
  can't be undone"). Today nothing in the UI warns about this one-way side effect.
- Set Up Forms: show which service the item actually resolves to ("Infrared Thermography (your copy)") when a global was matched to a
  fork, so a planner understands why it applies.

## Mobile impact
iOS/Android: work orders whose service is moved onto a fork should keep their forms and check-offs after sync. Check this once the fork
test has run.

## Not covered
The fork → open-work-order repoint itself, its negative (fork with no open WOs) and closed WOs staying put. See status for what is
needed.

## Evidence
- `QA-12Nov-ZP-5106-video-steps.mp4` (74 s): setup card (live registry) → Add New Asset → Set Up Forms → Build Quote with forks →
  control warning → card listing what was and was not run (contact sheet checked).
- `QA-12Nov-ZP-5106-1-set-up-forms-matches-fork-to-global.png` (PASS), `-2-build-quote-forked-services-no-warning.png` (PASS),
  `-3-build-quote-control-real-warning.png` (PASS, control).
- Raw: `.playwright-mcp/v26/eric7/r16-out.json`, `r17-out.json`, `v08-out.json`.
