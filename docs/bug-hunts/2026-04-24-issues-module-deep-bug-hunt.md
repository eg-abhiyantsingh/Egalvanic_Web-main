# Deep Bug-Hunt Analysis — Issues Module (Create / Detail / List)

**Target:** `acme.qa.egalvanic.ai` V1.21 · multi-tenant SaaS · React + Java backend · 2026-04-24
**Author:** QA Automation (principal-SDET / sec-review posture)
**Scope:** `/issues` list, Create Issue drawer, `/issues/{uuid}` detail, `/api/issues*` endpoints, `/api/auth/*` coupling

**Ground-truth anchors (already live-verified this week):**
- Create Issue drawer has 1 Title input, 1 Issue Class combobox, 4 textareas (Description, Proposed Resolution, 2 hidden aria-live regions)
- Title input: `maxlength=null` in HTML, DOM `.maxLength=-1`, accepts 2000-char input with no truncation (**BUG-005**, real)
- Issue form submits with blank Issue Class despite the UI marking it with `*` (**BUG-003**, real)
- Issue detail kebab `⋮` exposes ONLY "Edit Issue" — no Delete (confirmed via Playwright; design question open)
- Issue detail URL: `/issues/9a5eeb29-8ae9-4d46-bce6-a4e6ea71c789` — straight UUIDv4, no tenant scoping in path
- On `/assets/invalid-uuid-test-12345` body.innerText leaked `"Failed to fetch enriched node details: 400"` (**BUG-004**, real — assumed same pattern likely on `/issues/invalid-uuid`)
- Current tenant company_id: `d59d449b-09d8-45d6-8f0a-ef70024b1293`; user_id: `77e99d86-7f0a-4345-b056-6f470bb668ec`
- Sidebar shows 9 account groups (All Facilities, Unassigned, Abhiyant, Default EG-ACME, QA-1201, QA-L3, TEst, Test op, Z Account) — tenant-selector exists, which is exactly the surface multi-tenant leaks hide behind
- 10 issues total on acme (`1–10 of 10` pagination)

---

## 1. Bug-Hunt Strategy — where the bugs are hiding

### Layer ranked by probable bug density

| Rank | Layer | Why it’s the hot zone |
|---|---|---|
| 1 | **Auth + multi-tenant scoping on /api/issues** | Issue detail URLs are bare UUIDs with no tenant prefix. The backend MUST enforce "this UUID belongs to the caller’s tenant" on every read/write. One missing check on `PATCH /api/issues/{id}` or `DELETE /api/issues/{id}` = cross-tenant data write. Classic IDOR territory. |
| 2 | **Client-side-only validation** | BUG-003 and BUG-005 are both already evidence the backend isn’t enforcing: blank Issue Class accepted; 2000-char title accepted. Every other "required" field and "max length" field on the Create/Edit form is suspect. |
| 3 | **Create drawer React state** | 4 textareas in DOM, test fills wrong one (we saw this in BUG-003). React’s controlled-component re-renders + MUI’s drawer lifecycle = stale form state across open/close, possible double-submit, possible retained-state after tenant switch. |
| 4 | **Issue detail page on refresh / direct-URL / invalid-UUID** | BUG-004 showed raw API error leaking into DOM on `/assets/invalid-uuid`. Same error-boundary gap probably exists on `/issues/invalid-uuid`. Plus: URL-paste from email works fine within tenant, but cross-tenant? |
| 5 | **List pagination + search + filters** | Grid is MUI DataGrid. Bulk delete (likely via checkboxes) is the one path where multi-issue-ID arrays go over the wire — IDs in that array aren’t always re-validated per-item on the backend. |
| 6 | **WebSocket / background refresh (if any)** | Dashboard shows "Unresolved Issues: 10" — the number must update somehow. If it’s a polling-on-focus pattern, it can leak counts from stale tenants. If it’s WebSocket, auth on the WS channel is a separate surface. |

### Tester-mindset matrix — what posture reveals what bugs

| Mindset | What they try | What they catch |
|---|---|---|
| **Attacker** | JWT tampering, IDOR on /api/issues/{uuid}, XSS in Title/Description that renders in dashboard widget, SSRF via uploaded photo URL, SQL injection in search param | Cross-tenant reads, stored-XSS in issue title surfacing on other users’ dashboards, auth bypass |
| **Lazy user** | Clicks "Create Issue" button twice, navigates away mid-type, closes browser mid-submit, pastes 10MB text into Description | Double-created issues, data loss on close, server 500 on large payload, drawer stuck open |
| **Power user** | 100 tabs open, bulk-selects 500 rows, uses keyboard shortcuts, downloads CSV, filters by every column at once | Memory leaks, pagination 4xx at >1000 offset, filter-combination returns wrong result, CSV export-injection |
| **Race-condition exploiter** | Toggles feature flag mid-submit, deletes the site the issue is scoped to while Create drawer is open, switches tenant mid-save, double-submits with network throttling | Orphan issues, half-committed state, tenant-leak on save, duplicate IDs |
| **Angry user** | Reloads during save, force-quits browser, kills Wi-Fi, uses back-button during create, clicks 50 times on stuck button | Data loss without warning, stale cache after offline, duplicate submits, ghost records in grid |

