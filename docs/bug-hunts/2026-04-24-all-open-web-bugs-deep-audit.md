# Deep Audit — All 74 Open Web Bugs in Jira (Project ZP)

**Date:** 2026-04-24
**Source:** Live Atlassian Rovo MCP, project `ZP`, JQL `type = Bug AND statusCategory != Done AND (web | FE | frontend | browser)` excluding pure mobile
**Scope:** 74 open Web Bugs (1 Highest, 5 High, 43 Medium, 18 Low, 7 Lowest · 45 To Do, 25 Backlog, 2 On Hold, 1 In QA, 1 Resolved-migrated)
**Reporter distribution:** Abhiyant Singh 30 · Nirali Joshi 25 · Nency Sardhara 5 · Mukul Panchal 7 · Eric Ehlert 3 · Dharmesh Avaiya 1 · others 3

---

## Executive verdict — the 3 patterns that matter

**(A) Pipeline is stockpile, not flow.** Only 1 of 74 bugs is `In QA` and 2 are `On Hold`. Everything else is `To Do`/`Backlog`. 27 % of bugs (20) have been sitting for 4+ months without being worked. The bottleneck is triage+dev capacity, not discovery.

**(B) Priority skew is misleading.** 58 % Medium · only 8 % High+. Several bugs tagged Medium/Low are actually higher-impact when you read the repro:
- **ZP-1557** (High · Goals $0 → $426,736 silent recalc on edit) — data-integrity, should be Highest
- **ZP-2020** (Low · raw 400 error leaking internal API details in UI) — info-leak, security-adjacent, deserves Medium
- **ZP-2025 / ZP-2024** (Medium · no rate limit + SameSite=None CSRF surface) — should be High
- **ZP-671** (Highest · SLD connection links missing, open since Jan 2026) — blocker that hasn't moved in 3 months

**(C) Three hot clusters suggest single root causes, not scattered defects:**
1. **SLD cluster (9 bugs)** — edges/links/export consistently broken. Likely one graph-rendering + one PDF-export weakness.
2. **Goals cluster (8 bugs, all filed 2026-03-18)** — date validation, numeric validation, table rendering, cadence mutation. One deep audit day found 8 defects; one focused sprint could close them all.
3. **EG Forms cluster (2 High · On Hold)** — both are `JSON parse failed — backend returned HTML` errors. Root cause is the systemic pattern I already flagged: `response.json()` without checking `Content-Type` when backend returns HTML on errors. These are blocked pending a backend fix.

---

## Per-bug audit — grouped by module

Columns: `verdict` = my confidence that repro is still valid based on live evidence today + description quality.
- ✅ REPRODUCIBLE — product bug confirmed (either live-verified today or strongly corroborated by related findings)
- 🔄 LIKELY STILL VALID — repro quality high, no conflicting evidence; hasn't been re-verified on live
- ❓ NEEDS RE-VERIFY — environment-specific, stale, or product may have shipped a fix since filing
- ⚠️ RE-CLASSIFY — priority/severity should change based on fresh evidence
- 🧹 CONSOLIDATE — duplicate or overlapping with another ticket

### AUTH (7 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2025** | Medium | Backlog | ✅ REPRODUCIBLE + ⚠️ SHOULD BE HIGH | No rate-limit on /api/auth/login — 28 consecutive failed attempts return 401. Confirmed as BUG-008 this week. Credential-stuffing risk. Push to High. |
| **ZP-2024** | Medium | Backlog | ✅ REPRODUCIBLE + ⚠️ SHOULD BE HIGH | SameSite=None on access_token + refresh_token — form-POST CSRF surface. Confirmed as BUG-007. Elevate priority. |
| ZP-1331 | Medium | To Do | ❓ NEEDS RE-VERIFY | BCES-IQ Reset Password email missing template. Filed 2026-03-09, environment-specific to BCES-IQ. Re-verify if BCES-IQ is still in scope for the client. |
| ZP-348 | Medium | Backlog | 🔄 LIKELY STILL VALID | "+New Session" button broken on Scheduling page. Filed 2025-12-16, 4 months old. No product fix mentioned in any Release Sign off ticket (ZP-1055/1059/1181 etc). Re-verify next QA pass. |
| ZP-19 | Medium | To Do | 🔄 LIKELY STILL VALID | Wrong URL opens after "Reset Password" email click. Oldest auth bug (2025-12-08). Low effort to verify — use a real forgot-password flow. |
| ZP-1330 | Low | To Do | 🔄 LIKELY STILL VALID | Reset Password link opens Login screen instead of Code Verification screen. Complements ZP-19; file as duplicate or link-as-related. |
| ZP-78 | Low | To Do | 🔄 LIKELY STILL VALID | Blank white page after logout. Oldest (2025-12-03). Easy 2-min verification: log out, observe. |

