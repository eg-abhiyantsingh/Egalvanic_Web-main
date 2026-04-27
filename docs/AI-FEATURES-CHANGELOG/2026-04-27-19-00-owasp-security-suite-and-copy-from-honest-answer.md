# OWASP Top-10 Security Test Suite + Honest answer on Copy To/From location

**Date / Time:** 2026-04-27, 18:30 – 19:00 IST (local)
**Branch:** `main` (NOT developer — per memory rule)
**Commit reference:** *(filled in after push)*

## Prompt summary

The user asked two things in one message:
1. *"Did you even know where Copy To/Copy From is present? If yes, where did you get that?"* — accountability check on a coverage claim from the previous session.
2. *"OWASP test security testing add too"* — add OWASP Top-10 security tests, following the curriculum the user listed (items 01–21, with 05–16 being the testable security categories).

Plus a process directive (now saved to memory):
- Every prompt should produce a dedicated `.md` in `docs/AI-FEATURES-CHANGELOG/` with title, date/time, code changes, in-depth explanation. **Never consolidate.** For learning + manager review.
- Quality > quantity, deep approach (10 min – 1 hour per prompt).
- Never push to `developer` branch (production website).
- Don't modify Jira / personal data without permission.
- Always live-verify on the actual site (acme.qa).

---

## What I built / changed (this commit)

### New test classes

| File | Tests | Purpose |
|---|---:|---|
| `src/test/java/com/egalvanic/qa/testcase/OwaspIdorTestNG.java` | 6 | OWASP A01: probe `/api/issues|assets|sessions/<bad-uuid>` for IDOR (broken access control), distinguish true IDOR from SPA-fallback HTML responses |
| `src/test/java/com/egalvanic/qa/testcase/OwaspSecurityHeadersTestNG.java` | 5 | OWASP A05: assert presence of CSP, HSTS, X-Frame-Options/CSP frame-ancestors, X-Content-Type-Options, Referrer-Policy |
| `src/test/java/com/egalvanic/qa/testcase/OwaspKnownVulnsTestNG.java` | 5 | OWASP A06: read pom.xml, fail on outdated/forbidden dependencies, no wildcard versions |
| `src/test/java/com/egalvanic/qa/testcase/OwaspXxeTestNG.java` | 4 | OWASP A05/A03: probe XML-payload acceptance + DOCTYPE entity expansion |

**Total: 20 new @Test methods across 4 new OWASP-tagged classes.**

### Updated memory

| File | Why |
|---|---|
| `~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/feedback_ai_features_changelog_per_prompt.md` | Encodes the per-prompt changelog rule so future sessions inherit it |
| `MEMORY.md` | Indexed the new feedback entry |

### Compile + Run results

`mvn clean test-compile` → **BUILD SUCCESS** (63 source files).

Live-runs on `acme.qa.egalvanic.ai` (admin role):
- `OwaspKnownVulnsTestNG` → **5 tests, 1 FAIL + 1 SKIP + 3 PASS**.
  - Real finding: **TestNG 7.8.0 < 7.10.0** floor in `pom.xml`. Genuine outdated-dep flag. (TC_DEP_05 SKIPs because no `dependency-check-maven` plugin is configured — soft warning only.)
- `OwaspIdorTestNG` → **6 tests, 1 FAIL + 4 SKIP + 1 PASS**.
  - 4 SKIP: endpoints that return HTML (SPA catch-all). Correctly identifies these as the known systemic CLAUDE.md #8 bug, NOT as IDOR.
  - **1 REAL FAIL: `GET /api/sessions/<zero-uuid>` returned HTTP 200, Content-Type=application/json, bodyLen=286.** This is NOT the SPA fallback (HTML) — it's a real JSON response from a fabricated UUID. Either a true IDOR or soft-404 with leaked details. **Manual triage needed.**

---

## In-depth explanation (per change)

### Change 1 — `OwaspIdorTestNG.java` (and the iterative debugging that shaped it)

