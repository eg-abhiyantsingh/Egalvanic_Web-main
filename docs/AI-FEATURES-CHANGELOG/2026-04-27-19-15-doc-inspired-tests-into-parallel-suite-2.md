# Documentation-Inspired Tests added to Parallel Suite 2

**Date / Time:** 2026-04-27, 19:00 – 19:15 IST
**Branch:** `main` (NOT developer)
**Commit reference:** *(filled in after push)*

## Prompt summary

User asked me to read all 8 documents under `docs/ppt-document/` (PDFs, JPG, PNG, PPTX), understand them, and implement additional test cases into `parallel-suite-2`.

Documents read:
1. `HTTP_StatusCode.png` — visual chart of HTTP 1xx/2xx/3xx/4xx/5xx codes
2. `Mobile App Testing Concepts.jpg` — mind map of mobile-testing categories (mostly mobile-only; some adapt to web responsive)
3. `SQL.pdf` — 100+ page SQL reference
4. `Difference between Post, Put and Patch.pptx` — POST/PUT/PATCH semantics + idempotency
5. `Defects Category Definition.pptx` — 5 defect categories (Functional / Compat / Usability / Performance / Regression)
6. `QA Must Review And Analyze The Logs.pdf` — 6 criteria for QA log review
7. `Database Testing.pdf` — DDL/DML/DQL/DCL + aggregate functions
8. `API Testing Tools Comparison.pdf` — Swagger vs REST Assured vs Postman

## What I built / changed

### New test classes (3 classes, 17 tests total)

| File | Tests | Source document |
|---|---:|---|
| `src/test/java/com/egalvanic/qa/testcase/HttpStatusCodeContractTestNG.java` | 8 | HTTP_StatusCode.png |
| `src/test/java/com/egalvanic/qa/testcase/HttpMethodSemanticsTestNG.java` | 4 | Difference between Post, Put and Patch.pptx |
| `src/test/java/com/egalvanic/qa/testcase/ConsoleLogQualityTestNG.java` | 5 | QA Must Review And Analyze The Logs.pdf |

### TestNG suite XML (new)

| File | Purpose |
|---|---|
| `suite-doc-inspired.xml` | Wires the 3 new classes into a single TestNG suite for the new CI matrix entry |

### Workflow changes

| File | Change |
|---|---|
| `.github/workflows/parallel-suite-2.yml` | Added 9th matrix entry `doc-inspired` (17 tests, stagger=65). Updated header comment from "8 Groups" to "9 Groups" and total from "~180 TCs" to "~197 TCs". Bumped `max-parallel: 8 → 9`. Updated 2 places that printed "/ 8" to "/ 9". |

`mvn clean test-compile` → BUILD SUCCESS (66 source files).

---

## In-depth explanation per change

### Change 1 — `HttpStatusCodeContractTestNG.java` (8 tests)

**Source:** `HTTP_StatusCode.png` shows the categorized HTTP status codes:
- 1xx Informational
- 2xx Success (200, 201, 202, 204, 205)
- 3xx Redirection (300, 301, 302, 303, 304, 305)
- 4xx Client Error (400, 401, 402, 403, 404)
- 5xx Server Error (501, 502, 503, 507, 508)

**Why this class matters:** Status-code discipline is the API's vocabulary. Returning 200 + HTML for an unknown `/api/issues/<bad-uuid>` (the SPA-fallback bug from CLAUDE.md #8) MISLEADS every client into thinking the resource exists. This is the same pattern that made my OwaspIdorTestNG produce false-positive IDOR alerts in the previous prompt — fixing it at the test-coverage level surfaces it as a real, named contract violation.

**Tests:**
- TC_HSC_01: GET unknown `/api/issues/<bad-uuid>` → must be 404 (NOT 200/HTML — catches SPA fallback)
- TC_HSC_02: GET `/api/auth/me` with cookies → must be 200 + JSON
- TC_HSC_03: GET `/api/auth/me` WITHOUT cookies → must be 401 (NOT 200 = data leak)
- TC_HSC_04: POST malformed JSON to `/api/issues` → must be 400 (NOT 500)
- TC_HSC_05: GET `/api/non-existent-endpoint` → must be 404 (NOT 200/HTML)
- TC_HSC_06: PUT on a GET-only endpoint → must be 405 (or at least 4xx, NOT 200)
- TC_HSC_07: All 2xx successes return Content-Type: application/json
- TC_HSC_08: Login with unknown subdomain → must be 4xx (NOT 5xx)

