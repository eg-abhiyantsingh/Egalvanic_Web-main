# 2026-09-08 — QA: ZP-3937 [Web] Free-text manufacturer/model in equipment links meant the same real part produced different identities

**Prompt:** the ticket text with its 7-step QA review ("test this too").

## What was done
1. **Read what is actually stored** — the reference carries the equipment type, the catalog id and its subsettings (poles, amps, fuse class) and **no manufacturer, model or family text at all**. The manufacturer survives only as a browse filter and as the resolved record's own label.
2. **Confirmed browse-before-typing** — opening the catalog field issues an empty-query browse, so the dropdown lists real entries before a character is typed.
3. **Proved the manufacturer dropdown is type-scoped and narrows** — 17 rows for one maker, exactly one for another.
4. **Confirmed the Frame → Amps mechanism** — picking a breaker returns a Frame axis whose every option carries an ampere rating to patch in, and the Frame select duly appears. Selecting one was left undone rather than writing to shared library data.
5. **Ran both negatives** — 14 of 17 library entries are unlinked by name, and the editor states the rule itself; one entry still stores the legacy free-text shape and its chip and dialog both render cleanly.
6. **Substituted a stronger fact for the untestable agent step** — evaluation does not write to the library at all, which makes a double-mint from two runs impossible on this build.
7. Verdict, evidence, artifact page.

## Results (short)
- **PASS** — identity is the catalog entry plus subsettings; legacy references still load.
- **FINDING 1 (Low)** — manufacturer lists include ANSI/UL standards documents, which is harmless for identity now but removes any way to pose "a manufacturer with no parts of this type".
- Not covered: reading Amps back after a frame pick, a fresh save-and-reopen cycle (both write to shared data), and the agent double-mint check.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3937-catalog-entry-is-the-identity-verdict.md`
- Evidence: `docs/bug-evidence/zp-materials-library-equipment-linkage/`
- Artifact: https://claude.ai/code/artifact/920bf269-332a-4ce4-b0ee-54cdfe2d3081

## Depth notes (learning)
- **For an identity change, the proof is what is *absent* from the payload.** Confirming the stored reference has no manufacturer key is the whole ticket; a screenshot of the dropdown is not.
- **When a step is untestable, look for an adjacent fact that settles the same concern.** "No agent run is triggerable" is a dead end; "evaluation does not write to the library at all, so the race cannot happen" answers what the step was worried about.
- **Prefer read-only proofs on a shared tenant.** Resolving an existing link proves persistence as well as creating one, and leaves the tenant as you found it.
