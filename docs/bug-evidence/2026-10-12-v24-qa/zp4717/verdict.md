# ZP-4717 — [Web] Task #135: Normalize and honor a boolean EG form `visible` (backend + renderer)

**Verdict:** PASS. Tested on QA build `index-COeCBn0z.js`, 13 Oct 2026, about 14:05 to 14:35 IST. The build was checked again before this verdict.

The backend now stores every boolean or number `visible` as the string `"true"` or `"false"`. This holds on the product create route, the product update route, all three definition shapes, inside containers and column children, and on the service-spec apply path. Strings, expressions, missing keys and other values are left alone.

The web renderer hides and shows each test row correctly in three places: the Forms list preview, the builder (Block/Web/Mobile view) and the work-order form instance drawer. This is true both for the stored strings and for **raw booleans/numbers fed to the browser (SIMULATED)**.

Test data, all created by me:
- EG forms: **QA-DEMO ZP-4717 visible normalization (delete me)** `e293697b-b25c-4b8a-84d2-1f90a52758ce`, **QA-DEMO ZP-4717 visible bare-array create (delete me)** `d0428c63-…`, and **QA-DEMO ZP-4717 odd visible values (delete me)** `8601ba8d-…`. They are listed at https://acme.qa.egalvanic.ai/eg-forms.
- Service: **QA-DEMO ZP-4717 service spec (delete me)** `92cc6b0c-3a81-4f3d-9c2d-26f5d0c3cf38`, https://acme.qa.egalvanic.ai/services/92cc6b0c-3a81-4f3d-9c2d-26f5d0c3cf38. It is in draft and owns the form `93ce8f83-…`.
- One form instance on asset "asset" in work order **QA-DEMO ZP-4801 Tuned column (delete me)**, https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441 › More › Forms. It is a draft and was never submitted.

## The test form (one row per case; each label says the expected result)
B1 `false` · B2 `true` · B3 `0` · B4 `1` · B5 `2` · B6 `"false"` · B7 `"true"` · B8 `"toggle === 'show'"` · B9 no key · B10 `""` · B11 `null` · B12 `"{{ toggle === 'show' }}"`.

The same form also has:
- C1, a container with `visible:false`
- C2 children with `false`, `1`, `0` and `true`
- K1–K4, column children with `false`, `true`, `0` and `1`
- D1/D2, children of a nested container
- E1/E2, column children inside a container

## Checked

| Check | How | Result |
|---|---|---|
| Product create `{version:2,blocks}` | `POST /api/eg-forms`, then `GET /api/eg-forms/<id>` | PASS. false/0 → `"false"`, true/1/2 → `"true"` (also -1, 0.5 → `"true"`). `"false"`/`"true"` strings, the expression, `""`, `null`, objects, arrays and `"0"` are unchanged. A missing key stays missing. |
| Nested blocks | Containers, columns, a container inside a container, columns inside a container | PASS. Every level was normalized (C1, C2a–d, K1–K4, D inner/D1/D2, E1/E2). |
| Product update, bare-array definition | `PUT /api/eg-forms/<id> {definition:[…]}` | PASS. Normalized, and stored as `{blocks,version}`. |
| Product update `{blocks}` without version | PUT | PASS. Normalized. |
| Create with a bare array | POST | PASS. Normalized. |
| Title-only PUT | PUT `{title}` | PASS. The definition is untouched. |
| Service-spec path | Created my own draft service, then `POST /procedures-v2/services/<id>/versions {spec:{forms:[definition with raw booleans]}, apply:true}` → 201 (forms_created 1) | PASS. The service-owned EG form (`/eg-forms`, `service_id` = mine) and `export-spec` both hold strings only. Re-applying the version (`…/versions/<vid>/apply` → 200) keeps 0 raw values. |
| Note: version snapshot | `GET …/versions/<vid>` | The saved **spec snapshot** still holds the raw booleans (19 of them) as they were sent. Only the forms written from it are normalized, which matches the ticket wording. Not a defect, but anything that ever reads forms straight from a snapshot would see booleans. |
| Staff update route | Not reachable from the web; no staff tool used | NOT COVERED (out of reach) |
| Web: Forms list preview, stored strings | /eg-forms › search › click the row (side preview) | PASS. All 27 rows hide/show as their labels say (shot 2) |
| Web: builder | Edit Form › Block, Web and Mobile view | PASS (all 3 views) |
| Web: WO form instance drawer | Work order › More › Forms › row "asset" | PASS (shot 3) |
| Web: expression still works | In the instance drawer, Toggle = Show | PASS. B8 appears (shot 7). B12, the `{{ }}` brace form, stays hidden, as it did before this change. QA has no form that uses brace syntax; all 650 forms use plain expressions. |
| Web: raw boolean/number (frontend half) | SIMULATED. Playwright rewrote every EG-form GET reply to put back raw `false/true/0/1/2` (57 values rewritten) | PASS. The list preview, builder and instance drawer all hide false/0 and show true/1/2/no-key/""/null (shots 4, 5, 6). A false block no longer "leaks" into view. |
| Negative: "This form has no definition" | Mobile client decode | → Nency (iOS ZP-4743, Android ZP-4744, customer report ZP-4716) |
| Prod data | Ticket says the prod scan found 0 non-string `visible` | On QA, 0 of 650 forms had a non-string `visible` before my test |
| Roles | Writes done as Super Admin. Normalization runs on the server for every writer. | No role-specific behaviour in this ticket |
| LaunchDarkly | Bundle: the renderer and builder helpers are not flag-gated. `feature-eg-forms` (an older flag, ON for Acme QA) only gates the WO Forms tab. | No new flag; **no flip needed** |
| Performance | 4 API writes, each 200–700 ms; the definition payload is the same size; no extra calls in the renderer | OK |
| Console | Renderer pages | No new errors from the form. QA had unrelated rate-limit (429) noise from beamer and the CDN. |