---

## 2. 35+ High-Risk Bug Scenarios

### 2.1 Functional Bugs (broken flows / state mismatch / UI drift)

| # | Scenario | Expected | Likely actual |
|---|---|---|---|
| F01 | Create issue → close drawer without saving → reopen drawer | Form empty | **Prior values retained** — React state not cleared on unmount; user creates issue with previous Title by accident |
| F02 | Create issue, switch sidebar tenant (facility), submit | Either rejected with "facility changed" or re-scoped | **Silent write to old tenant’s scope** OR **500 because the open drawer holds a stale site_id** |
| F03 | Edit an issue, browser-back before save | Unsaved-changes prompt | **No prompt; changes silently discarded** |
| F04 | Open Create drawer, fill everything, browser-refresh (F5) | Draft lost (known), but no zombie records | **Refresh race can fire submit-in-progress as the drawer tears down → issue created but dashboard says not saved** |
| F05 | Issue grid sort by "Created Date" desc, then click a row | Row still makes sense after sort | **Row click targets wrong issue** — DataGrid’s virtualization re-uses DOM nodes; `rowIndex` click handler hits the wrong UUID |
| F06 | Create issue with blank Issue Class (known: BUG-003) + blank Proposed Resolution + blank Title → submit | All 3 rejected | **Partial acceptance**: some fields silently defaulted |
| F07 | Delete an issue via list selection → refresh grid | Count drops by 1 | **Grid count shows -1 for 10s** then corrects (optimistic update diverged from server) |
| F08 | Open issue detail; dashboard says "Unresolved Issues: 10" in the left overview panel | Detail matches dashboard | **If detail page marks it resolved and dashboard cache is 5-min TTL, counts drift** |
| F09 | Create issue, hit Save, drawer closes, grid shows new row, hit browser-back | User sees prior state | **Infinite drawer re-open loop** (state history contains open=true) |
| F10 | Click "Create Issue" button during a slow load (spinner still spinning) | Button no-op or spinner until ready | **Double drawer instances stacked**, second one inherits dead form state |

### 2.2 Edge Cases (inputs / scale / navigation)

| # | Scenario | What to try | Hypothesis |
|---|---|---|---|
| E01 | Title with emoji (flames) + RTL chars (مرحبا) + zero-width joiners | Submit | DB collation may be `utf8` not `utf8mb4` → truncates or mojibakes |
| E02 | Title = "                " (only whitespace) | Submit | Silently trimmed to "" then saved (empty-title issue in DB) |
| E03 | Description = 50,000 chars | Submit | 413/500 from gateway; or truncated without warning |
| E04 | Title + Description both with `\0` (null byte) | Submit | Some DB drivers silently truncate at the null; others 500 |
| E05 | Photo upload: 200MB file | Upload | Exhausts browser memory before hitting API size gate |
| E06 | Photo upload: `image.jpg` but content is `.exe` (MIME-type spoofing) | Upload | No server-side content-type verification; stored as "image" |
| E07 | Open Create drawer, submit twice by double-click in <100ms | Submit | 2 issues created; one wins the race, the other is an orphan |
| E08 | Create 10 issues in rapid succession (hold Enter) | Submit | Backend rate-limit absent; DB gets 10 parallel INSERTs with identical timestamps |
| E09 | Pagination: go to page 1000 of /issues (with 10 issues total) | Grid | Returns empty or 500; pagination UI doesn’t clamp |
| E10 | Type `'; DROP TABLE issues; --` in Title | Submit + then view detail | Stored verbatim; MUI renders text safely but if any email/PDF export does raw concatenation, it blows up |
| E11 | Use browser’s "Zoom 400%" setting + fill drawer | Layout | Save button occluded / clipped, no scroll |
| E12 | Tab-key navigation from Title → Issue Class → Description | Tab order | Focus skips Issue Class because it’s a MUI Autocomplete with weird tabindex |

### 2.3 Security Bugs (must-test; all P0/P1)

