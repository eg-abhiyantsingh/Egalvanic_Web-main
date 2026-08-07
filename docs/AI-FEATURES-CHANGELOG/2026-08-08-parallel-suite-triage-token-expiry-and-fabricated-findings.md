# Parallel-suite triage: a fabricated security finding, a date-token trap, and the real cause of the 401 storm

**Date:** 2026-08-08
**Time:** 02:15 – 03:05 IST
**Prompt:** "make sure this time i wil get 95% correct report for full parallel suite no fley test or any other issue."
**Commits:** `e4eee1a`, `4e892ce`, `823400e`

---

## Summary

Deep-triaged the six new G7 Sales failures from the in-flight parallel run, then followed one of
them into a much bigger finding: **the 401 storm I had blamed on session invalidation was actually
60-minute token expiry.** I had the diagnosis wrong and wrote it into two commit messages, so this
changelog corrects the record.

Nothing triaged today was a product regression.

---

## 1. A test was reporting a security hole that does not exist

`AccountsTestNG.testAcc_ApiFlatEndpointRequiresAuth` failed with:

```
BAC: GET /accounts/ should require auth, but returned 200 unauthenticated.
```

That reads like broken access control — the highest-severity thing automation can find. It is not
real. Measured live:

```
GET /api/accounts/    -> 200  text/html   (2089 bytes = index.html)
GET /api/account/v2   -> 401              (correctly enforced)
```

`/accounts/` is **not an API route**. Unmatched paths under `/api` fall through to the SPA
catch-all and serve the app shell. The test read that static HTML as an authenticated data response.
The real endpoint is `/api/account/v2` — **singular, v2** — and it enforces auth properly.

**Fix:** target the real endpoint, and skip with an explanatory message if the body is ever
`text/html` again, because no auth conclusion can be drawn from the SPA shell in *either* direction
(it would equally produce a false PASS on a route that had genuinely lost its auth guard).

### Why this one mattered most
A false negative wastes an hour. A fabricated `CRITICAL` security finding gets escalated, pulls in
backend engineers, and burns credibility that the whole suite depends on. Any API test that probes a
non-existent path under `/api` can manufacture one, so the content-type guard is now the rule.

---

## 2. The date-token search trap — 5 sites, 1 firing, 4 latent

`Opp26_SearchFilters` failed with:

```
Filtered row does not contain search token 'Aug': Jul 30, 2026 / Quote 31 july / ...
```

The token came from:

```java
String token = firstRow.split(" ")[0];    // first token of "Aug 5, 2026" == "Aug"
```

**The first cell of every grid in this app is the rendered date**, so the search term was a month
abbreviation. Nothing indexes formatted date strings, so the query proved nothing about filtering
and then failed on a legitimately-returned July row.

**Two independent defects here, both fixed:**

**(a) Token selection.** Added `BaseTest.searchableTokenFrom(rowText)` — skips dates, currency,
IDs and status/enum words; prefers the longest distinctive alphabetic word. Validated against the
exact failing row rather than trusted:

```
real failing row  -> Opportunity      (was "Aug")
date-only row     -> null             -> test SKIPS instead of failing falsely
acme account row  -> Substation
uuid/code row     -> null
```

**(b) The assertion was wrong, not just the input.** "Every filtered row must visibly contain the
token" is **not the filter's contract** — the v2 search also matches description / notes / contact /
domain fields the grid does not render, so a *correct* result set could never satisfy it. Replaced
with three falsifiable contracts:

1. the result set must not **grow**;
2. a row known to contain the token must not be filtered **away**;
3. the filter must actually **act** (unchanged count + zero visible matches = input ignored).

Rows matching on non-displayed fields are now logged as info. Applied at all 5 sites
(`Opp26/28/29`, `Acc08/10`) — four were latent and would have fired eventually.

---

## 3. BUG-B pinned down — and it was silently eating functional coverage

Three a11y failures shared identical violation counts across `/accounts` and `/opportunities`, which
suggested shared chrome. I expected third-party widgets (the app's CSP whitelists Beamer and DevRev,
and both are in the DOM). **Live inspection refuted that** — no violating node is third-party:

| axe rule | Impact | Node | Owner |
|---|---|---|---|
| `listitem` | serious | `li.MuiListItem-root` "Legacy Procedures" inside a `div`, in `MuiDrawer`/`nav` | **app sidebar** |
| `button-name` | critical | `button.MuiFab-root` under `div#root`, no accessible name | **app (global FAB)** |
| `button-name` | critical | `button.MuiIconButton-root`, icon-only, no `aria-label` | **app (page-level)** |

Byte-identical on both routes ⇒ **one defect each**, re-reported by every module that scanned.

**The worse consequence:** `TC_OPP_30` ("Detail opens healthy; **quote editor tabs render**") ran
`verifyAccessibility()` *before* its tab walk. The sidebar bug threw on every single run, so the
quote-editor tabs — the thing the test is named for — **were never actually tested.** A shared
markup defect had been silently deleting functional coverage.