## Bugs found
None for this ticket.

## Enhancement suggestions
- The builder should show a short notice when a block's rule is the literal `"false"` (for example "Always hidden"). Today the author can't tell that a block will never show.
- The renderer should either evaluate `{{ … }}` brace expressions (the ticket's own example uses them) or the builder should reject them on save. Today they quietly hide the block.
- Normalize `visible` in the stored service-version spec snapshot too, so the version history matches the live forms.

## Mobile part for Nency
- **ZP-4743 (iOS #709)** and **ZP-4744 (Android)**: open a form whose block has a boolean `visible` and confirm it no longer shows "This form has no definition".
- The P&H "Visual & Voltage Inspection" form is the reference (customer report ZP-4716).
- For a live form after this fix, use the service-owned form `93ce8f83-…`. Its strings are `"false"`/`"true"`, with false/0 rows hidden and true/1 rows shown.
- Also check the mobile renderer gives the same hide/show result as the web for every labelled row (B1–E2).

## Not covered
- **Staff update route:** no web or API path to it.
- **The original P&H row:** it was fixed by hand on prod and is not on QA.
- **iOS/Android tolerant decode:** for Nency.

## Proposed Jira comment
```
h3. QA result on QA (build index-COeCBn0z.js, 13 Oct 2026): PASS

My test form "QA-DEMO ZP-4717 visible normalization (delete me)" has one row per case: visible false / true / 0 / 1 / 2 / "false" / "true" / expression / no key / "" / null, plus containers, columns and nested blocks.

||Check||Result||
|Create and update a form (3 definition shapes): stored as "true"/"false"|PASS|
|Strings, expressions, missing key, "" and null left as they are|PASS|
|Nested containers and columns normalized too|PASS|
|Service spec apply (my own QA-DEMO service): service form stored as strings|PASS|
|Web list preview, builder (block/web/mobile) and work order form: false/0 hidden, true/1/no key shown|PASS|
|Same screens with raw true/false/0/1 sent to the browser (simulated): same result|PASS|
|Expression rule still works (B8 shows after Toggle = Show)|PASS|

!QA-13Oct-ZP-4717-3-stored-string-wo-form-instance-PASS.png|width=720!
!QA-13Oct-ZP-4717-5-SIMULATED-raw-boolean-wo-form-instance-PASS.png|width=720!
!QA-13Oct-ZP-4717-7-expression-rule-shows-B8-after-Toggle-Show-PASS.png|width=720!

Notes: the staff update route could not be reached from QA. The saved service version history still keeps the raw booleans; the forms written from it are fine. The mobile decode is checked by Nency (ZP-4743 iOS, ZP-4744 Android).
```

## Recommended status
**READY TO RELEASE.** No feature flag is involved, so no flip is needed.

## Evidence files (this folder)
2. `QA-13Oct-ZP-4717-2-stored-string-forms-list-preview-PASS.png`
3. `QA-13Oct-ZP-4717-3-stored-string-wo-form-instance-PASS.png`
4. `QA-13Oct-ZP-4717-4-SIMULATED-raw-boolean-forms-list-preview-PASS.png`
5. `QA-13Oct-ZP-4717-5-SIMULATED-raw-boolean-wo-form-instance-PASS.png`
6. `QA-13Oct-ZP-4717-6-SIMULATED-raw-boolean-builder-preview-PASS.png`
7. `QA-13Oct-ZP-4717-7-expression-rule-shows-B8-after-Toggle-Show-PASS.png`

Scripts and raw JSON: `.playwright-mcp/v24/zp4801-4717/` (e1, e4, e4sim, s2, g1, formdef.js).
