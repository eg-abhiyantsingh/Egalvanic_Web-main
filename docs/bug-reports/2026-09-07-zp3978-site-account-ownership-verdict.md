# QA Verdict — ZP-3978: Site Account Ownership & Work Order Mapping

**Ticket:** [Web] Site Account Ownership & Work Order Mapping - Implementation
**Environment:** acme.qa.egalvanic.ai · V1.36 · 2026-09-07 · admin seat
**Method:** full ownership lifecycle via UI with fresh QA-DEMO data + API traces
**Artifact:** https://claude.ai/code/artifact/7a4e0040-cbaf-48cb-a0b4-6ef05626c432

## Verdict: core flow VERIFIED (steps 2/3/4); steps 1 & 5 NOT IMPLEMENTED (single-owner model); step 6 mixed

| Step | Verdict | Detail |
|------|---------|--------|
| 1. Multiple accounts per site, distinct primary | **NOT PRESENT** | Edit Site = single required Account autocomplete; no site-accounts endpoints; Customers tree shows each site under exactly one account. Site "Manage Access List" is users, not accounts |
| 2. Transfer ownership A→B | **PASS** | Edit Site → Account → Save; `PUT /api/sld/update/{id}` with new `account_id` → 200; grouping updates (A: 0 sites, B: 1 site) |
| 3. Historical WOs stay under A | **PASS** | WO-1 (created under A) still shows Acct A in grid + panel + API after the transfer |
| 4. New WO maps to B | **PASS** | WO-2 wizard-created after transfer: `POST /api/ir_session/create` returned `account_id` = Acct B, stamped automatically (wizard never asks for account) |
| 5. Both accounts access site during transition | **NOT PRESENT** | Post-transfer, site exists only under B; A's account page shows Sites: none; only tie is historical WOs |
| 6. Edge: remove account from site with WOs | **MIXED** | ✅ Site can't be left ownerless (clearing Account disables Save). ⚠️ Deleting an account with mapped WOs succeeds via generic confirm ("Are you sure you want to delete this item?") with no WO-impact warning — soft delete (`/api/account/{id}/soft-delete` → 200); WO rows keep resolving the deleted account's name, so no data loss |

## The controlled experiment
1. Created `QA-DEMO ZP3978 Acct A` (`3fd1c782-a427-480a-af64-d50924cd11a8`) and `Acct B` (`a4499f71-ef53-4f10-9f6b-8c8fb1233948`).
2. Created `QA-DEMO ZP3978 Site` (`344ddef5-de54-4171-8bf0-15e75d5f1fff`), owner = A.
3. WO-1 `07beee76-086d-44cb-b46b-7cae8ed8ad77` (due 09/30) → stamped Acct A.
4. Transferred site to B (`PUT /api/sld/update`, 200).
5. WO-1 re-checked → still Acct A ✅. Site → B ✅.
6. WO-2 `1e6f95ca-0d1e-4fb6-bdad-3a8eab820550` (due 10/15) → stamped Acct B ✅.
7. Edges: clear-account blocked ✅; Acct A soft-deleted with generic confirm ⚠️ (WO-1 label still resolves).

## Observations for the team
- **Backfill caveat (confirm with backend):** seeded "WO CECCO A" on Common Site A (created 2026-08-26, pre-#1214) carries **Meta's** account id despite its name recording CECCO intent. Live retention provably works, so existing WOs were most likely backfilled from the site's *current* owner when the feature shipped → pre-feature history does not reflect past ownership (the exact Cecco→Meta case).
- **"Add Site" inherits its account invisibly:** Create Site dialog has no Account field; target account implied by which group's "Add Site" was clicked. Easy to create a site under the wrong account (this run did, first attempt).
- **"Viewing WOs by account" = grid column only:** WO list Account column (sortable/searchable) is the entire surface; account detail page (Details/Internal Team/Contacts/Quotes/Sites/Notes) has no Work Orders tab.

## Test data (labeled, left in place per convention)
Accounts A (soft-deleted during edge test) & B, site, and 2 WOs all named `QA-DEMO ZP3978 … (delete me)`.

## Evidence
- `docs/bug-evidence/zp-3978-site-ownership/01-wo1-under-acct-a-before-transfer.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/02-wo-list-same-site-two-accounts.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/03-customers-grouping-after-transfer.jpg`
