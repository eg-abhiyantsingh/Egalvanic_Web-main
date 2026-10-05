# Editing policy for the 22 Jira QA comments (Web v2.3 round 2, 5 Oct 2026)

These comments are posted on the Jira tickets under Abhiyant Singh's name, for developers to read.

## Decided already. Do not change these.
- **First line (QA result) and the Jira action line** are fixed as they appear in the draft (`jira-comments/<KEY>.txt`). Do not
  change them, even if an older finding suggested another wording. The owner rule is: move a ticket to To Do only when the
  failure is 100% certain. Only ZP-4421 and ZP-4398 met that bar. Both were re-proven on the newer build `index-C913tyjW.js`
  (see each ticket's results.txt, section "RE-CHECK on the NEW QA build").
- **Screenshot lines** (`!QA-…png|width=640!`) must stay exactly as written, in the same order. You may improve a caption line
  (`_…_`) only if it is wrong about what that image shows. Several images were replaced or cropped after the first check:
  the "without sign-in" JSON images for ZP-4303 and ZP-4368 are gone, the role-probe labels were cropped off, and ZP-4421 and
  ZP-4398 have new-build images. Ignore older findings about images that are no longer in the draft.
- **ZP-4505:** the greyed-out "Attributes + Library" during creation is the AGREED DESIGN. ZP-4504 comments, 2 Oct: Nency wrote
  "As per the discussion, during asset creation, the Attribute + Library option should be disabled"; Krunal wrote that the
  library match needs the asset's id. Reject any finding that says the create case is unverified or a guess. You may add a Not
  covered line about an older asset that existed before this build.
- **ZP-4303:** the owner moved it to READY TO RELEASE themselves (17:29 IST). Don't write "your call" or "left in Ready for QA".
- **ZP-4372:** the owner reassigned it to Dharmesh. The database checks need someone with database access; say that plainly.
- **ZP-4305:** not moved to To Do. The re-check found that /pull-through-work shows a blank page with only the old heading
  "Pull-Through Work", while a made-up address shows a blank page with no heading. So the page is gone and the old title is a
  cosmetic leftover. Describe it that way, with the comparison, and don't call it a working route.
- **ZP-4148:** not moved (Dharmesh: "DUPLICATE - do not action", close as duplicate of ZP-4138). Explain that
  /maintenance-portal/condition opens the same Condition Assessment page the user already has under Site Data, so no extra
  data shows, but the address is not blocked as the QA Review asks.
- **ZP-4529:** not moved. QA can't put a user at the real AI limit, so ask the developer plainly what the server sends when
  the limit is used up. The firewall message the fix added is not a limit/reset message.

## How to apply the earlier findings (`jira-comments/verify.json`, entries for your key: "evidence" and "ticket")
- Apply a finding when the notes (`results`, `results_30sep`) or the pixels support it. Before accepting a factual change,
  check it against the notes yourself. Checkers can be wrong too. Reject a finding the evidence contradicts, and say why.
- `missing_acs`: add the ones that matter as short lines under **Not covered** (create the section before *Test data* if it
  doesn't exist). Merge near-duplicates, and keep each line to one sentence in plain words. Don't list more than about 6.
- Items that passed on 30 Sep and weren't re-run today may be listed as one bullet, "Passed on 30 Sep (build
  {{index-DXSz_B6n.js}}), not re-run today: …". Do this only when the 30 Sep notes say so.
- Overstatements ("all", "every", "whole portal", "no 500 anywhere"): narrow them to what was checked.

## Style
- Plain words, short sentences, written for a developer. No internal jargon: say "user" (not "seat"), "for comparison"
  (not "control"), and "refused (QA's server answers with its standard error page; no data comes back)" the first time instead
  of "CDN error page". Don't name the tool, the daemon or any probe.
- Keep the structure: result line, build line(s), Jira line, then *Steps* (# numbered), *What happened* (* bullets),
  *Not covered* (optional), *Test data*, *Screenshots*.
- Jira wiki markup only: *bold*, {{monospace}}, "# " steps, "* " bullets, \{ \} \[ \] escapes. Don't create other markup:
  no +…+, -…- or ^…^ pairs, no bare { } [ ] |, and no ! except in the image lines.
- No passwords, tokens, email addresses or prices.
- Keep it reasonably short. A developer should take in the result in 10 seconds.