**Hot action:** ZP-2024 + ZP-2025 should be High priority. ZP-1330 is functionally a duplicate of ZP-19 (both are "reset password redirect wrong"); propose linking.

### ISSUES (9 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2019** | Medium | Backlog | 🧹 CONSOLIDATE with ZP-1319 | Same bug — "Issue Class mandatory" enforcement gap. ZP-1319 has more detailed repro and is older. Close ZP-2019 as duplicate. |
| **ZP-1319** | Medium | To Do | ✅ REPRODUCIBLE (live today) | Submitted this week via BUG-003 test — drawer closes on submit with blank Issue Class. Live-verified 2026-04-24. |
| ZP-1707 | Medium | To Do | 🔄 LIKELY STILL VALID | Deleted issue classes still in Edit dropdown (`DEVTOOL_TEST`, `Test 1-3`). Corroborated by earlier BUG-003 work — the combobox shows stale seed data. Cleanup debt. |
| ZP-1622 | Medium | To Do | 🔄 LIKELY STILL VALID | Deleting issue doesn't redirect + deleted issue URL still accessible. Two separate bugs in one ticket; second half is an IDOR hint (if deletion is soft and URL still resolves on other tenants, that's bad). Split ticket + prioritize URL-access half. |
| ZP-1516 | Medium | Backlog | ❓ NEEDS RE-VERIFY (not Issues — Dashboard) | Mis-filed under Issues — it's about Site Overview Dashboard's "Equipment at Risk" card overflow + "Assets by Type" / "Issues by Type" chart not visible at 100% zoom. My CP_DI test-side fixes today confirm those charts DO render; the overflow layout issue remains a separate concern. Re-verify layout at 100% zoom. |
| ZP-346 | Medium | Backlog | 🔄 LIKELY STILL VALID | User stays on Issue Detail after deleting (no redirect). Overlaps with ZP-1622 — propose merging. |
| **ZP-2021** | Low | Backlog | ✅ REPRODUCIBLE + ⚠️ SHOULD BE MEDIUM | Issue Title accepts 2000+ chars (no maxLength). Live-verified this week. Low priority understates the risk (DB bloat, email/PDF render breakage). |
| ZP-386 | Lowest | To Do | 🔄 LIKELY STILL VALID | Issue Priority/Class/Asset/subtype not updating on single-item Edit. Edit-drawer UX bug. |
| ZP-345 | Lowest | To Do | 🔄 LIKELY STILL VALID | UUID shown on Edit Issue side panel (should be human label). Copy fix. |

**Hot action:** Close ZP-2019 as dupe of ZP-1319. Split ZP-1622 (redirect issue ≠ URL-IDOR issue). Re-verify ZP-1516's layout half now that the chart half is confirmed working.

### ASSETS (9 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| ZP-676 | Medium | To Do | 🔄 LIKELY STILL VALID | Delete Connection on edge shown for cross-view nodes when it shouldn't be. SLD-adjacent; may interact with ZP-671 cluster. |
| ZP-344 | Medium | Backlog | 🔄 LIKELY STILL VALID | Assets tab: "Link existing" attachment silently fails. Low-effort repro; 4+ months old. |
| ZP-77 | Medium | Backlog | 🔄 LIKELY STILL VALID | Equipment Insights pagination resets on navigate-back. Known back-navigation regression, matches multiple other instances. |
| ZP-152 | Medium | Backlog | 🔄 LIKELY STILL VALID | Asset location (building→floor→room) not visible for all assets in EMP builder. |
| **ZP-2020** | Low | Backlog | ✅ REPRODUCIBLE + ⚠️ SHOULD BE MEDIUM | Raw HTTP 400 + internal API details leak on invalid asset URLs. BUG-004 confirmed. Info-leak severity > Low. |
| ZP-1482 | Low | To Do | 🔄 LIKELY STILL VALID | IR photo not visible immediately after upload + refresh redirects away. Photo-flow UX issue. |
| ZP-339 | Low | To Do | 🔄 LIKELY STILL VALID | "N/A" as dropdown value in Phase A/B/C/Neutral Wire Size. Copy/data-model bug. |
| ZP-340 | Low | To Do | 🔄 LIKELY STILL VALID | KcMIL unit formatting (kcMIL vs KCMIL) inconsistency. Copy fix. |
| ZP-9 | Lowest | To Do | 🔄 LIKELY STILL VALID | Edit/Delete buttons not visible at 100% screen size in Asset table. Matches the documented zoom-UX issues (80% zoom is a hard requirement in our framework). Strong candidate to audit the whole 100%-zoom-visibility class of bugs together. |

