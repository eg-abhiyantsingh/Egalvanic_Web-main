# Web Automation — Coverage Matrix & Gap Diagnosis (2026-06-03)

Phase-0 inventory + Phase-1 gap diagnosis of the eGalvanic web Selenium/TestNG suite.
Generated from a repo+frontend crawl (4 parallel analyzers → synthesis). SLD + Connections
excluded where hidden in the May 2026 release.

> "Covered" = exercised **and** a strong assertion proves the real outcome.
> "weak (load + 1 render)" = the page opens and one element is checked — it would NOT
> catch wrong data, a failed save, a broken filter, or a removed feature.

## Coverage matrix

| Module | Route(s) | Key flows/elements | States | Coverage | Verification |
|---|---|---|---|---|---|
| Site Overview | `/dashboard`, `/` | KPI cards, per-chart deep-links | loading/error/no-data/populated | scripted (DashboardBug, BugHuntDashboard) | **strong** (KPI/number/currency) |
| Sales Overview | `/sales-overview` | gated KPIs (`hasSalesCore`) | loading/error/company-NA/populated | scripted-shallow (Phase5) | weak (health-gate) |
| Ops Overview | `/ops-dashboard` | gated KPIs (`hasOpsCore`) | …/populated | scripted-shallow | weak (health-gate) |
| Panel Schedules | `/panel-schedules`, `/:id`, `/:id/view` | list → PanelEditor → PanelView | loading/error/empty/populated | smoke + Phase5 | weak |
| Arc Flash Readiness | `/arc-flash` | NFPA70E tabs; report-gen | loading/populated/generating/report-error | smoke + Phase5 + partial bug-hunt | weak→partial |
| Equipment Designations | `/equipment-designations` | NFPA70E lib table | lib-loading/empty/populated | smoke + Phase5 + partial | weak |
| Equipment Library | `/equipment-library`, `/:cat/:itemId` | taxonomy sidebar, faceted filters | loading/no-taxonomy/empty/populated | smoke + Phase5 | weak |
| SKM Library detail | `/skm-*-library/.../:oid` | device/cable/transformer detail | loading/error/populated | **NOT YET** | none |
| Assets | `/assets`, `/:assetId` | list/search/CRUD; 7 detail tabs; `?class=` | loading/error/no-SLD/empty/populated | scripted (deep) | strong (Smoke/Part3 reload re-read); weak Part2 (toast-only) |
| Connections | asset-embedded + `/connections` | create→grid→delete; core attrs | loading/error/empty/populated | scripted (deep) | mixed→weak (presence-leaning) |
| Issues | `/issues`, `/:issueId` | DataGrid CRUD; report-gen async; detail tabs | …; report SUCCEEDED/FAILED/TIMED_OUT | scripted (deep) | mixed→strong (cancel→row-count, search count) |
| Locations | `/locations` | Building/Floor/Room CRUD + tree | loading/error/empty/populated | scripted (deep but thin) | mixed→weak (Part2 7 asserts/42 tests) |
| Tasks | `/tasks`, `/:taskId` | List/Calendar, KPIs, CRUD, paging | loading/error/no-SLD/empty/populated | scripted (55 TCs) | **weak** (heaviest `pageText.contains` happy-path) |
| Work Orders | `/sessions`, `/:id`, `/:id/quick-count` | CRUD; 6 detail tabs; status | …; access-denied | scripted (deep) | mixed→strong create/cancel/search; weak presence + swallowed catches |
| Work Order Planning | `/planning` | plan CRUD/totals, editor round-trip, XSS | …; access-denied | scripted (32) | **strong** (persist round-trip, unique-name, cleanup) |
| Scheduling | `/scheduling` | calendar Month/Week/Day/Quarter | loading/empty/populated | Phase5 + Misc + BUG-022 | weak |
| Commitments / EMPs | `/emps` | list (CommittedQuotes), `emp` gate | …/populated | smoke + Phase5 | weak |
| Condition Assessment | `/pm-readiness` | PMReadinessDashboard | …/populated | smoke + Phase5 + partial | weak |
| Goals | `/goals` | date-range / negative / cadence validation | …/populated | scripted (adversarial 8) | mixed-strong (validation-reject) |
| **Opportunities** | `/opportunities`, `/:id` | list (no kanban); detail single-scroll (no tabs); `hasSalesCore` + SLD-scope | loading/error/empty/populated | **scripted (NEW — OpportunitiesTestNG: CRUD edge + API)** | **strong (NEW)** |
| Accounts | `/accounts`, `/:id` | 8 `?tab=` detail tabs | …/populated | smoke + Phase5 | weak |
| Report Builder | `/reporting/builder`, `/config/:id` | `?view=` configs/branding | …/populated | smoke + Phase5 | weak |
| Forms (EG/NETA/Legacy) | `/eg-forms` | `?view=` sub-views | …/populated | AI (EgFormAI), GenerateReportEgForm | weak (EgFormAI 56 TCs / 0 asserts) |
| Reporting Engine V2 | `/reporting`, `/reporting/legacy` | branding/templates/EG-Forms/generate | …; "Coming Soon" | scripted (39) | mixed→strong (4 hard asserts/39) |
| Attachments | `/attachments` | upload, list; perm-gate | …; upload-error; access-denied | Phase5 + BUG-027 | weak |
| Notes | `/notes` | list (gated) | loading/empty/populated | Phase5 | weak |
| Jobs / Quotes | `/jobs`, `/:id`, `/quotes/:id` | Job tabs; Quote conditional tabs | …; disabled-tab sub-states | **NOT YET** | none |
| Analyzer | `/analyzer`, `/:id`, `/analyzer-viewer/...` | list → analysis → file viewer | loading/error/empty/populated | **NOT YET** | none |
| SLD | (hidden May 2026) | canvas nav/zoom/edit | … | scripted (71, raw JS) | mixed→strong numerically, canvas presence-heavy |
| Authentication | login / company-code | role login, session, invalid→error | login/error/session | scripted (deep) + Consent | mixed→strong; Consent strong |
| Site Selection | facility picker | select, cancel→unchanged | input/open/selected | scripted (30) | mixed (cancel→URL strong) |
| Admin / Settings | `/admin`, `/admin/audit-log` | Settings tabs; AuditLog grid | …; access-denied | bug-only (BUG-020/028/029) | weak (tabs un-enumerated) |
| Z University | `/z-university` | learning UI | self-contained | smoke + Phase5 + XSS | weak + strong-on-XSS |
| Release Updates | `/release-updates` | Beamer-backed | conditional | bug-only (Beamer PII) | strong-on-PII-leak only |
| Maintenance | `/maintenance` | static placeholder | static | Misc + Phase5 | weak |

