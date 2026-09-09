# Retest: ZP-3782 and ZP-3783 (Fill Forms from Photos) — both still fail

**Date:** 2026-09-09
**Time:** 18:05 – 19:10 IST
**Prompt:** ZP-3782 + ZP-3783 ticket text pasted — "check this is passed or not"

**Artifact:** https://claude.ai/code/artifact/e1577f11-92cb-49a1-865f-051e1f31fe48

---

## Answer: neither is fixed

| Ticket | Expected | Today on V1.36 | Verdict |
|---|---|---|---|
| **ZP-3782** multiselect loses ticked options | value is a **list** of option keys, reads merged | value is the **comma-joined string** `'visual_inspection,thermography'`, refused by validation, field **not written at all** | **NOT FIXED** |
| **ZP-3783** option fields ship without options | descriptors carry the field's options | still `label · path · section · type · unit`; `field_index` in the same payload has every choice | **NOT FIXED** |

## ZP-3782 — the failure mode changed, the data loss did not

Three fresh jobs today, all with the identical warning:

```
BNewasdasd: `general_info.tests_performed` = 'visual_inspection,thermography'
  is not one of ['contact_resistance','insulation_resistance','thermography','visual_inspection']
  (use the option KEY)
```

**Changed since the ticket:** both ticks now reach the value — the collapse to a single scalar
`visual_inspection` is gone, and so is the `written twice (decisions d4 and d4)` collision warning.
**Not changed:** the merge produces a *string*, not a list, so validation refuses it.

Apply wrote **7 keys** and `general_info.tests_performed` was **absent**. Re-reading the persisted
instance confirms it — 7 non-empty values, no `tests_performed` key at all. The dialog reported only
"BNewasdasd — Signature Test · 7 values · draft", so a row the technician ticked twice vanished with no
signal. Net: a silent partially-wrong write became a silent total non-write.

## Honest boundary on ZP-3783

The descriptor sample comes from a job whose proposal was **fetched** today; both genuinely fresh jobs
returned `filled: 0` and therefore emitted no `questions[]`/`gaps[]` to sample. Two indicators say the
payload is assembled on read (its warnings match today's validator wording verbatim and differ from the
wording the ticket recorded; its `field_index` matches both fresh jobs), but I could not produce a fresh
questions-bearing payload. Named in the verdict as what would close it: any run that fills ≥1 form.

## Method notes worth keeping

- **A previous job blocks the next one.** Reopening the dialog restores the last job via
  `GET /form-fill/jobs/active?session_id=…` and shows its result screen with only *Cancel* and *Apply* —
  Cancel does not release it. The upload step returns only after the job is applied and dismissed, or via
  *Try other pages* on a no-match result.
- **Once a sibling is filled, the same sheet stops matching.** The model's own words: the sheet "matches
  three identical open instances for the same asset … assigning it would be a guess." Conservative and
  correct, but it means a rerun cannot re-fill that asset without clearing the filled sibling.
- **A near-miss to avoid.** Selecting the file with a page-wide `input[type=file]` query staged the sheet
  into the *Upload IR Photos* dialog instead of the form-fill dropzone. It was cancelled before upload —
  but always scope the file input to `[role="dialog"]` in this app.
- The **Forms tab is under *More*** on this work order, and a draft row did not open its renderer from the
  grid; the persisted values had to be read from `GET /eg-form-instance/{id}`.

## Files

- `docs/bug-reports/2026-09-09-QA-ZP-3782-ZP-3783-fill-forms-retest-verdict.md` (new)
- `docs/report-artifacts/2026-09-09-QA-ZP-3782-ZP-3783-fill-forms-retest.html` (new, 379 KB)
- `docs/bug-evidence/zp-3782-3783-fill-forms-retest/` — the sheet, the composed evidence card, the Forms grid
