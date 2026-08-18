# Build a service by hand — first asset class, forms, rules, tagged uploads — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-engineering-ai-pipeline **#46** · eg-pz-backend **#997** · eg-pz-frontend **#1177** · eg-pz-frontend **#1179**

---

## Verdict — live on QA and the core hand-build flow works end to end

I built a service by hand on QA (`QA-DEMO hand-built`, id `a6342955`) and walked the flow: Create Without AI → add asset class + first procedure → method editor → rules → attach-form picker. The backend accepted every step (`POST /api/procedures-v2/services` → **201**, plus live `/versions`, `/export-spec`, `/procedures` endpoints). **7 items verified PASS; several deeper behaviors are noted honestly, including one negative test I could not run cleanly.**

## ✅ Verified

| # | QA item | Result | Evidence |
|---|---|---|---|
| 1 | Create Without AI → empty state offers **Add asset class** beside Build with AI; class list reachable before any work | ✅ PASS | Empty state: *"This service is empty… Add asset class \| Build with AI"*, "No asset classes covered yet." |
| — | Status chip stops saying **"draft"** once first work lands | ✅ PASS | Adding the Transformer class + procedure flipped the chip **Draft → "Needs pricing"** |
| — | Rules render under the class (conditions + selected procedures), "checked in order — first match wins" | ✅ PASS | RULES: *"checked in order — first match wins"*, catch-all *"Every other asset → QA IR Scan"* |
| 2 | Attach existing form → the service gets its **own copy**, original untouched | ✅ PASS (contract) | The "Attach a Form" picker states it verbatim: **"A copy is added to this service — the original is left untouched"**, and lists the company form library |
| 2/3 | Method editor exposes **Attach existing form** and **New blank form** | ✅ PASS | Both buttons present in the QA IR Scan method editor |
| #1179 | Procedure **rename** — name is editable (was a read-only header) | ✅ PASS | The method editor's **Name** field is editable |
| #1179 | Add-asset-class dialog does **not reopen** itself after adding a class | ✅ PASS | Dialog closed and stayed closed after "Add" |

Evidence: `docs/bug-evidence/service-builder/create-service-forms-pricing-sections.png` (Create Service with paired **Forms + Pricing** sections, each with its own uploader + switch — item 7) and `attach-form-clone-contract.png` (the clone contract).

## ⚠️ One negative test I could NOT run cleanly (not reporting it as pass or fail)

**Unknown condition key / operator → 400.** I tried to POST a version whose rule carried an unknown `when` key (`qa_bogus_key_zz`), a string `criticality`, and a bogus nested operator. All returned **201** — **but this is inconclusive, not a failure**: after every POST the spec reverted to just the original `Default` catch-all, i.e. **my injected rules never persisted** through `POST …/versions`, so the validator never actually saw a bad rule. The rule grammar (`when: {criticality, subtype, …}`) is edited through the UI "Edit rules" surface, which is where the ZP-3747 validation lives. **I am explicitly not claiming the validation is broken** — my API path simply wasn't the one that feeds the validator. This needs the UI rules editor (add a bad condition and Save) to test properly; I'll do that on request.

## Not yet exercised (deeper flows)

- **Full clone proof** — the picker *states* "a copy… original untouched"; I did not attach → edit the copy → reopen the original to prove it byte-for-byte. (The contract text is the strong signal; the end-to-end diff is the gold standard.)
- **Tenancy negatives** — attach a global / other-company form → rejected; post a version with a rejected spec → 400. (The attach picker showed only company forms; I didn't force a foreign/global form id.)
- **COM-3 materialization** (item 5) — add a COM-3 rule ahead of the catch-all *and confirm an asset with COM 3 materializes its procedures*. The rules editor renders and pins the catch-all last (visible), but materialization against a real asset wasn't run.
- **#1179 list-dimming** — a one-field edit should dim the list (0.55 opacity/120ms) rather than swap to a spinner, keeping expansions/scroll. Not measured.
- **Build switches (#46)** — build with `create_forms`/`set_pricing` off → no invented forms/pricing; on → forms authored. Not run (needs the AI build path).

## Recommendation

The hand-build spine is solid on QA. To close the rest in one focused pass: (1) attach a form, edit the copy, reopen the original to prove isolation; (2) in the **Edit rules** UI add an unknown condition and Save → expect a readable 400; add a COM-3 rule and materialize it on a COM-3 asset; (3) force a foreign/global form id at the attach endpoint → expect reject. All are quick once set up; say the word.

## Method notes
- Service `a6342955` ("QA-DEMO hand-built (delete me)") left in place per the leave-test-data preference; it carries a Transformer class, a "QA IR Scan" procedure, and several spec versions from the (non-persisting) rule POSTs.
- Backend confirmed live by direct use, not by deploy-note: create returned 201 and every builder endpoint responded.