**Trade-off:** The tests assert against fabricated UUIDs / unknown endpoints that the server should never authorize. Even if the backend has bugs, these probes can't cause real data loss. Status-code discipline is therefore safe to test in production-like environments.

### Change 2 — `HttpMethodSemanticsTestNG.java` (4 tests)

**Source:** "Difference between Post, Put and Patch.pptx" — verbatim quotes from the slide:
- POST: "always creates a new resource on the server. Non-idempotent."
- PUT: "first identifies the given resource from the URL and if it exists, it then replaces the existing resource, otherwise a new resource is created. Idempotent."
- PATCH: "used for partial update. Non-idempotent."

**Why this class matters:** These semantics are the API's contract with every client. A PUT that creates duplicates on retry breaks safe retries (clients use idempotency to recover from transient network errors). A POST that returns 200 instead of 201 misleads automation. A PATCH that replaces (instead of merging) silently destroys data.

**Tests:**
- TC_MS_01: POST creating a resource returns 201 (not 200)
- TC_MS_02: PATCH on non-existent UUID returns 404 (not silent 200) — confirms PATCH = partial update of existing, not create
- TC_MS_03: PUT is idempotent — two identical PUTs yield the same status code
- TC_MS_04: OPTIONS advertises allowed methods (CORS preflight discipline)

**Approach trade-off:** TC_MS_03 (idempotency) is tested by sending two identical PUTs and asserting the status code matches. This doesn't prove byte-for-byte data idempotency, but it catches the most-common idempotency violations (e.g., second PUT returning 409 when first PUT succeeded). For full byte-level idempotency we'd need to GET after each PUT and diff the records — that's a follow-up since it requires real records to PUT against.

### Change 3 — `ConsoleLogQualityTestNG.java` (5 tests)

