# 2026-09-11 — All 53 Medium findings re-verified; the register rebuilt clean

**Prompt:** `continue` (carrying three standing asks: *"check medium priority too"*, *"have you update
medium priority too in artificate"*, *"whatever is fixed remove that form artificate so that artificate
looks more clean"*), then mid-turn: *"have you checked this atrtificat carefully"*.

## What shipped

| Deliverable | Where |
|---|---|
| Rebuilt register (Version 12) | https://claude.ai/code/artifact/9b83e732-0072-40c8-8771-035f7d76617e |
| Verdict doc | `docs/bug-reports/2026-09-11-QA-medium-53-reverification-verdict.md` |
| 20 live screenshots | `docs/bug-evidence/2026-09-11-register-rebuild/` |
| Raw verdicts + claims | same folder, `medium-53-verdicts.json` / `medium-53-claims.json` |
| Page source | `docs/report-artifacts/2026-09-11-QA-defect-register.html` (gitignored dir) |

## What I did

1. **Recovered the finished verification.** The previous session's 53-agent workflow completed after the
   session limit hit, so its results were never folded in. Reconstructed all 53 verdicts from the workflow
   journal (`wf_0794f3b3-6c1/journal.jsonl`) rather than re-running: 35 REAL, 3 PARTIAL, 7 FIXED,
   7 INVALID, 1 CANNOT_TEST.
2. **Reproduced nine by hand in the browser**, across six role logins in isolated browser contexts, with
   the offending element outlined red and a working control outlined green in the same frame.
3. **Answered the role-rendering ticket**: does not reproduce. Condition Assessment renders for all six
   roles (five captured); PM gets Maintenance Program. What survives is the ZP-4123 mismatch set.
4. **Rebuilt the page**: fixed and never-were-bugs rows deleted outright, not greyed; every screen bug in
   plain words with numbered steps and the endpoint demoted to a last line.
5. **Self-audited on the owner's prompt and found three of my own errors** — see below.

## The three errors I caught in my own page

- **A number that contradicted its own screenshot.** I wrote "a site with 143 of them" while the published
  frame reads `1–25 of 73`. Anyone comparing the two would have stopped trusting the page. Now 73.
- **A tally that did not match the page.** The counter said 27 data-only findings; the tables show 21 rows,
  because five of the quote findings are the same save call reported five times. Counter now reads 21, with
  the 27-to-21 merge stated in the open line.
- **One finding presented as two bugs.** The Electrical Engineer's dead menu link and the Facility
  Manager's two-addresses-one-answer are the same defect. Merged into one entry with both frames, which is
  also why the screen count is 9 and not 10.

## Depth note — why the second agent mattered

The refuter overturned three verdicts (#9, #10 to INVALID; #11 narrowed to PARTIAL). The #1 case is the
lesson: a foreign plan id returning HTTP 200 looks like a leak until you notice the body is the SPA shell
and an invented id returns the identical bytes. **200 is not success on this app** — content type and a
random-id control decide it. That single trap accounts for most of the invalid findings across this repo.

## Footprint

One labelled asset `QA-VERIFY n41b delete me` on WO `b67a3c26-583d-4f59-997e-95327c26c3ae`, created to
reproduce the stale-picker bug. Everything else read-only.

## Follow-up — the credentials question uncovered a build break

Owner asked: *"do you mean to say any id and password is incorrect?"* Answer: no, nothing in the repo is
wrong. One of **my** login attempts was — I typed the shared password for the Account Manager seat instead
of reading `AppConstants`, which has its own (`AM_PASSWORD = eOr2wZWpe1aE!`). All seven seats signed in on
the correct values.

**But checking that turned up a real defect in our own suite.** `AppConstants.java:79` had a note appended
after `getEnv(...)` that swallowed the statement's semicolon:

```java
public static final String EE_EMAIL = getEnv("EE_EMAIL", "…")  // …; +electric@ is the live EE seat;
```

`mvn compile` reported BUILD SUCCESS because the compiler skipped the unchanged file and reused the stale
`target/classes/…/AppConstants.class`. Touching the file exposed it: `AppConstants.java:[79,102] ';'
expected`. **A clean checkout or CI run could not have built the project.**

Fixed: note moved above the declaration, semicolon restored, and the now-false "EE has no QA account /
tests skip it" comment corrected. `mvn -o clean test-compile` → BUILD SUCCESS.

**Method note worth keeping:** `mvn … | tail` then `echo $?` reports *tail's* exit status, not Maven's, so
the first check said exit=0 on a broken build. Redirect to a file and read `$?` immediately.

Artifact updated to Version 13 with this recorded under "not product bugs".
