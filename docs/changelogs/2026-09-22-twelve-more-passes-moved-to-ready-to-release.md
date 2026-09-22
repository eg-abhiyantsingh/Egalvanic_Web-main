# Twelve more verified passes moved to READY TO RELEASE, with steps and screenshots (2026-09-22)

**Prompt:** "if ticket are passed that move then ready to qa to ready to release with comment steps and
screenshot if possible"

Artifact (v3, same URL): <https://claude.ai/artifact/1co6Lj66t8uzZbCNqAaEcb>
Evidence: `docs/bug-evidence/2026-09-22-ready-for-qa-deep/` — now 18 captures

## Screenshots now live on the tickets themselves

The Jira MCP has no attachment call, so the files were attached by driving the Jira web UI and uploading
straight into each issue's attachment input. **Fourteen screenshots are now attached to ten tickets**, so
a developer sees the evidence without leaving Jira.

Four screenshots were captured fresh for this round, because those tickets had text assertions but no
picture: the dashboard (ZP-4038), Condition Assessment (ZP-4084 and ZP-4113), Maintenance Program
(ZP-4044) and the PM Standards actions column (ZP-4152).

## Moved — 12 today, 17 in total this day

| Ticket | What the comment records | Attached |
|---|---|---|
| ZP-3677 | Service field holds two services as chips at once | 1 |
| ZP-4038 | Single-site dashboard, Open Issues by Type card, Maintenance nav | 1 |
| ZP-4044 | Server-side asset search, horizon sub-nav, tidied header | 1 |
| ZP-4066 | 19 real SLD issues, none of the false upstream type | 1 |
| ZP-4084 | One condition vocabulary; the word Serviceability is gone | 1 |
| ZP-4113 | Issue Report card plus both exports, scoped both ways | 1 |
| ZP-4131 | Settings Verifier directly after Export Engineering XML | 1 |
| ZP-4152 | Only company-owned standards expose the default action | 1 |
| ZP-4159 | Access hole closed, proved with a positive control | 2 |
| ZP-4170 | Six seeded transformer makers, CEB in the bus picker | — |
| ZP-4174 | Bus Duct shows System Voltage only; Panelboard keeps all | 2 |
| ZP-4186 | Extract returns 200 with a readable no-data message | 1 |

Plus the five moved earlier today: ZP-3990, ZP-4059, ZP-4111, ZP-4165, ZP-4212.

Every comment gives the build, numbered steps, what happens against what was expected, and — where the
ticket asks for more than was exercised — an explicit **"Not exercised"** line so the record is not
overstated. Only status and comments were touched; fix version, assignee, priority and labels are
unchanged.

## Thirteen passes deliberately held back

Each behaves correctly where it was checked, but the ticket asks for more than this pass covered:

| Ticket | Still unverified |
|---|---|
| ZP-4039 | Condition answers surviving a bulk COM calculator apply |
| ZP-4041 | The stated-history service picker |
| ZP-4043 | The portal read-only half, unreachable without a Portal Sales seat |
| ZP-4045 | Refusing a future service date, pre-acknowledging impossible deviations |
| ZP-4060 | The split bars carrying real numbers; this site has zero deviations |
| ZP-4068 | Continuous interpretation during a journal walk |
| ZP-4109 | Editing several methods in one applied version; this service has one |
| ZP-4110 | A per-device override rule firing; acme has no device rules |
| ZP-4112 | Re-dating an assessment without a full recalculation |
| ZP-4149 | The fork-typed config case, which acme has no example of |
| ZP-4167 | Verbatim trip-setting text, GF I2t and relay settings |
| ZP-4171 | AIC Rating on the switch library type |
| ZP-4266 | Nothing on QA — the failure is on production, and a QA pass does not clear it |

## Coverage notes added to two already-moved tickets

ZP-4038 and ZP-4044 each carry long QA Review lists. After moving them a second comment was added to each
naming exactly which bullets were **not** exercised, so nobody reads the transition as broader than the
evidence. ZP-4159 and ZP-4186 carry the same kind of caveat inside their main comment.

## Still open, unchanged

ZP-4042 (report history 500, day four), ZP-4218 (subcontracted flag never lands), ZP-4272 (column
arrangement lost), ZP-4138 (nav tile leaks), plus the unfiled Asset Classes regression. No new bug was
created; three findings still need one and each needs the owner's go-ahead.
