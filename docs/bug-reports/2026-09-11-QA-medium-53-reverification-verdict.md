# All 53 Medium findings re-tested live — 39 open, 14 dropped

**Date:** 2026-09-11 · **Environment:** acme.qa.egalvanic.ai, V1.36 · **Artifact:**
https://claude.ai/code/artifact/9b83e732-0072-40c8-8771-035f7d76617e (Version 11)

## Why

Owner, across three messages: *"check medium priority too"*, *"have you update medium priority too in
artificate"*, *"whatever is fixed remove that form artificate so that artificate looks more clean"*.
The register carried 53 Medium claims of which only 13 had been re-tested; the section was a
half-finished "re-verification" box rather than one readable list.

## Method

One verifier agent per claim against the live app, then a second agent per verdict told to disprove it
(53 verify + 22 refute completed; the refuter hit a session limit on the rest, but every verify verdict
landed). Primer forced: a working example beside every claim, the masked-HTML soft-404 trap called out
explicitly, and unknown-id distinguished from foreign-id.

**The refuter overturned three of its colleague's verdicts** — #9 and #10 from REAL to INVALID, #11 from
REAL to PARTIAL. That is the pass working as intended.

## Tally

| Verdict | Count |
|---|---|
| REAL | 35 |
| PARTIAL | 3 |
| FIXED | 7 |
| INVALID | 7 |
| CANNOT_TEST | 1 |

Raw verdicts: `docs/bug-evidence/2026-09-11-register-rebuild/medium-53-verdicts.json`
(claims as they stood: `medium-53-claims.json`).

## Fixed since written — removed from the register

#3/#26 cross-tenant create (no longer lands; victim's own create on the same site does — positive
control), #19 maintenance routes now guarded (Account Manager refused on both), #20 site picker present,
#33 Site Walks direct-URL now shows Feature Not Available, #37 Condition Assessment no longer prints a
raw permission error, #46 add-to-quote gate closed on both routes.

## Never bugs — removed from the register

#1 foreign plan id returns the SPA shell, byte-identical to a random id (the 200-means-success trap);
#9 live property definitions have no colliding names; #10 report-config writes gate on a permission by
design; #16 the "no manual path" leap is false — the seeded work type resolves real panels; #18 the
catalog described no longer exists; #21 the two roles do not hold the same permissions, so the premise
fails; #43 the critical warning IS present and names the conflict.

## Re-classified, not product bugs

- **#34 `/api/tasks/all` 500** — stays out. The owner already ruled it dead code on 10 Sep; the workflow
  re-confirmed the 500 but also that nothing in the bundle calls it.
- **#25 stale RBAC baseline** — our own contract test diffs live QA against a 15 Jun PROD export. Our
  test suite, not the product.
- **#44 PR #1391 only on `cicd/qa`** — release-management question (qa is 67 ahead / 25 behind dev).

## Nine reproduced by hand in the browser today, with screenshots

`docs/bug-evidence/2026-09-11-register-rebuild/` — #22 dead Created column (with the Due Date control in
the same frame), #41 new asset missing from the Add-Issue picker (fresh repro: badge 2→3 while the picker
stays at 280), #47 unknown quantity prices as ×0·$0·$0, #42 Label Placement has no tick-box column,
#40 Client Portal tile → Access Denied (two frames), #39 FM opens an unmapped site's work order,
ZP-4123 EE menu → Access Denied / FM refused at `/maintenance/program` but admitted at
`/maintenance-portal/program` / CP likewise, #17 issue page has no back control, #23 Technician web-blocked
but still holds `quotes.approve`.

## The role-rendering ticket (Web: Role-Based Access Rendering Issue) — does not reproduce

Condition Assessment renders for **all six roles** checked live today (Super Admin, PM, FM, Client Portal,
Account Manager, Electrical Engineer) — screenshots for five of them in the evidence folder. Project
Manager gets Maintenance Program and it renders. Both acceptance criteria are met.

**What is still wrong is the mismatch set, which is ZP-4123, not this ticket:** EE is shown a menu link to
a page that refuses them; FM is refused at `/maintenance/program` yet admitted to the same content at
`/maintenance-portal/program`; Client Portal reaches it the same way. Owner-stated intent recorded: CA for
every role and PM having Maintenance Program are both *by design*, so only the mismatches are defects.

## Not settled

- **#27** asset created against a non-existent equipment class — confirmed 10 Sep, read-back never
  resolved today (async write path). Open but unproven.
- **#30** raw cloud error printed in the dialog — the stored error is confirmed live (2,000-char AWS
  descriptor, still readable ~3 weeks on, readable by a non-creator Technician) and the render path is
  unambiguous in the current bundle, but a failing job takes ~17 min and cannot be forced.

## Footprint

Read-only except one labelled asset, `QA-VERIFY n41b delete me` (Circuit Breaker) on work order
`b67a3c26-583d-4f59-997e-95327c26c3ae`, created to reproduce #41. Left in place per the sandbox rule.

---

## Re-check of the removals (asked: *"are you sure they are invalid you have removed all of them?"*)

**First, the count.** The register holds **53 Medium** findings, not 59 (full severity split: P1 8,
Critical 2, High 41, Medium-High 2, Medium 53, Low 35 = 141). **I removed 13, not all of them. 40 remain
on the page.**

Re-ran the decisive check for the seven INVALID rulings on 2026-09-11 afternoon, live as Super Admin:

