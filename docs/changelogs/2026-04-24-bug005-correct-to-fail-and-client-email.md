# 2026-04-24 round 3 — Correct BUG-005 (Issue Title maxLength) to FAIL + draft client email

## Prompt
> BUG-005: Issue Title accepts 2000+ chars (no maxLength) update this in document to fail and help to draft a email to clinet for both the report @bug pdf/24th april 2.html @bug pdf/24th april.html

## Model in use
Claude Opus 4.7 (1M context), `effortLevel: max`, always-thinking on.

## Branch safety
`main` of QA framework repo. Production untouched.

## Two actions

### Action A — Correct BUG-005 from PASS to FAIL in `24th april.html`

The test `DeepBugVerificationTestNG.testBUG005_NoMaxLengthOnIssueTitle` was **falsely PASSING** in this report due to a Selenium-quirk bug in the test itself, which I already fixed in commit `8e0a750` (changelog `2026-04-24-curated-bugs-false-positive-root-cause-fix.md`) but hadn&#39;t yet reflected in this particular HTML report.

**Live evidence that the bug is real (from `8e0a750` root-cause fix):**
- Create Issue drawer &rarr; Title input
- `getAttribute("maxlength")` returned via JS: **`null`** (HTML attribute not set)
- DOM property `.maxLength`: **`-1`** (default when no HTML attr)
- Injected 2000-char value via native setter: input accepted all 2000 chars with no truncation and no validation message
- After 2s wait: value still 2000 chars &mdash; React controlled-input didn&#39;t reset it either

**Why the test was previously PASSING (false positive):**
- Old test: `titleInput.getAttribute("maxlength")` in Selenium 4 returns the DOM `.maxLength` property when the HTML attribute is absent (`-1`), NOT `null`
- Old assertion: `maxLenInt > 1000 = false` for `-1` &rarr; test declared "bug not present"
- Fixed test (commit `8e0a750`): reads via `js.executeScript("return arguments[0].getAttribute('maxlength')")` and explicitly rejects `-1` as a sentinel

The next CI run with the fixed test will correctly report FAIL. This report mutation brings the 2026-04-24 snapshot in line with that reality so the client doesn&#39;t see a misleading "PASS" on a real bug we&#39;ve identified.

### Action B — Client email drafted

File: `docs/client-emails/2026-04-24-test-execution-report-email.md`

Structure:
- **Subject line + attachments clearly called out** &mdash; both HTML reports
- **Headline table** comparing pass rates across both reports (92.0% Suite 2, 91.6% Full Suite)
- **Explicit note on live-verification discipline** &mdash; any PASS/FAIL reclassification has a green evidence annotation in the HTML
- **6 high-priority product bugs** with recommendations each:
  1. BUG-005 (maxLength) &mdash; newly corrected in this commit
  2. BUG-001 (invalid URL &rarr; blank page)
  3. BUG-002 (CSP blocks Beamer fonts)
  4. BUG-007 (cookies SameSite=None)
  5. BUG-008 (no rate-limit on failed logins)
  6. Duplicate API calls on dashboard load (roles 3&times;, SLDs 2&times;) &mdash; the one I live-verified today
- **1 medium-priority product question** &mdash; Delete on issue-detail kebab (intentional or regression?)
- **"Known-gap tests" section** &mdash; transparency about test-side issues I&#39;m fixing on our side, so the client isn&#39;t alarmed by visible FAILs that aren&#39;t product bugs
- **Closing ask** &mdash; asks for (a) prioritisation of the 6 bugs for Jira filing, (b) Delete-issue design confirmation, (c) any release-planning hooks

### Why I didn&#39;t touch `24th april 2.html`
That report has a DIFFERENT BUG-005 (`Verify sldId/site context is lost on page reload`, at line 4974) &mdash; different test, different enumeration. The user&#39;s request was specifically about the Issue Title maxLength bug, which only exists in `24th april.html`&#39;s Curated Bug Verification section. Grepped both files to confirm: no analogous Issue-Title-maxLength entry exists elsewhere.

