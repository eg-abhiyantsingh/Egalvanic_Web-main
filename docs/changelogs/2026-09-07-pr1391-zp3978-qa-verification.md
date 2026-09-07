# 2026-09-07 — QA verification: PR #1391 (WO details panel restore) + ZP-3978 (site account ownership & WO mapping)

**Prompt:** check the two tickets (PR #1391 restore-details-panel QA review; ZP-3978 site account ownership & WO mapping implementation) on QA and share artifacts after testing.

## What was done
1. **PR #1391** — walked all 5 QA-review checks live on acme.qa (V1.36): chevron position (SVG-path-verified DOM order), all 8 panel rows on 3 WOs (fallback + populated), Account row above Facility matching grid + `/full` API, Service from registry ("Infrared Thermography") vs "General" fallback, no Quote/Job rows, toggle both directions, Timeframe with real due date and fallback. Verdict **PASS 5/5**.
2. **ZP-3978** — ran the full ownership lifecycle with fresh QA-DEMO data: created 2 accounts + 1 site via UI, wizard-created WO-1 under A (stamped `account_id` A automatically), transferred site A→B (`PUT /api/sld/update`), proved WO-1 retention under A, WO-2 stamped B, edge-tested ownerless-site (blocked) and delete-account-with-WOs (soft-delete, generic confirm, no warning). Verdict: **core flow verified; multi-account & transition co-access not implemented; delete-confirm UX gap**.
3. Flagged the **backfill caveat**: seeded "WO CECCO A" carries Meta's id → pre-#1214 WOs likely backfilled from current owner; pre-feature history does not reflect past ownership.
4. Published 2 Artifact pages (one per ticket) with embedded pixel evidence; wrote 2 verdict docs + 5 evidence screenshots into the repo.

## Deliverables
- Artifact (PR #1391): https://claude.ai/code/artifact/a98c031d-5c16-43af-aa1e-38965850e188
- Artifact (ZP-3978): https://claude.ai/code/artifact/7a4e0040-cbaf-48cb-a0b4-6ef05626c432
- `docs/bug-reports/2026-09-07-pr1391-wo-details-panel-restore-verdict.md`
- `docs/bug-reports/2026-09-07-zp3978-site-account-ownership-verdict.md`
- `docs/bug-evidence/zp-1391-wo-details-panel/*.jpg` (2), `docs/bug-evidence/zp-3978-site-ownership/*.jpg` (3)

## Depth notes (learning)
- **Why the panel restore needed 3 different WOs:** each row has a fallback branch (`value || '—'`, `workTypeService?.name || 'General'`, due-date vs "No due date") — a single WO can only exercise one side of each branch. Coverage = populated + empty per row.
- **Why the ownership test used fresh data instead of the seeded Cecco→Meta site:** the seeded WO's account contradicted its name, but creation-order was unknowable from reads alone — only a controlled create→transfer→re-read sequence can falsify "historical WOs get remapped". It did: they don't.
- **Locator trap found:** `getByRole('button', {name:'Add Site'}).first()` grabbed the *first expanded group's* Add Site (Meta's), not the intended account's — the Create Site dialog carries the account context invisibly. Scope such clicks to the group's Collapse region.
- **MFA screen + `page.screenshot` hang gotchas** handled per project memory (Set up later; CDP `Page.captureScreenshot` with `captureBeyondViewport:false`).
