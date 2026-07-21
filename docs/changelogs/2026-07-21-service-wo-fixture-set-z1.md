# 2026-07-21 — Complete service-WO fixture set created on Z1 (all 6 type families)

**Prompt:** "continue creating" — continuation of the ZP-3000 service-work-order exploration:
create live work orders on QA covering every service *type family*, so per-type UI contracts can
be inspected and automated against real fixtures.

**Site:** Z1 (account "Z Account") — chosen because it holds the richest asset population
(switchboards Bus2/Bus5/MSB…, transformer T1, MCCs, panelboards, ~130 assets) and the classic
demo asset names used in the ZP-3000 videos.

## Fixture inventory (all created via the new Create-WO dialog, status Open)

| Type family | Work order | Session ID | Scope | Type-specific tabs seen |
|---|---|---|---|---|
| PM Forms | `Cleaning_WO_QA_2026-07-20` (due Jul 31) | `33c471bd-029a-447f-989a-85d525a41eb1` | all assets → 47 rows / **104 forms** | Forms(99+); form groups per class·subtype |
| AF | `AF_DataCollection_WO_QA_2026-07-21` (High, 8h, due Aug 21) | `9286797b-f332-478b-94d8-a09bdcb1b94c` | CB+Switchboard filter → 6 | **SLD**, **Equipment Designations**, per-asset Arc Flash column |
| IR | `IR_WO_QA_2026-07-21` (due Aug 21) | `2b217cd3-a05d-447f-979a-45f069331510` | all → 46 | **IR Photos** tab + column |
| COM | `COM_WO_QA_2026-07-21` (due Aug 21) | `7a12c5e2-c83d-4a47-86cc-c5d37a9b2439` | all → 46 | **Condition Assessment** dashboard tab; Tasks + C.O.M. columns |
| Checklist | `AFLabel_Checklist_WO_QA_2026-07-21` (due Aug 21) | `7b1f9d0b-b3fe-49ec-8a28-b201d0667bbe` | all → 62 | **Tasks(62)** tab (checklists still use tasks); Data Mask button |
| Schedule | `PanelSchedule_WO_QA_2026-07-21` (due Aug 21) | `fecde93a-ce99-4d39-bdc0-c3c0988f28c8` | all → 28 (panelboard+switchboard only) | **Panel Schedules** tab |

Work already performed inside the fixtures (completion-math seeds):
- Cleaning WO: 8 of 104 forms submitted — Bus2 (1-step, Pass), T1 (2-step dry-type, Pass×2),
  and all 6 MCC forms (`MCC-D-112/F-105/G-112/J-104/PH-N/PH-S`, Pass + notes).
- AF WO: schedule block Jul 21 7:00 AM · 8h · technician `abhiyant.singh+tec@…` + notes
  (green check in the list's Scheduled column); team = admin (certifier) + tec (field_technician).

## Behavioral findings recorded along the way

- **Rules-driven scope differs per service on the same site:** IR/COM → 46, Checklist → 62,
  Schedule → 28, AF(CB+SWB filter) → 6. Circuit Breaker ALONE under AF = **0 matching** (all Z1
  breakers are child assets, excluded by rules) — empty-WO trap with no warning beyond the
  preview count.
- **Auto-Schedule gating:** the button stays disabled until Est. Hours AND a Field Technician
  exist; proposed block length ≈ est-hours + breaks (8h → 9.5h suggestion).
- **Schedule block card has a delete-only icon** (red, `MuiIconButton-colorError`) — no edit;
  to change a block you delete and re-add via the Schedule "+" (block editor: Start Day/Time,
  Length, Consecutive Days, Assign Technician, Notes → **Add Block**).
- **Scheduled column indicator is icon-only** (SVG check/alert, no aria-label/title) — locators
  must assert the SVG, not text.
- **No asset-count mismatch on Z1** (preview = badge = grid for all 6 fixtures) — the
  dropped-Switch bug seen on "test site for api check" (API 11/14 vs grid 10/13) is tied to that
  site's `Main-…` mains Switch node, not universal.
- Quick Count = its own split-button (New / Copy) next to Actions → `/sessions/{id}/quick-count`
  tally screen; PM-Forms Actions menu = Multi-Select / Add Asset / EG FORMS Excel / Add Issues /
  Generate Report.
- Form drawers are native `<select>` + text input + Close/Save Draft/Submit — easily automatable;
  form count API (`/api/eg-form-instance/by-session/{id}/count`) decrements only on Submit.
  Header completion ring does NOT live-refresh after submits (stale until reload).

These fixtures are intentionally left in place as stable targets for the upcoming
per-type `ServiceWorkOrderTestNG` suites.
