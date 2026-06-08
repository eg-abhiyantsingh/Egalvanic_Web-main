# Opportunity → Quote → EMP: end-to-end flow, live-mapped with Playwright

- **Date:** 2026-06-08
- **Time:** 13:02 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** "go in deepth use tools playright or anything to understand end to end flow
  for opporunity like creating then clicking then creating emp like adding quote"
- **Method:** Live browser driving via Playwright MCP against `https://acme.qa.egalvanic.ai`
  (real admin login, Site **gyu**). No code changes in this step — pure reverse-engineering
  of the real UI so the previously *un-automatable* quote/EMP lifecycle can now be automated.

---

## TL;DR — the lifecycle

```
Opportunity (status Qualifying, Revisions 0, $0)
  │
  │  open detail  →  "+ Add Quote"
  ▼
Add New Quote dialog
  • Quote Type (radio):
       – Standard Quote  → one-time scope (pricing, labor, materials)
       – EMP Quote       → multi-year maintenance plan
            · Length (years): 1–10 (default 5)
            · Start Date: MM/DD/YYYY (default today)
            · ☐ Select specific assets
            · ☐ Provide Shutdown Schedule
  • Create Quote
  ▼
Quote a.k.a. "Rev"  →  "{OppName} - Rev {n}"   (route: /quotes/{uuid})
  • Type: EMP | Standard ;  Status: Draft ;  $0
  • ⚠ Opportunity allows only ONE active quote at a time.
    Add Quote is DISABLED with: "Opportunity already has an active quote.
    Reject the existing quote to add a new one."
  • Quote editor tabs: Overview | Pricing Matrix (disabled until priced) |
       Visualizer | Shutdowns | Planned Work Orders (disabled) | Attachments | Dataset
  • Status menu: Draft → Sent to Customer → Accepted → Rejected → Abandoned
  ▼
Set Status = Accepted
  ▼
"Quote Accepted - Create EMP" dialog
  • "Enter an EMP number to commit to this work."
  • EMP Number (required) → Create EMP
  ▼
EMP  →  keyed by EMP Number (e.g. EMP-PWX-001)   (page transforms in place)
  • Status: On Track ;  subtitle "Quote: {OppName} - Rev {n}"
  • Tabs gain: Progress Reporting (default) + Change Orders
  • Listed in /emps  (Created | Site | EMP # | Issues | Released | Progress | Status)
  • EMP is BOUND to its opportunity: its delete is disabled —
    "Delete the linked opportunity to remove this EMP"
  ▼
Opportunity → Closed Won, Revisions 1
```

A full live run was completed end-to-end: created `PWExplore_QuoteFlow` on gyu → added an
**EMP Quote** (5 yrs, start today) → opened Rev 1 → set **Accepted** → entered EMP number
`EMP-PWX-001` → **Create EMP**. Result verified: EMP-PWX-001 appears in `/emps` (On Track),
and the opportunity flipped to **Closed Won** with **Revisions 1**.

---

## Status mapping (quote status → opportunity pipeline status)

| Quote status (editor menu) | Opportunity pipeline status | Verified |
|---|---|---|
| Draft | Qualifying | ✅ (new quote → opp stays Qualifying) |
| Sent to Customer | Pending Response | (inferred from pipeline cards; not yet driven) |
| Accepted | **Closed Won** | ✅ (live: opp went Qualifying → Closed Won) |
| Rejected | Closed Lost | (inferred) |
| Abandoned | Abandoned | (inferred) |

> Note: legacy opportunities on other sites show a "Qualified" status that is not in the
> current quote-status menu — it is a legacy/grandfathered pipeline value.

---

## Concrete UI facts (for building locators)

### Create Opportunity dialog
- Title: **Create New Opportunity**
- **Facility** (required) — combobox, **defaults to `gyu`** regardless of the page's active
  Site. *This is the root cause of the earlier "created opp not found" bug:* if the page is
  scoped to another Site (e.g. `Yxbzjzxj`) but the dialog Facility stays `gyu`, the new opp
  lands on the gyu grid and is invisible on the active grid. **Fix already applied: scope the
  whole suite to Site `gyu`** so dialog-Facility and page-Site agree.
- **Opportunity Name** — second textbox; **Create** stays disabled until it has a value.

### Opportunity detail (`/opportunities/{uuid}`)
- Heading = opp name + status chip. Search-quotes box + **Add Quote** button.
- Quotes grid columns: **Title | Type | Created | Quote Total | Status | Committed | (actions)**.
  "Committed" is the EMP link column; per-row trash = **Delete Quote**.

### Add New Quote dialog
- `radio "Standard Quote"` (default) / `radio "EMP Quote"`.
- EMP fields: `spinbutton "Length (years)"` (1–10, default 5), `textbox "Start Date"`
  (MM/DD/YYYY), checkboxes `"Select specific assets"`, `"Provide Shutdown Schedule"`.
- Buttons: **Cancel**, **Create Quote**.

### Quote editor (`/quotes/{uuid}`)
- Title h4 = `"{OppName} - Rev {n}"`. Header status button `"Draft ▾"` opens a `menu` with
  menuitems: **Draft**, **Sent to Customer**, **Accepted**, **Rejected**, **Abandoned**.
