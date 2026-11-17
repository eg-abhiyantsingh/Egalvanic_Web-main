# ZP-4937: IR editor, creating an issue from a negative delta (cold minus hot) swaps the temperatures

**Verdict: PASS** for what the ticket and PR cover (Similar criteria, create and link). There are two open product points, both written down in the PR itself. They are listed under "Notes" and are not new bugs.

- **Build:** QA `index-oFAZQ3rK.js`, 17 Nov 2026, 11:22–11:57 IST.
- **Seat:** Super Admin (`VALID_EMAIL`).
- **Fix:** eg-pz-frontend PR #2019, merged to `cicd/qa` on 13 Nov. I read the PR. It changes `evidenceSides` in `irDelta.js`: a delta whose drawn value is negative is turned round, with Problem read at max and Reference at min. A positive or zero delta is kept as drawn. A link to an issue on **Ambient** criteria keeps the pick order.
- **Ticket comments:** one comment ("Merged to QA via PR #2019"). It adds no test rows, so the PR's own 7 QA steps were used as the rows.
- **Environment note:** the ticket's repro data (IR_4910 / IR_4912) is on **stage**. This round is QA only, so I rebuilt the same case on a QA-DEMO FLIR photo.

Evidence:
- Video `QA-17Nov-ZP-4937-negative-delta-video-steps.mp4` (58 s).
- Screenshots `QA-17Nov-ZP-4937-1…6-*.png`. They have outline boxes only, no text drawn in.
- Jira draft `ZP-4937.txt`.

## Test data (QA, all QA-DEMO)
- Work order "QA-DEMO ZP-4888 FLIR-IND retest": https://acme.qa.egalvanic.ai/sessions/9c6101aa-6624-4083-901c-dba6ceef85d5
- Photo "QA4888r-FLIRDemo-with-visual.jpg" (ir_photo d0d4d386-f910-4bfd-90e0-588a33913e9c, 320×240). It already had the camera spot Sp1 at 160.8 °F. I added:
  - Sp2 75.2 °F;
  - Bx1 (76.6–131.5);
  - Bx2, a hot box (142.9–154.7);
  - Bx3, a cold box (74.8–75.0);
  - Bx4, a big box (72.0–169.0);
  - deltas Dt1–Dt8.
- Issues created (all on asset "asset"):
  - "QA-DEMO ZP-4937 17 Nov cold-first Dt1 (Sp2 − Sp1)": https://acme.qa.egalvanic.ai/issues/33d51714-cbb4-41a8-98e1-e3c67313b9e3
  - "QA-DEMO ZP-4937 17 Nov hot-first Dt2 (Sp1 − Sp2)": https://acme.qa.egalvanic.ai/issues/a8515809-ba5c-4231-9480-e887407d6427
  - "QA-DEMO ZP-4937 17 Nov Ambient issue": https://acme.qa.egalvanic.ai/issues/fe6fd01e-5f3a-4357-927f-ff04b77123ca

