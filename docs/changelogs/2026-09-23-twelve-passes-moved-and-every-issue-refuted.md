# Twelve passes moved to READY TO RELEASE; every published issue adversarially re-checked (2026-09-23, evening)

**Prompt:** "ticket that are passed move them to ready to release status" … "and make sure our artificat is correct
all issue are real" (linking the Deep Pass board).

Boards republished at the same URLs: Readiness <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26> (v9) ·
Deep Pass <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu> (v6).
Evidence: `docs/bug-evidence/2026-09-23-rfq-passes-on-6NT43E1C/` — 18 captures + NOTES.md.

## QA rebuilt a third time today

`index-CPjC9Hwo.js` (morning) → `index-CH4p1H5S.js` (~14:00) → **`index-6NT43E1C.js`** (evening). Nothing moved on
yesterday's evidence: all ten "Pass · held" tickets were re-walked on 6NT43E1C first.

## Moved to READY TO RELEASE — 12, each with steps, what was seen, and an explicit "Not exercised" line

| Ticket | Re-verified today | Comment |
|---|---|---|
| ZP-4039 | Condition Assessment tabs 36 / 366; filter row + Bulk Ops; Findings pencils | 44432 |
| ZP-4041 | "8 assets had their condition revised automatically in the last 30 days" | 44433 |
| ZP-4043 | Equipment Health carries the "Assessment expired" band | 44434 |
| ZP-4068 | 3 JOURNAL chips; New Site Walk Count vs Journal; Journal Review + live interpretation | 44435 |
| ZP-4109 | *Infrared Thermography* → All methods = flat grid, **30 methods**, checkbox per row, filter, Edit selected | 44436 |
| ZP-4110 | Devices tray Manufacturer/Asset class/Service/Frame/Rules; Add device Service→Class→Device cascade | 44437 |
| ZP-4112 | Compliance **Visualizer** with Group by / Filters / Export / Full screen | 44438 |
| ZP-4149 | IR work order → config list resolves **"Infrared Thermography Report"** | 44439 |
| ZP-4167 | Junction Box hides Mains Type / Panel Type / Phase Config (and the Schedule + OCP tabs); Panelboard control keeps all | 44440 |
| ZP-4171 | Class editor → Core Attributes → Add → **Subtypes (empty = all)** | 44441 |
| ZP-4326 | Both entries read the same `maintenance-portal` flag — the ticket's whole ask | 44442 |
| ZP-4344 | robots.txt live, 12 vendor tokens, each sourced and dated; no tokenised paths listed | 44443 |

`corrective IR`, the service used as the ZP-4109 example on 21–22 Sep, no longer exists on acme; today's proof
uses *Infrared Thermography*, which is stronger — 30 methods, so the multi-select the ticket is about is genuinely
exercisable.

## "Make sure all issues are real" — a 13-agent adversarial pass killed or reshaped six of my own claims

Each published defect was given to an agent told to **refute** it against Jira, the evidence files and the current
bundle; then I re-walked in the browser what they could not.

| Claim | Verdict | What changed |
|---|---|---|
| **ZP-4272** | **my claim was wrong** | Retention *works* on `/assets`: hiding a column writes `gridLayout_v1_assets` and survives navigation. It fails only on the **work-order Assets grid**, a different component (writes nothing, column returns). Now "partial fix — shipped on the shared grid, not the WO grid". |
| **ZP-4151** | **✘ was invalid** | The secondary account's "Assets 0" is the **licence-entitlement meter**, not an asset roll-up. Acceptance line 2 is *unverified*, not failing. |
| **ZP-4322** | not reproducible | The tooltip stacking fix (PR #1508) is in the build, and on this viewport the card renders as a centre total + legend with **no donut arcs to hover**. |
| **ZP-4189** | not re-measured | PR #1506 landed after the 22 Sep 7,176 ms reading. Could not re-measure — the automated tab runs backgrounded, so Chrome records no paint timings. Labelled a 22 Sep figure. |
| **ZP-4218** | downgraded to *not verified* | A change shipped that surfaces the RFQ flow on already-subcontracted rows; the Convert path was never re-walked after it. |
| **ZP-4042** | softened | Real, and probed in the shape the client sends (with `sld_id`) from three seats on 14 Sep — but the ticket is **Medium**, so "cannot be signed off", not a formal blocker. |

Restated rather than dropped: **ZP-4138** (the nav is not gated on Portal Sales at all — it is hidden only when the
seat holds *none* of five staff roles), **ZP-4181** (an iOS Sentry ticket whose web mitigation is dead code),
**ZP-3919** (one defect — the email is not carried; Google/passkey are tenant-config absence, backup codes an
unbuilt acceptance item), **ZP-4030** (the message half is the defect; the AASA half rests on a developer comment,
not an acceptance line).

## A finding of mine is now FIXED — retracted

The "**all three Admin class grids open at 0–0 of 0 until you press refresh**" regression (BOaMecwk, C9NJAR1x,
CPjC9Hwo) **no longer reproduces on 6NT43E1C**: cold navigation and hard reload both render populated —
`/asset-classes` 1–25 of 49, `/connection-classes` 1–3 of 3, `/issue-classes` 1–8 of 8. Dropped from the
"needs a ticket" list.

## The set moved under us — 6 tickets arrived in Ready for QA

While the sweep ran: **ZP-3920, ZP-4291, ZP-4292, ZP-4309, ZP-4346, ZP-4347**. ZP-3920 is the backend half of
passwordless sign-in (no web surface); the other five need a walk and are marked **"not yet tested"** on both
boards rather than being counted as covered. ZP-4292 and ZP-4309 are ones I previously reproduced, so they are
re-test candidates rather than fresh ground.

Ready for QA is now **35** (was 41): 8 partial · 6 open defects · 7 cannot be exercised · 1 not located ·
8 no web surface · 5 not yet tested.

## Not done

No hold comments were written on the tickets that did not pass, and no new bugs were filed — both still need the
owner's go-ahead.
