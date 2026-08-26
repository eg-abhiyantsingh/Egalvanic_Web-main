# Live highlighted demos of every remaining issue

**Date:** 2026-08-26 · **Prompt:** "show other issue on live browser by highlighting the role text etc"
→ "show with slow speed and properly live" → "save that in memory"

Each defect driven step-by-step in a real browser, offending element outlined, working control element
outlined in green alongside it where one exists. Screenshots in
`docs/bug-reports/evidence/2026-08-26-live-demos/`.

| Issue | Demo | Live result |
|---|---|---|
| **UI-1** page-local sort | `slow_01..04` | 4-step sequence. Descending @25/page top = `8N-H1-1`; change ONLY rows-per-page → 100 and the same sort puts `FDR-PH-H1` on top with 17 `FDR-*` rows above where the old top row sat. "F" > "8", so they always outranked it — they were never compared. |
| **UI-2** Created column | `ui2_created_stuck` | 3 clicks, `aria-sort` never leaves `descending`, top row frozen. Due Date on the same grid (green) reorders + fires a request. |
| **UI-3** raw JS error | `demo_ui3` | `/sessions/<missing>` renders `Unexpected token '<', "<!DOCTYPE "... is not valid JSON` as the user-facing message. |
| **UI-4** blank page | `demo_ui4` | `/customers/<missing>`: `main` measured live = **0 chars, 0 buttons, 0 links**. |
| **A11Y-1** unnamed buttons | `demo_a11y` | `/sessions`: **26 of 46** buttons in `main` have no text, no `aria-label`, no `title` — the per-row delete controls. |
| **SEC-1** no rate limiting | `demo_sec1` | 8 failed sign-ins through the **real form**: every one "Invalid credentials", captcha `false`, Sign In never disabled, flat ~2.6s. |
| **RBAC-3** matrix drift | `demo_rbac3` | All 6 roles signed in live, `/auth/v2/me` diffed against the matrix in-page: **59 differing cells**, Account Manager `role_id` **MISMATCH**. |

## Account Manager role_id — what could NOT be established
The owner asked to see this one specifically. I could show the mismatch
(`92f38105…` recorded vs `392a2233…` live) but **not** whether the old role still exists, was renamed,
or was deleted: there is no roles-listing endpoint reachable from the browser session. Probed
`/api/roles`, `/api/roles/`, `/api/auth/roles`, `/api/users/roles`, `/api/permissions/roles`,
`/api/company/{id}/roles` — all return the SPA catch-all (`200 text/html`); `/api/company/roles`
returns `422`. So "the role was recreated" remains the **best explanation, not a proven one**, and
answering it needs DB or admin-API access I do not have.

## Technique saved to memory
`feedback_live_highlighted_demos` — the demo recipe (step banners, red target + green control,
vary-one-parameter) plus the browser-tooling gotchas: no `require`/`fs`/dynamic `import` inside
`browser_run_code_unsafe`; `filename:` scripts must be a bare arrow function and live under an allowed
root; `page.screenshot` writes to the Playwright host cwd and hangs on some SPA pages (use a fresh
tab); `input[name=email]` is `type=text`.