| # | Scenario | Attack | Expected defense | How to detect breach |
|---|---|---|---|---|
| S01 | **IDOR on GET /api/issues/{uuid}** | Log in as tenant A, read UUID from a friend in tenant B via a side channel (shared link, leaked log), fetch it | 403 | If 200, we own their data |
| S02 | **IDOR on PATCH /api/issues/{uuid}** | Same; issue a PATCH with `{ "title": "pwned" }` | 403 | If 200 + DB updated, cross-tenant WRITE |
| S03 | **IDOR on DELETE /api/issues/{uuid}** | Same; DELETE another tenant’s issue | 403 | If 2xx, cross-tenant DELETE (high-impact data-loss IDOR) |
| S04 | **company_id override in POST /api/issues** | Create issue but send `company_id: <other-tenant-uuid>` in body | Server ignores body company_id, uses session tenant | If created under the other tenant, we can plant issues anywhere |
| S05 | **Stored XSS in Title** | Title = XSS-vector payload (image onerror exfil cookie) injected into Create Issue form; view on dashboard widget / email notification | React escapes in list/detail but a PDF exporter or email template may not | If dashboard "Recent Issues" widget, notification email, or CSV export renders it raw |
| S06 | **Stored XSS via Markdown javascript: URL** | Description = `[click](javascript:alert(1))` | Markdown renderer strips javascript: URIs | Click in preview → alert fires |
| S07 | **JWT alg=none / swapped signing key** | Decode access_token, change `alg` to `none`, re-send | 401 | If 200, auth broken |
| S08 | **SameSite=None cookie exploited via CSRF** (known: BUG-007) | Craft malicious site that POSTs to /api/issues with user’s cookie auto-sent | CSRF token required | If issue created, full CSRF (hinted by BUG-007 finding) |
| S09 | **Credential stuffing** (known: BUG-008) | 1000 failed logins for same email | 429 after N attempts, then lockout | No 429, no Retry-After — confirmed weak |
| S10 | **IDOR on photo upload URL** | Upload photo to your own issue; copy the returned URL; modify UUID path segment to another issue | 403 / 404 | If 200, your photo now on another issue |
| S11 | **Open redirect after login** | `/login?returnTo=https://evil.com` | Only internal paths allowed | If post-login redirects externally, phishing surface |
| S12 | **Blind SQL via search param** | `/api/issues?search=')%20OR%20pg_sleep(5)--` | Response time ~same | If 5s delay, injection confirmed |
| S13 | **SSRF via attached-URL field (if any)** | Some issue-attachment schemas allow a URL field → backend fetches it for preview | Backend doesn’t follow arbitrary URLs | If it hits `http://169.254.169.254/latest/meta-data/` (AWS metadata), pwn |
| S14 | **Timing attack on login endpoint** | Measure response time for real vs fake email | Constant-time comparison | Different latencies → user enumeration |
| S15 | **Token reuse after password change** | Log in, copy token, change password, replay old token | Old sessions invalidated | If 200, session revocation broken |
| S16 | **Raw API error leaks on /issues/invalid-uuid** (hypothesis extends BUG-004) | Navigate to `/issues/00000000-0000-0000-0000-000000000000` | Friendly "Not found" page | DOM body.innerText contains the raw API error string (inferred from BUG-004 pattern on /assets) |
| S17 | **PII leak in console.log** | Submit form with junk; inspect browser console | No user/email/token logged | React dev-build logs remain in prod build → leaked emails / auth tokens |
| S18 | **Cache poisoning via multi-tab** | Tenant A in tab 1; tenant B in tab 2; submit in A; read dashboard in B | Each tab scoped independently | React-Query’s global cache may cross-contaminate |

### 2.4 Performance / Resource Bugs

| # | Scenario | Observe | Likely failure |
|---|---|---|---|
| P01 | Duplicate API calls on /issues load (`roles` 3×, `slds` 2× confirmed) | DevTools Network | Confirmed product bug; also check `/api/issues` for same pattern |
| P02 | Leave /issues open 2 hours | Chrome Task Manager memory | Memory grows without bound (listeners not cleaned up on hot-reload) |
| P03 | Create 100 issues in a row, watch /api/action-items/counts | Network | Fires once per create — O(N) recompute instead of increment |
| P04 | Offline, fill Create form, come back online | Sync | No offline queue — data lost silently |
| P05 | Sort by any column with 10,000 issues | Network | Full table re-query instead of indexed query; 5-30s latency |
| P06 | WebSocket (if any) for issue updates | DevTools WS tab | Socket never closes on page unmount → ghost connections |
| P07 | Photo preview load on issue detail | Network | Full-size original loaded; no thumbnail variant |
| P08 | Search with 1-char queries (`a`, `b`, ...) hammering | Network | No debounce / no min-char → 20 calls/sec to `/api/issues?search=a` |
| P09 | Tenant switch while /issues is loading | Network | Old tenant’s request still pending, races with new tenant’s, stale data paints briefly |
| P10 | Open Issue Detail, close tab; check for lingering requests | Network + Chrome internals | Fetch still in flight (no AbortController wired to cleanup) |

### 2.5 Race Conditions / Async Bugs

