# 2026-10-06 — Web v2.3: deep test of the 12 Ready-for-QA tickets (no Jira status changed)

**Prompt:** "test ready to qa ticket in depth. but dont update any status just test the ticket and create artifact just for this prompt."
**Build:** QA acme `index-Buq-IBvX.js` (deployed 17:31 IST; carries ZP-4435 PR #1668, ZP-4398 PR #1670 + BE #1478, ZP-3801 BE #1477). Seats: +admin@ (staff, 6 roles), +project@ (PM, T1), +fm@ (FM, T2).
**Page:** a new artifact just for this prompt (link in the session's final message). Evidence: `docs/bug-evidence/2026-10-06-v23-qa-deep/zp<n>/results.txt` + screenshots; `NOTES-draft.txt` is the running log.

| Ticket | Result |
|---|---|
| ZP-4398 | PASS — preview-entities `default_entity` = Performance sld (8,487 nodes, largest of 262); picker "Site"; real AI edit ran with sld_id context (96 s, edits applied) |
| ZP-4435 | PASS — keyboard path fixed (Search → Enter → Esc → Space → Tab → Enter → Esc → Esc, real keys); group badge + Space; 7/7 real-mouse thumbnail clicks; OBS second hover re-signs URLs; group hover not confirmed (badge covered by a member) |
| ZP-3801 | PASS — 429 at call 51 (per worker), form shows "Reason: Too many verification requests…" and keeps Create enabled; tenant 422s |
| ZP-3802 | PASS (sanity earlier) + deep additions; ZP-4675 already filed |
| ZP-4305 | PASS with open point — resolve/reopen + after photo + upload gate OK; header stays "Resolved" until the form closes (unchanged) |
| ZP-4372 | PASS (web) — 22 pages, 0×5xx; timings unchanged; DB checks with Dharmesh |
| ZP-4394 | PARTLY — section_states on by-node PASS; edit w/o role switch PASS; class filter not exercisable (no class-scoped equipment on QA); section-done toggle not reproducible |
| ZP-4148 | FAIL as written — feature/tier gate, 0 "Portal Sales" in bundle; PM opens /condition + /sld; FM T2 gets the portal (AC4) |
| ZP-4301 | FAIL (frontend) — toast shows `collapsed_node_not_deletable` (API client uses body.error, never body.message); web force-deletes so the refusal never happens |
| ZP-4590 | FAIL on QA — 95/90 °F → Nominal; no "Minor" in any class |
| ZP-4529 | NOT CONFIRMED — simulated empty 200 still "HTTP 200"; no limit text in the chunk |
| ZP-4423 | NOT WEB-TESTABLE — pipeline only |

**Incident (test data):** during the ZP-4301 bulk-delete check the row ticked as "Fuse 1" was a second asset of that name on Android Site 2 (`d45b3d40-92aa-4e00-b80c-b97bf95abb5c`, not collapsed) and the web's force flag deleted it (200). No restore in the web app. Disclosed to the owner.
**Test data left (labelled):** issue f8f71b23 (QA-DEMO ZP-4305, Open), library entry "QA-DEMO ZP-4394 CB-only tester (delete me)", view 13292fe7 (fuses un-collapsed), the Copy report config now carries a "QA deep test 6 Oct 2026" cover line.
**Tooling:** `QaDriverDaemon` gained a `hover` command (real Selenium mouse move + optional click); 3 daemons (admin ×2, PM/FM) driven by `q.sh`/`qj.sh`.