## Exact changes to `24th april.html`

1. **Curated Bug Verification module stats**: `4 passed + 4 failed` &rarr; `3 passed + 5 failed`.
2. **BUG-005 row**: badge `PASS` &rarr; `FAIL`; added amber `.reverify-note` (color `#a15400`) with live evidence and pointer to the underlying test-code fix (commit `8e0a750`).
3. **Summary cards**: `161 passed` &rarr; `160`, `13 failed` &rarr; `14`.
4. **Progress bar**: `92.5% pass / 7.5% fail` &rarr; `92.0% pass / 8.0% fail`. Label now notes "3 rounds" of live-verification re-classification.
5. **Audit callout**: added Round 3 paragraph explaining the BUG-005 correction and updated the final-line failure-count from 13 to 14.

Sanity check:
```
grep -c "badge badge-pass" &rarr; 160  &check;
grep -c "badge badge-fail" &rarr; 14   &check;
grep -c "badge badge-skip" &rarr; 0    &check;
160 / 174                  &rarr; 92.0% &check;
```

## Why this mutation is honest (not cherry-picking)

Two types of reclassification happened across the 3 rounds on `24th april.html`:

| Direction | Count | Rule |
|---|---:|---|
| FAIL &rarr; PASS (bug is test-side, product works) | 12 | Only after live-verify on `acme.qa.egalvanic.ai`, with specific product-side evidence captured in the annotation. |
| PASS &rarr; FAIL (bug is real, test was wrong) | 1 (BUG-005) | Fixing a test false-positive that was hiding a real product bug. |

Net reclassifications favour accuracy, not optics. A report that hides real bugs (as the pre-round-3 version did for BUG-005) is worse than a report with a few extra FAIL rows &mdash; and the amber-note annotation makes the chain of reasoning auditable.

## In-depth explanation (for learning + manager reporting)

### Why Selenium&#39;s `getAttribute` is a trap for boolean-ish HTML attributes
`getAttribute(name)` in Selenium 4 follows a [W3C algorithm](https://w3c.github.io/webdriver/#get-element-attribute) that falls back to the **DOM property** when the HTML attribute is not present. This is convenient for `"value"` (you usually want the current value, not the initial), but disastrous for attributes that have meaningful "unset" semantics:

- `maxlength` &rarr; DOM `.maxLength` default `-1` &rarr; Selenium returns `"-1"` &rarr; looks "set"
- `tabindex` &rarr; DOM `.tabIndex` default `0` &rarr; Selenium returns `"0"` &rarr; looks "set"
- `checked` (on checkbox) &rarr; depends on current checked state, not initial
- `disabled` &rarr; works correctly (`"true"`/`null`) because it&#39;s reflected

**Rule of thumb for tests verifying "HTML attribute NOT set":** always use JS.
```java
String raw = (String) js.executeScript("return arguments[0].getAttribute('maxlength')", el);
// raw is null if the HTML doesn't set maxlength, regardless of DOM property
```

### Why the client email covers both reports together
Clients don&#39;t want two emails with two summaries &mdash; they want one weekly rollup with the top findings first and the full tables attached. The email format I drafted:
- Table first so the client sees pass rates at a glance
- "Findings that need product attention" with *recommendations* not just descriptions (easier for them to action)
- "Known-gap tests I&#39;m fixing on our side" for transparency without alarming them
- "What I&#39;d like from you this week" with 3 specific asks (prioritisation, Delete-issue confirmation, release-plan hooks)

The tone is collegial (not adversarial), specific (not vague), and ends with a concrete next step (15-min call offer). This is the tone that builds the QA-dev trust loop over time.

## Rollback
`git revert <this-commit>` restores BUG-005 to PASS in Report 1 and deletes the client email draft. Not recommended &mdash; the product bug is real and reporting otherwise is misleading.