## Rows
| # | PR QA step / case | Result |
|---|---|---|
| 1 | Cold-first spot delta **Dt1 = Sp2 − Sp1 = −85.7 °F** › Create issue › "Which delta?" preview | PASS. Problem Temp 160.8 °F (Sp1), Reference 75.2 °F (Sp2). The hotter spot is Problem. On stage on 16 Oct, the cooler one was. |
| 1 | Same spots hot-first, **Dt2 = +85.7 °F** | PASS. The same preview: Problem 160.8 (Sp1), Reference 75.2 (Sp2). |
| 2 | Save & create issue from Dt1, then read the Add Issue form | PASS. Priority **Critical**, Severity Criteria Similar, Problem 160.8, Reference 75.2, Delta T 85.6, Severity **Critical**, the same as the Dt2 form. On 16 Oct it was Nominal / Low. |
| 2 | Issue created from Dt1, and one from Dt2 for comparison. Stored values via `GET /api/issue/{id}` | PASS. Both issues hold Problem 160.9 °F, Reference 75.2 °F, Severity Criteria Similar (1 entry), Severity Critical, Delta T 85.7 °F, priority Critical. The create body's `origin` for Dt1 has `a` = Sp1 (71.6 °C) and `b` = Sp2 (24.0 °C), so the hot side is written as Problem. |
| 3 | Separate boxes, cold box first: **Dt3 = Bx3 − Bx2 = −67.9 °F** | PASS. Problem 154.7 °F (Bx2 max), Reference 74.8 °F (Bx3 min). |
| 3 | Same boxes, hot box first: **Dt4 = Bx2 − Bx3 = +79.8 °F** | PASS. The same Problem 154.7 / Reference 74.8, so the result does not depend on pick order. |
| 4 | Overlapping, with positive deltas kept as drawn: Dt5 = Bx2 − Bx4 (+82.7), Dt6 = Bx4 − Bx3 (+94.1), Dt7 = Bx3 − Bx4 (+3.0), and spot vs box Dt8 = Sp2 − Bx4 (+3.2) | As the PR says: unchanged and kept as drawn. Dt5: 154.7 (Bx2) / 72.0 (Bx4 min). Dt6: 169.0 (Bx4) / 74.8. **Dt7: 75.0 (Bx3) / 72.0.** Dt8: 75.2 (Sp2) / 72.0. See Note 1. |
| 5 | Link the negative Dt1 to an issue on **Ambient** criteria | As the PR says: the pick order is kept, and there is still exactly **1** Severity Criteria entry. The stored issue now reads Problem 75.2 °F (Sp2), Reference 160.9 °F (Sp1), **Severity Nominal, priority Low, Delta T 85.7 °F**. The PUT sends those values plus `origin.a = Sp2`. See Note 2. |
| 5b | Link the negative Dt1 to the **Similar** issue (made hot-first) | PASS. Turned round: the PUT sends Problem 160.9 (Sp1), Reference 75.2, Severity Critical, priority Critical, 1 criteria entry. |
| °C | Editor in °C, Dt1/Dt2/Dt3 previews | The deltas list shows −47.6 / +47.6 °C. The preview Problem/Reference stay in °F (160.8 °F / 75.2 °F), the unit the issue form uses. The assignment is the same as in °F. |
| link | Link of the negative delta: preview in the Link dialog | PASS. Ambient issue: "Problem Temp 75.2 °F (Sp2) Reference Temp 160.9 °F (Sp1)", the pick order. Similar issue: "Problem Temp 160.9 °F (Sp1) Reference Temp 75.2 °F (Sp2)", turned round. |

- **Saves:** `POST /api/ir_photo/{id}/save` 200 each time. `POST /api/issue/create` 201 (×2). `PUT /api/issue/update/{id}` 200 (×2).
- **Console:** no app errors, only the third-party `ERR_FAILED` entries.

## Notes (open product points, no new bug filed)
1. **Overlapping markers still depend on pick order.** This is the PR's "What is NOT fixed" section. On this photo, the cold box against the big box gives **+3.0 °F** (Dt7, Problem 75.0 °F) in one order and **+94.1 °F** (Dt6, Problem 169.0 °F) in the other. Their severities would be very different. The PR records this as a follow-up that needs a product decision (hotspot or largest difference). Suggest a follow-up ticket if one does not exist.
2. **Ambient plus a negative delta still gives "Nominal / Low" next to "Delta T 85.7 °F"** (screenshot 6). This is by design in PR #2019: on Ambient, B is the air, so it never becomes Problem Temp. The result is that the issue shows the same mismatch the ticket complained about: Delta T is the absolute difference, while Severity uses the signed one. The ticket asked for either a warning offering to swap, or a fill from the absolute rise. Neither happens on the Ambient link path. Owner or product to decide whether that path needs a warning. Not filed as a bug, because the PR states it on purpose.

## Incident (mine, fixed)
At about 11:41 IST a mis-aimed drag in my run moved the camera spot **Sp1** from (160,120) to (170,130) px, so it read 160.9 °F instead of 160.8 °F, and the photo was saved like that. The three issues above were created and linked while Sp1 read **160.9 °F**. That is why the issues say 160.9 and the previews say 160.8. At 11:48–11:50 IST I dragged Sp1 back to (160,120). The measure call confirms x 160, y 120, 160.8 °F, and the photo was saved again.

I also removed the duplicate delta "Dt4" that my first exploration had added to IR_62588 ("Remove Dt4", saved). That photo is back to Dt1–Dt3. Dt3 was already there before today.

## Not covered
- Stage repro data (IR_4910 / IR_4912): QA only this round.
- PR step 6 (stage data).
- The ZP-5037 est-hours part of the same PR: a different ticket.
- Circle/line markers.
- A zero delta (equal temperatures).
- Roles other than Super Admin.
- Mobile not checked. The IR editor is web only. iOS/Android have no delta tool.

## New defects
None.
