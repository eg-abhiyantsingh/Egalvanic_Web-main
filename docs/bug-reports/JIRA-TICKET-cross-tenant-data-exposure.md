# JIRA TICKET — ready to paste

> Not filed automatically. Per project rule, Jira is never modified without per-ticket approval.
> Attach both PNGs from `docs/bug-evidence/security-cross-tenant/` when creating the issue.

**Issue Type:** Bug &nbsp;·&nbsp; **Labels:** `security` `access-control` `idor` `multi-tenancy` `backend`
**Component:** Backend / API &nbsp;·&nbsp; **Affects Version:** V1.36 (QA)

---

## Title

**[Security / API] Cross-tenant data exposure — a customer admin can read another company's facilities, work orders and sales data via `/api/company/{company_id}/*`**

---

## Environment

* **Environment:** QA (`https://acme.qa.egalvanic.ai`) — **production status UNVERIFIED, must be checked**
* **Platform:** Web (backend API; reproducible from any HTTP client)
* **Browser/App Version:** Google Chrome latest stable · app build **V1.36**
* **Device:** Desktop (not device-specific — server-side authorisation defect)
* **Tenants involved:** `EG-ACME` (`d59d449b-09d8-45d6-8f0a-ef70024b1293`) and `Demo Company` (`93611164-13e6-47da-b2cd-a150e73173f6`)

---

## Preconditions

* User is logged in to tenant A (EG-ACME) as an ordinary customer admin.
* The account is **NOT** internal eGalvanic staff — `/api/auth/v2/me` returns **`is_eg_admin: false`** (roles: Super Admin). This matters: it rules out intended cross-tenant staff access.
* The `company_id` UUID of tenant B is known. (UUIDs are not secrets — they appear in URLs, API payloads, exports and support tickets, and any user of tenant B can read their own from `/api/auth/v2/me`.)

---

## Steps to Reproduce

1. Log in to `https://acme.qa.egalvanic.ai` as a customer admin of EG-ACME.
2. Confirm the caller is not internal staff — open the browser console and run:
   `await (await fetch('/api/auth/v2/me',{credentials:'include'})).json()` → note **`is_eg_admin: false`**.
3. Request **another company's** data by substituting their `company_id` in the path:
   `await (await fetch('/api/company/93611164-13e6-47da-b2cd-a150e73173f6/slds',{credentials:'include'})).json()`
4. Repeat for `…/sessions` and `…/sales-attention`.
5. As a control, run the same call with your **own** `company_id`, and run `await fetch('/api/reporting/configs/00023b86-19cc-40b5-b54e-606d2947d97d',{credentials:'include'})` (a Demo-owned report config).

---

## Actual Result

Steps 3–4 return **HTTP 200 with the other company's live data**. Every returned row carries the **foreign** `company_id`:

| Endpoint | Returned to EG-ACME |
|---|---|
| `GET /api/company/{demo}/slds` | 2 facilities — `test`, `Demo` — each with Demo's `company_id` + `account_id` |
| `GET /api/company/{demo}/sessions` | 2 work orders — **"DEMO Company QA"**, "1aug test workorder" |
| `GET /api/company/{demo}/sales-attention` | Demo's facility names, `sld_id`, `account_id` |

The reverse direction was also confirmed (a Demo session read **188 EG-ACME facilities**, 52 KB).

Step 5 shows the endpoint is otherwise healthy (own company → 188 rows) **and** that the same substitution is correctly refused elsewhere: `/api/reporting/configs/{foreign-id}` → **HTTP 401 `{"error":"Access denied"}`**.

---

## Expected Result

Any request whose `{company_id}` path parameter does not match the authenticated user's company must be rejected with **401/403**, exactly as `/api/reporting/configs/{id}` already does — unless the caller is `is_eg_admin`, if cross-tenant staff access is intended.

---

## Severity

**Critical** — cross-tenant confidentiality breach in a multi-tenant SaaS. One customer can read another customer's facility inventory, work-order titles and account identifiers. The only barrier is knowledge of a company UUID, which is not a secret.

*(Downgrade to High if the team judges the exposed fields low-sensitivity. Escalate to Blocker if production is affected or if write operations prove equally unguarded — see Open Questions.)*

## Priority

**Highest (P1)** — verify production before anything else.

---

## Open Questions / Not Yet Determined

1. **Is production affected?** Verified on QA only; the same code path is likely. **Check first.**
2. **Full route inventory.** 8 `/api/company/{id}/*` endpoints probed: 3 clearly leak, 2 returned 0 rows (ambiguous — Demo may simply have none), 2 route differently. Assume more are affected until audited.
3. **Write operations untested.** Only GETs were issued. Whether `POST/PUT/DELETE` under `/company/{id}/` are equally unguarded is **unknown and should be treated as urgent** — a write-side equivalent would be materially worse.

---

## Suggested Fix

Enforce the `{company_id}` ↔ authenticated-company check in **one shared place** (middleware/decorator) rather than per route, with an explicit `is_eg_admin` bypass if staff access is intended. Then add a contract test that walks every `/company/{id}/` route with a foreign id and asserts 401/403, so this cannot regress silently.

---

## Attachments

1. `SEC-EVIDENCE-1-cross-tenant-proof.png` — the four-step proof in one frame: caller identity (`is_eg_admin: false`), the leaked `slds` response with foreign `company_id`/`account_id`, the own-company control (188 rows), and the `/reporting/configs` **401** contrast.
2. `SEC-EVIDENCE-2-blast-radius.png` — foreign work orders (incl. "DEMO Company QA") and sales-attention rows with foreign `account_id`, plus the own-company control (1066 rows).

Both under `docs/bug-evidence/security-cross-tenant/`. Full technical write-up:
`docs/bug-reports/2026-08-11-SECURITY-cross-tenant-data-exposure-company-endpoints.md`
