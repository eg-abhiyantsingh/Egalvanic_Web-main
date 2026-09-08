# ZP-3941 — per-section pricing for multi-section gear — captures (QA V1.36, 2026-09-08)

## `unit_attributes_available` is on procedure detail, and it gates by class
`GET /api/procedures-v2/procedures/{id}` now returns `unit_attributes_available` alongside `methods`, `rules`, `node_class_id/name/key`, and `node_class_has_schedule`. Observed values across every MCC / Switchboard / Motor Controller / Panelboard procedure on the tenant (39 procedures examined out of 215):
| class | `unit_attributes_available` |
|---|---|
| **MCC** | `[{"key":"sections","name":"Sections"}]` |
| **Switchboard** | `[{"key":"sections","name":"Sections"}]` |
| Motor Controller | `[]` |
| Panelboard | `[]` |
| ATS, Battery, Busduct, Busway, Cable, Capacitor Bank, Circuit Breaker, Disconnect Switch, Fuse, Generator, Junction Box, Load, Loadcenter, Meter, Motor, Motor Starter, Other, PDU, Rectifier, Relay, Series Capacitor | `[]` |
So the payload tells the client exactly which classes may offer a per-unit axis, and it is empty for every class without a count-like numeric attribute — the negative case (control hidden, not shown empty) is satisfied at the contract level.

## Migration sections_a3 — MCC methods carry `unit_attribute: "sections"`
Counting methods by class across all services:
| class | methods with `unit_attribute` | without | values |
|---|---|---|---|
| **MCC** | **15** | 0 | `sections` |
| **Switchboard** | **25** | 2 | `sections` |
| Motor Controller | 0 | 11 | — |
| Panelboard | 0 | 13 | — |
Every MCC method examined carries it, across services that predate this work (Arc Flash Data Collection, Arc Flash Label Placement, Cleaning, Clean/Tighten/Torque, Condition Assessment, De-Energized Visual Inspection, Infrared Thermography, Insulation Resistance Testing, NETA Testing) — consistent with sections_a3 having run and being applied service-wide. Switchboard retains its earlier sections_a1 flip (25 of 27 methods); the two without a unit attribute are noted rather than explained.
Labor samples (per-section minutes): MCC Arc Flash Data Collection → `[{est_mins:10, Journeyman Electrician},{est_mins:10, Electrical Engineer}]`; MCC Arc Flash Label Placement → `[{est_mins:5}]`; MCC Cleaning → `[{est_mins:24}]`; the matching Switchboard methods carry identical figures with the same axis.

## `node_class_has_schedule` (used by the sibling schedule-rows work)
Panelboard **true**, Switchboard **true**, MCC false, Motor Controller false.

## Not covered
The Pricing control itself (Per asset / Per section radio, the "minutes per section" relabel, saving and reopening) was not driven in the procedure editor UI; the axis was verified through the payload the control reads. Pricing a real job against an MCC with `sections = 12` and against an MCC with the attribute blank was not run — no MCC asset with a populated section count was located on the tenant, so the ×12 multiplication and the 1× fallback are unverified end to end.
