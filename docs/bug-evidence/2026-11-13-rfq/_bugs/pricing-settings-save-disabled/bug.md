# [Web] Pricing settings: "Save Changes" stays disabled when a quote only has a Minimum or Mobilization override

**Suggested assignee:** Krunal (frontend). **Severity:** Medium. **Related:** ZP-5245 (labor minimum + mobilization), ZP-5225 (round-up).
**Video first:** `QA-13Nov-BUG-pricing-settings-save-disabled-video-steps.mp4` (48 s)

## Simple explanation
- On a Plans v2 quote, open the gear "Pricing settings" and turn on only **Minimum** (or only **Mobilization**), then save. That works.
- Open Pricing settings again. **Save Changes is greyed out before you touch anything**, and it stays greyed out whatever you change.
- The only way to save is to switch **Round up** on and off again. That saves `labor_round_hours = 0` on the quote, so the quote
  silently stops following the company's round-up default.
- Cause: when the quote has another override but no round-up value, the Round up field starts with the text `"undefined"`, which
  fails validation and disables Save.

## Links (QA)
- Quote used: https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df (QA-DEMO ZP-5242 13 Nov mobilization units)
- Any v2 quote with labor shows it. Another example: https://acme.qa.egalvanic.ai/plans/65ccf113-2d50-4b51-bda6-9139df8ea9a0

## Actual vs Expected
- **Actual:** after saving a Minimum-only (or Mobilization-only) override, reopening Pricing settings shows Save Changes disabled. It stays
  disabled after changing Mobilization, Minimum or the inflation rate. The Round up row shows "Off".
- **Expected:** Save Changes is enabled once something changes. Round up shows the company default (or Off) and is not sent unless the
  user changes it.

## Test data
- Environment: QA, https://acme.qa.egalvanic.ai, build `index-C6DvLko9.js`, 13 Nov 2026.
- Seat: Super Admin (QA staff seat). Project Manager and Account Manager seats have the same gear and dialog (reproduced only with Super Admin).
- Quote id `d20d398a-26de-424c-b672-b34e92d1a9df`: Plans v2, service Arc Flash Data Collection (rate-priced), 4 assets, company
  pricing defaults all Off.
- Re-proven 3 times: ~13:38 IST (Mobilization-only), ~13:46 IST (Minimum-only), 14:12 IST (Minimum-only, this video).

## Steps to reproduce (clean start)
1. Sign in → Sales → Quotes → open a v2 quote that has labor lines and no pricing overrides.
2. On the Pricing tab click the gear **Pricing settings**. Round up and Minimum are Off; Save Changes is enabled. *(screenshot 1)*
3. Turn on **Minimum** and type **8** (leave Round up alone) → **Save Changes**. The labor rows now say "topped up to the 8h minimum". *(screenshot 2)*
4. Click the gear **Pricing settings** again. **Save Changes is already disabled.** *(screenshot 3)*
5. Turn on **Mobilization → Per move = 30**. **Save Changes stays disabled.** *(screenshot 4)*
6. (Workaround) Turn **Round up** on and off again. Save Changes becomes enabled → Save. The quote is saved, but with an explicit
   `labor_round_hours: 0`. *(screenshot 5, end of video)*

The same happens if step 3 sets only Mobilization instead of Minimum.

## Notes for the developer
- API evidence (`GET /api/plans/{id}` → `instructions.pricing`):
  - after step 3: `{labor_min_hours: 8}` (no `labor_round_hours`);
  - after the step 6 workaround: `{labor_round_hours: 0, labor_min_hours: 8, mobilization_minutes: 30}`.
- Workaround side effect: `labor_round_hours: 0` is an explicit per-quote override. If the company later turns on a round-up default,
  this quote keeps ignoring it.
- Code (PlanDetail chunk, `jt()` that opens the dialog): the Round up value is initialised with
  `` `${hasAnyOverride ? instructions.pricing.labor_round_hours : company}` ``. When the quote has a Minimum or Mobilization override
  but no round-up key, this becomes the string `"undefined"`. The Save button is disabled by `!Be`, where
  `Be = m !== "" && isFinite(Number(m)) && …`, and `Number("undefined")` is NaN.
- Minimum and Mobilization already use a null-safe fallback (`x != null && x !== "" ? x : company`). Using the same for Round up should fix it.
- Not affected: a quote with no overrides, or one that already has a Round up override.

## Screenshots
1. `QA-13Nov-BUG-pricing-settings-save-disabled-1-dialog-no-override-save-enabled.png`: step 2, no overrides, Save enabled (orange).
2. `QA-13Nov-BUG-pricing-settings-save-disabled-2-minimum-8-saved-topped-up.png`: step 3 saved, "topped up to the 8h minimum" (orange).
3. `QA-13Nov-BUG-pricing-settings-save-disabled-3-reopen-save-disabled-before-any-change.png`: step 4, Save disabled before any change (red).
4. `QA-13Nov-BUG-pricing-settings-save-disabled-4-after-change-save-still-disabled.png`: step 5, Mobilization 30 set, Save still disabled (red).
5. `QA-13Nov-BUG-pricing-settings-save-disabled-5-workaround-saved.png`: workaround saved (stores round-up 0).

## JQL run (no existing ticket)
`project = ZP AND (summary ~ "Pricing Settings" OR summary ~ "rate missing" OR summary ~ "unpriced" OR summary ~ "requires scheduling")`
and `project = ZP AND created >= -45d AND (text ~ "Pricing Settings" OR text ~ "labor rounding" OR text ~ "mobilization" OR text ~ "Save Changes disabled")`.
