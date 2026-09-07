# 2026-09-07 — ZP-3978 round 3: create the asset INSIDE the work order + re-check the account-change side impact

**Prompt:** "you need to go in work order then create asset wrong follow dear" (correction of round 2),
plus "try to test fast" / "save in memory to test fast".

## The correction
Round 2 created the asset from the **site-level Assets page** and let the wizard's service rules pull
it into the WO's scope. That is not the path a technician takes. Round 3 redid it properly:
**open the work order → Actions → Add New Asset**, so the asset is born inside the WO.

## What was done (batched into few browser calls, per the new speed directive)
1. WO-2 (`1e6f95ca…`, Acct B) → Actions → **Add New Asset** → `QA-DEMO ZP3978 Asset-in-WO`
   (Switchboard, `0794a33d-a759-4952-afd9-534bdaf07746`), `POST /api/node/create` → 201.
   Assets badge went 1 → 2 immediately.
2. Issues tab → Actions → **Add Issues** → `POST /api/issue/create` → 201.
3. Transferred the site **Acct B → Acct C** so the WO's account and the site's owner diverge.
4. Re-checked: WO-2 still **Acct B**; Assets (2) and Issues (1) intact; both asset rows render
   (site-level Panelboard + in-WO Switchboard).

## VERIFIED BUG — stale asset picker in Add Issues
Immediately after creating the asset inside the WO, `Actions → Add Issues` showed an asset dropdown
listing only the pre-existing asset; the just-created Switchboard was **absent**, so the issue could
not be attached to it. After a page reload the picker listed both. The Assets tab badge had already
incremented — the two views disagree within the same work order.

## Confirmed (unchanged from round 2)
An account change is a pure pointer move on `sld.account_id`. Assets belong to the SITE, issues carry
no account reference, and WOs keep the account they were created under — regardless of whether the
asset was created at site level or inside the work order.

## Deliverables
- Artifact (updated, same URL): https://claude.ai/code/artifact/7a4e0040-cbaf-48cb-a0b4-6ef05626c432
- `docs/bug-reports/2026-09-07-zp3978-site-account-ownership-verdict.md` (Round 3 + the bug)
- `docs/bug-evidence/zp-3978-site-ownership/06-wo2-in-wo-asset-after-transfer.jpg`
- New memory: `feedback_test_fast_batch_actions` (batch whole flows into one browser call)

## Depth notes (learning)
- **Why the in-WO path matters beyond pedantry:** it exercises a different code path — direct scope
  attachment instead of rule-based resolution — and that path is exactly where the stale-picker bug
  lives. The site-level route would never have surfaced it.
- **The WO Actions menu is the real hub** (10 items: Upload IR Photos, Add New Asset, Add Existing
  Asset(s), New Quick Count, Manage Forms, Fill from Photos, Export/Import Excel, Add Issues,
  Generate Report) — worth targeting directly in future WO tests.
- **Speed lesson:** the same lifecycle that took ~15 calls in round 1 took 4 in round 3 by batching
  navigate → fill → submit → assert → capture into single `browser_run_code_unsafe` calls.
