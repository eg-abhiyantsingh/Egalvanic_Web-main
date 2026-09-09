# ZP-3782 & ZP-3783 retest — Fill Forms from Photos · QA verdict: **both still fail**

**Tickets:** [ZP-3782](https://egalvanic.atlassian.net/browse/ZP-3782) (multiselect answers lose ticked
options) · [ZP-3783](https://egalvanic.atlassian.net/browse/ZP-3783) (review-dialog questions ship
option fields without their options) — both still **To Do**
**Artifact:** https://claude.ai/code/artifact/e1577f11-92cb-49a1-865f-051e1f31fe48
**Retested:** 2026-09-09 · **Env:** `acme.qa.egalvanic.ai` · V1.36, bundle `index-jYhUcFb4.js` · tenant acme
**Method:** the original ticket's own sheet re-uploaded through the product UI (Work order → Forms →
Actions → Fill from Photos → Read pages), with a fetch shim capturing every `/api/form-fill/` response.
**Three fresh jobs run today** — `6293a4f4` (restored, applied), `fbe3581f`, `8079487d`. Applied state
re-read from `GET /eg-form-instance/{id}` afterwards, not taken from the apply response alone.

**Fixture:** WO `465df0c4-39a0-4787-a815-642729c29707`, form **Signature Test** (`2d9d80c0…`), sheet
`paper_form2_page1-uploaded-sheet.jpg` — Equipment ID *BNewasdasd*, **Visual Inspection ✓ and
Thermography ✓** (two of four ticked), Verdict left blank.

---

## Verdict

| Ticket | Expected result | What happens today | Verdict |
|---|---|---|---|
| **ZP-3782** | value is a **list of option keys** `["visual_inspection","thermography"]`, multiple reads merged | value is the **comma-joined string** `'visual_inspection,thermography'`, refused by validation, and **the field is not written at all** | ❌ **NOT FIXED** |
| **ZP-3783** | question/gap descriptors carry the field's **options** so the dialog can render a real picker | descriptors still carry only `label · path · section · type · unit`; `field_index` in the same payload has the options | ❌ **NOT FIXED** |

## ZP-3782 — the failure mode changed, the data loss did not

**What the model produces now** (identical across all three jobs today, `proposal.warnings`, 2 entries):

```
BNewasdasd: `general_info.tests_performed` = 'visual_inspection,thermography'
  is not one of ['contact_resistance','insulation_resistance','thermography','visual_inspection']
  (use the option KEY)
```

So **both ticks now reach the value** — the ticket's "affects[] carries one scalar entry
(`visual_inspection`)" collapse is gone, and so is the `written twice (decisions d4 and d4)` collision
warning. But the merged value is a **comma-joined string**, not a list, so the validator refuses it.

**What apply writes** (`POST /form-fill/jobs/6293a4f4/apply` → 200, `job.status: "applied"`) —
`applied.forms[0].wrote` for instance `3d47b92a`, **7 keys**:

```
equipment_details_columns.equipment_id  "BNewasdasd"
general_info.inspector_name             "Marcus Webb"
general_info.inspection_date            "2026-08-04"
general_info.contact_email              "m.webb@fieldserv.com"
general_info.ambient_temperature_f      78
general_info.asset_condition            "fair"
general_info.additional_notes           "Cabinet interior dusty. Recommend cleaning at next outage."
```

`general_info.tests_performed` is **absent**. Re-reading the persisted instance afterwards
(`GET /eg-form-instance/3d47b92a`) confirms it: **7 non-empty values, and the `tests_performed` key does
not exist**. The Forms grid shows the instance as `0/7 · Draft · Sep 9, 2026, 05:04 PM`.

**Still silent.** The dialog reported only *"BNewasdasd — Signature Test · 7 values · draft"*. Nothing
told the reviewer that a row the technician ticked twice was discarded — the rejection lives in
`proposal.warnings`, which the dialog does not surface. The ticket's severity rationale therefore stands
unchanged: a technician ticks two tests, none land, nobody is told.