**Fix:** `A11yVerifier` now offers both scopes, and the a11y check in `TC_OPP_30` runs **last**:

- `assertNoBlockingViolations` — whole page; for dedicated a11y tripwires (`TC_ACC_14`, `TC_OPP_43`),
  which stay **correctly red**.
- `assertNoPageSpecificViolations` — excludes sidebar/drawer/FAB/third-party; for functional tests.

Validated on a local fixture reproducing the real sidebar defect (not assumed):

```
whole page -> button-name [.MuiFab-root, .MuiIconButton-root] + listitem [li]
page only  -> button-name [.MuiIconButton-root]
```

The sidebar `<li>` and global FAB are excluded **while a genuinely page-owned nameless button still
fails** — proving the scoped assertion is not a no-op, which was the risk worth checking.

Failure text now names each violating node's CSS target, so an a11y red is actionable instead of
reading "2 node(s): Buttons must have discernible text".

---

## 4. CORRECTION: the 401 storm was token expiry, not session invalidation

I had attributed the 57-call 401 cluster to *"the backend invalidates a user's earlier session when
that user authenticates again"* and wrote that into `396ace7`, `e4eee1a`, and the code comments.
**I measured it, and it is false:**

```
login #1 -> token1
login #2 -> token2            (different token)
token1 /auth/v2/me -> 200     <-- STILL VALID after the second login
token2 /auth/v2/me -> 200
```

Concurrent logins as the same account are harmless. The real cause:

```
JWT claims: exp - iat = 3600s exactly
login body: expires_in  = 3600
```

The suite caches its token once in `setUp()`, and a full run lasts **longer than an hour**. Every API
class executing past the 60-minute mark authenticates with a dead token and 401s on every call. The
failure text then blames the endpoint (*"async-delete semantics changed: now returns 401"*), which is
how one expired token became 57 bogus contract failures.

**It was never a parallelism defect.** A parallel run is simply long enough to cross the expiry line;
sequential runs hit it too whenever an API class lands after the first hour.

**Fix — refresh on a clock instead of reacting to 401s.** `getAuthenticatedRequestSpec()` resolves
through a new `freshToken()`, which re-mints when within 5 minutes of the recorded expiry.
`loginAndGetToken()` now stores that expiry from the server's own `expires_in` — a field the API has
been sending all along and the framework ignored. `withAuthRetry()` remains as a safety net, but is
strictly worse: by the time it fires, the assertion has already seen a wrong status.

---

## 5. Two collision hypotheses tested and REFUTED

Worth recording, because chasing either would have been wasted effort:

- **Site/role state does not collide across threads.** `activeSiteId` and `active_role_id` live in
  **localStorage**, and Selenium gives every thread a fresh browser profile. A
  `"Wrong site selected: X — switching to Test Site"` log line is a thread correcting its own
  browser default, working as designed.
- **`AccountV135RegressionTestNG`'s second browser is not a leak.** It is a deliberate, bounded
  second browser for the ZP-3210 parity check, logs in as a *different* account (PM), and is quit in
  a `finally`.

Combined with the earlier result of **zero data collisions across 1100+ tests**, the parallel design
in `fullsuite-parallel-testng.xml` (`parallel="tests"`, classes serial *within* a `<test>`) holds up.

---

## 6. Per-thread accounts: recommendation withdrawn

I previously called one-account-per-thread "the actual fix". Having checked the inventory, **it is
not viable as-is**: there is exactly **one** admin-level account (`+admin@`). Every other account is a
narrower role — PM, Technician, FM, EE, Account Manager — and the suite's tests assume admin-level
access. Assigning threads to those accounts would trade a handful of auth failures for **mass
access-denied failures**.

It is also moot now: with expiry identified as the real cause, per-thread accounts would not have
fixed anything. If per-thread isolation is ever wanted for other reasons, the QA tenant first needs
additional **admin-level** accounts provisioned — which means creating users, so it needs the owner's
say-so.

---

## Depth explanation — what to take from this

**A failing test is a claim, and claims need verifying — including my own.** Two things today looked
settled and were not. The BAC failure was a real assertion failing against a real 200 response; only
checking the *content-type* revealed it was reading an HTML page. And my own session-invalidation
diagnosis was plausible, fit the evidence, and was wrong — a two-minute `curl` sequence refuted it.
I had already shipped code and told the owner based on it.

**The most expensive bugs in a test suite are the ones that make it quieter, not louder.** The
fabricated BAC finding is loud and gets caught. `TC_OPP_30` throwing on a sidebar `<li>` before it
ever reached the quote-editor tabs is silent — the suite reported a known-bug tripwire while a whole
functional path went untested for an unknown number of runs. Ordering matters: **never put an
environmental or cross-cutting check ahead of the functional assertion a test is named for.**

**Prefer clocks to retries.** `withAuthRetry` absorbs a 401 after the fact. Honouring `expires_in`
means the 401 never happens. When the server tells you when something dies, believe it.
