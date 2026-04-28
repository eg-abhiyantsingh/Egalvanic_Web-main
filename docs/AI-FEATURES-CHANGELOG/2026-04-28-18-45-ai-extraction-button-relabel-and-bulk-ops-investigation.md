# AI Extraction — TC_AIExt_01 surface fix + Bulk Ops investigation (open follow-up)

**Date / Time:** 2026-04-28, 18:45 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** TC_AIExt_01 PASSES live (was failing on every run before today). TC_AIExt_08 (Bulk Ops AI Extract) **disabled** as a follow-up — needs live exploration with you.

## Honest summary of what I learned

### What I got right

The pre-existing `findAIExtractButton()` helper in AIExtractionTestNG.java had **6 wrong labels** for the AI extraction trigger:

```
"AI Extraction", "Extract with AI", "Upload Nameplate",
"Smart Extract", "Extract from Image", "AI Extract"
```

None of these match the **actual** production label which (per your screenshot) is **"Extract from Photos"** (with an "s"). Every test using this helper failed silently or fell into null-handling branches.

I added "Extract from Photos" + "Extract from Photo" to the front of the helper's label list. After the fix, the in-drawer button is now reliably found.

### What I got wrong (twice)

**Mistake #1**: TC_AIExt_01..06 all open the **Create form** (`assetPage.openCreateAssetForm()`) and look for the AI extraction button there. But the Create form (Add Asset) only collects basic info — the AI extract button is in the **Edit drawer's CORE ATTRIBUTES section** (post-creation, post-class-selection). I verified this by reading the Create form screenshot — it has Asset Name / QR Code / Asset Class / Subtype only.

Fixed `TC_AIExt_01` to open the Edit drawer via `assetPage.clickKebabMenuItem("Edit Asset")` and look for the button there. Now PASSES.

**Mistake #2**: I assumed the in-drawer "Extract from Photos" button was THE AI extraction trigger. You corrected me: *"you are clicking on wrong button for extract ai photo. do you know bulk edit bulk ops or not"* — meaning the **primary** AI extraction surface is the **Bulk Ops** workflow (or possibly Bulk Edit) on the Assets page toolbar, not the per-asset in-drawer button.

I attempted TC_AIExt_08 to open Bulk Ops and find the AI Extract option there. Failed across multiple iterations:
- First: Bulk Ops doesn't open a dropdown menu — it puts the page into **bulk-selection mode** (rows get checkboxes, top toolbar shows "Select items to edit or delete" + "Cancel")
- Second: After enabling selection mode, I tried checking the first row's checkbox via React's native-setter pattern, then looking for action buttons. Result: `Discovered buttons: [aaabhiyant admin]` — only the user avatar was visible.

Either:
1. The native-setter checkbox click navigated away / closed the selection mode silently
2. The action menu lives behind a secondary click I haven't found
3. The AI Extract is in **Bulk Edit ▼** dropdown (which has a chevron icon), not Bulk Ops

**Decision**: rather than continue guessing, I disabled TC_AIExt_08 with `@Test(enabled = false)` and a clear TODO comment. The right path forward is to explore the Bulk Ops / Bulk Edit workflow live with you — record the exact click sequence — then encode it as a test.

## Files changed

### `src/test/java/com/egalvanic/qa/testcase/AIExtractionTestNG.java`

1. **`findAIExtractButton()` helper** — added `"Extract from Photos"` + `"Extract from Photo"` to the front of the label list. Added comment explaining why (multiple FE rename cycles, soft contract for resilience).

2. **`TC_AIExt_01`** — switched from Create form to Edit drawer. Now correctly verifies the per-asset Extract from Photos button exists in CORE ATTRIBUTES.

3. **`TC_AIExt_08`** (added then disabled) — attempted Bulk Ops AI Extract verification. Selection-mode workflow is correctly identified, but the actual AI Extract option couldn't be located via the click sequence I tried. Marked `@Test(enabled = false)` with a TODO comment.

## Test outcomes (local)

| Test | Before | After |
|---|---|---|
| TC_AIExt_01 (button visible) | FAIL — wrong label "AI Extraction" + wrong surface (Create form) | **PASS** — finds "Extract from Photos" in Edit drawer |
| TC_AIExt_02..07 | Likely all silently broken (depend on TC_AIExt_01's setup) | Unchanged in this commit — recommend follow-up sweep to update them all to Edit-drawer surface |
| TC_AIExt_08 (Bulk Ops AI Extract) | Did not exist | DISABLED — needs live exploration with user |

## What I'd ask you for the follow-up

I need to see exactly how the Bulk Ops AI Extract workflow is triggered. Could you walk me through it once (or share screenshots of each step)? Specifically:

1. From the Assets page, which exact button do you click to start the AI Extract bulk workflow — Bulk Edit ▼ dropdown OR Bulk Ops button?
2. After that click, what happens? (dialog opens? menu appears? selection mode?)
3. Do you need to select assets first or is there a direct "AI Extract all" button?
4. What's the next click after that?

Once I have the exact sequence, encoding it as a test takes 10-15 minutes vs hours of guessing.

## Pattern across this session — now 7 instances

This is the **7th wrong-label / wrong-surface anti-pattern** caught today:

| Test | Wrong about | Real surface |
|---|---|---|
| TC_Misc_02 | "Maintenance State" + Calculations tab | "Condition of Maintenance (COM)" top card |
| TC_Misc_03 | /slds page | Edit drawer combobox |
| TC_Copy_09 | step-1 dialog title only | Step-2 ("Select fields to copy") |
| TC_Report_01 | Asset detail kebab | /admin Settings → Forms tab |
| TC_CONN_081 | Edit drawer + log-only | Add drawer + falsifiable textContent |
| **TC_AIExt_01..06** | **"AI Extraction" label + Create form** | **"Extract from Photos" + Edit drawer** |
| **TC_AIExt_08** (open) | **Assumed in-drawer was THE AI extract** | **Probably Bulk Ops / Bulk Edit on Assets page** |

The recipe is consistent every time: a test that has never been run live + screenshot-verified ships broken. **The ONLY reliable way to write tests is open-the-page-and-look**, then encode what's actually there.

## Pre-merge rule recommendation (from this session's data)

Worth proposing to the team:
- Every new test must run green at least once before merge
- Screenshot evidence of what the assertion saw must be attached
- Helper-method label lists are NOT a substitute for live verification — they're a fallback after the assertion has been live-verified once

7 broken tests caught in one session is too many. The cost of pre-merge verification is far less than the cost of these silent failures accumulating in CI.

---

_Per memory rule: this changelog is for learning + manager review. The fix is in the diff; this doc is the why for each piece._
