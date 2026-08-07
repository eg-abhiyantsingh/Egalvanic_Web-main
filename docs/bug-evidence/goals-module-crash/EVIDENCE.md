# `/goals` crashes to the Application Error boundary for every role — module unusable

**Severity:** HIGH — the Goals module is completely inaccessible; no role can open it.
**Environment:** `acme.qa.egalvanic.ai`, badge **V1.36**
**Verified:** 2026-08-06 and re-verified 2026-08-07 (independent sessions, fresh page loads)
**Detected by:** `NewModulesSmokeTestNG.testTC_NM_03_Goals` (also blocks 12 `GoalsTestNG` tests, which skip on their data preconditions)

## Steps to reproduce
1. Log in to `https://acme.qa.egalvanic.ai` as `abhiyant.singh+admin@egalvanic.com`.
2. Navigate to **`/goals`** (sidebar *Goals*, or the URL directly).
3. Wait ~5 s.

## Actual result
The route renders the app's crash boundary instead of the module:

> 🐛 **Application Error**
> We're sorry, but something went wrong with the application. Our team has been automatically notified.
> Please try refreshing the page or come back later.
> If you contact support, please provide this error ID: **`e1caca80dac94f6793d4083008e8a78f`**
> ↻ Refresh Page · Try Again

Screenshot: `goals-application-error.png`

**Console error (the crash itself):**
```
TypeError: Cannot read properties of undefined (reading 'length')
```

**On-screen error IDs captured (two independent sessions — reproducible, not a one-off):**
- `5daf74a67017…` (2026-08-06)
- `e1caca80dac94f6793d4083008e8a78f` (2026-08-07)

## Expected result
`/goals` renders the Goals module (list/table of goals, or a legitimate empty state).

## Key diagnostic — the crash happens BEFORE any data fetch
All **42** XHR/fetch requests on the failing page load were captured. **Not one is a goals endpoint.** The traffic is only: `auth/v2/me`, `auth/v2/refresh`, `company/alliance-config`, `legal/acceptance/check`, `beamer/user-hash`, plus LaunchDarkly / DevRev / Sentry telemetry.

That means this is **not** a bad API response being mishandled — the component throws during its **initial render**, before it ever issues its data request. The `TypeError: ... reading 'length'` on a first render strongly suggests an array prop/state consumed without a default (e.g. `const { goals } = data` then `goals.length`, where `data` is `undefined` on the first pass) — i.e. a missing `?? []` / optional-chaining guard, or a hook whose initial state is `undefined` instead of `[]`.

Note: the single `401` on `/api/auth/v2/me` is normal token-refresh behaviour — it is immediately followed by a successful `auth/v2/refresh` and a `200` retry, and the session is fully authenticated (the shell, sidebar and header all render). It is **not** the cause.

## Scope — affects every role
Reproduced under **Super Admin** (operational console) and, independently, under **Admin** (the test's Access-Denied→Admin retry path also lands on the crash). So this is not a permission/console-split issue: no role can use Goals.

Corroboration that it is not environmental: on the same sessions, sibling modules (`/tasks`, `/issues`, `/assets`, `/sessions`, `/customers`, `/panel-schedules`, `/sales-overview`) all render normally.

## Suggested next step for engineering
Pull the Sentry event for error ID `e1caca80dac94f6793d4083008e8a78f` — it will carry the component stack and pinpoint the exact `.length` access. The fix is almost certainly a default value on the goals collection before first paint.

## Test-suite status
`testTC_NM_03_Goals` is **correctly red** and should stay red until this is fixed — the automation is reporting a genuine product defect, not a stale expectation. The 12 `GoalsTestNG` skips should clear once the module renders.
