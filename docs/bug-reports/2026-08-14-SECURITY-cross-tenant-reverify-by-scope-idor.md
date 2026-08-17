# Cross-tenant data exposure (P1) — re-verification: **PARTIALLY FIXED, breach still LIVE on a sibling route family**

**Investigated:** 2026-08-14 · **Build:** QA **V1.36** · **Hosts:** `acme.qa.egalvanic.ai` (attacker) + `demo.qa.egalvanic.ai` (victim/ground-truth)
**Original ticket:** *Web: [Security / API] Cross-tenant data exposure* — a customer admin of EG-ACME (`is_eg_admin:false`) could `GET /api/company/{OTHER}/slds|sessions|sales-attention` and receive HTTP 200 with another tenant's live data.
**Question asked:** "check this is fixed or not."

---

## Verdict — one line

**The three reported routes are fixed. The vulnerability is not.** The same cross-tenant read is **live today** on at least **3 other routes** (and a family of ~10 more that share the pattern), because the fix was applied to the `/api/company/{id}/*` route family only, and the guard is **per-route**, not global. A customer admin of EG-ACME can still read another tenant's contacts (PII), site/issue counts, and session→node mappings.

> I nearly shipped a "FIXED" verdict off the first three routes. An adversarial review of that verdict (3 independent skeptics + a judge) flagged that "these 3 routes are guarded" had been over-generalised to "tenant isolation holds." Testing the predicted gaps found the live leaks below. **The re-test changed the answer.**

---

## Update — 2026-08-17 re-verification (still LIVE + a new confirmed route)

Re-ran the full battery **3 days later** with fresh auth for both tenants. **Nothing has been fixed.** All previously-confirmed leaks still return the victim tenant's data, byte-for-byte identical to the victim's own session (SHA-256 match on every body). Additions from this pass:

- **NEW confirmed leak — `GET /api/sld/{id}` (singular).** A customer admin of EG-ACME (`is_eg_admin:false`) fetching Demo's SLD id `24eb08b1…` receives the **entire foreign SLD document** — the single-line diagram plus its `nodes / edges / issues / tasks / quotes / comments / mappings` — **200, byte-identical** to Demo's own view (`sha256[:16]=089545a705103994` on both sides). This is the purest form of the gap: a top-level resource fetched by its own id, **no `company_id` in the path** for the per-route guard to key on. The own-tenant control returns acme's own SLD (1.67 MB for a populated diagram), so a populated foreign SLD would leak in full. This route is **not** in the `by-<scope>` list above — it is a plain `/{id}` resource route, i.e. a third shape the fix missed.
- **Precise route note.** The leak is on the **singular** `/api/sld/{id}`. The plural `/api/slds/{id}`, `/api/sessions/{id}`, and `/api/session/{id}` fall through to the SPA `index.html` (200-HTML, no data) — they are not live API routes. `GET /api/ir_session/{id}` is **properly isolated** (foreign id → SPA shell, own id → JSON), so session-by-id is *not* leaking; the exposure is SLD-by-id.
- **Write surface is bounded (confidentiality, not integrity, on these routes).** Non-destructive method probe (`OPTIONS` + wrong-method, no mutation payload sent): both `/api/sld/{id}` and `/api/contact/by-sld/{id}` return `Allow: GET, OPTIONS, HEAD`; `PUT/DELETE/PATCH` → **405**. So a foreign object cannot be *modified* through these specific routes. Cross-tenant **write** on other `by-<scope>` routes remains UNTESTED by design — I did not send mutation payloads that could corrupt the Demo tenant.
- **Guard-shape re-confirmed.** `/api/company/{garbage-uuid}/slds` and `/api/company/{malformed}/slds` both → 422 (identical 118 bytes); `/api/company/{demo}/zzz-nonexistent` → SPA shell. Same conclusion as Aug 14: the guard is inside each `/company/{id}/*` handler, not a global `before_request`, and there is no tenant-existence oracle.

Evidence receipts (live bodies, this pass): `docs/bug-evidence/cross-tenant-by-sld-idor/2026-08-17-sld-by-id-leak.json` and `…-reverify-summary.json`.

---

## What IS fixed (verified)