**Cross-cutting non-functional:** A11y (axe) **strong** — all ~60 routes + detail tabs (Phase4); Perf **moderate** (client-side budgets only); Visual **moderate** (pixel-diff); Security **moderate** (hand-rolled OWASP IDOR/XXE/headers + REST-Assured; no ZAP/DAST); Resilience/offline **moderate** (CDP offline, Chromium only). **MISSING entirely:** cross-browser, DB/JDBC integrity, i18n/localization, true load/stress (JMeter/k6 are scaffolded but not load-profiled).

## Gap diagnosis — why the suite under-finds bugs (highest yield first)

1. **Skip-and-pass guards turn missing/broken features green** (~267 `return`-as-pass, 420 SKIP). `if (!exists(X)) return;` exits PASS — masks the exact removed feature a bug-hunt should catch. → Replace skip-return with `Assert.fail`; reserve `SkipException` for genuinely env-gated cases.
2. **Tests act but never assert an outcome** (EgFormAI 56/0 asserts; LocationPart2 42/2; AssetPart2 40/4; ReportingEngineV2 39/4; SLD 71/9). 3,224 `logStep` used as "evidence" — logging never fails a build. → End every test asserting a concrete value/persisted state.
3. **Swallowed exceptions** (472 empty `catch{}`, 54 log-only, 21 catch-and-return). The throw IS the symptom; absorbing it keeps the test green. → Never wrap assertions/core actions in bare catch.
4. **Health gates wired into only 8/74 files; `verifyPageHealth` called 10×.** Console errors + 4xx/5xx go unobserved despite `BrowserErrorCapture` existing. → Call `verifyPageHealth` after each nav, or set `STRICT_HEALTH_GATES=true`.
5. **Presence-only / tautological assertions** (~102 `isDisplayed`, 52 `assertNotNull(locator)`, 35 `.size()>=0`). Pass whenever the page renders. → Assert value/state, not presence.
6. **Save verified by toast, not re-read** (AssetPart2 persistVerify=0). A save that doesn't persist passes. → Reload and `assertEquals(persistedValue, newValue)`.
7. **`Thread.sleep` instead of conditional waits** (26). → `WebDriverWait`/`ExpectedConditions` on the post-condition.
8. **Brittle positional-index XPath** (21). → Anchor on `data-testid`/role/text.
9. **No DB/JDBC, i18n, true load, cross-browser, or DAST/ZAP.** → Add JDBC integrity (or adopt the DB-less substitute as policy), parameterize `-DbrowserName`, JMeter/k6 load, document i18n/DAST scope.
10. **Boundary/BVA is one control deep** (Phase4 drives only the Planning search box). → Drive boundary providers into each module's real numeric/date fields, assert accept/reject.
11. **API layer thin and lenient** (auth+users; accepts 200 OR 401/403/404). → Tighten to exact status/body; fail (not skip) on missing token; broaden beyond auth/users.

## Top-8 modules for CRUD-edge automation next

1. **Opportunities** ✅ *(done this session — OpportunitiesTestNG)*
2. **Accounts** — 8 `?tab=` detail tabs unexercised functionally.
3. **Panel Schedules** — list → PanelEditor → PanelView round-trip; edit-persist + view/edit parity.
4. **Equipment Library** — taxonomy + faceted filters with `no-taxonomy` edge.
5. **Jobs / Quotes** — zero coverage; rich conditional/disabled tabs.
6. **Scheduling** — calendar views + booking + empty-state.
7. **Attachments** — upload/list, perm-gate, no-SLD, upload-error.
8. **Arc Flash Readiness** — 4 NFPA70E tabs + report-gen success/error content.