**Source:** "QA Must Review And Analyze The Logs.pdf" — 6 review criteria adapted to BROWSER console (since that's what Selenium can observe):

| PDF criterion | Test in this class |
|---|---|
| 1. Meaningful details only | TC_LOG_05 (no bare "Error"/"Failed" with no context) |
| 2. Errors with stack traces | TC_LOG_02 (backend stack traces in browser console = info leak — opposite direction) |
| 3. Correct levels (error/warning/info) | TC_LOG_04 (login flow shouldn't trigger errors) |
| 4. NO sensitive information | TC_LOG_01 (no email/password/token leaks in console) |
| 5. Every entry has timestamp | (Browser console adds these automatically; not explicitly tested) |
| 6. Logging doesn't impact perf | TC_LOG_03 (< 50 errors per page load = no spam) |

**Why this class matters:** Browser console is visible to anyone with DevTools. Anything written there is effectively public. Sensitive-data leaks (emails, tokens, passwords) and backend stack-trace echoes (revealing framework, packages, file paths) violate criterion 4. Console spam (>50 errors per page load) violates criterion 6 and ALSO degrades real-user perceived perf (browser flushes the console buffer regularly).

**Implementation note:** Uses `FlakinessPrevention.getConsoleErrors(driver)` which is auto-installed by BaseTest's `@BeforeClass`. The buffer accumulates errors during the entire class lifecycle, so even errors logged on initial dashboard mount are visible to the test.

**Trade-off — sensitive-data whitelist:** TC_LOG_01 has a whitelist for the test user's own email (since login flows often echo the email being logged in with — that's normal, not a leak). The whitelist matches `egalvanic.com` and `AppConstants.VALID_EMAIL`. If the same approach were used for a real production user, the whitelist would need to drop their email. Acceptable for QA-environment testing.

### Change 4 — `suite-doc-inspired.xml` (new TestNG suite)

Standard TestNG XML wiring the 3 new classes into a single suite. Header comment documents which document each class derives from, and explicitly notes the OTHER documents (SQL, Mobile, API tools, Defects categories) and why they didn't spawn dedicated test classes:
- **SQL + DB Testing PDFs** — already covered by `CriticalPathTestNG.CP_DI_*` tests that verify count consistency across endpoints (which is the most-relevant SQL-testing concept for our integration tests)
- **Mobile App Testing JPG** — out of scope for current Web suite; would need a separate mobile-viewport coverage suite
- **API Tools comparison PDF** — informational; we already chose Selenium-XHR-based probing for our API tests
- **Defects Category PPTX** — taxonomy used by `SmartBugDetector` to classify failures (Functional / Compat / Usability / Perf / Regression) — informs reporting, not new tests

### Change 5 — `parallel-suite-2.yml` (workflow update)

Added a new matrix entry as the 9th group:

```yaml
- group: doc-inspired
  name: "Documentation-Inspired Tests"
  xml: suite-doc-inspired.xml
  tests: 17
  stagger: 65
```

Updated the workflow header (8→9 groups, ~180→~197 total TCs) and bumped `max-parallel: 8 → 9` so all 9 groups can actually run simultaneously. Two cosmetic occurrences of `/ 8` in the summary output were updated to `/ 9`.

---

## Verification

- `mvn clean test-compile` → **BUILD SUCCESS** (66 source files; was 63 before this commit + 3 new files = 66 ✓)
- TestNG XML files validated by structure; no DTD errors
- Workflow YAML reviewed manually for matrix consistency
- Live execution of the new classes deferred to next CI cycle (Parallel Suite 2 already runs on workflow_dispatch — triggering it now would actually run all 9 groups against acme.qa)

---

## Learning notes for manager review

1. **Documentation can be a testing input, not just a deliverable.** The 8 documents in `docs/ppt-document/` had been treated as background reading. Today they each became either (a) a new test class or (b) a documented reason why an existing class already covers the concept. That conversion is what turns "we read it" into "we test it".

2. **The HTTP_StatusCode.png chart was the most actionable single document.** Eight @Test methods came directly from that one image because each row in the chart (200, 201, 401, 404, 405, 500, etc.) is a contract the API either honors or violates. Test 1 alone (TC_HSC_01) catches the SPA-fallback bug that confused our IDOR tests in the previous prompt — same systemic issue, now with a name and a CI tripwire.

3. **PPTX content extraction works without a PowerPoint license.** I used `unzip` + a Python regex pass over `ppt/slides/slide*.xml` to pull text from the two pptx files. That's a one-liner technique any QA engineer can use to mine teaching artifacts for testable content.

4. **Some documents inform existing tests rather than spawning new ones.** SQL + DB Testing PDFs are already covered by the count-consistency tests in CriticalPathTestNG (which assert "dashboard count = grid pagination count" — the most relevant SQL-test invariant for our integration suite). Defects Category Definition is the taxonomy SmartBugDetector uses internally to classify failures. Calling out "this document doesn't need a new test class because X already covers it" is just as important as adding new ones.

5. **Adding the 9th matrix group is a 5-minute change once the test classes exist.** parallel-suite-2.yml uses a TestNG XML per matrix entry, so adding a group is: (a) write the XML, (b) add a matrix block referencing it, (c) bump max-parallel and any group-count cosmetic mentions. Repeatable for any future themed test bundle.

---

## Open follow-ups

- **Live-run the new 9th group via Parallel Suite 2 dispatch** to capture real findings on `acme.qa` — likely candidates: TC_HSC_01 (SPA-fallback caught), TC_HSC_03 (anonymous data leak — strong assertion), TC_LOG_03 (console error count).
- **Mobile-responsive web tests** from the Mobile App Testing Concepts mind map — would need a 4th class testing key pages at 375x667 (mobile), 768x1024 (tablet), 1920x1080 (desktop) viewports. Not done in this commit because it's its own scope.
- **REST Assured API testing layer** as suggested by the API Tools Comparison PDF — would replace the current Selenium-XHR probes with a proper REST client. Big architectural lift; queued for a future sprint.

## Rollback

```
git revert <this-commit>
```
Tests are additive — reverting removes the 3 new classes + suite XML + workflow group, no impact on existing 8 groups.
