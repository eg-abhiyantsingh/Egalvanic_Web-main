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
`selectWoPriority`, `selectEquipment`, `getEquipmentOptions`, `addFirstTeamMember`,
`addScheduleBlock(assignTechnician)` (＋ → assign first tech → optional Auto-Schedule → Add Block),
`isCreateButtonEnabled`, `clickCreateWorkOrder`, plus a private `selectFirstAutocompleteOption`. All
locators are accurate to the current dialog (the old create methods were stale for it).

## `WorkOrderCreateTestNG` (new, scoped to "Android Qa Site1")
- **WOC_01** — dialog opens with every field/section + verified defaults (Priority=Medium, Photo Type=FLIR-SEP, Start Date set).
- **WOC_02** — **Create gating**: disabled with empty WO Name, enabled once filled.
- **WOC_03** — Equipment dropdown lists options incl **Megger**.
- **WOC_04** — Schedule ＋ reveals a block with **Assign Technician** + **Add Block**.
- **WOC_05** — Team ＋ reveals the **"Select user"** member dropdown.
- **WOC_06** — **End-to-end**: name + Priority High + Est. Hours + Description + Due Date + team member +
  schedule block (with technician) + Megger → **Create** → search (real keystrokes) → **open** the new
  WO detail (/sessions/{id}).
- **WOC_00** — disabled DOM-discovery diagnostic.

## Suite wiring
- New `suite-workorder-create.xml`.
- Added `WorkOrderCreateTestNG` to `suite-workorder-issue.xml` (the existing **workorder-issue** CI
  group) so it runs in both parallel suites with **no** dashboard-array changes. Bumped that group's
  display name/count in `full-suite-dashboard.sh` (234 → 240).

## Validation
_Headed (no headless), `mvn test -DsuiteXmlFile=suite-workorder-create.xml`, site "Android Qa Site1"._

**6/6 PASS on the first run — `Tests run: 6, Failures: 0, Errors: 0` (BUILD SUCCESS).**

```
site 'Android Qa Site1' selected=true
PASSED: WOC_01_FormStructureAndDefaults (24s)
PASSED: WOC_02_CreateGatedOnName (26s)
PASSED: WOC_03_EquipmentOptions (26s)
PASSED: WOC_04_ScheduleBlockControls (26s)
PASSED: WOC_05_TeamMemberControl (26s)
PASSED: WOC_06_CreateWorkOrderEndToEnd (104s)   # schedule block w/ technician "abhiyant admin"; WO created, found & opened
```

Carried forward from the asset-location work: site selection and grid search both use **real
keystrokes** (the tenant's ~130-site virtualised picker and the work-order grid search don't react to a
JS value-set).