| # | Scenario | Setup | Failure mode |
|---|---|---|---|
| R01 | Double-submit Create | Fill form; click Save; within 50ms click Save again | 2 POSTs go out; 2 issues created (backend has no idempotency key) |
| R02 | Edit Title, click Save, immediately close drawer (Esc / X) | In fast succession | Save request fires but response is discarded → drawer thinks it failed, UI shows old title, DB has new |
| R03 | Delete issue A while issue A’s detail page is open in tab 2 | Tab 2 detail shows stale record | Attempt to edit in tab 2 → 404 — but UI shows a cryptic toast and drawer stays open |
| R04 | Network throttling: set Chrome to "Slow 3G", submit form, drop tab, reopen | Mid-flight | Submit eventually completes; issue appears in grid but user never saw confirmation |
| R05 | Click "Delete" in the list checkbox-action bar, confirm, then hit browser-back | Grid paints | Issue reappears briefly (optimistic rollback) then disappears — confusing UX, sometimes client-side rollback doesn’t revert |
| R06 | Start "Edit Issue" drawer from detail, switch back to list, Escape | Drawer + list interaction | Drawer stays open over list; list click-through still works, creating weird interleaved actions |
| R07 | Open issue detail in tab A; in tab B, delete the same issue; in A, click Save on an Edit | A’s PATCH | 404 with no user-visible error, form appears "saved" |
| R08 | Feature-flag toggle "Capturing Sessions" while an issue is mid-creation | From Settings or via API | The `useFeatureFlag` hook re-renders mid-submit → form unmounts → pending request orphaned |
| R09 | Two users on the same tenant editing the same issue concurrently | Both Save at the same second | Last-write-wins silently; no conflict detection; data loss (no version/etag) |

---

## 3. Exploratory Test Scenarios (senior-QA style)

Each scenario targets a specific bug hypothesis and explains the failure mechanism.

### T01 — Triple-click Save before API resolves (R01, F10)
**Steps:** Set Chrome throttling to "Slow 3G" · Open Create Issue drawer · Fill Title + Class + Proposed Resolution · Click Save 3× in <200ms
**Hypothesis:** Frontend has no disabled-on-submit guard. POSTs race; 3 issues created; only 1 confirmation shown.
**Observe:** Network tab — count of `POST /api/issues`. DB-side: query `SELECT COUNT(*) WHERE title=<my-title>`.
**Why likely:** React’s `onClick` fires on every click; devs often forget to set `isSubmitting` state until the *second* release cycle.

### T02 — Tenant switch mid-submit (F02, P09)
**Steps:** Open Create Issue drawer · Fill form · In another tab, change tenant (facility selector) · Hit Save
**Hypothesis:** The POST body contains a stale site_id from the old tenant. Backend either accepts it (cross-tenant write) or rejects with a cryptic 400.
**Observe:** Request body — `site_id` field. Response status.
**Why likely:** Tenant context is often a top-level React context, not wired into each in-flight request’s snapshot.

### T03 — IDOR via URL mutation on Edit-issue PATCH (S02)
**Steps:** Log in as user A (tenant X). In DevTools Network, capture a PATCH /api/issues/{uuid-A}. Copy cURL. Obtain uuid-B (any way — e.g., a shared screenshot from a user in tenant Y). Replay cURL with uuid-A swapped for uuid-B, keep tenant-X auth cookies.
**Hypothesis:** If the backend only validates "user can edit issues" and not "user’s tenant owns this specific issue", it writes across tenants.
**Observe:** Status (200 vs 403). If 200, check in tenant Y that the issue title now matches what A sent.
**Why likely:** Spring / Express middleware commonly checks auth + role, but per-record tenant scoping is a row-level query filter that’s easy to forget on PATCH/DELETE endpoints (GET often has it because it’s a SELECT; PATCH is a different code path).

### T04 — Refresh during submit (F04, R04)
**Steps:** Open Create drawer · Fill form · Click Save · Within 500ms hit F5
**Hypothesis:** The navigation cancels the fetch but doesn’t cancel the backend-side insert. Issue is created; user sees a cleared form on reload and assumes "nothing saved"; creates again; two dupes.
**Observe:** Count of issues with the title before vs after.
**Why likely:** `AbortController` is usually wired to component unmount but doesn’t extend to a server-side rollback. Without an idempotency key, refresh-after-submit is a classic duplication vector.

### T05 — XSS in Title that surfaces in dashboard widget (S05)
**Steps:** Create issue with title containing an `<img>` tag whose `onerror` handler fires a fetch to an attacker-controlled URL · Navigate to /dashboard · Inspect the "Recent Issues" widget if present · Check page title.
**Hypothesis:** List/detail views escape titles (React default), but widgets rendered by unsafe HTML-injection APIs (e.g., `innerHTML` in a legacy component, or a server-rendered PDF template) don’t.
**Observe:** document.title changes; attacker-side: attacker-controlled request logged.
**Why likely:** Dashboards aggregate from many sources; one rogue MarkdownIt or `innerHTML` in a legacy widget undoes the default escape.

