# Opportunities — coverage expansion (15 new tests), deep live triage, + parallel-suite wiring

- **Date:** 2026-06-08
- **Time:** ~15:10 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** "add more test case i dont think so you have covered everything in opportunes and
  also add that tab in parallel suite too" (+ follow-up: go deep, iterate trial-and-error with
  Playwright, divide into parts, cover everything, make CI faster).

## What was done

### 1. Multi-lens coverage gap analysis (workflow)
Ran a 10-agent workflow (1 inventory + 7 diverse gap lenses + synthesis) over the existing
~40 `TC_OPP_*` tests, the POM, the live-flow doc, and BUGS.md. It produced a deduplicated,
prioritized list of genuinely-missing cases (deduped against existing coverage). Implemented the
high-value subset as **15 new tests (testOpp48–63)**.

### 2. New tests (gyu-scoped, self-cleaning, assert FUNCTION not health)
- **Pipeline transitions, DRIVEN (not just enum-validated):**
  - `testOpp48` — Draft→Sent⇒opp **Pending Response**, then Rejected→**Close Opportunity**⇒**Closed Lost** (status re-derives live on each transition).
  - `testOpp49` — Abandoned quote ⇒ opp **Abandoned**.
- **Quote lifecycle:**
  - `testOpp50` — Reject → **"Create Revision"** produces **Rev 2** and re-activates the opp (the real single-active-quote unblock).
  - `testOpp54` — Standard vs EMP quote-type config divergence (one-time-scope description vs multi-year-plan + "Provide Shutdown Schedule"/"Select specific assets"/"1 to 10 years").
  - `testOpp62` — a created quote (Rev 1) **survives a hard reload** of the detail (server-committed, not phantom).
  - `testOpp63` — cold **deep-link to /quotes/:id** mounts the correct quote editor ("{Opp} - Rev 1").
- **EMP binding integrity:**
  - `testOpp51` — Create-EMP dialog **requires an EMP Number** (blank ⇒ Create disabled; real ⇒ enabled).
  - `testOpp52` — committed EMP's `/emps` row delete is **disabled** with the **"Delete the linked opportunity"** binding reason.
  - `testOpp53` — deleting the source opportunity **cascade-removes** its EMP from `/emps`.
- **Grid / KPI reactivity:**
  - `testOpp59` — grid `quote_count` cell reacts **0→1 on add**; detail active-quote list excludes the deleted quote (→0). (Observed: the grid "Revisions" column counts revisions created, incl. deleted — see findings.)
  - `testOpp60` — pipeline **KPI cards** react: +1 Qualifying on create, then −1 Qualifying / +1 Closed Won on accept.
- **Validation:**
  - `testOpp61` — EMP-quote **Length(years)** bounded to 1–10 (default 5; 0/11 clamped or blocked).
- **API contract + security (REST Assured, live-grounded endpoints):**
  - `testOpp56` — scoped `GET /company/{id}/opportunities` **rejects unauthenticated** reads (401/403).
  - `testOpp58` — authed list contract: `{success:true, count, opportunities[]}`, count == array length.
  - `testOpp57` — **tripwire (quarantined)** asserting the flat `/opportunities/` & `/quotes/` endpoints SHOULD require auth (they return 200 unauth → **BUG-E**).

### 3. Deep live triage (Playwright) — behaviors learned, not assumed
Running the 15 back-to-back surfaced failures that single runs hid; each was root-caused live:
- **Reject is a 2-step flow.** Selecting "Rejected" opens a **"Quote Rejected"** dialog
  (*"create a new revision … or close the opportunity? … once rejected, this quote's status
  cannot be changed."*) — buttons **Cancel / Close Opportunity / Create Revision**. The chip
  stays "Draft" until you choose. → reworked testOpp48 (Close⇒Closed Lost) and testOpp50
  (Create Revision⇒Rev 2). Added POM `chooseRejectClose()` / `chooseRejectCreateRevision()`.
- **Grid `quote_count` counts revisions created** (incl. deleted/rejected) — it does NOT drop to
  0 when the only quote is deleted (verified via 15s of fresh reloads). The authoritative
  active-quote exclusion is on the **detail** (already covered by TC_OPP_25). → testOpp59 asserts
  the add direction + detail exclusion and logs the grid value.
- **Standard accept also opens the Create-EMP dialog** — the EMP-vs-Standard divergence is in the
  quote CONFIG (multi-year fields), not the accept step. → re-framed testOpp54 to the config divergence.
- **Quote-editor render lag** (BUG-A) — the status chip (`div[role=button]`, not `<button>`) and
  the Add-Quote "Create Quote" button mount async and get briefly overlaid by a toast. → made
  `createQuote`/`clickAddQuote`/`forceClickSave` tolerant (poll-for-enabled + JS-click, ignore
  overlays), `currentQuoteStatus` poll for the chip, and `setQuoteStatus` verify-and-retry the transition.

### 4. New finding — BUG-E (filed in BUGS.md)
Flat `GET /api/opportunities/` and `/api/quotes/` return **200 unauthenticated** (a null-field
template — no data leak) while the company-scoped sibling enforces **401**. Auth-enforcement
inconsistency (OWASP API1/API5). `testOpp57` is the quarantined red tripwire.

### 5. Parallel-suite + full-suite wiring ("add that tab in parallel suite too")
- Opportunities was only in **parallel-suite-2** (curated). Moved it into **parallel-suite.yml**
  (Suite 1 — Core Regression) alongside Assets/Issues/Work Orders, where a core functional SALES
  module belongs; bumped `max-parallel` 9→10; removed the duplicate from Suite 2 (no double-run).
- Added `OpportunitiesTestNG` to **fullsuite-testng.xml** (module 42) — it was entirely absent.
- `suite-opportunities.xml` runs the whole class (functional gate, excludes `known-product-bug`),
  so all new green tests are automatically included; testOpp57 stays quarantined.

## Validation (live, non-headless, Site gyu)
- The 14 new gate tests run **back-to-back: 14 passed, 0 failed, 0 skipped**.
- **Full Opportunities functional gate** (`suite-opportunities.xml`, excludes `known-product-bug`):
  **41 tests → 38 passed, 0 failed, 3 skipped** (was 24/0/3 before this work — **+14 new green**,
  **zero regressions** to the existing tests despite the shared POM-click/status-helper changes).
- The 3 skips are unchanged preconditions (RBAC needs a low-perm user; SLD-switch needs ≥2 SLDs;
  AI-gated needs the flag off). Run logs: `/tmp/opp_full_gate2.log`, `/tmp/opp_final_validation.log`.
