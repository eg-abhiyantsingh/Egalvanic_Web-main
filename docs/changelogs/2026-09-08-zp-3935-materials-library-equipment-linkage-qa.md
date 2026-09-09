# 2026-09-08 — QA: ZP-3935 [Web] Materials-library entries had no way to record which real piece of equipment they represent

**Prompt:** the ticket text with its 8-step QA review ("test this ticket too").

## What was done
1. **Confirmed the Equipment Link column carries a real identity** — three entries on the tenant persist one, and reopening any of them resolves the stored reference to a named catalog record live.
2. **Walked the linkage manager** — type, manufacturer filter, catalog entry, poles, amps, fuse class, a clear action, and helper text stating the identity rule.
3. **Proved the unpriced queue is live** — the toolbar count matches exactly the one row whose price is blank, and the filter resolves to it.
4. **Deliberately skipped every write path** and said so — creating a link, clearing one, pricing an entry out of the queue would all mutate shared library data; the read, resolve and display halves were verified instead.
5. **Noticed the ticket's own form no longer exists** — this work introduced manufacturer and model as free text; a later change removed those keys in favour of the catalog entry, so step 1 cannot be performed as written.
6. **Answered the agent-dedupe concern with a stronger fact** — evaluation does not write to the library at all, so an agent stub cannot race a curated row on this build.
7. Verdict, evidence, artifact page.

## Results (short)
- **PASS on the read paths** — column, linkage manager, resolve, and the unpriced queue.
- **FINDING 1 (Low, documentation)** — the manufacturer-and-model linkage this ticket introduced no longer exists; the only entry still holding a free-text key is a survivor from before that change, which is itself the backwards-compatibility case, and it loads cleanly.
- Not covered: all write paths, both validation negatives (one needs a crafted write, the other a global row this tenant does not expose), and the agent dedupe.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3935-materials-library-equipment-linkage-verdict.md`
- Evidence: `docs/bug-evidence/zp-materials-library-equipment-linkage/`
- Artifact: https://claude.ai/code/artifact/b242ff8a-fe6a-40cb-bac1-85676e04bb05

## Depth notes (learning)
- **On a shared tenant, decide the read/write line up front and state it in the report.** "Not covered because it writes to shared data" is a defensible gap; an unexplained gap is not.
- **A superseded ticket is worth testing anyway** — the entry still carrying the old shape *is* the backwards-compatibility case that the newer ticket depends on, and it would never be tested if this one were skipped.
- **The oldest ticket in a chain explains the newest.** Reading 3935 → 3936 → 3937 in order is what made the identity change legible as a deliberate progression rather than three unrelated findings.
