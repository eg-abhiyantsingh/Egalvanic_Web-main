# Work Order creation — full "Create New Work Order" dialog coverage

- **Date:** 2026-06-25
- **Prompt:** Create Work Order workflow spec — WO Name, Priority, Est. Hours, WO Description, Facility,
  Photo Type, Start/Due dates, Team member, a Schedule block (Assign Technician → Auto-Schedule →
  Add Block), Equipment, then Create; afterwards search for and open the new work order.
- **Type:** New page-object methods (`WorkOrderPage`) + new test class (`WorkOrderCreateTestNG`, 6 active
  + 1 disabled diagnostic) + suite wiring.

---

## What the form does (discovered live, then asserted)

Driven live on site **"Android Qa Site1"** (Playwright MCP) before writing assertions. Work Orders
(/sessions) → **Create Work Order** opens the **"Create New Work Order"** dialog:

| Field | Notes |
|---|---|
| **WO Name / #** * | text — the **only empty required field** |
| **Priority** * | MUI Autocomplete, **defaults "Medium"** (Low/Medium/High) |
| **Est. Hours** | number |
| **WO Description** | textarea |
| **Facility** * | Autocomplete, **defaults to the current site** |
| **Photo Type** * | Autocomplete, **defaults "FLIR-SEP"** |
| **Start / Due Date** | **typeable** MM/DD/YYYY text inputs (Start defaults to today) |
| **Team** | **＋** icon → "Select user" Autocomplete |
| **Schedule** | **＋** icon → block (Start Day/Time, Length, Consecutive Days, **Assign Technician**, Notes) + **Auto-Schedule** + **Add Block** |
| **Equipment** | Autocomplete, optional — options include **Megger** |

**Key behaviour:** Priority/Facility/Photo Type/Start Date arrive pre-filled, so **Create is gated on
WO Name alone**. All dropdowns are MUI Autocompletes; the date inputs accept typed values.

## `WorkOrderPage` — new create-dialog API
`isCreateWorkOrderDialogOpen`, `woFieldPresent`, `fillWoName`, `fillEstHours`, `fillWoDescription`,
`typeDueDate`, `getStartDateValue`/`getDueDateValue`/`getPriorityValue`/`getPhotoTypeValue`,
`selectWoPriority`, `selectWoFacility`, `selectWoPhotoType`, `pickDate(calendarIndex, monthsForward, day)`
(picks a date via the **calendar icon** + month navigation), `selectEquipment`, `getEquipmentOptions`,
`addFirstTeamMember`, `addScheduleBlock(assignTechnician)` (＋ → assign first tech → optional
Auto-Schedule → Add Block), `isCreateButtonEnabled`, `clickCreateWorkOrder`, plus a private
`selectFirstAutocompleteOption`. All locators are accurate to the current dialog (the old create methods
were stale for it).

### All 16 spec steps are exercised
After review, the explicit **Facility** select (step 6), **Photo Type** select (step 7), the **calendar-icon**
date picker for **Start** (step 8) and **Due** (step 9), and the **Auto-Schedule** click (step 13) were
added (initially these relied on defaults / typing). Team member, technician and equipment use the
first-available option (no concrete names were given in the spec). Date picker: Start = today (current
month, enabled); Due = the 15th of next month (a fully-enabled future month, exercising month navigation).

## `WorkOrderCreateTestNG` (new, scoped to "Android Qa Site1")
- **WOC_01** — dialog opens with every field/section + verified defaults (Priority=Medium, Photo Type=FLIR-SEP, Start Date set).
- **WOC_02** — **Create gating**: disabled with empty WO Name, enabled once filled.
- **WOC_03** — Equipment dropdown lists options incl **Megger**.
- **WOC_04** — Schedule ＋ reveals a block with **Assign Technician** + **Add Block**.
- **WOC_05** — Team ＋ reveals the **"Select user"** member dropdown.
- **WOC_06** — **End-to-end (all 16 steps)**: name + Priority High + Est. Hours + Description +
  **Facility select** + **Photo Type FLIR-SEP select** + **Start/Due via calendar icon** + team member +
  schedule block (technician + Auto-Schedule + Add Block) + Megger → **Create** → search (real
  keystrokes) → **open** the new WO detail (/sessions/{id}).
- **WOC_07** — **calendar-icon date picker**: sets Start (today) and Due (15th of next month, via month
  navigation) through the calendar icon and asserts the inputs update.
- **WOC_00** — disabled DOM-discovery diagnostic.

## Suite wiring
- New `suite-workorder-create.xml`.
- Added `WorkOrderCreateTestNG` to `suite-workorder-issue.xml` (the existing **workorder-issue** CI
  group) so it runs in both parallel suites with **no** dashboard-array changes. Bumped that group's
  display name/count in `full-suite-dashboard.sh` (234 → 241).

## Validation
_Headed (no headless), `mvn test -DsuiteXmlFile=suite-workorder-create.xml`, site "Android Qa Site1"._

**7/7 PASS — `Tests run: 7, Failures: 0, Errors: 0` (BUILD SUCCESS).**

```
site 'Android Qa Site1' selected=true
PASSED: WOC_01_FormStructureAndDefaults (52s)
PASSED: WOC_02_CreateGatedOnName (26s)
PASSED: WOC_03_EquipmentOptions (26s)
PASSED: WOC_04_ScheduleBlockControls (26s)
PASSED: WOC_05_TeamMemberControl (26s)
PASSED: WOC_06_CreateWorkOrderEndToEnd (90s)   # Facility+Photo Type selected, dates via calendar icon (day 25 / next-month 15), schedule block w/ technician; WO created, found & opened
PASSED: WOC_07_DatePickerViaCalendarIcon (27s)
```

Notes from iterating to 7/7:
- The initial 6-test version passed; adding the explicit steps surfaced one **timing flake** — Create
  enables a moment after the WO Name registers (React validation), so WOC_02's single check was made a
  ~8s poll. (The end-to-end never saw it because it fills many fields before checking.)
- Carried forward from the asset-location work: site selection and grid search both use **real
  keystrokes** (the tenant's ~130-site virtualised picker and the work-order grid search don't react to a
  JS value-set).