**Net change since the ticket:** a silent *partially wrong* write (one option kept) became a silent
*total non-write* (nothing kept). No wrong data is stored now, which is a small improvement; the
recorded inspection data is still lost, which is the defect.

## ZP-3783 — the contract is unchanged

From the proposal payload served today, gap descriptors for option-typed fields:

```json
{"label":"Weather Conditions","path":"general_info.weather_conditions",
 "section":"General Info","type":"radio","unit":null}
```

Keys: `label, path, section, type, unit` — **no `options`**. Same for `overall_result` (switch) and
`loto_verified` (checkbox). Meanwhile `proposal.field_index["2d9d80c0…"]` in the **same payload** carries
36 fields with their options intact:

```
general_info.weather_conditions  radio        ["clear","rain","overcast"]
verdict.verdict                  select       ["pass","pass_with_conditions","fail"]
general_info.tests_performed     multiselect  ["visual_inspection","insulation_resistance","contact_resistance","thermography"]
general_info.asset_condition     select       ["excellent","good","fair","poor"]
general_info.overall_result      switch       ["pass","fail"]
```

So the dialog is still handed "render a picker" with nothing to populate it, while the index sitting
beside it holds the choices. Nothing in the payload shape has moved since the ticket was written.

**Honest boundary on this one.** The descriptor sample comes from job `6293a4f4`, whose proposal was
**fetched today** but whose read ran earlier. The two genuinely fresh jobs produced **0 fills**, so
neither emitted a `questions[]` or `gaps[]` array to sample. Two things make me confident the payload is
assembled on read rather than replayed from storage — its `warnings` match today's validator wording
verbatim (and differ from the wording the ticket recorded), and its `field_index` has the same shape as
both fresh jobs — but I could not produce a fresh questions-bearing payload. **What would close it:** any
form-fill run that fills at least one form; check whether `fills[].questions[].fields[]` then carries
`options`.

## Why the fresh runs filled nothing (not a defect)

Both fresh jobs returned `filled: 0`, with the model's own explanation:

> "the uploaded sheet (Equipment ID 'BNewasdasd', inspector Marcus Webb, 2026-08-04) matches three
> identical open instances for the same asset (79efbcf2, abce4034, b4fa27c8) and there is nothing on the
> page … to say which instance it belongs to; assigning it would be a guess."

Once the first sibling was filled, the sheet could no longer be attributed among the identical remaining
siblings, so it declined rather than guessing. That is correct, conservative behaviour — the same
graceful no-match path recorded on 2026-08-18 — not a regression. It does mean the same sheet cannot be
re-filled onto the same asset without clearing the filled sibling first.

## Two workflow notes for whoever retests this

- **A previous job blocks a new one.** Reopening the dialog restores the last job
  (`GET /form-fill/jobs/active?session_id=…`) and shows its result screen with only *Cancel* and *Apply*.
  Cancel does not release it. The upload step only comes back after the job is applied (then *Done*) or
  via *Try other pages* on a no-match result. Worth knowing before concluding "the dialog won't let me
  upload".
- **The Forms tab is under *More*** on this work order, and the form grid's row click did not open the
  renderer for a draft instance — the persisted values had to be read from the API.

## Test data

- Work order — https://acme.qa.egalvanic.ai/sessions/465df0c4-39a0-4787-a815-642729c29707
- Form instance filled today — `3d47b92a` (Signature Test, BNewasdasd), 7 values, Draft
- Jobs run today — `6293a4f4-abc2-45b2-86ba-471bd0aac6b9` (applied) · `fbe3581f-440d-4d7a-80fd-1961ecd1238b` · `8079487d-3564-48b0-92b3-a7e4b51983c4`
- Evidence — `docs/bug-evidence/zp-3782-3783-fill-forms-retest/`

**Footprint:** one apply, writing 7 values to an already-draft form instance on an existing QA work
order. Nothing was submitted and nothing was deleted. A sheet accidentally staged into the *Upload IR
Photos* dialog was cancelled before upload — no stray photo was added.
