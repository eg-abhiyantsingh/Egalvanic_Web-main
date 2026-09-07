# 2026-09-07 — ZP-3978 round 2: create an asset in a work order + sweep the side impact of an account change

**Prompt:** "also you need to create asset also in work order and check side impact too if you change account what will happen" (+ "always share test data full url in artifact, save that in memory")

## Why round 2 was needed
Round 1 proved the account mapping on **empty** work orders (0 assets, 0 issues). That leaves the
real question unanswered: when a site changes hands, what happens to a WO that actually contains
work — scoped assets and issues raised against them? A pointer move and a cascading re-parent look
identical on an empty record.

## What was done
1. **Created an asset** on the QA-DEMO site: `QA-DEMO ZP3978 Asset` (Panelboard),
   `ed12b661-6674-48a8-b6be-520ce6b69ada` — `POST /api/node/create` → 201.
2. **Created WO-3** (`42d3093a-5e97-4df9-b551-c5dee9000dea`) via the 4-step wizard; the Infrared
   Thermography service rules resolved the asset into scope live ("1 of 1 matching asset selected").
3. **Raised an issue** on that asset inside the WO (NEC Violation) — `POST /api/issue/create` → 201.
4. **Created Acct C** and transferred the site **B → C** (`PUT /api/sld/update`, 200).
5. **Swept every attached entity** post-transfer: WO account, asset scope, issue, counts, asset
   detail page, WO ledger across all three owners, console/page health.

## Result: an account change is a pure pointer move
| Entity | Verdict |
|---|---|
| Site owner | changed to Acct C (intended); asset count follows the site |
| WO-3 account | **retained Acct B** — populated WOs behave like empty ones |
| Asset scope / Issue / counts | intact (Assets 1, Issues 1, `badges.open_issues: 1`) |
| Asset itself | intact — site-scoped, not account-scoped |
| Console | clean (only pre-existing 3rd-party DevRev `plug.js` integrity noise) |

Nothing downstream of the site is account-scoped, so there is no cascade and no orphaning. The
feature's risk is **reporting/billing attribution**, not data integrity.

## Also found
- **Cosmetic:** wizard Review step prints "Scope: starts empty" even when the Scope step resolved
  1 asset and the created WO has it.
- **Test-data URLs** now listed in full in both artifacts + the verdict doc (owner request), and the
  rule saved to memory as `feedback_full_test_data_urls_in_artifacts`.

## Deliverables
- Artifact (updated, same URL): https://claude.ai/code/artifact/7a4e0040-cbaf-48cb-a0b4-6ef05626c432
- `docs/bug-reports/2026-09-07-zp3978-site-account-ownership-verdict.md` (Round 2 section + full URLs)
- `docs/bug-evidence/zp-3978-site-ownership/04-wo3-asset-issue-intact-after-transfer.jpg`, `05-customers-site-under-acct-c.jpg`

## Depth notes (learning)
- **Why an empty record can't answer this:** with 0 children, "keeps its old account" and
  "re-parents everything" produce the same observation. Adding one asset + one issue creates the
  divergence that makes the two hypotheses distinguishable — the same reason round 1 needed a
  controlled create→transfer→re-read instead of reading seeded data.
- **Drawer vs dialog:** Add Asset and Add Issue render as `.MuiDrawer-paper`, so
  `[role="dialog"]` queries return null and read as "form failed to open". Query the drawer by its
  heading text instead.
- **The empty-site interstitial** ("Let's get your assets in" → Upload Anything) intercepts the
  first Create Asset click on a site with no assets; "Not now" clears it.