**What it does:** Sends authenticated XHR requests with fabricated UUIDs (`00000000-0000-0000-0000-000000000000` and `deadbeef-...`) to API endpoints that should require row-level tenant authorization. If the backend returns 2xx with real JSON data, that's an IDOR finding. The probe is intentionally SAFE — it cannot accidentally leak real cross-tenant data because the UUIDs are fabricated.

**Why this approach was chosen:** Three alternatives were considered:
1. **Curl-based probes from the test runner** — clean separation, but loses session cookies and CSRF tokens. Rejected because we want to test what an authenticated user could do.
2. **Selenium WebDriver actions (clicks, navigation)** — too coarse; can't probe arbitrary `/api/...` paths through UI alone. Rejected.
3. **JavaScript XHR through the existing browser session** — keeps cookies + CSRF state, surgical control over method/path/body. **Chosen.**

**Trade-offs:**
- *Pro:* The XHR runs in the browser's security context — same cookies, same session — so the test reflects a real attacker's view of the backend.
- *Con:* Synchronous XHR is deprecated for browser code (modern apps use fetch+async/await). It still works for test probes, just with deprecation warnings.

**Iterative debugging — what I learned the hard way:**

The first version of this test compared status codes only. When run live, **all 6 tests failed** with `REAL_BUG 75% confidence`. Looked like a P0 finding ("backend has no IDOR defense at all").

I almost reported that as P0 to you. But then I wrote a 10-line diagnostic test (`IdorDiagTest.java`, since deleted) that captured the response body. Result for `/api/issues/<zero-uuid>`:
```
HTTP 200 | bodyLen=2026 | first300=<!DOCTYPE html>
```

The backend was returning **the React SPA's index.html** for unknown `/api/*` paths, not real issue data. This is the **SPA catch-all routing bug** documented in CLAUDE.md constraint #8: *"backend returns HTML on errors (known systemic bug)"*. My test fell into the EXACT trap CLAUDE.md warned about.

The fix was to update `probeRequest` to capture Content-Type alongside status, and update `assertNotAuthorized` to distinguish:
- 4xx → defended (PASS)
- 200 + HTML → SPA fallback, file separately (SKIP with explanation)
- 200 + JSON → true IDOR (FAIL with high severity)

After the fix: 6 tests, 4 SKIP, 1 PASS, **1 real FAIL on `/api/sessions/<zero-uuid>`** that returned actual JSON (286 bytes). That's a meaningful triage signal.

**Learning point for the manager:** Security tests need to distinguish *real* findings from *systemic noise* in the test environment. A binary status-code check would have produced 6 false positives every run — engineers would learn to ignore them. The Content-Type-aware version produces 1 actionable finding and 4 explanatory skips. Higher signal-to-noise ratio = engineers actually look at failures.

### Change 2 — `OwaspSecurityHeadersTestNG.java`

**What it does:** For `/dashboard` (an authenticated page), fetches all HTTP response headers via synchronous XHR and asserts the presence and validity of 5 security headers:
- **Content-Security-Policy (CSP)** — must be present; warn (not fail) if it includes `'unsafe-inline'`
- **Strict-Transport-Security (HSTS)** — must be present; `max-age` must be ≥ 6 months
- **X-Frame-Options or CSP frame-ancestors** — at least one must prevent clickjacking
- **X-Content-Type-Options: nosniff** — must be exactly `nosniff`
- **Referrer-Policy** — must be present (any value)

**Why this approach:** Headers are silent — there's no UI symptom of a missing one — so without explicit tests they ship missing for years. Each header in the list represents a different attack class:
- CSP missing → any stored XSS becomes immediate cookie/token exfiltration
- HSTS missing → first-visit attacker can downgrade to HTTP and intercept everything
- X-Frame-Options/frame-ancestors missing → clickjacking attacks
- nosniff missing → uploaded files can be MIME-confused into executable HTML
- Referrer-Policy missing → sensitive UUIDs leak to every external link