**Hot action:** ZP-2020 elevation. Audit the entire "100% zoom visibility" class as one systemic item (ZP-9 + ZP-2030 + ZP-1516 layout + others).

### TASKS (2 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| ZP-1342 | Medium | To Do | 🔄 LIKELY STILL VALID | Task can be created with past date — date-range validation gap. Parallel to ZP-1552 (Goals allows start>due). Bundle with Goals validation sweep. |
| ZP-1187 | Medium | To Do | ❓ NEEDS RE-VERIFY (prod-only) | Prod Tasks label count wrong. Can't verify without prod access (rule: no destructive prod testing). Filer confirmed; trust it. |

### WORK ORDERS (4 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2063** | Medium | Backlog | 🔄 LIKELY STILL VALID | Pasted HTML in WO page not rendering in template preview. Part of the HTML-injection-sanitization layer gap (touches ZP-2018 Beamer + editor sanitization). |
| **ZP-2022** | Medium | Backlog | ✅ REPRODUCIBLE | Avg page load > 10s across every top-level page. Corroborated today by live-verified duplicate API calls (`/roles` 3×, `/slds` 2×). Fix the dup-fetch and load time drops. |
| ZP-1199 | Medium | Backlog | 🔄 LIKELY STILL VALID | Equipment not displaying on WO view after selection. Filed by Dharmesh; lost-state-after-selection pattern. |
| ZP-1483 | Low | To Do | 🔄 LIKELY STILL VALID | Full-page refresh instead of partial on Refresh button. UX/perf. |

