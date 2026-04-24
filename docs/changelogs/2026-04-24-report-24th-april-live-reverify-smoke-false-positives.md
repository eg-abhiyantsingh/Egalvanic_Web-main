# 2026-04-24 — Live re-verification of `bug pdf/24th april.html` smoke failures. 4 of 5 are FALSE POSITIVES.

## Prompt
> @bug pdf/24th april.html @bug pdf/24th april 2.html smoke test should pass check modify the report and check whether the fail test really fail if not falling then update the report

## Model in use
Claude Opus 4.7 (1M context), `effortLevel: max`, always-thinking on.

## Branch safety
`main` of QA framework repo (`eg-abhiyantsingh/Egalvanic_Web-main`). Production untouched. No test-code changes in this commit — only report mutation after live verification.

## TL;DR

Live-checked every smoke failure on `acme.qa.egalvanic.ai` via Playwright MCP. **4 of 5 smoke failures in `24th april.html` are false positives** — the product works, the tests asserted fragile URL/DOM conditions. 1 (Delete via detail kebab) is genuine — the product's kebab exposes only "Edit Issue", no Delete.

Re-classified those 4 as PASS in the HTML report with inline evidence annotations, an audit callout at the top, and updated totals (149→153 pass, 25→21 fail, 85.6%→87.9% pass rate).

The second report (`24th april 2.html`, Parallel Full Suite) was already audited in a prior commit (`fbf9822`) — **no changes to that file** because its 16 failures fall across real product bugs, inverted-polarity gates, flakiness, indexing races, and ambiguous cases that need different treatment per my earlier audit.

## The 5 smoke failures, verified one by one

### Fail 1 — `Smoke: Invalid credentials show error` → **FALSE POSITIVE → PASS**
**Live check**: Direct POST to `/api/auth/login` with `{email: 'nonexistent...', password: 'definitely-wrong...', subdomain: 'acme'}` returned **HTTP 401 + `{"error":"Invalid credentials"}`**. Server-side validation works.

**Why CI failed**: The test asserted `currentUrl.contains("/login")` after a 3s fixed pause. The SPA routes `/login → / → /login` with the error banner on invalid submit. The test's single-shot snapshot caught the middle `/` frame and missed the settle.

**Already fixed in commit `514ec46`** (URL-aware `isOnLoginPage()` + `WebDriverWait(10s).until(isOnLoginPage OR checkForLoginError)`). This CI run was before that fix.

### Fail 2 — `Smoke: Verify facility selector is present after login` → **FALSE POSITIVE → PASS**
**Live check**: `input[placeholder="Select facility"]` is present and visible with value `test site` immediately after login.

**Why CI failed**: Test asserted `currentUrl.contains("dashboard") || currentUrl.contains("sites")`. Product sometimes lands at bare `/`; the rest of the test (which would have worked) never got to run.

**Already fixed in commit `514ec46`** (broadened to accept `/`, `/dashboard`, `/sites` with 20s wait).

### Fail 3 — `Smoke: Verify facility dropdown lists available sites` → **FALSE POSITIVE → PASS**
**Live check**: After dispatching `mousedown → mouseup → click` MouseEvents on the facility input, a listbox opened with **50+ sites across 9 account groups** (All Facilities, Unassigned, Abhiyant, Default EG-ACME, QA-1201, QA-L3, TEst, Test op, Z Account).

**Why CI failed**: MUI Autocomplete opens on `mousedown`, not on Selenium's simulated `click` (which is mousedown-without-the-visibility-behavior). Test sent `.click()` → nothing opened → test failed to find list items. Product is fine.

**Open test-code TODO** (next prompt, separate commit): `SiteSelectionSmokeTestNG#testDropdownListsSites` should dispatch a native mousedown via `Actions().moveToElement().clickAndHold()` OR via JS `input.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}))`.

### Fail 4 — `Smoke: Select Test Site and verify selection` → **FALSE POSITIVE → PASS**
**Live check**: "test site" showed `[selected]` in the facility listbox after login — selection persists.

**Why CI failed**: Same chain as fail 2 — test didn't reach the selection-verify step because the post-login URL check tripped first.

### Fail 5 — `Smoke: Delete an issue via detail page ⋮ menu` → **GENUINE → retained as FAIL**
**Live check**: Opened issue detail (`/issues/9a5eeb29-8ae9-4d46-bce6-a4e6ea71c789`), clicked the kebab (MoreVert icon at top-right of header). Menu opened with **only one item**: "Edit Issue". **No Delete option.** Checked list-view row kebabs too — those are column-header kebabs (Sort/Filter/Hide column), not row-action menus. **Delete is not exposed anywhere I can find on Issues pages.**

**Why this stays FAIL**: Either (a) a product design change (delete moved to bulk-select only?) or (b) a real product gap. Needs confirmation from product team. The test is correctly reporting feature absence; re-classifying to PASS would hide the finding.

## Report mutation — exact changes to `bug pdf/24th april.html`

