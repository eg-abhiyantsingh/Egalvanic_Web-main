# 2026-07-20 — Service Work Order (Services / Procedures V2) — Video Analysis + Live QA Verification

**Prompt:** "check all this video related to work order new implementation" (4 Loom videos in
`testcase/service_workorder_video/`) + "run in local and check in web to understand more depth".

**Method:** ffmpeg frame extraction (266 frames @ 8s) analyzed by an 11-agent parallel workflow,
whisper-cpp transcription of all narration (~35 min), then live hands-on verification against
`https://acme.qa.egalvanic.ai` with Playwright (Admin role, site "test site for api check").

**Feature epic:** Jira **ZP-3000 "SERVICES"** (reporter Eric Ehlert). The videos are the dev's
walkthrough deck + demos, recorded Jun 27 2026 on a localhost:3005 dev build.
**As of 2026-07-20 the feature IS deployed on QA** (badge still says V1.34).

---

## 1. What the videos say (summary per video)

| Video | Length | Content |
|---|---|---|
| `…530951422` | 11:20 | 13-slide engineering deck: the Services / Procedures V2 / Methods / Rules data model |
| `…531993510` | 5:24 | Live demo: **Services Explorer** admin page (`/services`), real procedure/method/rule examples |
| `…532120223` | 2:00 | Live demo: **creating a WO from a service** (Work Type + scope modifier + rule instantiation) |
| `…532512893` | 16:41 | Live demo: **all 6 service-type WO experiences** on web + iPad (IR, COM, Checklist, Schedule, PM Forms, AF) |

### The data model (video 1 — deck)
- **Service** = what the contractor sells (Infrared Thermography, NETA Testing, Clean-Tighten-Torque…).
  Table `services`: type family + `de_energized` flag. Global by default, companies can fork.
- **Procedure V2** = one slot per **(company_id, node_class_id, service_id)** — hard unique key
  (partial unique index `WHERE is_deleted = false`). No more per-variant procedure forks.
- **Methods** (`implementation_methods`) = concrete tests owned by the procedure; each carries
  **labor type + est. minutes** and **0..N EG-form mappings** (shared-scope forms = once per WO).
- **Rules** (`default_implementation` JSONB on the procedure) = "the brain": ordered
  `{when, methods}` list, **first match wins**; `methods: []` = exclude asset; `when: {}` = catch-all.
  Match keys: `node_subtype`, `com`, `pole_count`, `system_voltage`; operators gt/gte/lt/lte/eq;
  AND across keys, OR within lists. Resolved **per asset at WO-creation time**
  (`work_order_materializer.py`).
- **PM Plans** become just *service + cadence* prescriptions; the rules resolve the concrete work.
- localdev scale: 11 services, 212 procedures, 390 methods, 369 method-form links.

### Service type drives the whole WO UX (video 4)
Six types: **AF, IR, COM, Checklist, Schedule, PM Forms.** Each defines the WO detail tabs, the
per-asset columns, the click action, the completion formula, and the mobile flow. Tasks module is
**no longer used** except by Checklist type. Right-click/long-press keeps View Full Asset / Add
Form / Add Issue. AF WOs embed the SLD + a new Equipment Designations tab (library vs direct-entry
now both allowed for AF readiness).

---

## 2. Verified live on QA (2026-07-20)

- `/services` **Services — Explorer** exists in the Admin sidebar. Now **13 services**
  (videos had 11; new: *Shutdown (Composite)* — 0 classes, *UPS Maintenance*).
  Drill-down verified: Clean-Tighten-Torque → ATS → Standard (60 min) vs Enhanced (90 min),
  rule "Criticality (COM) is 2, 3 → Enhanced", forms incl. shared **Torque Record**. Read-only.
- **Create WO dialog** (`/sessions`): `WO Name/# *`, `Facility *`, **`Work Type *`** (13 services
  + **General**), Due Date, **Scope** builder (default "All assets in scope", filters = classes /
  locations / power schemes, **Start Empty Instead**, live "*N matching assets*" preview + View
  list), **Team** rows (user + role: Certifier / Field Technician / +), collapsed **Advanced
  Settings** (Priority, Est. Hours, WO Description, Photo Type, Start Date). Create disabled until
  required fields set. De-energized services (CTT) additionally surface an **Auto-Schedule** button.
- Created 3 live WOs (left on the site as fixtures):
  - `QA_ServiceCheck_IR_2026-07-20` (IR) → `/sessions/8793f5a5-6b0d-4a2e-a622-95900efbb934`
  - `QA_ServiceCheck_COM_2026-07-20` (COM) → `/sessions/22534775-ec80-4618-a65d-7551ddf3fadd`
  - `QA_ServiceCheck_CTT_2026-07-20` (PM Forms) → `/sessions/59bfeb65-655e-4810-8bcd-febb81ac7dac`
