# ZP-4802 — Hide opportunities (mint one per quote); tenant-gate opportunity soft-delete

**Verdict:** PASS — build `index-D1IWZ6SI.js` (v2.4 code live on QA). Backend deployed before frontend (confirmed: `/plans`, `/quote/create-standard` accept a missing `opportunity_id`).

## Checked
| Check | How | Result |
|---|---|---|
| Create Quote dialog | AM seat, /opportunities → New Quote | Fields = Quote Title/#, Facility, Standard/EMP toggle, Use Legacy Quotes. **No opportunity picker, no info tooltip, no subtitle, no New Opportunity dialog.** PASS (shot -2) |
| Quote created (v2) | Standard, non-legacy | POST `/api/plans` → 201, lands on `/plans/{id}`; a hidden opportunity is minted (status `qualifying`, named after the quote, on the quote's site+account). PASS |
| Quote created (legacy) | Use Legacy Quotes ON | POST `/api/quote/create-standard {sld_id,title}` → 201, lands on `/quotes/{id}`; hidden opportunity minted. PASS |
| Quotes grid columns | /opportunities | Created, Quote, Facility, Type, Value, Status, Actions — **no Opportunity column**. PASS (shot -1) |
| Delete prompt | draft quote row delete | "Delete Quote — Only draft quotes can be deleted. Delete <title>?" — **does not name an opportunity**. PASS (shot -8) |
| `/opportunities/<id>` route | typed | **Redirects to the Quotes list** (route unrouted). PASS (shot -6) |
| Add to quote from issues | /issues → Add to Quote | Dialog "New quote / Existing quote" — **no opportunity picker**; creates the quote without an opportunity_id. PASS (shot -9) |
| Delete legacy EMP linked to an opportunity | /emps row delete | Delete button **enabled**, prompt "Delete EMP … reversed by an admin". PASS (shot -10) |
| Orphan guard (409) | soft-delete minted opp while its quote is live | `DELETE /opportunity/{id}/soft-delete` → **409 "This opportunity still has 1 quote(s). Delete them first."**; after deleting the quote → 200. PASS |
| Tenant: soft-delete another company's opportunity | AM + PM (is_eg_admin:false) vs opp `15e74616…` on site `f86052b7…` (company fdcadc86, NOT acme) | → masked-404 HTML, opportunity stays `is_deleted:false`. Random uuid + malformed id also refused. PASS (shot -11) |
| Positive control | AM soft-deletes its OWN minted opportunity | → 200 "Opportunity soft deleted successfully". Confirms 404 on foreign is a real refusal, not a blanket block. PASS |
| Site-reach on POST /plans | AM, foreign site / malformed / random site | all masked-404 (refused). PASS |
| Roles | AM, PM, Super Admin | Same gate behaviour; staff is_eg_admin excluded from attacker role per [[feedback_check_is_eg_admin…]] |

Cleanup: all QA-DEMO quotes + minted opportunities I created were hard-deleted / soft-deleted.

## Bugs found
**B1 (adjacent, not blocking this ticket) — `POST /quote/create-standard` with a malformed `sld_id` returns 500 `internal_error`, not 404/400.**
- Steps: as any quote-capable seat, `POST /api/quote/create-standard {sld_id:"not-a-uuid", title:"x"}`.
- Actual: 500 `{"error":"internal_error","trace_id":…}`. Expected: 400/404 (the ticket says malformed id → 404). `POST /plans` handles the same bad id gracefully (masked-404). Only `create-standard` 500s.
- Severity: Low (needs an authed seat deliberately sending a bad id; no data exposure). Assignee: Avani (backend). No existing ticket covers it.

**B2 (adjacent to the tenancy theme, HIGH) — `GET /api/quote/by-sld/{foreign_sld}` and `/lightweight` leak another company's quotes to any customer seat.**
- Steps: as PM/AM/EE/FM (is_eg_admin:false), `GET /api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39/lightweight`.
- Actual: 200 JSON, 6 of the other company's quotes (titles incl. "EMP for SLD f86052b7…", "Transformer-Repair"). Controls: own site = own quotes; random uuid = 0. The site belongs to company fdcadc86 (NOT acme; staff seat itself gets 422 on `/api/sld/{that}`), so this is cross-tenant.
- This is NOT fixed by ZP-4802 (which only gated `soft-delete` + `POST /plans` reach). `opportunity/by-sld` is correctly 422'd, but `quote/by-sld` is not. Severity: High (cross-tenant read). Assignee: Avani. Same root family as ZP-4709 (nodes/sld) — mention overlap to owner.

## Enhancement suggestions
- Quotes grid has no Type/Status filter chips exposed as a toolbar (only a status "cycle" button) and no date-range filter — add explicit filter controls.
- `/opportunities/<id>` silently redirects; a one-line toast ("Opportunities are now managed as Quotes") would avoid confusing a user who followed an old deep link.

## Mobile part for Nency
iOS/Android quote creation must also drop the opportunity picker and mint one per quote; confirm the mobile soft-delete of a foreign opportunity id is refused (404) and own is allowed.

## Not covered
Didn't drive EMP accept→create flow end-to-end (out of this ticket's scope). B2 leak confirmed only on read, not probed for write.

## Proposed Jira comment
h3. QA — ZP-4802 PASS (QA build index-D1IWZ6SI.js)

Opportunities are hidden from the quote flow and soft-delete is tenant-gated:
* Create Quote / Add-to-Quote-from-issues — *no opportunity picker, tooltip, subtitle or New Opportunity dialog*; a hidden opportunity is minted per quote (status qualifying, on the quote's site+account).
* Quotes grid — *no Opportunity column*; delete prompt no longer names an opportunity.
* {{/opportunities/<id>}} redirects to the Quotes list; a legacy EMP linked to an opportunity is deletable again.
* Orphan guard: soft-deleting an opportunity with a live quote → *409*; after deleting the quote → 200.
* Tenant: {{DELETE /opportunity/\{another-company\}/soft-delete}} from a customer seat (is_eg_admin:false) → refused, opportunity stays live. Positive control: own minted opportunity → 200.

!QA-12Oct-ZP-4802-2-create-quote-dialog-v2.png|width=720!
!QA-12Oct-ZP-4802-11-soft-delete-tenant-gate-proof-PM.png|width=720!

Two adjacent finds (separate tickets): (1) {{POST /quote/create-standard}} with a malformed sld_id → 500, not 404; (2) {{GET /quote/by-sld/\{foreign\}}} still leaks another company's quotes to a customer seat (same family as ZP-4709).

## Recommended status
**READY TO RELEASE** for ZP-4802's own scope (all AC pass). File B1 + B2 as separate tickets (B2 High, cross-tenant).
