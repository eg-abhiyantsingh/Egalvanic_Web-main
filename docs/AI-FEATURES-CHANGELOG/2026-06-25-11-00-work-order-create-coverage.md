# Work Order creation — automated coverage of the full "Create New Work Order" dialog

- **Title:** Modelled and tested the entire Create Work Order flow (form + gating + Team/Schedule/
  Equipment sub-flows + end-to-end create-and-open), passing 6/6 on the first headed run.
- **Date:** 2026-06-25
- **Time:** 11:00
- **Prompt:** Create Work Order workflow spec (name, priority, hours, description, facility, photo type,
  dates, team member, schedule block with technician + auto-schedule + add block, equipment, create).

---

## What changed (plain summary)

Added a `WorkOrderPage` API for the current "Create New Work Order" dialog and a 6-test class
(`WorkOrderCreateTestNG`) covering: the form structure + defaults, the Create-button gating, the
Equipment/Team/Schedule sub-flows, and a full end-to-end create that then finds and opens the new work
order. Wired into the existing `workorder-issue` CI group.

## Depth explanation (for learning + manager review)

**Discover-first turned a 16-step spec into a 1-field gate.** The spec lists sixteen steps. Driving the
real dialog first revealed that Priority, Facility, Photo Type and Start Date all arrive **pre-filled**,
so the *Create* button is gated on **WO Name alone**. That single observation reshaped the whole test
design: WOC_02 asserts exactly that gate (disabled empty → enabled after name), and the end-to-end test
treats every other field as an enrichment rather than a blocker. You only learn that by watching the
form, not by reading the spec.

**The stale-locator trap.** `WorkOrderPage` already had `fillName`/`fillDescription`/`selectPriority`,
but their locators targeted older placeholders ("Enter Work Order Name", "Description") that the current
dialog doesn't use (it's "e.g., Q1 2024 Maintenance" and "Describe the scope of this work order..."). I
added a fresh, accurately-targeted set rather than assume the old methods still matched — pulling the
real field `outerHTML` first so the locators were right on the first compile.

**Modelling progressive-disclosure sub-flows.** Team and Schedule are ＋-icon affordances: the member
dropdown, the schedule block (with Assign Technician), and the "Add Block" button only exist *after* you
click the icon. The page object encodes that order — click ＋, (optionally) assign the first technician,
click Auto-Schedule if it became enabled, then Add Block — and treats the whole thing as best-effort so
a data-poor site can't fail the core create. WOC_04/05 assert the controls appear; WOC_06 actually
drives them (it assigned technician "abhiyant admin" and added a block).

**Reused the hard-won keystroke lessons.** Two findings from the asset-location work were applied
up-front here, which is why this passed 6/6 first try: (1) the tenant's ~130-site virtualised facility
picker only filters on **real** keystrokes, so site scoping uses `sendKeys`; (2) the grid search is a
debounced server-side filter that ignores JS value-sets, so persistence is verified with a real-keystroke
search. Carrying forward a previously-diagnosed environment quirk is the difference between a clean run
and another debug cycle.

**Validation philosophy:** proven, not compiled — headed run, 6/6 green, and WOC_06 closes the loop the
spec describes (create → search → open the new work order's detail page).

## Validation

_Headed (no headless), site "Android Qa Site1"._ **6/6 PASS — BUILD SUCCESS** (`Tests run: 6,
Failures: 0`). WOC_01 24s · WOC_02 26s · WOC_03 26s · WOC_04 26s · WOC_05 26s · WOC_06 104s (created a WO
with Priority High + Megger + a technician-assigned schedule block, then found and opened it).
