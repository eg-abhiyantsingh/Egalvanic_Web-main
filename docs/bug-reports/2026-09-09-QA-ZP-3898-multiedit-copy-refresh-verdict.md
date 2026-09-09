# ZP-3898 — EG form multi-edit copy refresh · QA verdict: **PASS**

**Ticket:** [ZP-3898](https://egalvanic.atlassian.net/browse/ZP-3898) — "[Web] Copying a section in EG
form multi-edit updated the data but not the fields on screen, so the copy looked like it had failed"
(Ready for QA) · fix: eg-pz-frontend **#1272** (prod hotfix, cherry-picked to stag/qa/dev)
**Artifact:** https://claude.ai/code/artifact/9bfb2949-a484-467f-a40d-7bb3e9937f5b
**Tested:** 2026-09-09 · **Env:** `acme.qa.egalvanic.ai` · V1.36, bundle `index-jYhUcFb4.js` · tenant acme
**Method:** driven in the product UI as Project Manager, with a console shim recording every
`console.error`/`warn` for the React-warning check. Values read straight from the live inputs
immediately after each action — no save, no reload — then re-read after a settle delay. Persistence
confirmed from `GET /eg-form-instance/{id}`, not from the screen.

**Fixture:** WO `465df0c4-39a0-4787-a815-642729c29707` ("Atest123 New WO"), **Signature Test**
template, three instances multi-edited via **Forms → Bulk Ops → select 3 → Edit**:
`11f1eeb7` (primary), `3d47b92a`, `61d4cf40`.

---

## Verdict — the fix works, on both paths it was meant to fix

| # | Checklist item | Result |
|---|---|---|
| 1 | Environment: dev + QA (also stag/prod), one file changed | ✅ behaviour present on QA (branch/diff check not possible from QA — see boundaries) |
| 2 | Copy a section from the first card → other cards update **on screen immediately** | ✅ **PASS** |
| 3 | Same with a **Full Form** copy → lands on all three at once | ✅ **PASS** |
| 4 | Same through **Grid Edit** (shared caller-side write path) | ✅ **PASS** |
| 5 | Save and reopen → screen values == persisted values | ✅ **PASS** (verified via API) |
| 6 | **Negative, most important:** type into a pinned participant card — no echo, no clobber | ✅ **PASS** |
| 7 | **Negative, reverse:** copy FROM a participant → primary still updates | ✅ **PASS** |
| 8 | Pagination: copy applied on page 1 visible on page 2 | ⚠️ **NOT EXERCISED** — see below |
| 9 | No React warnings (update-depth, set-state-in-render) | ✅ **PASS** |

## The measurements

**Check 2 — section copy.** All three cards started at `Marcus Webb / 78`. Typing on card 1 only
changed card 1 (`QA ZP-3898 copy check / 91`), leaving 2 and 3 alone. After the copy, **immediately**:

| | card 1 | card 2 | card 3 |
|---|---|---|---|
| after copy | QA ZP-3898 copy check / 91 | **QA ZP-3898 copy check / 91** | **QA ZP-3898 copy check / 91** |

Toast: *"Copied General Information to 2 other asset(s) — not saved yet"*. Re-read 3 s later:
unchanged. Before the fix these two cards stayed blank/old while the toast still claimed success.

**Check 3 — Full Form copy.** Cards 2 and 3 went from `Reverse from participant / 91` to
`FullForm scope test / 44` on the copy, toast *"Copied Full Form to 2 other asset(s)"*.

**Check 4 — Grid Edit.** `Grid Edit → Pick Fields → Inspector Name → Open Editor` gives one row per
form. Editing **only rows 2 and 3** and pressing *Apply 2 Changes* immediately showed
`Grid row2 value` and `Grid row3 value` on those two cards, with card 1 untouched.

**Check 6 — the regression the effect ordering guards against.** Typed
`Participant typed OK` into card 3's Inspector Name **character by character at 60 ms/char**. The
value stuck exactly — not reverted, not duplicated, not echoed back — and cards 1 and 2 were
unaffected, both immediately and after a 2.5 s settle.

**Check 7 — reverse direction.** With `Reverse from participant / 91` on card 3, copying from that
participant updated **card 1 (the primary)** and card 2 on screen.

**Check 5 — persistence.** After *Save 3 as Draft*, re-read from the API:

| instance | on screen before save | persisted |
|---|---|---|
| `11f1eeb7` | FullForm scope test / 44 | FullForm scope test / 44 |
| `3d47b92a` | Grid row2 value / 44 | Grid row2 value / 44 |
| `61d4cf40` | Grid row3 value / 44 | Grid row3 value / 44 |

All three `modified_at 2026-09-09T12:25:50`. Screen and saved data agree.

**Check 9 — console.** No React warnings of any kind through every action above: no update-depth, no
set-state-in-render, no key warnings. The only console noise is unrelated third-party
(`[DevRev] Failed to load PLuG SDK`).

## Worth flagging — the copy is not one click, and the default option can look like the old bug

The copy button does not copy immediately. It opens **"Copy &lt;section&gt; to N other assets?"** with two
choices:

- **Only fill blanks** — *the default* — "existing answers are kept; each asset only gains what it was
  missing"
- **Overwrite everything** — "every asset ends up matching &lt;asset&gt; exactly"

My first click produced no toast and no change, which looked exactly like the reported bug — the
dialog was simply waiting for a choice. More importantly: **on the default option, a card that already
holds a different answer is left alone by design**, which presents identically to "the copy did
nothing". Anyone retesting this ticket (or triaging a repeat report) should confirm which option was
used before concluding the copy failed. Every check above used *Overwrite everything*, because the
participant cards already held differing answers.

## Boundaries

- **Check 8 (pagination) not exercised.** The working set was three forms — a single page, with no
  pager rendered — so "copy applied on page 1 shows on page 2" could not be reached. It needs more
  instances of one template than fit a page. This is the one part of the ticket's own reasoning
  (the caller owns the full set while only a page is mounted) that remains unverified here.
- **Check 1 partially.** QA cannot read the frontend branches or the changed file from this
  environment, so "one file changed / present on four branches" is taken from the ticket. What is
  verified is that the *behaviour* is correct on the QA build.
- Only one template (Signature Test) and one asset class were used; the ticket reproduced originally
  on a Circuit Breaker form. The code path is shared, but strictly this is one template's evidence.

## Test data

- Work order — https://acme.qa.egalvanic.ai/sessions/465df0c4-39a0-4787-a815-642729c29707
- Instances left holding test values (Draft, not submitted): `11f1eeb7` "FullForm scope test",
  `3d47b92a` "Grid row2 value", `61d4cf40` "Grid row3 value", ambient 44
- Evidence — `docs/bug-evidence/zp-3898-multiedit-copy-refresh/`

**Footprint:** three existing Draft form instances on a QA work order now carry these test strings in
place of the earlier demo values. Nothing was submitted; per QA convention the test data is left in
place and clearly labelled.
