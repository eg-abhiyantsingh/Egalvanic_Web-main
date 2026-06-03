# Full-Spectrum Quality Gates — a11y, perf, network, UI↔API, ZAP, k6

**Date:** 2026-06-03
**Time:** 12:15 IST
**Prompt:** "cover everything … find as much bug as possible" — full-spectrum web test architect audit + build (Phases 0-5).

---

## What this delivers

The framework already had strong functional + AI machinery (verifiers, Monkey,
AIPageAnalyzer, SmartBugDetector, SelfHealing) but it ran **green because the
health gate was WARN-only and the strong gate was opt-in/rarely called**, and it
had **zero** accessibility, client-perf, network-resilience, or UI↔API-integrity
coverage. This change closes those gaps.

### New verifiers (`src/main/java/com/egalvanic/qa/utils/verify/`)
- **A11yVerifier** — official Deque axe-core Selenium binding (`com.deque.html.axe-core:selenium:4.10.1`). WCAG 2 A/AA scan; hard-fails on critical/serious (contrast, button-name, ARIA, alt-text), warns on minor. CSP-safe.
- **PerfVerifier** — client-side Navigation Timing + FCP via pure JS (no dep). `assertWithinBudget()`. Boundary: not a load test.
- **NetworkConditions** — CDP `Network.emulateNetworkConditions` via raw `executeCdpCommand` (version-stable): offline / slow3G / throttle for resilience testing.
- **UiApiConsistencyVerifier** — DB-less data-integrity: monkey-patches fetch+XHR to record each API response's item count, then `assertGridMatchesApi()` fails when the UI grid total disagrees with the server (stale cache / dropped / duplicated rows). Endpoint-agnostic.

### Wiring
- `BaseTest.verifyAccessibility()` + `verifyPerformance()` opt-in gates (alongside `verifyPageHealth()`).
- `BrowserErrorCapture.assertNoSevereErrors(..., ignoreSubstrings)` overload; `verifyPageHealth` now passes `HEALTH_GATE_IGNORE` so genuine app errors fail but baseline auth/transient noise doesn't.
- `HEALTH_GATE_IGNORE` += `auth/v2/me`, `auth/v2/refresh`, `alliance-config`.

### New tests / suites
- **Phase4QualityGatesTestNG** + `suite-quality-gates.xml` — data-driven across 7 modules: a11y scan, perf budget, 10 boundary/negative/XSS search inputs, offline resilience.

### Non-functional scaffolds (run where the tools live)
- `security/zap-baseline-scan.sh` — OWASP ZAP baseline|full DAST (Docker), HIGH-risk CI gate. **Boundary: baseline DAST, not a manual pentest.**
- `perf/k6-load-test.js` — load|stress|spike|soak against the API tier.
- `.github/workflows/security-and-load.yml` — manual-dispatch job for both, reports as artifacts.

---

## Bugs the new layer surfaced on the FIRST run (real, not noise)

axe-core flagged **critical/serious WCAG violations on every module**:
- **`button-name` (critical)** — icon-only buttons (Edit/Delete plan, etc.) have no accessible text. (Ironically this is why our Selenium locators must rely on `title='Edit Plan'`.)
- **`color-contrast` (serious)** — text below the WCAG contrast ratio.
- **`aria-progressbar-name` (serious)** — progress bars with no accessible name.

These auto-generate `ready-bug/` repro files via the existing SmartBugDetector → ReadyBugGenerator hook.

False positives from the first run (auth/transient console noise, graceful offline empty-state) were triaged and tuned out so the suite's signal stays trustworthy.

---

## Decisions taken (user)
- **DB testing**: no direct DB access → verify integrity via the app's own API responses (UiApiConsistencyVerifier).
- **Security + load**: scaffold ZAP + k6 for the user to run.
- **Cross-browser**: skipped (kept the hardened Chrome path).

## Commits
- `fdd8455` a11y + perf + network + boundary gates
- `42804df` signal-quality tuning (noise filtered, real a11y bugs retained)
- `00210ac` UI↔API consistency + ZAP + k6 scaffolds

## How to run
```
mvn test -DsuiteXmlFile=suite-quality-gates.xml -Dheadless=false     # a11y/perf/boundary/offline
mvn test -DsuiteXmlFile=<any-suite> -DSTRICT_HEALTH_GATES=true        # flip warn-gate to hard-fail
BASE_URL=https://acme.qa.egalvanic.ai bash security/zap-baseline-scan.sh
PROFILE=load k6 run perf/k6-load-test.js
```

## Remaining honest gaps
- DB-level integrity (no creds) — covered indirectly via API.
- Reporting/PDF render-proof — `AssetLoadVerifier.assertPdfRendered()` exists; needs wiring into report tests.
- Cross-browser — deferred by decision.
- Deeper API contract/schema tests — need confirmed response shapes.
