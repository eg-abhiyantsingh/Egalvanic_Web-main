# [Web] Work-Order scope-preview leaks another company's asset inventory + building locations

**Severity:** Critical · **Priority:** High
**Environment:** QA (Project Z), V1.36 · `acme.qa.egalvanic.ai` + `demo.qa.egalvanic.ai`
**Endpoint:** `POST /api/ir_session/scope-preview` (drives Create-WO "Work Type auto-selects matching assets")

---

## What's wrong (one line)
The endpoint that resolves which assets a Work Order Type covers reads whatever `sld_id` you send without checking it belongs to your company — so a logged-in user of Company B can list Company A's entire asset inventory and building locations from their own browser.

## Impact
A normal, ordinary logged-in user of one tenant can pull another tenant's full equipment inventory — asset names, equipment classes, and **physical location PII** (site / floor / room). No admin rights, no tampering, works on the user's own host. This is the auto-selection feature's own driving call.

## Steps to reproduce
1. Log in as a **Company B** user (used: `demo.qa.egalvanic.ai`, `shubham.goswami@egalvanic.com`, company `93611164`).
2. On **Company B's own host**, POST a **Company A** SLD id:
   ```
   POST https://demo.qa.egalvanic.ai/api/ir_session/scope-preview
        {"sld_id":"<companyA_sld>","work_type_id":"<any A work type>","asset_scope":null,"enrich":true}
   ```
3. **Actual:** HTTP **200** JSON — **84 of Company A's assets**, each with `label` (`ats`, `ATS-EM-EL`, …), `node_class_name`, and **`room_label`** like `"712 5th Ave / Floor 1 / Retail Switchgear Room"`, `"Building B1 / Floor f1 / Room R1"`.
   **Expected:** rejected — the SLD is not the caller's company's.
4. Confirmed **both directions** (Company A token + a Company B SLD → Company B's assets).

**Self-discovering:** `GET /api/ir_session?limit=50` returns 1734 sessions (incl. the other tenant's) and `/users/{uid}/slds` enumerates SLD ids — so the attacker doesn't need to know the id in advance.

## Why Critical (not the earlier request-tamper Highs)
Auth *is* enforced (no token → 401; company is bound from the JWT, not the host). But there's **no caller-company gate on `sld_id`**. Unlike the sibling `eg-form-instance` leaks (pin, previous) that only leak when you aim the request at the *other tenant's host*, this one leaks on the **caller's own host** — a normal Company B browser reaches it directly. So it's normal-user-reachable, hence Critical.

Sibling endpoints also leak to the foreign token: `GET /api/sld/{A_sld}` and `/api/lookup/nodes/{A_sld}` return A's edges/nodes.

## Controls (so it's not mis-called)
- No token → **401** (auth works).
- Random/unknown `sld_id` → 200 but **empty** (unknown-id is a different code path from foreign-tenant — the foreign one returns real data).
- Garbage `work_type_id` → 200, 0 assets (work_type is a real filter; the `sld_id` is still read and its tenant not checked).

## Root cause / fix (for the dev)
Same platform-wide gap as the NETA-3 `by-session`, EG-pin, ghosts-`previous`, and covered-services findings: the tenant guard is per-route, and `ir_session/scope-preview` doesn't have it. **Add the sld→company check against the caller's JWT `company_id`; return 404/403 on mismatch.** Audit the whole `ir_session` + `eg-form-instance` + `procedures-v2` family together — this is now the 5th instance of the same missing check.

## Evidence
Independently reproduced (adversarial panel + hand re-verify), demo→acme on the demo host, distinct companies confirmed via `/auth/me` (JWT-resolved, host-independent). 200 + JSON with acme labels + `room_label` PII = leak (not a masked-404).