| Route (foreign company/sld id) | Before (2026-08-11) | Now |
|---|---|---|
| `GET /api/company/{id}/slds` | 200 + data | **422 `permission_denied`** |
| `GET /api/company/{id}/sessions` | 200 + data | **422** |
| `GET /api/company/{id}/sales-attention` | 200 + data | **422** |
| + 11 more `/api/company/{id}/*` GET/POST-for-list routes | — | **422** (14/14) |
| `POST /api/account/by-company/{id}/v2` (Accounts list) | — | **422** |

All return a byte-identical 118-byte `{"error":"permission_denied","message":"You do not have access to this company's data."}`. The own-company control returns 200 (199 SLD rows / 55,597 bytes), so the endpoints are healthy and the 422s are genuine denials, not a session artifact.

**This guard is a real tenant check, not an RBAC accident.** Proof: I set `x-active-role-id` to an EG-Admin overlay the acme account does not even list, which made `/api/auth/v2/me` resolve `is_eg_admin:true` — an identity that passes *any* permission check — and the foreign reads **still** returned 422. A permission-decorator would have let `is_eg_admin:true` through; a tenant guard does not. So there is **no `is_eg_admin` staff bypass** on this family either.

---

## What is NOT fixed — 3 confirmed live cross-tenant leaks

Same attacker: EG-ACME **customer** admin, `is_eg_admin:false`, company `d59d449b…`, on `https://acme.qa.egalvanic.ai`. Target: tenant **Demo** (`93611164…`). Every response below is **byte-identical to what Demo's own session sees** for the same id (ground truth captured live), and each has an own-tenant + random-id control.

| # | Route | Attacker → | What leaks | Control (own) | Control (random) |
|---|---|---|---|---|---|
| **1** | `GET /api/contact/by-sld/{demo_sld}` | **200** | Demo's **contact PII** — `sandbox@egalvanic.com`, name, job title, contact id | acme's own contacts | masked-404 |
| **2** | `GET /api/issues/open-by-site?company_id={demo}` | **200** | Demo's **site names + open-issue counts** (`sld_name:"Demo", count:2`) | 832 acme issues | empty |
| **3** | `GET /api/mapping/node-session/by-session/{demo_session}` | **200** | Demo's **session→node mapping** (`node_ids:["d435fd13…"]`) | empty | empty |

