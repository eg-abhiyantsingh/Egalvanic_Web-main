# Issue-classes template check + dashboard issue-type test update

- **Date:** 2026-06-22
- **Prompt:** "@testcase/issue_classes_template.xlsx check the latest file [and] update test case"
- **Type:** Data-template review + test alignment.

---

## Checked `testcase/issue_classes_template.xlsx` (latest)

3 sheets — **Classes** (7), **Core Attributes** (21), **Options**. Far cleaner than the asset
template (no junk classes, no copy-paste descriptions, referential integrity intact, no
DELETE-flagged rows).

**7 issue classes:** NEC Violation, NFPA 70B Violation, OSHA Violation, Repair Needed,
**Replacement Needed**, Thermal Anomaly, Ultrasonic Anomaly. New since the tests were written:
the `exclude_ai` column and the `multi_select` type.

**Findings (template-side, reported not fixed — not test-case scope):**
1. `Options.Types` reference list is **incomplete** — it lists `calculated, number, select,
   table_with_column_headers, textarea, textfield` but the attributes also use **`multi_select`**
   (NEC Violation × 2) and **`temperature`** (Thermal Anomaly × 2). The vocabulary sheet should
   add those two so it covers every type actually used.
2. **`Ultrasonic Anomaly` has 0 core attributes** — defined as a class but carries no fields
   (the other anomaly classes do). Likely incomplete.
3. `Thermal Anomaly/Severity` is `calculated` yet carries options (Nominal/Intermediate/Serious/
   Critical) — probably intentional (the computed-output domain), but inconsistent with `Delta T`
   (`calculated`, no options). Worth a glance.

## Test update (the "update test case" ask)

`DashboardBugTestNG` hard-coded the dashboard issue-type list as **6** classes in two places
(`testBUGD03` legend-dup check + `testBUGD04_IssueTypeCategoriesPresent`) — it was **missing
`Replacement Needed`**, which the latest template defines. Updated both arrays to the full **7**
(in template order) and added a `// per testcase/issue_classes_template.xlsx` source note.

Both tests use lenient logic (occurrence-count / `found >= 1`), so adding the class is safe;
`mvn test-compile` clean. `DashboardBugTestNG` was the only file carrying the issue-class list
(IssuesSmoke uses just "NEC Violation").
