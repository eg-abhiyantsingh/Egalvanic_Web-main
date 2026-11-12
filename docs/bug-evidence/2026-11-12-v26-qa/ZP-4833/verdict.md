# ZP-4833 · Planned work: linked sub's Team on a schedule; "Schedule locked for confirmation" copy (QA, 12 Nov 2026)

**Verdict: PARTLY** on QA, Web v2.6 build `index-C1ZRIunF.js` (checked 16:01 and 16:55 IST).
- The copy fix passes. This is the half that was missing on 5 Nov, and it is now on QA.
- Scheduling a **linked subcontractor's Team** is **BLOCKED**. QA has no accepted company link, and on today's build a link cannot be accepted in the UI because of the ZP-4830 bug.

Seats used:
- Super Admin (staff).
- Project Manager (customer, is_eg_admin false) for the API scope check.

## Checked
| Check | How | Result |
|---|---|---|
| Notifications OFF: Activity copy | Operations › Planned Work › "QA-DEMO ZP-4583 any-crew demo 19 Oct" › Schedule. The planning data says: `notify_enabled` false, lock at 19 Oct, notices all "off", 0 revisions | **PASS**: Activity reads "Schedule locked for confirmation · abhiyant admin · Oct 19, 4:17 PM". On 5 Nov the same work order read "Schedule sent for confirmation" (screenshot 1) |
| Notifications ON, notice really sent (control) | "QA-DEMO ZP-4583 r8 CA holiday Jul 2027" (`notify_enabled` true, lead-0 notice sent) | PASS: still "Schedule sent for confirmation to N recipients" (screenshot 2) |
| Notifications ON, first notice not sent yet: still "Schedule sent for confirmation" | SIMULATED: GET `/api/ir_session/<WO>/planning` rewritten to `notify_enabled: true` for the same work order. QA has no real work order in this state, and locking one with notices ON would email customer contacts | PASS (SIMULATED): the same lock entry reads "Schedule sent for confirmation" (screenshot 3) |
| Schedule a linked sub's Team (no 404 "Crew not found") | Admin › Subcontractors; `GET /api/vendors` (0 of 12 subs have an active link); `GET /api/crews/assignable` 200 | **BLOCKED**: no linked Team exists to pick (screenshot 4) |
| Only in-house crew members land on the work blocks | — | BLOCKED (same reason) |
| API scope (PM, customer seat) | `GET /api/ir_session/{own QA-DEMO WO}/planning` vs another company's WO `aed4636d…` vs a random id | Own: 200 JSON. Foreign: 404 "Work order not found". Random: 404. The staff seat also gets 404 for that foreign id, so the foreign probe is **inconclusive** (no positive control); the random-id refusal holds |
| Flags | `feature-planned-work-v2` = true (live) | ON only. The copy change is not behind its own flag |
| Performance | Schedule dialog open | No call over 2 s seen in the run |

## Bugs found
None in the part that could be tested.

JQL run: `project = ZP AND (summary ~ "Collaborators" OR summary ~ "company link" OR summary ~ "Withdraw" OR …)`. See ZP-4830/verdict.md; no ticket covers the copy.

## Enhancement suggestions
When notifications are off, the Activity "Notice settings & recipients" link could add a short hint: "Scheduling emails are off — confirm by hand". This tells the planner that nothing went to the customer.

## Mobile impact
None. This is web Activity copy. Mobile schedules are unaffected.

## Not covered (and why)
- A linked subcontractor's Team on a schedule.
- In-house versus linked members on the work blocks.

Both need an accepted subcontractor link (ZP-4830). That needs:
- a second QA company with a non-staff admin login to accept;
- the Requests Accept button, which today never renders (ZP-4830 bug 1).

## Proposed Jira comment
See `ZP-4833.txt`.

## Recommended status
**Stay In QA.** The copy part passes. Retest the linked-Team schedule once ZP-4830 is fixed and a second QA company admin seat exists to accept the link.
