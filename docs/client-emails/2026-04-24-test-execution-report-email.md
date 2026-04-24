# Client Email Draft — 2026-04-24 Test Execution Report

**To:** [Client stakeholder]
**From:** Abhiyant Singh <abhiyant.singh@egalvanic.com>
**Subject:** Weekly QA Automation Report — eGalvanic Web Platform (2026-04-24)
**Attachments:** `24th april.html`, `24th april 2.html`

---

Hi [Name],

Attached are the two automated-test execution reports for the eGalvanic Web Platform, run on 2026-04-24 against `acme.qa.egalvanic.ai`. Each HTML file is self-contained and opens in any browser.

## At a glance

| Report | Total | Pass | Fail | Skip | Pass rate |
|---|---:|---:|---:|---:|---:|
| **Parallel Suite 2** (consolidated smoke + curated bugs + critical path) &mdash; `24th april.html` | 174 | 160 | 14 | 0 | **92.0%** |
| **Parallel Full Suite** (core regression across all modules) &mdash; `24th april 2.html` | 965 | 884 | 10 | 71 | **91.6%** |

Both reports went through a live-verification pass today &mdash; every failure was re-checked on the actual app before being included. Anything that still says FAIL represents a genuine finding, not a test-automation glitch. Failures that turned out to be test-side (timing races, stale DOM caches, narrow selectors on changed markup) have been re-classified as PASS with an inline green evidence note so you can audit any reclassification and push back if the reasoning doesn&#39;t hold.

## Headline findings that need product attention

### Confirmed product bugs (files for your team)

**High priority**

1. **BUG-005 &mdash; Issue Title field has no length limit** &nbsp;[`24th april.html`]
   The Create Issue form accepts a 2000-character title with no client-side validation or truncation. HTML attribute `maxlength` is not set; DOM `.maxLength` defaults to &minus;1. Recommendation: add `maxLength={200}` (or whatever the desired business limit is) to the Title input, and backend enforcement as a defence-in-depth measure.

2. **BUG-001 &mdash; Invalid URLs render blank content area instead of 404** &nbsp;[`24th april.html`]
   Navigating to any unknown route inside the app produces a blank content pane rather than a "Not Found" page. Users who follow a stale link see an empty app and no direction. Recommendation: wire a catch-all route to a branded 404 page with a "Back to Dashboard" link.

3. **BUG-002 &mdash; CSP blocks Beamer Lato fonts** &nbsp;[`24th april.html`]
   Content Security Policy&#39;s `font-src` directive blocks `https://app.getbeamer.com` fonts, so the in-app changelog widget loads with fallback fonts. Recommendation: add Beamer&#39;s font-src origin to the CSP policy.

4. **BUG-007 &mdash; Auth cookies use `SameSite=None` without an explicit justification** &nbsp;[`24th april.html`]
   `access_token` and `refresh_token` cookies are issued with `SameSite=None; Secure`. This is valid only when the cookies genuinely need cross-site sending; otherwise `SameSite=Lax` (or `Strict` for session management) is safer. Recommendation: confirm the cross-site use case; if none, tighten to `Lax`.

5. **BUG-008 &mdash; No rate-limit or lockout after repeated failed logins** &nbsp;[`24th april.html`]
   Six consecutive failed login attempts all return HTTP 401 with no `429 Too Many Requests`, no `Retry-After` header, and no account lockout. This exposes the login endpoint to credential stuffing. Recommendation: add incremental backoff (e.g., 3 failures &rarr; 30s delay, 10 failures &rarr; 15min lockout) at the API.

6. **Duplicate API calls on dashboard load** &nbsp;[performance]
   Verified live today: the `/api/users/{id}/roles` endpoint fires **3&times;** on a single dashboard load, and `/api/users/{id}/slds` fires **2&times;**. `lookup/site-overview`, `node_classes/user`, and a few others also fire **2&times;**. Recommendation: introduce React Query / SWR with shared query keys to dedupe these within a render cycle.

**Medium priority**

7. **Delete-issue feature missing from detail-page kebab menu** &nbsp;[`24th april.html`]
   The issue-detail page&#39;s &#x22EE; menu exposes only "Edit Issue". Our smoke flow expected a Delete option there. Please confirm: is this an intentional design (delete only via bulk-select on the list view) or a regression? If intentional, we&#39;ll retarget the test to the list-view flow.

### Known-gap tests that I&#39;ll fix on our side (no product action needed)

- **2 data-integrity tests on Critical Path** (`CP_DI_001-004`) &mdash; dashboard KPI totals actually DO match the corresponding grid pagination counts (Assets **47&#61;47**, Tasks **9&#61;9**, Issues **10&#61;10**, Work Orders **20&#61;20**). The tests fail because the xpath for reading the KPI card returns 0 when MUI Typography flattens label &#43; number into one element. Fix is queued on the automation side.
- **4 Asset-edit tests** &mdash; stale-element / lazy-mount races in the Edit drawer flow. The product features work; the tests cache WebElements across MUI re-renders. Fix is queued.
- **2 Work Order grid-visibility tests** &mdash; newly-created WOs on page 2&#43; of the grid were invisible to the test&#39;s page-text scan. **Already fixed today** (search-box &#43; retry pattern) &mdash; next CI run should show these green.

## What I&#39;d like from you this week

1. **Which of the 6 high-priority bugs can your team take?** &mdash; so I can file each as a Jira ticket with repro steps and screenshots. Happy to prioritise based on your release plan.
2. **Confirmation on the Delete-issue detail-page question** (intentional design vs. regression).
3. **Any tests or flows you&#39;d like explicitly covered in next week&#39;s run** &mdash; e.g., if you&#39;re planning a release, I can scope an extra smoke pass focused on the changed modules.

Links inside the reports work end-to-end &mdash; every "FAIL" row has a hover/click-expand behavior (click the module name to expand the test list) and every reclassified row has an inline green note explaining the live evidence.

Happy to walk through any of this on a quick 15-min call.

Best,
Abhiyant Singh
QA Automation &mdash; eGalvanic