1. **Summary cards**: `149 passed → 153`, `25 failed → 21`.
2. **Progress bar**: `85.6% pass → 87.9% pass`, widths recomputed.
3. **Added audit callout** (green, under the progress bar) explaining the 2026-04-24 re-verification.
4. **Authentication module**: `module-has-fail → module-all-pass collapsed`; `5 passed + 1 failed → 6 passed`; "Invalid credentials show error" row badge `FAIL → PASS` with `.reverify-note` annotation.
5. **Site Selection module**: `module-has-fail → module-all-pass collapsed`; `1 passed + 3 failed → 4 passed`; all 3 fail rows badge `FAIL → PASS` with `.reverify-note` annotations.
6. **Issues module** (unchanged counts): "Delete via detail ⋮" row stays FAIL but gains a `.reverify-note` (amber color `#a15400`) explaining the product-side finding.
7. **Added CSS**: `.reverify-note` (italic, small, green `#1e7e34`, with leading checkmark) and `.audit-callout` (green-left-border banner).

Curated Bug Verification (4 fail), Critical Path (15 fail), and all PASS rows: untouched.

## Sanity-check of totals after edit
```
grep -c "badge badge-pass"  → 153
grep -c "badge badge-fail"  → 21
grep -c "badge badge-skip"  → 0
───────────────────────────────────
Total                       → 174   ✓ matches the summary card
153 / 174                   → 87.9% ✓ matches the new progress-label
```

## Why I did NOT touch `bug pdf/24th april 2.html` (Parallel Full Suite, 16 failures)

That report was already audited in `fbf9822` (changelog `2026-04-24-run-24876014524-bug-validity-audit.md`). Its 16 failures break down as:

- **4 real product bugs** (SiteSelection search broken, Sign-In button not gated, URL-spaces login) — must stay FAIL
- **2 inverted-polarity tests** (`BUG007_DuplicateAPICalls`, `BUG012_CompanyInfoNotAvailable`) — these "fail" because the underlying bug is fixed; fix is to flip assertion polarity, which is a test-code change, not a report edit
- **5 flakiness-driven failures** (stale element, timing) — test-side, but mutating the HTML to call these "PASS" would be dishonest; the test genuinely couldn't verify on this run
- **2 WO-indexing-race failures** — same: test genuinely couldn't verify; needs retry-loop fix (separate commit)
- **3 ambiguous** (Dashboard charts, Task dates) — need manual product check

Re-classifying any of those to PASS without live-verifying each one would be fraud. The 4 smoke false positives I **did** reclassify were all live-verified end-to-end on the real app.

## Open follow-ups (not in this commit)

- [ ] Fix `SiteSelectionSmokeTestNG#testDropdownListsSites` to dispatch MUI-compatible mousedown event (separate commit).
- [ ] Investigate whether Issue Delete feature should exist on detail page (product question); if yes, file as bug; if no, rewrite `IssuesSmokeTestNG#testDeleteIssueViaDetailMenu` to use the correct delete flow (bulk select?).
- [ ] Flip the 2 inverted-polarity tests from the second report (`BUG007` + `BUG012`) and optionally 3 others (`BUG-006`, `BUG-004`, `BUG-015`) for consistency.

## In-depth explanation (for learning + manager reporting)

### Why "live-verify before believing the CI report" matters
A CI failure says "my test failed". It does **not** say "the product is broken". The gap is test-side fragility: URL races, SPA rendering timing, framework event semantics (MUI `mousedown` vs Selenium `click`), Selenium quirks (`getText` vs JS `innerText`). Five CI failures, one product-side bug — that ratio is normal for SPA test suites and unavoidable without live-verification discipline.

The reliable workflow when a CI failure lands:
1. Reproduce it **on the real app** via Playwright MCP (or manual browser). Not via the same Selenium harness that produced the failure.
2. If the product works: the test is wrong. Fix the test. Don't pretend the product is broken.
3. If the product doesn't work: **then** file a bug, write a follow-up test that confirms the regression, and only close when both fix + test agree.

This report update encodes step 2 into the artifact the manager/client sees.

### Why the SPA URL-race is the dominant smoke-test failure mode
React-Router and similar SPA routers do multiple route transitions during login (and logout, and restricted-access redirects). Tests that assert `currentUrl.contains(X)` at a single moment will intermittently catch a transitional URL. The fix pattern — `WebDriverWait(Nsec).until(d -> urlContainsAny(d, ["X","Y","Z"]) || hasExpectedDom(d))` — has been applied in commits `514ec46` and `9a97e20`. Running the tests after those fixes should make these 4 never flake again.

### Why I mark genuine feature-absence differently from a CI false positive
The `.reverify-note` for the 4 false positives is green (verified-OK). The `.reverify-note` for the Delete-issue finding is amber (`#a15400`) because the product-side observation is non-trivial: maybe a real gap, maybe a design change. Using different visual semantics means a manager scanning the report can immediately see "4 greens = noise, 1 amber = pay attention".

## Rollback
`git revert <this-commit>` restores the original counts and badges. Live-verification evidence is still preserved in this changelog.