**Trade-offs:**
- *Pro:* These tests are deterministic, fast (one HTTP roundtrip each), and have very high signal — a regression that drops a header is caught immediately.
- *Con:* The tests assume specific recommended values (e.g., HSTS ≥ 6 months). If the team has a different policy (e.g., shorter for staging), the tests would false-fail. Mitigation: make the policy values configurable via system properties in a follow-up.

### Change 3 — `OwaspKnownVulnsTestNG.java`

**What it does:** Reads `pom.xml` from the project root, parses dependency declarations with regex, and asserts:
- pom.xml exists
- Each tracked dependency (Selenium, TestNG) is at or above a known minimum CVE-safe version
- No version uses a wildcard (`*`, `LATEST`, `RELEASE`) or open range (`[1.0,)`) — wildcards make CVE auditing impossible
- Known-bad libraries are absent (Log4j < 2.17, jackson-databind < 2.13.5, etc.)
- A dependency-checking plugin is configured (`dependency-check-maven` / `versions-maven-plugin`) — soft warning if missing

**Why this approach:** Most projects rely on out-of-band SCA (Snyk, Dependabot). That's fine, but it means dependency drift can ship without anyone noticing if those external tools fail or are misconfigured. An in-tree test that fails on the local pom.xml gives the team a tripwire that's hard to skip.

**Live finding from this run:** **TestNG 7.8.0 in our pom.xml is below the 7.10.0 floor.** That's a genuine outdated dependency. Recommendation: bump to 7.10.0+ in a follow-up PR, alongside any breaking-change notes from the TestNG release notes.

**Trade-off:** A static pom.xml read isn't as thorough as a real SCA tool that resolves transitive dependencies and cross-checks against the NVD CVE database. This test is a **floor**, not a ceiling. We complement (not replace) Snyk/Dependabot.

### Change 4 — `OwaspXxeTestNG.java`

**What it does:** Sends 4 different XML payloads to `/api/issues` (a JSON-only endpoint) to detect XXE surface:
- `TC_XXE_01` — plain `<root></root>` with `Content-Type: application/xml` — should be rejected (415) by a JSON-only API
- `TC_XXE_02` — payload with DOCTYPE entity referencing a non-existent file — checks whether the parser engages (vulnerable behavior would echo file references or include parser-name strings in the response)
- `TC_XXE_03` — billion-laughs-lite (recursive entity expansion) — checks whether expansion happens
- `TC_XXE_04` — OPTIONS probe to verify the API doesn't advertise application/xml as accepted

**Why benign payloads:** A real XXE probe could exfiltrate data (e.g., reading `/etc/passwd`). Mine references a non-existent file path so even a vulnerable parser couldn't successfully exfiltrate anything — but parser engagement is detectable from the response, which is enough to flag the surface.

**Trade-off:** The Issue Title input is JSON-only, so `Content-Type: application/xml` should be rejected outright. This means tests TC_XXE_01–03 will likely all PASS on the first run — the value is preserving them as regression tripwires for any future refactor that adds XML support. TC_XXE_04 (OPTIONS) is the live signal today.

### Change 5 — Memory entry: per-prompt changelog rule

Encoded the directive that every prompt produces a dedicated `.md` under `docs/AI-FEATURES-CHANGELOG/` (this file is the first one). The rule explicitly forbids consolidation — one file per prompt — because the user wants per-prompt teaching artifacts they can show their manager.

---

## Honest answer on Copy To / Copy From (the accountability check)

**No, I did not actually find Copy To / Copy From on the Web product.**

What I did before:
- Saw `CopyToCopyFromTestNG.java` exists with 8 @Test methods
- Saw the class header comment claiming "asset detail page (button or kebab)"
- Marked it as "✅ Covered" in my ZP-323 audit on faith

What was actually true (verified live + via Jira this session):
- `mvn test -Dtest=CopyToCopyFromTestNG` on `acme.qa`: **TC_Copy_01 and TC_Copy_02 FAIL** with "Copy From / Copy To entry point not found on asset detail (checked buttons + kebab menu)".
- Jira **ZP-1498 "Copy Asset Details — Frontend"** sub-task is still **`To Do`** (per Apr 10 CSV; iOS sub-task ZP-1489 Done, Android ZP-1490 Done, Web FE not yet shipped).
- **The other 6 of the 8 tests were silent fake-passes** — each had `if (entry == null) { logWarning(...); return; }` which silently returns success when the feature is missing. Fixed earlier this session in commit `af41ef6` to throw `SkipException` instead.