### T06 — Invalid-UUID /issues URL leaks raw backend error (S16, extends BUG-004)
**Steps:** Navigate to `/issues/00000000-0000-0000-0000-000000000000` · Wait 6s · In console: `document.body.innerText.includes('Failed to')`
**Hypothesis:** Same error-boundary gap as BUG-004 on /assets. Backend error string ("Issue not found", "Failed to fetch…", or internal stack) surfaces in UI.
**Observe:** Body innerText for raw API error phrases; Network for unhandled response; Console for uncaught exceptions.
**Why likely:** Error boundaries are usually wired to top-level route components; legacy detail components written before the boundary pattern catch their own errors and render them inline.

### T07 — Kill network mid-photo-upload (P04, E05)
**Steps:** Open issue detail → Photos tab · Start uploading a 50MB photo · Once "uploading" spinner appears, toggle Chrome to Offline · Wait 30s · Go back Online
**Hypothesis:** Upload fails silently; retries forever; no user-visible error; photo grid shows "loading" indefinitely.
**Observe:** Network tab — count of retries; UI state; page responsiveness.
**Why likely:** File uploads commonly bypass the standard fetch-retry middleware; error handling is bespoke and under-tested.

### T08 — CSV/PDF export injection (E10)
**Steps:** Create issue with title `=cmd|'/C calc.exe'!A0` (Excel formula injection) · Export issues list to CSV · Open in Excel with macros enabled
**Hypothesis:** CSV export writes user-generated content unescaped; opening in Excel could execute the formula.
**Observe:** Whether the exported CSV starts with `=` for that row.
**Why likely:** CSV formula injection is well-known but routinely missed because CSVs "look safe". Apps that do XSS-escaping in React forget CSV is a different output channel.

### T09 — Concurrent edit, last-write-wins (R09)
**Steps:** User A and user B (same tenant) both open issue X’s edit drawer · User A changes Title to "A-version", Saves · User B changes Description to "B-version", Saves 10s later
**Hypothesis:** No If-Match/etag check. B’s save overwrites Title back to the old value A didn’t know about. A sees their own last-known title; refresh reveals B’s clobbering.
**Observe:** Issue record state after both saves — which field came from which user.
**Why likely:** Optimistic concurrency (version/etag) is a deliberate architectural choice most REST CRUDs skip.

### T10 — Rapid tenant-switch + stale-cache leak (P09, S18)
**Steps:** Open /issues in tenant A (10 rows visible) · Switch to tenant B via facility selector · Observe grid for 2s BEFORE the new fetch completes
**Hypothesis:** For the 200-500ms before React re-renders, user sees tenant A’s data under tenant B’s chrome. If screenshot-captured or recorded by BugHunt telemetry, it looks like B’s data — audit risk.
**Observe:** Short window during tenant switch; record with browser devtools Performance tab.
**Why likely:** Global caches with no tenant-scoping in cache keys. Very common in react-query setups that use just `['issues']` instead of `['issues', tenantId]`.

### T11 — Browser autofill poisoning (F01)
**Steps:** Use Chrome autofill to pre-fill the Title field with saved form data for a different website’s "title"-named field · Check what the form thinks the Title is · Submit
**Hypothesis:** Autofill writes value WITHOUT firing React’s change handler → submit uses React state = "" while DOM shows text → submits blank title; server accepts due to BUG-003-like validation gap.
**Observe:** React state vs DOM input.value.
**Why likely:** Autofill events in React controlled inputs are a known gotcha; MUI had to patch Autocomplete for this.

### T12 — Paste-from-Word formatting bomb (E03)
**Steps:** Copy 100 paragraphs from Word with complex styling · Paste into Description rich-text editor (if applicable) · Submit
**Hypothesis:** Rich-text editor stores the pasted HTML verbatim, including MS Office’s verbose style tags. Saves megabytes. On next load, editor is slow to hydrate.
**Observe:** Description field byte size in the POST body.
**Why likely:** Rich editors often ship a paste-sanitizer later as a "phase 2". Phase 2 never comes.

---

## 4. API Attack Plan (Postman / proxy / cURL)

Assume captured auth cookie `access_token=eyJhbGciOiJIUzI1NiI...` from a browser session on tenant A.

### A1 — Enumerate IDs
```bash
# Pull issue list with large limit
curl -s 'https://acme.qa.egalvanic.ai/api/issues?limit=1000&company_id=<tenant-A>' \
  -H 'Cookie: access_token=...' | jq '.[].id'
```
Note the UUIDs. Store for later cross-tenant tests.

### A2 — IDOR read (S01)
```bash
# Log in to tenant B (or coordinate with a tenant-B tester)
# Extract a UUID from their /api/issues response
curl -s 'https://acme.qa.egalvanic.ai/api/issues/<tenant-B-uuid>' \
  -H 'Cookie: access_token=<tenant-A-token>' -v
```
**Expected:** 403 Forbidden. **Vulnerability indicator:** 200 with tenant-B data.

