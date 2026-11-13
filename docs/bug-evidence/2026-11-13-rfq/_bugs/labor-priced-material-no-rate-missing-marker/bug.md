# [Web] Quote shows a labor-priced material as "$0 / $0" with no "rate missing" marker when the work order has no rated labor

**Suggested assignee:** Krunal (frontend). **Severity:** Medium (a quote can go to the customer with a silent $0 line).
**Related:** ZP-5242 (Mobilization Units + labor-priced material add-ons).
**Video first:** `QA-13Nov-BUG-labor-priced-material-no-rate-missing-marker-video-steps.mp4` (61 s)

## Simple explanation
- A material can be priced by **labor time** (for example "10 min per unit"); its price comes from the work order's blended labor rate.
- When the work order has **no rated labor** (all labor rows removed or subcontracted), the material can't be priced. The API
  marks it `unpriced: true, rate_missing: true` with cost 0 and sell 0, which is correct.
- The quote screen shows the row as a normal **"$0 / $0"** line, with no "rate missing" / "unpriced" marker. Labor rows in the same
  situation get a red "NO RATE" chip.
- The customer view and the proposal preview show the $0 line too, so a quote can go out with a free trip charge and nobody notices.

## Links (QA)
- Quote: https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df (QA-DEMO ZP-5242 13 Nov mobilization units)
- Customer view: https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df?view=customer
- Material library: https://acme.qa.egalvanic.ai/materials → "QA-DEMO ZP-5242 13 Nov trip charge 10 min"
  (id `0d80e613-e914-44a7-89f0-37cadd0c3d17`, unit Mobilization Units, Labor time 10 min)

## Actual vs Expected
- **Actual:** with no rated labor the material row reads "QA-DEMO ZP-5242 13 Nov trip charge 10 min · ×6 Mobilization Units · $0 · $0",
  with no warning. Totals show $0, and the customer view shows a $0 price line.
- **Expected:** the row shows a visible marker such as "RATE MISSING — no rated labor on this work order" (like the labor "NO RATE"
  chip and the pink row), and ideally the quote's warning banner lists it, so the estimator fixes it before sending.

## Test data
- Environment: QA, https://acme.qa.egalvanic.ai, build `index-C6DvLko9.js`, 13 Nov 2026.
- Seat: Super Admin (QA staff seat).
- Quote `d20d398a-26de-424c-b672-b34e92d1a9df`: Plans v2, Arc Flash Data Collection (rate-priced), 4 assets. Labor: Electrical
  Engineer 0.67 h ($100 / $150 per h), Journeyman 0.83 h ($10 / $100 per h).
- Material `0d80e613-e914-44a7-89f0-37cadd0c3d17`: Mobilization Units, `labor_minutes: 10`.
- Re-proven 3 times: API at ~13:30 IST, UI at ~13:51 IST, and this video at 14:14 IST.

## Steps to reproduce (clean start)
1. Builder → **Materials** → **Create Material**: Unit = **Mobilization Units**, Priced by = **Labor time**, Minutes per unit = **10** →
   Create Material. The list shows "10 min @ blended". *(screenshot 1)*
2. Open a v2 quote that has labor lines → Pricing tab → **Manage Materials** → **+ Add a material…** → pick the new material →
   **Save Changes**.
3. With labor present the row is priced: ×6 Mobilization Units, **$50 / $122** (6 × 10 min = 1 h × blended $50.20 cost / $122.33 sell).
   *(screenshot 2)*
4. In the LABOR table click the trash icon ("Remove this labor type from the work order") on **both** labor rows. The page says
   "2 labor types removed from this work order … Restore…".
5. Look at the MATERIALS table: the add-on shows **$0 / $0** and no marker. *(screenshot 3)*
6. Click **Customer View**: the $0 line is shown to the customer as a normal line. *(screenshot 4)*

Same result in the API if, instead of step 4, every labor type is switched to subcontracted (Sub = Yes): `unpriced + rate_missing`, $0.

## Notes for the developer
- API at step 5 (`GET /api/plans/{id}` → `content.workorders[0].materials[]`):
  `{name: "QA-DEMO ZP-5242 13 Nov trip charge 10 min", quantity: 6, unit: "Mobilization Units", labor_minutes: 10, cost: 0, sell: 0, unpriced: true, rate_missing: true}`.
- With labor (step 3) the same line is `{cost: 50.2, sell: 122.33, blended_rate: 122.33}`, so the engine part works as the ticket describes.
- Frontend: the PlanDetail chunk never reads `rate_missing` / `unpriced` on materials. The materials aggregation keeps only
  name / unit / qty / cost / sell. Labor rows already have this treatment through
  `ls(t)`: "no rate on file — hours are unpriced" + "NO RATE" chip + pink row.
- The quote total and the proposal preview ("investment … $0.00 (pre-tax)") also have nothing that warns.
- QA fixture restored after the video: both labor types back, both QA-DEMO materials on the quote.

## Screenshots
1. `QA-13Nov-BUG-labor-priced-material-no-rate-missing-marker-1-labor-time-material-in-library.png`: step 1, library row "10 min @ blended" (orange).
2. `QA-13Nov-BUG-labor-priced-material-no-rate-missing-marker-2-with-labor-add-on-priced.png`: step 3, priced $50 / $122 (orange).
3. `QA-13Nov-BUG-labor-priced-material-no-rate-missing-marker-3-no-labor-0-dollars-no-marker.png`: step 5, labor removed (orange), $0 / $0 with no marker (red).
4. `QA-13Nov-BUG-labor-priced-material-no-rate-missing-marker-4-customer-view-0-dollar-line.png`: step 6, customer view $0 line (red).

## JQL run (no existing ticket)
`project = ZP AND (summary ~ "Pricing Settings" OR summary ~ "rate missing" OR summary ~ "rate_missing" OR summary ~ "unpriced")`.
