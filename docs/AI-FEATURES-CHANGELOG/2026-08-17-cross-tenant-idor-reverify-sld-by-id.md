# 2026-08-17 — Cross-tenant IDOR re-verify: resource-level gap closed, new `/api/sld/{id}` leak confirmed

## Prompt
Close the one gap the `/api/company/{id}/*` route-family audit structurally can't see:
**resource-level IDOR** — fetching a foreign tenant's object by its *own* id
(`/api/slds/{id}`, `/api/sessions/{id}`), where there is no `company_id` in the path
for a guard to key on. (Continuation after an adversarial workflow rejected an earlier
"FIXED" verdict on the cross-tenant P1.)

## What was done
- Re-authenticated both QA tenants (acme = customer admin `is_eg_admin:false`; demo =
  `is_eg_admin:true`) and ran a 6-test residual battery from the acme session against
  demo's ids, each with a 3-way control (demo-owner positive control / acme-attacker /
  acme-own), scored by **SHA-256 body equality**, not byte-count heuristics.
- Tests: (1) active-role matrix incl. an EG-Admin overlay the acme account does not own,
  (2) sibling `by-company`/`by-sld` blueprints, (3) `?company_id=`/`?sld_id=` query
  selectors, (4) own-id row provenance, (5) **resource-level object-by-id IDOR**,
  (6) guard shape (garbage/malformed/nonexistent).

## Findings
- **NEW confirmed leak: `GET /api/sld/{id}` (singular).** acme reads demo's **entire SLD
  document** (nodes/edges/issues/tasks/quotes/mappings) — 200, byte-identical to demo's
  own view (`sha256[:16]=089545a705103994`). This is the exact "no company_id in path"
  shape the prompt targeted, and it is a **third route shape** not in the Aug 14 report
  (which covered `by-<scope>/{id}` and `?company_id=`).
- **Precise scope:** the plural `/api/slds/{id}`, `/api/sessions/{id}`, `/api/session/{id}`
  are SPA-shell (not live routes); `/api/ir_session/{id}` is **correctly isolated**. So
  the resource-by-id exposure is SLD-by-id specifically.
- **Re-confirmed still live (3 days after Aug 14):** `contact/by-sld` (PII) and
  `issues/open-by-site?company_id=` both still byte-identical to demo.
- **Write surface bounded:** both leaking routes are `GET, OPTIONS, HEAD` only
  (PUT/DELETE/PATCH → 405) → confidentiality, not integrity. No cross-tenant mutation
  payload was sent (would risk corrupting the demo tenant); cross-tenant write on other
  routes left UNTESTED by design.
- **`/api/company/{id}/*` family remains genuinely fixed** — 422 for foreign/garbage/
  malformed alike, no `is_eg_admin` bypass, no tenant-existence oracle.

## Verdict
The reported P1 repro is fixed; **the breach is not closed.** Keep the ticket OPEN.

## Artifacts
- Report: `docs/bug-reports/2026-08-14-SECURITY-cross-tenant-reverify-by-scope-idor.md`
  (added "Update — 2026-08-17 re-verification").
- Jira: `docs/bug-reports/JIRA-TICKET-cross-tenant-by-scope-idor.md` (added `/api/sld/{id}`
  as lead repro step; title/version updated).
- Evidence receipts: `docs/bug-evidence/cross-tenant-by-sld-idor/2026-08-17-sld-by-id-leak.json`,
  `…-reverify-summary.json`.