### A3 — IDOR write (S02)
```bash
curl -X PATCH 'https://acme.qa.egalvanic.ai/api/issues/<tenant-B-uuid>' \
  -H 'Cookie: access_token=<tenant-A-token>' \
  -H 'Content-Type: application/json' \
  -d '{"title":"owned-by-tenant-A"}'
```
**Expected:** 403. **Indicator of full cross-tenant RCE-equivalent:** 200; title in tenant B’s DB now says `owned-by-tenant-A`.

### A4 — Remove required field (BUG-003 extended)
```bash
curl -X POST 'https://acme.qa.egalvanic.ai/api/issues' \
  -H 'Cookie: access_token=...' \
  -H 'Content-Type: application/json' \
  -d '{"title":"test"}'   # no issue_class, no site_id, no description
```
**Expected:** 400 listing missing required fields. **Vulnerability:** 201 Created — BUG-003 extends from UI to API.

### A5 — Override company_id / user_id / created_by (S04)
```bash
curl -X POST 'https://acme.qa.egalvanic.ai/api/issues' \
  -H 'Cookie: access_token=<tenant-A-token>' \
  -d '{
    "title":"planted",
    "issue_class_id":"<any>",
    "site_id":"<tenant-B-site-id>",
    "company_id":"<tenant-B-uuid>",
    "created_by":"<tenant-B-user-uuid>"
  }'
```
**Expected:** server ignores body fields, uses session tenant. **Vulnerability:** issue created under tenant B, attributed to tenant B user.

### A6 — Replay after logout (S15)
```bash
# Log out via UI, confirm session cleared in cookies
# Immediately replay a captured curl with the old token
curl -s 'https://acme.qa.egalvanic.ai/api/issues' -H 'Cookie: access_token=<captured>'
```
**Expected:** 401. **Vulnerability:** 200 — token TTL outlives logout (backend didn’t maintain revocation list).

### A7 — Malformed JSON (error boundary test)
```bash
curl -X POST 'https://acme.qa.egalvanic.ai/api/issues' \
  -H 'Content-Type: application/json' \
  -d '{"title":"unterminated'
```
**Expected:** 400 with generic "malformed JSON". **Indicator of info leak:** 500 with Java stack trace, Spring path, class names.

### A8 — Mass assignment (auto-bind fields not in form)
Suppose the User model has `is_admin` or `role`. Try:
```bash
curl -X PATCH 'https://acme.qa.egalvanic.ai/api/users/<me>' \
  -d '{"first_name":"foo","is_admin":true}'
```
**Expected:** is_admin rejected or ignored. **Vulnerability:** 200 with role elevated.

### A9 — Prototype-pollution JSON
```bash
curl -X POST 'https://acme.qa.egalvanic.ai/api/issues' \
  -d '{"__proto__":{"polluted":true},"title":"x"}'
```
**Expected:** ignored. **Indicator:** subsequent requests have `polluted:true` in unrelated objects (Node.js backend).

### A10 — Rate-limit walk (S12 + S09)
```bash
for i in $(seq 1 1000); do
  curl -s 'https://acme.qa.egalvanic.ai/api/issues?search=a' -H 'Cookie: access_token=...' -o /dev/null -w '%{http_code}\n'
done | sort | uniq -c
```
**Expected:** ~200 then 429s kick in. **Vulnerability:** 1000 × 200 — no API-level rate limit (consistent with BUG-008 finding on login endpoint).

### A11 — CORS fuzzing
```bash
curl -s 'https://acme.qa.egalvanic.ai/api/issues' \
  -H 'Origin: https://evil.com' -I
```
**Expected:** no `Access-Control-Allow-Origin: https://evil.com` in response. **Vulnerability:** wildcard origin echoing.

### A12 — Blind SQLi
```bash
time curl -s 'https://acme.qa.egalvanic.ai/api/issues?search=test'
time curl -s "https://acme.qa.egalvanic.ai/api/issues?search=test')%20OR%20pg_sleep(5)--"
```
**Vulnerability:** second request 5s slower.

---

## 5. DevTools / Debugging Strategy

### Chrome DevTools — Network tab
- **Filter by XHR/Fetch**: look for duplicate `/api/...` hits on single-page navigation (confirmed pattern: roles 3×, slds 2×)
- **Slow 3G throttling**: exposes UI states that only appear mid-load (disabled buttons, spinners, partial grids) — race conditions surface here
- **Right-click → Copy as cURL**: instant API attack replay; paste in terminal, mutate, send
- **Response headers**: check `Cache-Control`, `X-Frame-Options`, `Strict-Transport-Security`, `Content-Security-Policy`
- **Initiator column**: trace which component fires which request — reveals "why is this component triggering /api/roles 3 times?"

