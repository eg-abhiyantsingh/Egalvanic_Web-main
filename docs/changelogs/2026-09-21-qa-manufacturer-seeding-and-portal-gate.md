# QA: manufacturer/type options + the Portal Sales gate (2026-09-21)

**Prompt:** relayed dev note — maintenance portal gated on a new "Portal Sales" role; *"check this in qa first,
this code is now in qa"*; *"Manufacturer in core attributes will be phased out in favor of manufacturer in
engineering. I am guessing the lack of options is a seeding issue - please copy any missing enum data from dev.
There should be options for manufacturer, and after choosing manufacturer, for type."*

Page: <https://claude.ai/artifact/UwTBd5iS4vAdwzftPTCSgL>
Evidence: `docs/bug-evidence/2026-09-21-qa-manufacturer-seeding/`
Checked on QA, build `index-BV-phiFE.js`.

## 1 · Manufacturer → Type: the chain works, the list is short

Opened a Panelboard → edit → **Engineering**:

* **Manufacturer** dropdown = **5** options: CEB, EATON/CUTLER-HAMMER, GE, SCHNEIDER/SQUARE D, SIEMENS.
* Chose **SIEMENS** → **Panel Type** populated with **29** designations (C1, C2, CDP-6, CDP-7, EQ, EQIII,
  ES, LPB, LPP, NLA, NLAB, NPA …). Cancelled without saving.

So the dependent-picker wiring is correct. The guess about seeding is right:

| Source | Count |
|---|---|
| `/api/eqp-lib/panel-manufacturers` (what the picker reads) | **5** |
| `/api/eqp-lib/manufacturers` (engineering catalogue) | **448** |
| `/api/skm-library/manufacturers` | 104 |
| panel designations across all 5 | **88** (SIEMENS 29, EATON 20, SCHNEIDER 20, GE 18, CEB 1) |

The picker lists manufacturers that have **at least one panel designation**. 5 of 448 do. Seeding designations
is what makes a manufacturer appear.

## 2 · Extra finding: the duplicate Manufacturer field

A Panelboard's form carries **two** Manufacturer fields today — the library-backed one under **Engineering**
(with Panel Type beside it) and a second, **required**, free one under **Custom Attributes**. The user is asked
for the manufacturer twice and only one is catalogue-backed. Worth retiring the duplicate as part of the
phase-out rather than leaving it behind.

## 3 · Portal Sales gate — confirmed on QA

From a staff seat with 5 roles but not Portal Sales: no `/maintenance-portal/*` section in the rail, and
`/maintenance-portal/reports` → **Access Denied**. `/maintenance/*` (Program/Compliance/Reports) sits under
Site Data, matching "most regular users will see this in site data only". (Same evidence as the ZP-4138 pass
earlier today.)

**Not confirmed:** could not enumerate the role catalogue to prove "Portal Sales" exists on QA by name —
`/api/settings/roles`, `/api/admin/roles`, `/api/users/roles`, `/api/lookup/roles` all return the HTML
fallback; `/api/company/roles` 422s. The gate's behaviour is demonstrated, which is the part that matters.

## What I did NOT do

**Did not copy enum data from dev.** It is a data write into a shared environment and I have no dev DB access;
it belongs to whoever owns the seeding migration. Supplied the before-state numbers above so the seed can be
verified afterwards.
