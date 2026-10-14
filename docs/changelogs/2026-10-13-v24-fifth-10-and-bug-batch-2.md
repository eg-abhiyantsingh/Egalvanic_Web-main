# Web v2.4: bug batch 2 + fifth set of 10 tickets (13 Oct 2026, evening)

Owner said: "yes. continue test next 10 ticket and create bugs … use simple words adding more screenshot more proper and detail
steps". I saved that rule to memory (`feedback_bug_tickets_simple_words_more_screenshots_detailed_steps.md`).

## Bug batch 2
- **How they were filed:** all from abhiyant.singh@, assigned to Avani, Web v2.4, sprint Z-26-10-S1, To Do, with a "Relates" link.
- **Re-verified:** every bug re-checked on QA `index-C14cwR0U.js` before filing.
- **Format:** each ticket has 3–6 screenshots and click-by-click steps.

| Key | Pri | Title | Shots | Relates |
|---|---|---|---|---|
| [ZP-4865](https://egalvanic.atlassian.net/browse/ZP-4865) | Medium | A user not assigned to a site can still change that site's SLD connections and assets | 5 | ZP-4849 |
| [ZP-4866](https://egalvanic.atlassian.net/browse/ZP-4866) | Medium | A Client Portal user can open the Arc Flash Readiness page by typing /arc-flash | 5 | ZP-4699 |
| [ZP-4867](https://egalvanic.atlassian.net/browse/ZP-4867) | Low | A Client Portal user can download the whole company user list (names and emails) from the forms API | 3 | ZP-4709 |
| [ZP-4868](https://egalvanic.atlassian.net/browse/ZP-4868) | Low | Form multi-pick: ticking one search match clears the search box and jumps back to the full list | 4 | ZP-4753 |
| [ZP-4869](https://egalvanic.atlassian.net/browse/ZP-4869) | Low | SLD PDF export: the TO tag on a left-side terminal is drawn over the asset symbol | 6 | ZP-4800 |
| [ZP-4870](https://egalvanic.atlassian.net/browse/ZP-4870) | Low | EG form builder: the Web View preview shows dataset fields empty | 4 | ZP-4751 |
| [ZP-4871](https://egalvanic.atlassian.net/browse/ZP-4871) | Low | Work order Forms tab: forms with no key are listed under another form's name | 3 | ZP-4751 |

- **Test data:** for ZP-4865, the QA-DEMO connection and asset were changed and then restored.

## Fifth set of 10: SLD group posted (QA index-C14cwR0U.js)
| Ticket | Result | Jira |
|---|---|---|
| [ZP-4603](https://egalvanic.atlassian.net/browse/ZP-4603?focusedCommentId=45152) distinct SLD symbols + converter picker | PASS | READY TO RELEASE |
| [ZP-4513](https://egalvanic.atlassian.net/browse/ZP-4513?focusedCommentId=45153) Motor Overload class + MC symbol | PASS | READY TO RELEASE |
| [ZP-4619](https://egalvanic.atlassian.net/browse/ZP-4619?focusedCommentId=45154) layout ops on orphan children | PASS | READY TO RELEASE |
| [ZP-4514](https://egalvanic.atlassian.net/browse/ZP-4514?focusedCommentId=45155) cable drop node buses | PASS | READY TO RELEASE |
| [ZP-4621](https://egalvanic.atlassian.net/browse/ZP-4621?focusedCommentId=45156) batch moves + collapse queued | FAIL: queued collapse 400 / dead letter | To Do |

New bugs (Avani, Web v2.4, To Do):
- [ZP-4876](https://egalvanic.atlassian.net/browse/ZP-4876) Medium: re-importing an unchanged SKM export changes asset classes. 4 shots; relates ZP-4603.
- [ZP-4877](https://egalvanic.atlassian.net/browse/ZP-4877) Low: the server accepts self-loop and AC↔DC cables the app blocks. 3 shots; relates ZP-4514.

Addendum on [ZP-4865](https://egalvanic.atlassian.net/browse/ZP-4865?focusedCommentId=45157): an unassigned EE can also move assets through `batch-positions` (3 shots).

## Fifth set of 10: assets/records group posted (QA index-C14cwR0U.js)
| Ticket | Result | Jira |
|---|---|---|
| [ZP-4671](https://egalvanic.atlassian.net/browse/ZP-4671?focusedCommentId=45164) QR Labels paging | PASS (8,481-asset site opens with one 50-row page) | READY TO RELEASE |
| [ZP-4580](https://egalvanic.atlassian.net/browse/ZP-4580?focusedCommentId=45165) Extract from AI on Schedule/Engineering | FAIL: Schedule tab not refreshed after Apply | To Do, Eric → Avani |
| [ZP-4710](https://egalvanic.atlassian.net/browse/ZP-4710?focusedCommentId=45166) readings / thermal / direct IR | FAIL (High): users who can't open the asset can read and edit its readings | To Do, Eric → Avani |
| [ZP-4560](https://egalvanic.atlassian.net/browse/ZP-4560?focusedCommentId=45167) EG form record-sink key lock | partial; torque seed rows open | stays In QA |
| [ZP-4593](https://egalvanic.atlassian.net/browse/ZP-4593?focusedCommentId=45168) DC/solar + AC/DC rule | partial; node update accepts any voltage_id | stays In QA |

New bug:
- [ZP-4882](https://egalvanic.atlassian.net/browse/ZP-4882) (Low, Avani): SLD Issues panel calls Inverter/Rectifier a "Transformer without secondary voltage". 3 shots; relates ZP-4593.

Not filed (needs a second look): `/edge/preview-rules` answers FM/EE for an unassigned SLD.

- **SLD:** ZP-4603, 4619, 4621, 4513, 4514.
- **Assets / records:** ZP-4580, 4593, 4560, 4710, 4671. ZP-4671 moved Ready for QA → In QA.

## Owner's video bug + reassignments (13 Oct ~18:45 IST)
- [ZP-4880](https://egalvanic.atlassian.net/browse/ZP-4880) (Medium, Avani, To Do):
  - **Bug:** Asset › Maintenance › Add Service History with a future "Last serviced" date shows the raw "API call failed: 400 - {…}".
  - **Attachments:** 4 screenshots from the owner's video plus the video itself.
- **Owner rule:** reopened Eric tickets go to Avani. Moved Eric → Avani: ZP-4801, 4594, 4535, 4561, 4564, 4621 (all To Do).