- Type-specific detail pages verified:
  - **IR**: 0% ring; tabs Assets/Issues/**IR Photos**/Attachments/**More**(→Tasks, Forms);
    per-asset IR Photos column; Locations rail (All / No Location); Actions = Enable Multi-Select /
    Upload IR Photos / Add Asset / Add Issues / Generate Report; Quick Count; Close Work Order.
  - **COM**: tabs Assets/**Condition Assessment** (dashboard)/Issues/Attachments/More;
    per-asset **Tasks + C.O.M.** columns.
  - **PM Forms (CTT)**: tabs Assets/**Forms (47)**/Issues/Attachments/More; per-asset Forms counts
    (CB=4, Fuse=3, Panelboard=4, Transformer=3 — rules resolving per asset class/attrs).
  - Rule-based scope differs by service: IR matched 11 assets, CTT matched 13/14 (pulls in CBs,
    fuses, relay; IR excludes child assets).
- **New API surface** (all observed 200/201):
  - `GET /api/procedures-v2/services`
  - `GET /api/procedures-v2/procedures?service_id={uuid}`
  - `GET /api/procedures-v2/procedures/{uuid}`
  - `POST /api/ir_session/scope-preview`
  - `POST /api/ir_session/create`  ← service WOs (note: `ir_session` for ALL types)
  - `GET /api/ir_session/{id}/full` / `/assets` / `/team`
  - `GET /api/eg-form-instance/by-session/{id}/count` and `/node-status`
  - `GET /api/sld/{sld_id}/scope-options`

## 3. Bugs found on QA (verified, falsifiable)

1. **Asset-count mismatch — grid silently drops the "Main-…" Switch node.**
   IR WO: API `/assets` returns **11** nodes (incl. `Main-VFD-MAINS-1783088823020 [Switch]`),
   Assets tab badge = 11, Locations rail = "11 assets", but the DataGrid renders **"1–10 of 10"**
   — the Switch row is missing. Reproduced identically on the COM WO. On the CTT WO: API = **14**,
   badge = **14**, grid = **"1–13 of 13"** (same node dropped).
2. **Scope preview ≠ created WO contents (CTT).** Preview said "**13** matching assets"; the
   created session contains **14** (the mains Switch was added at create time but not shown in
   preview). Either the preview under-reports or create over-includes — surfaces disagree.
3. Watch-items from the dev-build videos (not yet reproduced on QA; verify before filing):
   COM progress shown as 20% / 23% / 39% on three surfaces for the same WO; saving an **empty**
   panel schedule flips the asset to Schedule-done; Equipment Designations KPI denominator jumps
   138→14 without visible filter; PM-FORMS header dial 3%→2% regression after submit;
   "Suggested PM Plan (Optional)" marked with a required asterisk; per-asset Forms badge=1 while
   the drawer shows 4 forms.

## 4. Test-suite impact

- **`WorkOrderPage.selectFirstWorkType()` selects "Arc Flash Data Collection"** (first option) —
  since the v1.35 fix, TC_CWO_003 (WorkOrderTestNG:470) and WorkOrderCreateTestNG:107 have been
  creating **AF-type service WOs** with full rule-driven asset instantiation, not neutral WOs.
  → switch these to `selectWorkType("General")` and assert the created WO is type-General.
- Detail-page assertions (TC_WOD_002 Tasks section, IR Photos, Locations) now depend on the
  **type of the WO row clicked** — the list mixes General, planned (EMP) and service WOs.
- Whole new surface needing dedicated suites: Services Explorer (read-only render of
  services/procedures/methods/rules), Create-WO scope builder (filters, OR groups, Start Empty,
  preview count vs created count), per-type detail contracts (tabs/columns/actions per the 6
  types), completion math per type, shared-form (Torque Record) semantics, More→Tasks/Forms,
  right-click menu, Close Work Order, Auto-Schedule for de-energized services, and the
  `procedures-v2` / `ir_session` / `eg-form-instance` API contracts (pagination/auth/validation).
- The presenter's own words: "QA wise, there's a lot that we need to go through because this is
  refactoring a significant portion of the application… we're definitely going to need to QA this
  thing into the ground."

## 5. Artifacts

- Transcripts: whisper-cpp small model — session scratchpad `audio/v[1-4].txt`.
- Frame analysis: 11-agent workflow `wo-video-frame-analysis` (815k tokens, 277 tool uses).
- Live fixtures: the three `QA_ServiceCheck_*_2026-07-20` WOs listed above.