| # | Ruling | Re-check today | Holds? |
|---|---|---|---|
| 1 | foreign `pm_standard_id` accepted | Real standard `NFPA 70B 2026` → **200 application/json, 6 services**; unknown id → **200 text/html**, the SPA shell, 0 rows. A non-own id returns no data at all. | ✅ invalid |
| 9 | issue property names collide | **WRONG — put back.** 50 classes, 84 property names, **23** distinct once trimmed/lowercased. One genuine collision: `Current Draw (A)` vs `Current Draw (A) `. Plus one blank name. | ❌ **reinstated** |
| 10 | report-config write authz | `reports.manage`: Technician **true**, PM **false**, FM **false**. Permission-gated exactly as the matrix grants it. | ✅ invalid |
| 16 | panel-schedule flag unseeded | `Panel Schedule Updates` service exists on the tenant (manual path available). *Did not re-fetch all 216 procedure details today* — resting on the workflow's sweep for the 0-flag half. | ✅ invalid (partly carried) |
| 18 | 53-method fix catalog has no forms | 20 services live; **no "Issue Resolution — Any Asset" among them.** The catalog described is gone. | ✅ invalid |
| 21 | PM and FM hold an identical permission pair | PM 95 perms **with** `accounts.view` + `accounts.manage`; FM 75 perms **with neither**. Premise false. | ✅ invalid |
| 43 | real kA conflict gives only an ordinary warning | CB5: `aic_rating 65`, `warnings 1`, **`critical_warnings 1`** reading *"Interrupting rating mismatch: asset AIC Rating 65 kA vs bound library frame 25 kA @ 600 V (Powerpact HJ)"*. | ✅ invalid |

**Six of seven held. One did not, and is back on the page** in the reports/services table with its true
scale stated (one pair, plus one blank name) so it is not over-read.

**The seven FIXED rulings.** #19, #20 and #37 were re-confirmed by me in the browser this morning across
PM/FM/CP/AM/EE seats (screenshots in the evidence folder). **#3/#26 and #33 rest on the second tenant and
were not re-run this afternoon** — they carry the workflow's evidence and controls, and are marked as such.
#46 rests on the workflow's 409-with-controls result.

**Method note:** three of my first probes used the wrong call shape (query-string instead of the real
parameter, `data` wrapper assumed where the response is a bare array, `methods` assumed nested where the
list only returns `method_count`). A wrong-shaped probe returns a confident-looking wrong answer — always
land a **positive control** first, which is what caught all three.

---

## Test-data URLs (owner: *"also add url if you creating bugs or work order or asset link"*)

**Created by this session — one record only:**
- Asset `QA-VERIFY n41b delete me` (Circuit Breaker), created 11 Sep to reproduce the stale-picker bug:
  https://acme.qa.egalvanic.ai/assets/aa24924b-1984-4b44-ac8b-71ece88c2aba

**Records the findings use:**

| What | Name | URL |
|---|---|---|
| WO, stale-picker repro | 11 sep abs · Android Site 2 · Test op | https://acme.qa.egalvanic.ai/sessions/b67a3c26-583d-4f59-997e-95327c26c3ae |
| Quote, ×0 material | QA-DEMO SCCR upstream-fuse quote (delete me) | https://acme.qa.egalvanic.ai/plans/488ee745-53a2-4846-aa26-c3089f6b6ad1 |
| WO, missing tick box | ZP-4018 QA session-scope + partial (delete me) · Addtioanl Site | https://acme.qa.egalvanic.ai/sessions/afea6fa4-e7a7-47e7-a0fd-9933afbc64a3 |
| WO, site-scope leak | "test" · Android Site 2 · Test op | https://acme.qa.egalvanic.ai/sessions/8d9aad65-fd8c-4fb7-bbc2-05f2e76cc688 |
| Issue, no back control | NFPA 70B Violation on 11N-H1-2 | https://acme.qa.egalvanic.ai/issues/b9492227-53d3-48d4-87c6-357691d27418 |
| WO behind the failed photo-fill job | Infrared Thermography (I.R) · Chicago illinois | https://acme.qa.egalvanic.ai/sessions/fcc37c67-01fc-4940-87f3-8028fc86e97a |
| Breaker falsely "Library Matched" | CB2 · Android Site 2 | https://acme.qa.egalvanic.ai/sld?focusNode=7ee495b9-7b81-4e59-a58c-26e5ccb01ba9&sldId=aadcee4c-7dd0-45b3-81b9-309c5c166084 |
| Panelboards, trimmed PM plan | 11N-H1-1 / 12N-H1-1 | https://acme.qa.egalvanic.ai/assets/532b10d9-fba9-4a76-8f18-1f86638f7195 · https://acme.qa.egalvanic.ai/assets/9af47d76-a69d-4bdd-8739-695fa4cd1daf |
| Quote that zeroes on save | QA-VERIFY n32 orphan-walk delete me | https://acme.qa.egalvanic.ai/plans/3e5d3340-3754-4716-bd67-efd79c75daa7 |
| Quote, save-validation probes | QA-VERIFY n7 generate-validation delete me | https://acme.qa.egalvanic.ai/plans/fdec1116-2ab9-4e6c-8940-c171355e50b7 |
| Grids with the dead Created column | Work Orders · EMPs | https://acme.qa.egalvanic.ai/sessions · https://acme.qa.egalvanic.ai/emps |
| Page the menu offers and the app refuses | Maintenance Program | https://acme.qa.egalvanic.ai/maintenance/program · https://acme.qa.egalvanic.ai/maintenance-portal/program |

Also on the artifact as an **Open it here** line inside each screen bug, plus a closing
*Every record these findings use* table. Artifact now at Version 15.