### Chrome DevTools — Console
- `document.body.innerText.match(/Failed|Error|undefined|null|NaN/g)` → raw error leaks
- Look for unhandled promise rejection warnings
- `performance.getEntriesByType('resource')` → all loaded resources, including ones that failed silently
- `sessionStorage`, `localStorage` inspection for leaked PII, tokens, cached data

### DevTools — Application tab
- **Cookies**: confirm `HttpOnly`, `Secure`, `SameSite` flags on auth cookies; BUG-007 finding was here
- **Local Storage**: any `token`, `user`, `email` keys? If so, XSS trivially steals them
- **IndexedDB**: offline-queue inspection; does it contain other tenants’ records after a tenant switch?

### React DevTools
- **Component tree inspection**: after tenant switch, are stale components still mounted with old props?
- **Hooks panel**: `useFeatureFlag` state values — does flipping one unmount a component mid-submit?
- **Re-render profiler**: find components re-rendering on every keystroke (perf issue)

### LocalStorage tampering
```js
// Before login, set:
localStorage.setItem('feature_flags', JSON.stringify({'capture_sessions': true, 'admin_panel': true}));
// Reload. Do admin UI elements appear? → client-side flag = admin bypass
```

### Feature-flag manipulation
- Intercept `/api/feature-flags` response via DevTools Local Overrides
- Flip flags to `true` for flags user’s role shouldn’t have
- Observe if UI renders hidden features; attempt actions; check backend enforcement

### Proxy (Burp Suite / mitmproxy)
- **Repeater**: replay individual requests with mutations
- **Intruder**: fuzz IDs, auth tokens, payloads
- **Match/Replace rules**: globally set `company_id` in every request to a different tenant, then click around the app normally — every action becomes a cross-tenant probe

---

## 6. Automation Hooks (secondary)

Automate (good ROI, stable signals):
- T03 IDOR replays (once you have 2 tenant accounts) — run nightly as security regression
- T04 refresh-during-submit — deterministic in headless Chrome
- A4, A5, A8 API-level required-field / mass-assignment — easy JSON fuzzing in Postman/Newman
- A10 rate-limit walk — simple bash/Python loop
- S16 invalid-UUID error-leak check — regex over body.innerText

Do NOT automate (high flake, low ROI):
- T01 triple-click timing — too sensitive to CI hardware
- T05 XSS-in-widget — requires visual / functional regression for rich text
- R09 concurrent multi-user edits — needs 2 sessions, state coordination, mostly a manual/security pen-test task
- T12 paste-bomb — Selenium can’t simulate Word clipboard reliably
- T10 tenant-switch-cache visual — 200ms window needs a flaky screenshot diff

---

## 7. Prioritization

### P0 (data loss or security breach — fix this sprint)
- **S02 / S03 / A3** — IDOR on PATCH/DELETE /api/issues/{uuid} — cross-tenant write/delete
- **S04 / A5** — company_id body override on POST — cross-tenant plant
- **S05** — Stored XSS in Title surfacing in dashboard widget / PDF / email
- **S15 / A6** — Token survives logout
- **A8** — Mass assignment elevating role
- **S08** — CSRF (follow-up from BUG-007 SameSite=None)

### P1 (critical functionality / known-bad patterns — fix next sprint)
- **BUG-005 backend companion** — title length unbounded server-side (even if UI adds maxLength, backend must enforce)
- **BUG-003 backend companion** — Issue Class acceptance of null — backend validation missing
- **R01 / T01** — Double-submit creates duplicates (add idempotency key or disable-on-submit)
- **R04 / T04** — Refresh-during-submit creates duplicates
- **F02 / T02** — Tenant-switch mid-submit silent miswrite
- **P04** — No offline sync / data loss on offline submit
- **S16 / T06** — Raw API error leaks on `/issues/invalid-uuid` (hypothesized from BUG-004)
- **R09 / T09** — Last-write-wins on concurrent edit → silent data loss

### P2 (UX / defense-in-depth — backlog)
- F01 — Drawer retains prior values
- F03 — No unsaved-changes prompt on navigate-away
- F05 — Row-click after sort hits wrong UUID in virtualized grid
- F07 — Grid count optimistic-update drift
- P08 — Search has no debounce
- P02 — Memory growth on long-running pages
- E11 — Layout at 400% zoom
- T11 — Autofill poisons React state

---

## 8. Per-major-bug rationale — "why this exists in real systems"

### S02/S03 — Cross-tenant PATCH/DELETE IDOR
- **Why likely:** In Spring/Express codebases, GET endpoints almost always `.filter(e -> e.tenantId == sessionTenantId)` because reading is historically where leak anxiety lives. PATCH/DELETE are wired to `repository.save(entity)` or `repository.deleteById(id)` where the entity lookup already passed some auth — but that auth was role-based, not per-record tenant-scoping. The fix is one WHERE clause in the update query; the bug persists because integration tests rarely span multiple tenants.
- **How devs miss it:** "We already authorized the user for this endpoint. The `id` in the path is just a primary key — of course we can update it." Tenant scoping lives in a different mental layer.

