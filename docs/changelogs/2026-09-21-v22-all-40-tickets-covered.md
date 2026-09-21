# Web v2.2 — all 40 tickets now carry a verdict (2026-09-21, round 3)

**Prompt:** "updated artifact of v2.2 web? for all ticket" — cover every ticket, not just the 12.

Board: <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26> (v3)
All work done on the shipping QA build `index-BV-phiFE.js`.

## Where the release stands

| | Count |
|---|---|
| Verified, no defect found | 22 |
| Open defects | 8 |
| Cannot be settled on QA (not deployed / no data / other surface) | 10 |
| **Total** | **40** |

## Method

Three oracles, cheapest first:
1. **Bundle markers** — the shipped QA bundle was pulled and grepped for each ticket's own named
   symbols (`work_type_service_ids`, `subtype_keys`, `condition_required`, `proposal_photos`,
   `EG-Utils.msi`, `methods/bulk-edit`, …). Tells you the frontend half is here; nothing more.
2. **Live API** — the endpoints those markers call, probed with a real admin session.
3. **Browser** — the surfaces a user actually touches.

Most tickets say "dev only, not yet in QA" in Jira. Nearly all of them **are** on QA. The env field
stays unreliable; test on QA regardless (standing rule).

## The one new defect: ZP-4292 reproduces

Created an asset in WO `/sessions/1a9c5d13-…` with **all 17 services unticked**. The saved asset came back
with `forms_status` and `svc_mask_arcFlash` populated — the same service columns as assets that *do* carry
services — while ten sibling assets in the same work order show all three columns empty.
Node `1497796d-fec3-4c12-ba35-cd3bef6bb9b5`, labelled "QA-DEMO ZP-4292 delete me".

**Extra finding not in the ticket:** the *edit* Asset drawer has **no Services section at all** (create does).
So a user cannot see what was registered, let alone remove it.

## Clean passes worth naming

* **ZP-4167 / ZP-4174** — bus hide flags exactly as specified, *with a negative control*: Junction Box
  `true/true`, Disconnect Switch `true/false`, Busduct `true/true`, and Panelboard / Switchboard both
  `false/false`. The scoping is right, not just the flag.
* **ZP-4170** — 6 of 6 seeded transformer makers present; CEB in the bus/panel picker with exactly 1
  designation, as the ticket promised.
* **ZP-4131** — SKM menu item present *after* Export; dialog explains the three audit steps and the
  Windows/PTW32 requirement *before* offering the download. Download not clicked.
* **ZP-4113** — Issue Report card on Maintenance → Reports; both export buttons on Condition Assessment.
* **ZP-4112** — Compliance → Visualizer tab live; `/program-compliance/{sld}/expected` → 200.

## Not deployed / not settleable

`ZP-4111` (quick-search `sources` param absent — skm / bogus / omitted all return an identical 40 rows,
where a bogus source must 400) · `ZP-4066` (SLD OTA viewer bundle) · `ZP-4062` (needs a bulk gesture) ·
`ZP-4040` (needs a NULL-wo_view service; demo tenant) · `ZP-4082` (prod-only hotfix) ·
`ZP-4067`, `ZP-4128` (pipeline / asset-agent, no web surface).

## Over-reports avoided

* **ZP-4170** nearly filed as "seeded makers missing" — the payload uses `mfr_name`, not `name`. Dumping
  the raw row first showed all six present. *Read the shape before calling a miss.*
* **ZP-4086 / ZP-4171** — zero class props carry the new flags, but both default false/empty and the
  frontend ships the controls. Absence of data ≠ absence of feature. Logged as present-unexercised.