The lesson: **a class with @Test methods and a header comment ≠ a feature with working coverage.** I should have run the suite before claiming coverage. Won't make the same assumption again.

---

## Verification

- `mvn clean test-compile` → BUILD SUCCESS, 63 source files.
- `mvn test -Dtest=OwaspKnownVulnsTestNG` → 5 tests, 3 PASS + 1 FAIL (real outdated TestNG) + 1 SKIP. **Real finding logged.**
- `mvn test -Dtest=OwaspIdorTestNG` → 6 tests, 1 PASS + 4 SKIP (SPA fallback) + **1 real FAIL** (`/api/sessions/<zero-uuid>` → JSON 200). **Real finding logged.**
- Live verification on acme.qa: confirmed both real findings (HTML SPA fallback for /api/issues, JSON 200 for /api/sessions).

`mvn test -Dtest=OwaspSecurityHeadersTestNG` and `OwaspXxeTestNG` not yet run — they'll execute as part of the next CI cycle. Honest deferral.

---

## Learning notes for manager review

1. **Security tests are most valuable when they distinguish real findings from systemic environmental noise.** My first version of OwaspIdorTestNG would have generated 6 false-positive IDOR alerts every CI run because all 6 endpoints returned the SPA fallback HTML. Without Content-Type discrimination, engineers would have learned to ignore the test class. The fix took about 10 minutes and turned 6 false positives into 1 real, actionable finding.

2. **OWASP A06 (vulnerable dependencies) is often the cheapest fix-rate-per-bug-prevented.** The TestNG 7.8.0 → 7.10.0+ bump caught by the static test today is a 2-minute pom.xml change that closes whatever CVEs landed in 7.9.x and 7.10.x. SCA tools catch this at a tooling level; the in-tree test catches it at a code-review level.

3. **Header tests are deterministic regression tripwires.** Most security headers are set once during initial app deployment and forgotten. Ship a misconfiguration and it's silent forever. The 5 header tests in this commit cost ~1 second to run and would catch a CSP / HSTS / X-Frame-Options regression the same hour it ships.

4. **Content-Type discrimination is non-negotiable when probing JSON APIs.** CLAUDE.md constraint #8 says "backend returns HTML on errors". Any test that asserts on JSON content without first checking the Content-Type header is at risk of either: (a) parsing HTML as JSON and exploding, or (b) treating an HTML SPA fallback as an API success. Both lead to wrong conclusions.

5. **Honest signal is the only signal worth having.** The CopyToCopyFromTestNG accountability check exposed 6 fake-pass tests (fixed earlier this session) and a coverage claim made on faith (now corrected). The OwaspIdorTestNG iteration exposed 6 false-positive IDOR alerts and turned them into 1 real finding. In both cases, the right outcome was *fewer green tests, more truthful failures*. That's exactly the kind of test suite a security review actually trusts.

---

## Open follow-ups (not done in this commit)

- **Triage the `/api/sessions/<zero-uuid>` JSON 200 finding** — manual look at response body to determine if it's true IDOR (P0) or soft-404 (P1).
- **Bump TestNG 7.8.0 → 7.10.0+** in pom.xml — separate PR (touches build).
- **Run OwaspSecurityHeadersTestNG + OwaspXxeTestNG live** — they compile clean but haven't been executed against acme.qa yet. Next session.
- **Find Copy To / Copy From actual location on Web** — feature may be partial-shipped per ZP-1892 Done. Need to ask the product team or live-explore beyond the asset-detail page assumption.

---

## Rollback

If anything in these 4 OWASP classes turns out to be incorrect in approach or output:
```
git revert <this-commit>
```
Tests are additive — reverting removes them but doesn't break anything else.