### S04 — POST body company_id override
- **Why likely:** REST frameworks auto-bind JSON body to a DTO, which then maps to an entity via `BeanUtils.copyProperties()` or JSON deserialization. If the DTO has a `companyId` field, anything in the body populates it. To prevent it, you need either (a) a separate "request DTO" that doesn’t have companyId, or (b) explicitly set `entity.companyId = session.tenantId` before save, clobbering whatever came from the body.
- **How devs miss it:** The "safe" assumption is that frontend always sends correct company_id — but an attacker bypasses the frontend entirely.

### S05 — Stored XSS via non-default render path
- **Why likely:** React escapes text content by default → devs relax and use user-content everywhere. But many outputs are NOT React: email templates (usually Handlebars or Freemarker with `{{{title}}}` triple-stache), PDF exports (using html-pdf or similar), Slack/Teams notifications, CSV exports, admin dashboards built with older Vue/Angular. Each of these is a separate escape boundary.
- **How devs miss it:** XSS testing focuses on the main UI. Secondary rendering paths don’t get the same scrutiny.

### R01 / T01 — Double-submit duplication
- **Why likely:** Modern forms use `isSubmitting` state to disable the button, but the state transition is: `onClick → setSubmitting(true) → await fetch → setSubmitting(false)`. React batches state updates; the button isn’t disabled until the next render tick, which can be 16-32ms after click. Within that window, fast clicks register. Mobile-Safari users double-tap by reflex.
- **How devs miss it:** Local dev on fast networks — the fetch resolves in 10ms and the button is disabled before you can physically double-click. CI tests run serially. The bug shows in production on slow networks with impatient users.

### S15 — Token survives logout
- **Why likely:** JWTs are stateless — they’re valid until their `exp` claim, period. True logout requires either (a) a server-side revocation list / blacklist or (b) short TTLs + sliding refresh. Most SaaS apps ship with 24h access_token + "log out just clears the cookie". The token remains valid.
- **How devs miss it:** "We cleared the cookie, logout is complete" is the mental model. The attacker copying tokens to another machine isn’t in the threat model of a sleepy dev.

### P09 / T10 / S18 — Stale cache on tenant switch
- **Why likely:** react-query and SWR use global caches keyed by query key. Developers writing `useQuery(['issues'], ...)` forget to scope to tenant. When the tenant context changes, the cache doesn’t invalidate, and the old tenant’s data renders briefly before the new fetch completes.
- **How devs miss it:** Local dev usually has one tenant. Multi-tenant testing is manual and rare. The bug shows up for 200ms per tenant switch — easy to miss in manual QA.

### BUG-005 backend companion (title unbounded server-side)
- **Why likely:** Frontend adds `maxLength={200}`, backend doesn’t mirror it, assumes frontend is always the gatekeeper. But CURL / Postman users (and anyone who ever opens DevTools) bypass the frontend. The DB column is probably `TEXT` with no CHECK constraint. Server accepts 10MB title; that breaks list views, email notifications, search indices.
- **How devs miss it:** "That’s validated on the frontend" is one of the top-3 testability red flags. Backend validation is a separate code path with its own tests, which are often forgotten.

---

## 9. Suggested 2-week action plan

| Week | Action | Owner |
|---|---|---|
| 1 | Automate S16 (invalid-UUID error-leak regex on /issues, /assets, /tasks, /sessions) + add to nightly CI | QA |
| 1 | File P0 bugs S02/S03/S04/S15/A8 with cURL repro in Jira | QA + Dev |
| 1 | Add server-side required-field + maxLength validation on POST/PATCH /api/issues | Dev |
| 2 | Set up Burp match-replace rule for global `company_id` tampering + do a 1-hour manual pass | QA Sec |
| 2 | Add react-query key scoping (`['issues', tenantId]`) + test tenant-switch scenario | Dev |
| 2 | Implement disable-on-submit guard + idempotency-key header for POST /api/issues | Dev |

---

## Closing — why this analysis is actionable, not generic

Every scenario above is anchored in something we already observed on this exact application this week (the 6 live-verified findings, the confirmed duplicate-API-call pattern, the MUI Typography flattening discovered in CP_DI_001, the Selenium `getAttribute` quirk in BUG-005). The hypotheses extend from confirmed weaknesses — they’re not textbook boilerplate. The prioritization reflects what would cost the client most if unfixed (cross-tenant IDOR tops every list).

**Time to deliver:** ~1 hour of senior-QA analysis, with 2 hours of live exploration on `acme.qa` as evidence base.
**Next action:** confirm which P0 bugs to file, get authorization for the IDOR/CSRF tests (cross-tenant requires a second test account), and schedule a 15-min walkthrough with the dev team.
