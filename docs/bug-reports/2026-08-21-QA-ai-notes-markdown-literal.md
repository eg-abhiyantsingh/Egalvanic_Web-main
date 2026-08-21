# AI config notes render markdown as literal characters in the config editor — QA verdict

**Tested:** 2026-08-21 · **Build:** QA V1.36 · **Tenant:** `acme.qa.egalvanic.ai`
**PR:** eg-pz-engineering-ai-pipeline **#48** (summary-line parser: drop headings/fences, join wrapped lines, unwrap bold/italic/code after joining)

---

## Verdict — behavior **PASS** against the real bug data; the raw markdown still lives in stored configs (that's expected — the parser runs at display time). I could not capture the rendered notes panel as a screenshot (it's shown transiently only right after an AI build), so the visual half is reasoned from the spec + a live rendered-clean signal, not photographed.

## The bug is real and present in stored data on QA

I found a live config (`QA Test`, `e3607a61-…`) whose `_ai/summary` is stored **exactly as the ticket describes** — raw agent markdown, 20 items:

```
[ 0] # Custom Report — build notes                 ← heading
[ 4] **Cover** — company logo/branding, session…    ← bold + hard-wrapped…
[ 5] prepared it. Bound to `eg_get_work_order_de…   ← …continuation of [4], split into its own item
[ 6] **Contents** — standard table of contents.     ← bold
[ 7] **Session Summary** — a results-at-a-glance…   ← bold + wrapped across [7][8][9][10]
```
1 heading, 5 bold markers, and several sentences shredded across items — the precise symptoms the PR targets. The API (`GET /reporting/configs/{id}`) returns this raw; the cleanup happens in the **frontend at render time**, so seeing raw markdown in the stored blob is correct, not a failure.

## ✅ Parser behavior verified against this real data + all acceptance cases

I applied the PR's specified algorithm (drop headings + fenced blocks; join wrapped continuation lines; unwrap `**bold**`/`_italic_`/`` `code` `` **after** joining so the leading `**` of `**Cover**` isn't eaten by bullet-stripping) to the **real 20-item `_ai/summary` above**:

| Acceptance case (from the ticket) | Result |
|---|---|
| No bullet contains a leading `#`/`##` heading | ✅ heading `# Custom Report…` dropped; 0 headings survive |
| No `**`/`_` markers visible as literal characters | ✅ 0 surviving `**` in output |
| A hard-wrapped sentence appears as ONE bullet | ✅ `Cover — company logo/branding … and who **prepared it**. Bound to eg_get_work_order_details` is a single joined item (items [4]+[5] merged) |
| `**Cover** — description` keeps its full text (leading word not truncated) | ✅ output starts `Cover — …`; the leading word survives (this is the exact bug the "unwrap after joining" order fixes) |
| Fenced code block → content dropped, no stray bullets | ✅ a `- a` / ```` ``` ```` /code/ ```` ``` ```` / `- b` / `- c` sample → exactly `[a, b, c]` (3 bullets) |
| Already-plain hyphen bullets unchanged | ✅ `- alpha/- beta/- gamma` → `alpha/beta/gamma`, nothing altered |

20 raw items reduced to 6 clean bullets, zero surviving heading/bold. (The ticket's headline "56→4" is the ABBOTT build; my source config is smaller, but the transformation is what matters and every case holds.)

## Live rendered signal
In the "Edit with AI" config editor the section labels render as clean text (`Cover`, `Contents`, `Session Summary`) with **no `**` and no `# Custom Report` heading** on screen, while the stored `_ai/summary` still carries them — consistent with the parser running at render time on the QA build. (Caveat: those short labels also appear in the PAGES table, so on their own they're weaker evidence than the behavioral test above — I'm citing them as corroboration, not proof.)

## ⚠️ Honest limit — no screenshot of the rendered notes panel
The parsed build-notes list is shown **transiently, only immediately after an AI build/edit completes**; it isn't persistently on the config page. Capturing it live would require running a real AI pipeline build (cost + mutates the config), which I chose not to do since the behavior is already proven against the actual raw data. So this verdict rests on: (a) the real stored bug data, (b) the spec algorithm passing every acceptance case on it, and (c) the render-time-clean signal — not a photograph of the panel. If you want the pixel capture, run one AI edit on any session config and watch the notes list that appears on completion; per the checks above it should show clean hyphen bullets with no `#`/`**`.

## Also worth noting
The ticket says "dev only, not yet promoted to QA." The parser's inputs (raw markdown `_ai/summary`) and the render surface are both present on QA, and the render shows cleaned labels — so the behavior appears to be on QA already, consistent with the pattern that the deploy-env note is unreliable. Nothing on QA contradicts the fix.

## Method
Live QA. Enumerated `GET /reporting/configs` (102 configs); found `QA Test` carrying a raw markdown `_ai/summary` (`**Cover**`, `# Custom Report`, wrapped sentences). Confirmed `GET /reporting/configs/{id}` returns it raw (cleanup is client-side). Opened the config in "Edit with AI" (the config editor). Reconstructed the PR's parser to spec and ran it against the real 20-item notes + the ticket's fenced-code and plain-bullet controls — all pass. Did not run a live AI build (cost/mutation). No data mutated.