- Overview Quote Summary: Title, Site Name (`gyu`), Customer Account (`TEst`), Description
  (EMP boilerplate), Recipient (combobox; "No contacts available" + "Manage contacts" link),
  Created Date, **Status** combobox. Metrics: Total Customer-Facing Price, Margin, Total Cost,
  Total Hours, Work Orders, Line Items, Assets. "Annual Customer-Facing Prices" table.
- After **Accepted**: header shows **Create EMP** button; the Status combobox locks.

### "Quote Accepted - Create EMP" dialog
- Heading: **Quote Accepted - Create EMP**.
- `textbox "EMP Number"` (required) → **Create EMP** (disabled until filled).

### EMP detail / `/emps`
- EMP detail h4 = the EMP number; subtitle `"Quote: {OppName} - Rev {n}"`; status `On Track`.
  Tabs: **Progress Reporting** (default), Overview, Pricing Matrix (disabled), Visualizer,
  Shutdowns, Planned Work Orders (disabled), **Change Orders**, Attachments, Dataset.
  Progress dashboard: Procedures %, Work Orders %, Issues Flagged, Open Issues.
- `/emps` grid columns: **Created | Site | EMP # | Issues | Released | Progress | Status | (actions)**.
  Row delete is disabled: *"Delete the linked opportunity to remove this EMP."*

---

## What this unblocks in the automation suite

Previously skipped/quarantined because the quote lifecycle "couldn't be set up" — now it can:

1. **TC_OPP_25 — deleted quotes excluded from count/value.** Create a quote (Revisions→1),
   delete it (Delete Quote), assert Revisions→0 and value unaffected. (Was SKIP: "requires
   quote-lifecycle setup".)
2. **TC_OPP_19–22 — pipeline status transitions, DRIVEN not just validated.** Drive the quote
   status menu and assert the opportunity's pipeline status flips (Accepted → Closed Won
   verified live).
3. **NEW business rule — single active quote.** After one quote exists, assert **Add Quote**
   is disabled and the "reject the existing quote" tooltip is present.
4. **NEW — EMP commit + binding.** Accept a quote → Create EMP with a number → assert the EMP
   shows in `/emps` and its delete is disabled ("Delete the linked opportunity…").

These become real page-object methods on a new `QuotePage`/`EmpPage` (or extensions to
`OpportunitiesPage`) plus TestNG cases scoped to Site `gyu`, consistent with the rest of the
Opportunities suite.

## What was built + validated (this session)

After mapping the flow, the understanding was turned into real automation (not just notes):

**POM (`OpportunitiesPage`)** — new quote/EMP-lifecycle methods: `isAddQuoteEnabled` /
`addQuoteDisabledReason`, `clickAddQuote`, `selectQuoteType("EMP"|"Standard")`, `createQuote`,
`waitForQuoteRow`, `quoteRowCount`, `openFirstQuoteRow`, `deleteFirstQuoteOnDetail`,
`setQuoteStatus` (+`waitForStatusButton`), `isCreateEmpDialogOpen`, `fillEmpNumberAndCreate`,
`isEmpDetail`, `empExists`.
- Key locator lesson: the quote-editor status control is a **MUI Chip rendered as
  `div[role="button"]`** (text "Draft ▾"), NOT a `<button>` element — the a11y tree merely
  labels it "button". The finder must query `button,[role=button]`. (Found via a live
  Playwright DOM inspection after the first selector missed it.)

**Tests (`OpportunitiesTestNG`, gyu-scoped, self-cleaning):**
- `testOpp25_DeletedQuotesExcluded` — was a documented SKIP; now **drives** add-quote →
  count 1 → delete-quote → count 0 (deleted quote excluded). **PASS.**
- `testOpp44_SingleActiveQuoteRule` (new) — after one quote, Add Quote is disabled with the
  "reject the existing quote" reason. **PASS.**
- `testOpp45_EmpCommitFromAcceptedQuote` (new) — create opp → add EMP quote → open editor →
  set status **Accepted** → enter EMP number in the Create-EMP dialog → commit → assert EMP
  detail, EMP listed in `/emps`, and opportunity driven to **Closed Won**. **PASS.**

**Validation (live, non-headless, Site gyu):**
- The 3 new tests run together: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
- Full Opportunities functional gate (`suite-opportunities.xml`, excludes `known-product-bug`):
  **Passed 24 / Failed 0 / Skipped 3** (was 21/0/4 — +2 new tests, and TC_OPP_25 flipped
  SKIP→PASS). No regressions.
- No NEW product bug surfaced: the create→quote→EMP→Closed-Won path is functionally correct
  (BUG-A's `Qe` console noise is present but does not block the flow). These are positive,
  passing functional tests — so they sit in the gate, not the `known-product-bug` quarantine.

## Evidence
Playwright snapshots/console logs captured under `.playwright-mcp/` during the 2026-06-08
session (Create dialog, Add Quote dialog with EMP fields, quote editor + status menu,
Create-EMP dialog, EMP detail, `/emps` grid, opp grid showing Closed Won / Rev 1, and the
DOM-inspection of the status Chip). Run logs: `/tmp/opp_quote_final.log`, `/tmp/opp_gate.log`.