**Evidence:** `docs/bug-evidence/cross-tenant-by-sld-idor/idor-evidence.png` (each leak shown next to Demo's identical own-session response) and `evidence.html`. The `idor-evidence.png` was rendered from bodies fetched live from the acme origin — the in-browser fetch returned `sandbox@egalvanic.com` for leak 1.

### The pattern — and why more routes are latently vulnerable

The unguarded routes all share the shape **`/api/<resource>/by-<scope>/{object_id}`** (or a `?company_id=`/`?sld_id=` query selector). They resolve the object by its id and return its children **without checking the object belongs to the caller's tenant**. The `/company/{id}/*` family got a tenant guard; this family did not.

I harvested the whole `by-<scope>` family from the app's JS bundles. Confirmed-leaking are the three above. The rest returned **empty for the Demo tenant only because this particular tenant has no data on those objects** — they are the *same unguarded code path* and will leak on any tenant that does have data:

```
/api/quote/by-sld/{id}                         (empty on Demo; acme own = data)
/api/opportunity/by-sld/{id}                   (empty on Demo; acme own = 58 rows)
/api/eg-form-instance/by-session/{id}          + /count + /node-status + /by-node/{id}
/api/form-instance/by-form/{id}  · /by-task/{id}
/api/planned_workorder/by-quote/{id}
/api/planned_workorder_line/by-workorder/{id}
/api/shortcut/by-node-class/{id}
/api/graph/nodes/{id} · /graph/nodes/{id}/enriched · /graph/edges/{id}
```

**These are not "14/14 routes guarded."** They are "14 routes in one family guarded, and a second family — which the web bundle alone does not fully enumerate (iOS-only and role-gated routes are outside it) — is not."

---

## Root cause (as far as black-box testing can see)

The tenant check is bound to **registered routes under the `/company/{company_id}/` blueprint**, applied per-route (a shared helper — hence the byte-identical error — but *not* a global `before_request`). Two observations pin this down:

- `GET /api/company/{demo}/zzz-not-a-real-route` → **masked-404 HTML, not 422**. A global/blueprint `before_request` would have 422'd before routing. It didn't, so the guard runs *inside* each route handler.
- `GET /api/company/{random-uuid}/slds` → **422** (same as a real foreign id). So on a *guarded* route the check fires for any non-own company id, before the tenant is even known to exist (no 422-vs-404 tenant-existence oracle — good).

Per-route guards have a built-in failure mode: **every new or sibling route must remember to apply the guard.** The `by-<scope>/{id}` routes did not. That is the exact class the fix left open.

---

## Corrections to the original 2026-08-11 ticket

1. **"14/14 / no leaks" would have been wrong.** The correct statement is: the reported `/company/{id}/*` family is fixed; the cross-tenant class is **still live** on `by-<scope>/{id}` routes. I am flagging this because I generated the route-family audit that produced the "14/14" number — it was complete *for that family* and misleading *as a closure statement*.
2. **The ticket's "reverse direction also confirmed (a Demo session read EG-ACME facilities)"** should be de-emphasised: the demo account is `is_eg_admin:true` (internal staff). A staff account reading a customer tenant may be *intended* support access, not a leak — a different question from the customer→customer breach that is the actual P1. (Separately: staff→customer `/company/{id}/*` reads are now **also** 422, so if that access was intended, there may be a functional regression for internal tooling — worth its own ticket, not this one.)
3. **The suggested fix said "add an `is_eg_admin` bypass if staff access is intended."** Note the implemented guard blocks `is_eg_admin:true` too. Decide that explicitly before adding a bypass — and if a bypass is added, the fix below (extend the guard to `by-scope` routes) must be re-verified afterwards.

## Secondary observation (not the headline; needs a separate look)

`GET /api/auth/v2/me` with `x-active-role-id` set to a role the caller does **not** hold (`e9ad3158…`, the EG-Admin overlay) returned `is_eg_admin:true, roles:["Admin"]`. The cross-tenant guard held regardless, so **no data leaked via this path** — but a `/me` that reflects `is_eg_admin:true` for an unowned role is worth checking against *other* authorization decisions (UI gating, any check that trusts `is_eg_admin`). It may also be legitimate (the acme test account is an `@egalvanic.com` address that could be entitled to that overlay globally). Verify before treating as a bug.

---

## Recommendation

1. **Keep the P1 OPEN.** Retitle to reflect that the `/company/{id}/*` family is fixed and the breach persists on object-id-scoped routes.
2. **Apply the tenant check to the `by-<scope>/{id}` family and to `?company_id=`/`?sld_id=` selectors.** The durable fix is a single enforcement point that resolves the object's owning tenant and compares it to the caller's, applied by default rather than per-route. Confirmed leaks to fix first: `contact/by-sld`, `issues/open-by-site`, `mapping/node-session/by-session`; then the latent list above.
3. **Add a CI contract test** (fits beside `APISecurityTest`) that, for every object-id/`by-scope` route, fetches a foreign tenant's object and asserts 4xx, with an own-tenant 200 control — keyed on company/object **UUID and role ID**, never role name (the V1.36 rename already broke a name-keyed test once).
4. **Production is UNVERIFIED** (no prod tenant pair, and the change is not attributed to a commit/flag). Re-run this whole battery on prod before closing.

---

## Method notes

- Two real tenants (acme customer + demo staff), Cognito bearer per tenant, requests issued to each tenant's own subdomain (`eg-pz` gateway host returns `401 "Invalid authentication configuration"` — tenant context comes from the subdomain).
- Every "leak" is confirmed three ways: (a) attacker 200 body **byte-identical** to the victim's own-session body for the same id; (b) own-tenant control returns the *attacker's* data (route works when scoped); (c) random-id control returns empty/404 (not a blanket 200).
- The `by-<scope>` family was enumerated from a BFS over the app's lazy JS chunks (17.8 MB), not guessed. Blind spots that remain and are stated, not hidden: iOS-only routes and role-gated lazy chunks are outside a single web session's bundle.
- Adversarial verification of the verdict: `verify-crosstenant-fix-verdict` workflow — 3 refutation lenses (completeness / inference / risk-of-reassurance) + a judge. It is what turned a wrong "FIXED" into this report.
