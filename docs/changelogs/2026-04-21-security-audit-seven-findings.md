# 2026-04-21 — Security Audit: 7 security findings added to deep bug report

## Prompt
> `@bug pdf/` add security bugs also

The user asked for security findings to be added to the consolidated bug PDF. This runs a full OWASP-aligned passive + non-destructive security audit on the eGalvanic platform and folds the verified findings into the existing 14-bug report, growing it to 21 bugs.

## Scope constraints (self-imposed)
- Authorized QA testing of our own platform (acme.qa.egalvanic.ai, V1.21)
- Passive probes preferred; active probes kept safe and minimal (6 failed logins, no credential stuffing against real accounts)
- No destructive payloads, no data modification, no Jira/personal data touched
- No secrets exfiltrated; evidence stored locally in `/tmp/bug-verification/screenshots-security/`

## What was actually probed

| # | Probe | Tool | Result |
|---|---|---|---|
| 1 | Login page HTTP response headers | Playwright `response.headers()` | **6 security headers missing** (HSTS, CSP, XFO, XCTO, Referrer-Policy, Permissions-Policy) |
| 2 | API response headers (comparison) | Playwright response listener | API backend **properly hardened** — HSTS + CSP frame-ancestors + XFO: DENY + XCTO: nosniff all present |
| 3 | Cookie flag audit | `context.cookies()` | `access_token` + `refresh_token` correctly HttpOnly+Secure; SameSite=None (widens CSRF). DevRev + Beamer tracker cookies lack Secure/HttpOnly. |
| 4 | `localStorage` / `sessionStorage` audit | `page.evaluate` on Storage API | **No auth tokens in client-side storage** (earlier false-positive was regex matching `ue_session`). Platform is doing this part correctly. |
| 5 | JWT decode (access_token) | base64url + `JSON.parse` | AWS Cognito RS256, TTL 3600s (60 min) — reasonable |
| 6 | User enumeration via login errors | 2 login POSTs (no-user vs bad-pw) | **No enumeration** — both return identical `{"error":"Invalid credentials"}` + 401 |
| 7 | Rate limit probe | 6 failed logins in series, different emails | **No rate limiting** — all 6 returned 401, no 429, no Retry-After, latency 741-1217ms |
| 8 | Clickjacking PoC | Local HTML iframing `/login` | **Framable** — login page renders inside attacker iframe (confirmed via XFO/CSP header absence in BUG-015) |
| 9 | HTTPS redirect | HTTP → HTTPS navigation | Correctly redirects (no bug) |
| 10 | Login autocomplete | DOM inspection on `/login` | `autocomplete="new-password"` (should be `current-password`) |
| 11 | Signup password policy | DOM inspection on `/signup` | `minlength=null`, `pattern=null` — no client-side guidance |

## 7 new bugs added to the report (BUG-015 → BUG-021)

| ID | Severity | Title |
|---|---|---|
| BUG-015 | **HIGH** | Login/static HTML served without HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy (CloudFront/S3 distribution has no response-headers policy) |
| BUG-016 | MEDIUM | Auth cookies (`access_token`, `refresh_token`) use `SameSite=None` — widens CSRF surface on every state-changing API call |
| BUG-017 | MEDIUM | No rate-limiting on `/api/auth/login` — 6 failed attempts, zero 429 responses, no CAPTCHA, no lockout |
| BUG-018 | MEDIUM | Clickjacking — login page can be iframe-embedded by attacker sites (consequence of BUG-015) |
| BUG-019 | LOW | Login password input uses `autocomplete="new-password"` — should be `current-password` so password managers autofill stored credentials |
| BUG-020 | LOW | Third-party widget cookies (DevRev `devrev_plug_user_ref`, Beamer `_BEAMER_*`) lack Secure / HttpOnly flags |
| BUG-021 | LOW | Signup password field has no client-side `minlength` / `pattern` — users only learn policy after server rejection |

**Report totals:** 21 bugs (4 HIGH, 12 MEDIUM, 5 LOW). Was 14 (3 HIGH, 9 MEDIUM, 2 LOW).

## Things I checked and did NOT find (don't re-audit)
- ❌ Tokens leaked in response body — login returns tokens via `Set-Cookie`, not JSON body. ✓ Good.
- ❌ JWTs in `localStorage`/`sessionStorage` — no auth tokens there. ✓ Good.
- ❌ `alg=none` JWT — Cognito RS256 is used.
- ❌ User enumeration — identical error messages.
- ❌ HTTP → HTTPS downgrade — properly redirects.
- ❌ X-XSS-Protection header — explicitly set to `0` which is modern correct practice (the legacy header is deprecated).

## Files changed

### Report + PDF
- [/tmp/bug-verification/build-final-report.js](/tmp/bug-verification/build-final-report.js) — added `SS3 = screenshots-security/`, appended BUG-015..BUG-021 entries, updated summary counts (14→21, 3/9/2→4/12/5), subtitle, methodology text
- [bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.html](bug%20pdf/eGalvanic_Deep_Bug_Report_20_April_2026.html) — 3.67 MB regenerated with 21 bugs
- [bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.pdf](bug%20pdf/eGalvanic_Deep_Bug_Report_20_April_2026.pdf) — 4.75 MB regenerated with 21 bugs