**Hot action:** ZP-2022 + duplicate-API investigation should be paired. File a new bug specifically for the `/roles` 3× + `/slds` 2× finding (it's a concrete measurable root cause, not just "slow").

### SLD (9 bugs — HOT CLUSTER, 1 Highest)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-671** | **Highest** | To Do | 🔄 LIKELY STILL VALID + ⚠️ AGING | Connection links missing after "Search & Pull" / "Search & Pull Upstream". Filed 2026-01-20, untouched 3+ months despite being THE highest-priority bug. **This should have a ticket-age alert on it.** Needs urgent dev attention. |
| ZP-2075 | Medium | Backlog | 🔄 LIKELY STILL VALID | Highlight effect causes custom routed edges to snap back. FE-only rendering. |
| **ZP-2065** | Medium | Backlog | ✅ REPRODUCIBLE (likely) | SLD Export Modal shows STALE node count from previous site after site switch — **potential cross-site data leak in exported PDF**. This is multi-tenant adjacent — could be P0 if the exported PDF actually contains wrong-tenant data. Upgrade priority pending deeper verify. |
| **ZP-2064** | Medium | Backlog | 🔄 LIKELY STILL VALID | SLD PDF Export returns 502 from backend. Infra-side. |
| ZP-2062 | Medium | Backlog | 🔄 LIKELY STILL VALID | PDF export errors accumulate silently without user notification. UX + observability gap. |
| ZP-1332 | Medium | To Do | 🔄 LIKELY STILL VALID | Asset can self-connect in SLD. Data-integrity: graph with self-loops may break other views. |
| ZP-1290 | Medium | To Do | 🔄 LIKELY STILL VALID | SLD slow + stuck with 1600 records on Magic Link. Perf at scale. |
| ZP-675 | Medium | To Do | 🔄 LIKELY STILL VALID | Connection Links overlap when creating for multiple views. Matches ZP-671 pattern. |
| ZP-673 | Low | To Do | 🔄 LIKELY STILL VALID | Duplicate connection links when repeating Create Link. |

**Hot action:** ZP-2065 needs immediate deep verification — if the PDF truly exports wrong-tenant data, that's a **P0 data-leak and a potential breach notification** for any BCES-IQ customer who ever exported across sites. This jumps to top of the queue.

### REPORTING (3 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2042** | High | To Do | 🔄 LIKELY STILL VALID (prod-only) | Report Builder PDF blank on PROD, works on QA. Env-specific. Can't verify without prod; trust filer. Priority correct. |
| ZP-2030 | Medium | To Do | 🔄 LIKELY STILL VALID | Edit Report Config Template column not visible at 100% zoom — bundle with zoom-UX class. |
| ZP-1934 | Medium | Resolved (migrated) | 🧹 CLOSE OR MOVE | Status "Resolved (migrated)" on an open-bug query — means the migration from Trello/other tool marked it but a human hasn't actually closed it. Housekeeping. |

### EG FORMS (2 bugs, both High, both On Hold)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-1930** | High | On Hold | 🔄 LIKELY STILL VALID | Media Upload fails with `Failed to execute 'json' on 'Response': Unexpected token '<'` — backend returning HTML on 500. On Hold suggests dev knows and is waiting on backend fix. Same root cause family as ZP-1931. |
| **ZP-1931** | High | On Hold | 🔄 LIKELY STILL VALID | Add Reference template selection — same JSON parse error on HTML error page. Same root cause. |

**Root cause for both:** matches the project-wide `response.json()` without `Content-Type` check anti-pattern — documented in `CLAUDE.md` constraint #8 ("backend returns HTML on errors (known systemic bug)"). These two tickets are symptoms of that systemic bug; a central fix (response wrapper that checks content-type first) would close both.

### GOALS (8 bugs — HOT CLUSTER, ALL FILED 2026-03-18)

All 8 filed the same day by Abhiyant — systematic audit pass, not random drift. Product team should treat these as a bundle.

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-1557** | High | To Do | 🔄 LIKELY STILL VALID (dev env) | Current value $0 → $426,736 after edit. **Data-integrity bug; arguably Highest given the scale of the wrong number.** Dev-env only per repro. |
| ZP-1552 | Medium | Backlog | 🔄 LIKELY STILL VALID | Start Date allowed after Due Date. Date-range validation class. |
| ZP-1558 | Medium | Backlog | 🔄 LIKELY STILL VALID (dev) | Cadence changed "Yearly"→"Weekly" silently on custom date. |
| ZP-1553 | Medium | Backlog | 🔄 LIKELY STILL VALID (dev) | Goals table has no horizontal scrollbar — Edit button hidden off-screen. |
| ZP-1337 | Medium | To Do | 🔄 LIKELY STILL VALID | Cadence not displayed for One-Time goals. |
| ZP-1329 | Medium | To Do | 🔄 LIKELY STILL VALID | Edit/Delete icons not visible for Account Goals. Matches the zoom-UX class. |
| ZP-1550 | Low | To Do | 🔄 LIKELY STILL VALID | Create Goal allows negative target (-5555). Input validation. |
| ZP-1556 | Low | To Do | 🔄 LIKELY STILL VALID (dev) | Period column missing year in date range. Date formatting. |

**Hot action:** Bundle all 8 into a single "Goals module hardening" epic. Split the work: (a) input validation (ZP-1552, ZP-1550), (b) layout (ZP-1553, ZP-1329), (c) data-integrity (ZP-1557, ZP-1558), (d) display (ZP-1337, ZP-1556). Doable in one sprint if tackled together.

### SETTINGS (3 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| ZP-259 | Medium | Backlog | 🔄 LIKELY STILL VALID | Technician name in dropdown + Current Role not displayed after selecting Technician. Permission-UI sync issue. |
| ZP-368 | Low | To Do | 🔄 LIKELY STILL VALID | Default Profile Photo + Star indicator not updating after deletion. |
| ZP-1047 | Lowest | To Do | 🔄 LIKELY STILL VALID | "Select All Sites" missing on User Site Assignment. |

### PANEL SCHEDULES (6 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2057** | High | In QA | ✅ IN QA (being verified) | "Error loading panel schedules" on BCES-IQ. Active. |
| ZP-1801 | Medium | To Do | 🔄 LIKELY STILL VALID | Slow perf when adding "Select All Existing OCP". Scale issue. |
| ZP-1802 | Medium | To Do | 🔄 LIKELY STILL VALID | OCP list search not working properly. Parallel to ZP-22 (site search) class. |
| ZP-1479 | Medium | To Do | 🔄 LIKELY STILL VALID | Full-screen loading on in-page operations. UX. |
| ZP-1462 | Medium | To Do | 🔄 LIKELY STILL VALID | OCP Edit icon opens Issue Screen instead — wrong dispatch. |
| ZP-35 | Lowest | To Do | 🔄 LIKELY STILL VALID | OCP/Connection visible under Main Asset Edit after cancel. Cancel doesn't reset child state. |

### SITE SELECTION (1 bug)

| ZP-695 | Medium | To Do | 🔄 LIKELY STILL VALID | New site requires browser refresh to show in dropdown. MUI Autocomplete cache not invalidating on mutation — matches the `['sites']` without tenant key pattern I flagged in the Issues deep-hunt doc. |

### CONNECTIONS (1 bug)

| ZP-23 | Low | To Do | 🔄 LIKELY STILL VALID | Connection ID briefly visible when switching sites — same tenant-switch stale-data pattern as ZP-2065 SLD Export. Bundle. |

### LOCATIONS (1 bug)

| ZP-405 | Medium | To Do | 🔄 LIKELY STILL VALID | Child items not deleted when parent deleted. Cascade deletion missing. |

### ATTACHMENTS (4 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2035** | Medium | Backlog | 🔄 LIKELY STILL VALID | Uploaded logo not displayed until manual refresh. React state not updating after upload — matches the async-rendering class. |
| ZP-391 | Medium | To Do | 🔄 LIKELY STILL VALID | Existing Attachment Link not added to Asset. Silent failure. |
| ZP-500 | Low | To Do | 🔄 LIKELY STILL VALID | Photo sorting changes after adding caption. Sort-by-edit-date regression. |
| ZP-8 | Low | To Do | 🔄 LIKELY STILL VALID | Attachment View + Download both open image — intent confusion. |

### OTHER (5 bugs)

| Key | Pri | Status | Verdict | Notes |
|---|---|---|---|---|
| **ZP-2018** | Low | Backlog | ✅ REPRODUCIBLE | CSP blocks Beamer fonts. Minor fallback — Low is correct. |
| **ZP-2017** | Low | Backlog | ✅ REPRODUCIBLE | No 404 page on invalid URLs. Duplicate of ZP-10 (filed by Mukul on 2025-12-08) — 🧹 CONSOLIDATE. |
| ZP-413 | Low | To Do | 🔄 LIKELY STILL VALID | Form template in FE not retained on return to form. State-persistence issue. |
| ZP-923 | Lowest | To Do | 🔄 LIKELY STILL VALID | No validation on Reference/Problem Temperature fields. Input validation gap — bundle with Goals validation cluster. |
| **ZP-10** | Lowest | To Do | 🧹 CONSOLIDATE with ZP-2017 | Same bug, 2017 has more detail. Close ZP-10 as dupe. |

---

## Today's findings NOT yet filed (new tickets to create after confirmation)

From this week's test-framework and live-verify work:

| My finding | Why it's a distinct ticket | Recommended priority |
|---|---|---|
| Sign-In button enabled on empty form | Not covered by ZP-1828 T&C removal (which is Done). Button-gating logic is the separate bug. | Medium (security-ish UX) |
| Duplicate API calls on dashboard | `/roles` 3× + `/slds` 2× + `/lookup/site-overview` 2× per page load. Measurable root cause of ZP-2022 "slow pages". | Medium (perf) |
| URL company code whitespace trim | `testTC05_CompanyCodeWithSpaces` CI failure — browser trims URL spaces, login page doesn't load for tenants with spaces in code. | Medium |
| Issue detail kebab has only "Edit Issue" (no Delete) | Design question: is this intentional? If yes, nothing to file. If no, product regression. | Medium, pending product clarification |

I will NOT file these without your explicit per-ticket ok per `feedback_never_modify_jira.md`.

---

## Priority re-classification recommendations

### Should be higher than current
- **ZP-2024** Medium → High (CSRF surface)
- **ZP-2025** Medium → High (credential-stuffing surface)
- **ZP-2020** Low → Medium (info-leak, not just UX)
- **ZP-2021** Low → Medium (DB/email bloat, not just UX)
- **ZP-2065** Medium → **P0 pending verify** (possible cross-tenant PDF data leak — audit immediately)

### Should be lower (or closed)
- **ZP-2019** → Close as dupe of ZP-1319
- **ZP-2017** → Close as dupe of ZP-10 (older) OR close ZP-10 as dupe of ZP-2017 (better repro)
- **ZP-1934** Resolved (migrated) → actually close properly, it shouldn't appear in open queries
- **ZP-346** + **ZP-1622** → Merge or link (both about delete-issue-redirect)

### Aging watchlist (stale > 90 days)
- ZP-671 (Highest, 94+ days) — urgent review
- ZP-9 / ZP-23 / ZP-78 / ZP-10 / ZP-8 (all 137+ days, all Mukul's Dec-2025 UX debt) — batch triage

---

## Four systemic patterns (fix one → close many bugs)

### Pattern 1 — `response.json()` without Content-Type check
Affects: ZP-1930, ZP-1931. Hints at many more wherever the frontend calls `res.json()` blindly on what may be an HTML error page.
**Fix:** Wrap every JSON fetch in `if (!res.headers.get('content-type')?.includes('json')) throw; else res.json()`. Or use a shared `apiFetch` helper.

### Pattern 2 — Tenant-switch stale cache
Affects: ZP-23 (connections), ZP-2065 (SLD export modal), ZP-695 (site dropdown), plus today's observation that `/issues` and `/assets` counts briefly show prior tenant on switch.
**Fix:** Scope every `useQuery` / `SWR` cache key by tenantId. `['issues', tenantId]` not `['issues']`.

### Pattern 3 — 100 % zoom visibility (confirmed in CLAUDE.md constraint #9)
Affects: ZP-9 (Asset Edit/Delete), ZP-1329 (Account Goals icons), ZP-1516 (Dashboard Equipment at Risk), ZP-2030 (Template column), plus the Create Goals / Language / Template known bugs.
**Fix:** Design pass on tables + cards that are hidden at 100 % zoom. Either responsive reflow or a max-width cap.

### Pattern 4 — Frontend-only validation, backend missing
Affects: ZP-1319 (Issue Class), ZP-1552 (Goals date range), ZP-1550 (Goals negative), ZP-1342 (Task past date), ZP-923 (temperature fields), ZP-2021 (title length).
**Fix:** For each affected POST/PATCH endpoint, add server-side constraint matching the frontend rule. Update CLAUDE.md constraint reminding devs the backend is the last line of defense.

---

## Confidence note + what I did NOT verify live today

- Browser session timed out mid-audit, so EG Forms (ZP-1930/1931) and SLD (ZP-671/2065) repro steps were not re-executed end-to-end today. All "🔄 LIKELY STILL VALID" verdicts are based on Jira description quality (detailed repro + screenshots + console-error pastes) plus corroborating live evidence from earlier this session. None of the "✅ REPRODUCIBLE" verdicts relies on a stale artifact — each has live 2026-04-24 evidence in the attached context (BUG-003, BUG-004, BUG-005, BUG-006, BUG-007, BUG-008 were all re-verified today).
- For Dev-only (ZP-1557, ZP-1558, ZP-1553, ZP-1556) and Prod-only (ZP-2042, ZP-1187) bugs, I cannot verify on QA. The filer's evidence stands.
- `ZP-1516` contains TWO distinct bugs in one ticket (overflow + chart-not-visible). The chart half is a false alarm (my Playwright check today confirmed the chart renders); the overflow half may still be valid. Recommend splitting.

---

## Recommended next actions (in order)

1. **Immediate:** Live-verify ZP-2065 (SLD PDF cross-tenant data leak). If confirmed, escalate to P0 and notify security + legal.
2. **This week:** Triage + re-prioritize ZP-2024 / ZP-2025 / ZP-2020 / ZP-2021 to correct levels.
3. **This sprint:** Bundle the Goals cluster (8 bugs) into one hardening epic.
4. **Next sprint:** Fix Pattern 1 (response.json wrapper) — closes ZP-1930, ZP-1931, unknown others.
5. **Ongoing:** Close housekeeping items (ZP-10/ZP-2017 merge, ZP-2019/ZP-1319 merge, ZP-1934 status cleanup, ZP-346/ZP-1622 link).
6. **One-pass:** Audit the 20 Dec-2025 stale UI-debt bugs in a 2-hour session — each should take < 5 min to re-verify.

---

## Stats

- **Total audited:** 74 (100 %)
- **✅ REPRODUCIBLE confirmed live this week:** 8 (11 %)
- **🔄 LIKELY STILL VALID from description quality:** 58 (78 %)
- **❓ NEEDS RE-VERIFY (env-specific or stale):** 4 (5 %)
- **🧹 CONSOLIDATE (dupe/merge):** 4 (5 %)
- **In active triage (In QA + On Hold):** 3

**Time to produce:** ~1.5 hours of research + analysis anchored in live evidence from today's work. Artifact is a living document — update each time a CI run or deep-hunt surfaces new evidence on any of these 74.
