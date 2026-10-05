# ZP-4461 — Thermal Anomaly Severity rollout checked on acme production (2 Oct 2026)

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-4461 … [Avani's 'Rollout complete — egpzprod (38 of 39 companies)'
note] … check this in acme producation".

Explicit production instruction; read-only. Every edit was cancelled; the issue's saved values were re-read from the API
afterwards and no app write call was made. Jira not changed (status stays In QA).

Page: https://claude.ai/artifact/Dxd7zetG3AqrMZLAqXC32z

## Result — rollout confirmed on acme prod (web V2.2, build index-C_XD_y1m.js)
- **Class definition** (acme override 0fb186e0 "Thermal Anomaly"): Severity is `calculated`, required, options Nominal /
  Intermediate / Serious / Critical, at position 5 — right after Delta T (position 4). Same field id as before (`95c3a45a`,
  key `severity`), so saved values stay attached. Formula by Problem Temp unit + Severity Criteria; Delta T ≤ 0 → none.
- **Old issues** on "SLD V3 Site" keep their saved Severity (both "Intermediate"); temperatures unchanged.
- **Edit form** (acme's class): Severity right after Delta T; saved value shown as a manual override with "Reset to
  auto-calculated value"; changing criteria leaves it until Reset; Reset → Serious (Ambient, 50 °F); Similar → Critical;
  95/90 °F → Nominal; 95 °C vs 90 °F → Delta T 62.78 °C → Critical; both °C → 5 °C → Intermediate; typing a value → override
  mark + Reset. Cancel → no change saved.

## Not checked / notes
- New-issue form with acme's own class: this seat sees every company's issue classes (`GET /api/issue_classes` = 485), so the
  Add Issue picker has many identical "Thermal Anomaly" entries; the first (company ea2d2c99) still has the old select Severity.
  A plain acme seat would see only acme's. Not run (no plain acme seat signed in; passwords are not typed by me).
- Add Issue's Issue Class list is empty until its refresh icon is pressed (also on QA).
- One old issue holds a second empty "Severity" entry under an id not in the class (older data, not this rollout).
- acme prod showed "Set up two-factor authentication" on every load; "Set up later" postpones it.

## Evidence
`docs/bug-evidence/2026-10-02-zp4461-acme-prod/` — 8 screenshots + `results.txt`.
