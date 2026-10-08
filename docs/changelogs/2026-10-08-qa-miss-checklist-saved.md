# QA miss checklist saved to memory

- **Date:** 2026-10-08 · **Prompt:** "next time remeaing all this point that we miss. save all that in memory"
- **Saved:** `feedback_qa_miss_checklist.md` (Claude memory), pinned as the FIRST Feedback entry in MEMORY.md ("READ BEFORE EVERY VERDICT").
- **14 points**, each from a real 7–8 Oct miss:
  1. Feature flags tested ON and OFF; tell the owner which flag to flip; check the prod bundle has the gate (ZP-4732).
  2. Google/AI output checked for correctness: place_id match, neighbours, other countries, a hide option (ZP-4735).
  3. Record the role's own network before saying a call must be refused (ZP-4699 step 5).
  4. User changes are in Activity Logs, not Audit Log (ZP-4697).
  5. Repeat the identical input before claiming a behaviour change (ZP-4697 no-domain).
  6. Stay in the owner's role scope (CP only).
  7. Label every screenshot PASS or FAIL (ZP-4699).
  8. Re-check the live build before each verdict.
  9. Check the owner's flag claims against the live LD value.
  10. Customer-visible empty data in a side note = a lead (ZP-4729).
  11. Check the Rovo comment author.
  12. Confirm the push is in sync.
  13. Re-check Jira for overlapping stories just before filing (ZP-4731).
  14. Hotfix tickets are tested on stage, and Ready for QA → In QA comes first.
- Detail for 1–2 lives in `feedback_test_flag_off_and_real_world_correctness.md`.