### TestNG regression gate
- [src/test/java/com/egalvanic/qa/testcase/SecurityAuditTestNG.java](src/test/java/com/egalvanic/qa/testcase/SecurityAuditTestNG.java) — 308-line new file with 7 `@Test` methods (`testBUG015_MissingSecurityHeaders` → `testBUG021_SignupNoClientPasswordPolicy`). Each uses inverted assertions that fail-loudly when the underlying bug is fixed.
- [security-audit-testng.xml](security-audit-testng.xml) — new TestNG suite. Run with `mvn clean test -DsuiteXmlFile=security-audit-testng.xml`.
- [src/main/java/com/egalvanic/qa/constants/AppConstants.java](src/main/java/com/egalvanic/qa/constants/AppConstants.java) — 7 new `FEATURE_SEC_*` constants for ExtentReport feature names.

### Probe scripts (kept for re-audit)
- `/tmp/bug-verification/security-audit.js` — main passive audit
- `/tmp/bug-verification/security-audit-part2.js` — rate-limit + enumeration probes
- `/tmp/bug-verification/security-audit-part3.js` — HTTPS redirect + JWT decode + autocomplete
- `/tmp/bug-verification/security-evidence.js` — DevTools-style overlay screenshots
- `/tmp/bug-verification/ratelimit-evidence.js` — 6-attempt brute-force probe with overlay
- `/tmp/bug-verification/autocomplete-evidence.js` — DOM attribute audit overlay
- `/tmp/bug-verification/clickjack-nofont.js` — clickjack PoC (uses CDP capture to bypass font-loading deadlock)

### Evidence bundle (not committed — `/tmp` only)
- `headers-login.json` — login page response headers proving 6 missing
- `api-headers.json` — authenticated API response headers proving backend IS hardened
- `cookies.json` — post-login cookie dump (auth vs widget)
- `storage.json` — localStorage + sessionStorage contents
- `access-token-decoded.json` — decoded JWT header + payload (Cognito RS256, TTL 3600s)
- `rate-limit-6.json` — 6-attempt probe results
- `user-enum.json` — no-user vs bad-pw response comparison
- `bug015-headers.png`, `bug016-cookies.png`, `bug017-ratelimit.png`, `bug018-clickjack.png`, `bug019-autocomplete.png` — overlays for report

## In-depth learning notes (per user preference for deep explanations)

### Why the static HTML headers matter (BUG-015)
CloudFront serves the eGalvanic SPA shell (HTML, JS bundle) from S3 with default headers — only CloudFront/S3 metadata, no security headers. But the API backend (`/api/*`, served by gunicorn) has a proper headers middleware that injects HSTS, CSP frame-ancestors 'none', XFO: DENY, XCTO: nosniff, Referrer-Policy. So half the app is hardened, half isn't.

The fix is cheap and architectural: attach an [AWS CloudFront Response Headers Policy](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/creating-response-headers-policies.html) to the SPA distribution. There's even an AWS-managed `SecurityHeadersPolicy` that sets the safe defaults. This takes one Terraform block or console click; zero code changes.

### Why SameSite=None is a tradeoff, not a fix (BUG-016)
SameSite=None sends cookies on cross-site requests — useful for embedded widgets, cross-subdomain apps, or when the SPA lives on a different domain from the API. eGalvanic's SPA and API are on the SAME origin (`acme.qa.egalvanic.ai/api/*`), so Lax would work. Lax allows same-site navigation while blocking cross-site form-POST, which is exactly the CSRF class you want to block. If the API needs to be callable cross-origin (e.g., for a mobile app using the same cookies), the right answer is to keep SameSite=None AND add CSRF tokens OR strict Origin-header checks server-side. Currently neither is visible.

### Why `autocomplete="new-password"` breaks password managers on login (BUG-019)
The HTML Autocomplete spec defines `current-password` to mean "the user is entering their existing password — autofill stored credentials for this site." `new-password` means "the user is creating a password — suggest a strong one." Password managers use this signal to decide between autofill vs. generate. When a login form mis-declares `new-password`, password managers either don't autofill (user types manually, weakens posture) or offer to generate a new password (which the user clicks through and ends up with mismatched credentials). The fix is one JSX string change in the login component.

### Why rate limiting isn't Cognito's job (BUG-017)
AWS Cognito's built-in rate limiter is coarse (per IP, mostly for abuse, not credential stuffing). Production apps always add a finer layer at: (1) AWS WAF on CloudFront — rate-based rules like `WafV2RateBasedStatement >5 reqs/min per source IP`, or (2) application middleware like `express-rate-limit` keyed on `IP+email` to stop targeted brute-force. Without either, an attacker with a leaked password list can cycle through thousands of credentials/minute against high-value emails.

## How to verify the fixes (for dev)
When each bug is addressed, the corresponding `testBUG01X` method will fail because the inverted `Assert.assertTrue(bugPresent, "... FIXED ...")` will trip. At that point, flip the assertion to `assertFalse(bugPresent)` or delete the test — this file then becomes the regression gate blocking the finding from reappearing.

```bash
# Run just the security suite locally
mvn clean test -DsuiteXmlFile=security-audit-testng.xml
```

## What I deliberately did NOT test
- SQL injection payloads (destructive; requires auth-scoped permission to probe per-endpoint)
- Stored XSS via file upload (could pollute other users' data)
- IDOR — would require systematic ID-swapping against production records
- Privilege escalation — requires multiple test accounts at different role tiers
- Rate-limit probes beyond 6 attempts (avoid pollution)
- Third-party vendor security (DevRev, Beamer, Sentry) — out of scope for platform audit

These are good next-round candidates; each should be its own deliberate engagement with explicit scope.
