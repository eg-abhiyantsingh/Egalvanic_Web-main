# All 48 Ready-for-QA tickets walked in depth (2026-09-22)

**Prompt:** "you need to check all ticket ready for qa. in deepth"

Artifact: <https://claude.ai/artifact/1co6Lj66t8uzZbCNqAaEcb> — "Ready for QA Sweep"
Evidence: `docs/bug-evidence/2026-09-22-ready-for-qa-deep/` — 14 captures + `NOTES.md`
Acceptance criteria per ticket, extracted from the Jira text: `docs/test-runs/2026-09-22-ready-for-qa-acceptance-checklist.md`
Bundle: `index-C9NJAR1x.js`. Seats: `+admin@` (Super Admin) and `+fm@` (Facility Manager).

## Scope

`fixVersion = 14156 AND status = "Ready for QA"` returned **48** tickets (the five moved to READY TO
RELEASE earlier today are already out of this set). A subagent first turned all 48 Jira descriptions into
a per-ticket checklist of falsifiable checks so the walk tested what each ticket actually claims.

| | Count |
|---|---|
| Passing, exercised in the UI | 24 |
| Open defects | 4 (+1 unfiled) |
| Cannot settle on this tenant | 13 |
| No web surface | 7 |

An important change of conditions: this pass ran on **Android Site 2 (343 assets)**, a site with real
data, where the 21 Sep pass used a site with none. Several checks that read zero yesterday now read real
numbers — ZP-4041's auto-revision line, ZP-4084's condition distribution, ZP-4039's findings count.

## The headline: ZP-4218 is worse than filed, and the cause is on the wire

The ticket says the RFQ button does not generate when a labor line is marked subcontracted. In fact **a
labor line cannot be marked subcontracted at all on web**, so the RFQ flow it gates can never appear.

Clicking the `Sub` cell opens "Convert to subcontracted labor", whose own body states the RFQ flow lives
in the Rate column once converted. Pressing **Convert** fires `POST /api/plans/{plan_id}/generate`, which
returns **200** and reprices the plan — and the flag stays `No`, across a reload, three times.

The payload shows why:

```
"pricing": {
  "rate_overrides": {},
  "subcontracted": [],                                             <- sent empty
  "subcontracted_overrides": {"e08f8c64-…-b23946aab5ab": true}     <- the conversion
}
```

and the response carries `"subcontracted": false` for that line. The frontend records the conversion only
in `subcontracted_overrides`.

## Other open defects

* **Asset Classes opens empty** — third reproduction today, fourth across two bundles, still no ticket.
  Cold load `0–0 of 0`; the grid's own refresh icon calls `GET /api/node_classes/user/{userId}` → 200 with
  49 rows and fills it. Mounting the page fires no classes request at all.
* **ZP-4042** — report history 500 for the fourth day, trace `2de0ac923d4f48288f8c36026a9d5ec8`.
* **ZP-4272** — hide a column, leave the page, come back: the column is back. No grid state in storage, no
  preferences endpoint, `onColumnOrderChange` wired to nothing.
* **ZP-4138** — route gated correctly, nav tile still offered to both staff seats.

## Filed as broken, working here

* **ZP-3677** — the Add Assets Service field now holds multiple services as chips. Verified with two.
* **ZP-4266** — Edit with AI ran to completion on QA and reported exactly what it changed. *Precondition
  the ticket omits:* the button only exists on a config that has pages; an empty config has no button at
  all and its preview reads "Data preparation failed".
* **ZP-4186** — the extract path returns **200** with "No data was extracted", not a 500.
* **ZP-4066** — the SLD issues panel lists 19 real issues and none of them is the false
  "upstream can't deliver" phase-config type.

## Cannot be settled, each with the one thing missing

* **ZP-4061** (and the portal halves of ZP-4042 / ZP-4043) — every `/maintenance-portal/*` route answers
  Access Denied for both staff seats because the portal is gated on the **Portal Sales** role and no QA
  seat holds it. **One role on one seat unblocks three tickets.**
* **ZP-3675** — no panel on this tenant has circuits. The one panel with content exports its single
  off-panel breaker correctly. *Minor find:* the read-only View Schedule page for an empty panel renders a
  blank card with no empty state while still offering Export PDF.
* **ZP-4207** — the 502 does not reproduce; the only walk offering Interpret is empty and returns a clean
  400 with good copy. *Separate bug:* the two walks that hold entries are marked Read and offer **no
  Interpret control at all**, while still telling the user to "Run Interpret".
* **ZP-4150** — no add-from-issue control on any of four screens. Reported as not found, not as not built.
* **ZP-4261** — the ledger has no page. *Seen in passing:* Activity Logs shows "REQUESTS 520" in its tiles
  over a grid reading "No rows, 0–0 of 0"; and `/activity-logs` renders blank while `/admin/activity-logs`
  works.
* Also blocked on data: **ZP-4127** (needs a continuous SKM segment), **ZP-4153** (needs an interpreted
  journal unit), **ZP-4086** (needs a company-owned class with core attributes), **ZP-4185** (no auth
  refresh failure occurred across two seats and 90 minutes), **ZP-4062** (the confirm writes to a shared
  work order), **ZP-4040** (one of four checks done).

## No web surface

ZP-4067 · ZP-4082 (production only) · ZP-4128 · ZP-4176 · ZP-4183 (Android stack) · ZP-4190 · ZP-4216.

## A third instance of the AWS-account leak

`POST /api/reporting/configs/{id}/ai-edit` returns an `execution_arn` carrying account `165183897698` on
the **success** path, as does every `/api/form-fill/jobs/{id}/status` response. ZP-3834 covers the failed
photo-fill job only; the same identifier is exposed on two healthy endpoints.

## One thing to flag

The panel-schedule Export PDF writes a file. I intercepted `URL.createObjectURL` and the anchor click to
keep it in memory; the interception did not hold and `~/Downloads/11N-H1-1_Panel_Schedule.pdf` (60,867
bytes) was written. Its text was read to answer ZP-3675. No further exports were run. Delete it if you do
not want it.

## Writes made

Only on records created for QA and labelled as such: the AI edit on `QA-DEMO fork regression ZP-staff-write
(delete me)`, and three Convert attempts on `QA-DEMO ZP4220 v2 EMP priced (delete me)` which the server did
not persist. The Add Assets dialog was cancelled. The bulk Mark As confirm was deliberately not run.
