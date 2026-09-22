# Ready for QA — acceptance checklist (48 tickets)

Source: Jira JQL `fixVersion = 14156 AND status = "Ready for QA" ORDER BY key ASC` (48 issues, pulled 2026-09-22).
Every check below is drawn **only** from the ticket's own description text (QA Review / Acceptance criteria / Steps to Reproduce / Expected Result). Nothing is invented.
Quoted strings in backticks are what the tester must match literally on screen.

---

### ZP-3675 — Web : In Panel Schedule Assets Are Missing from Exported PDF.
- **Surface:** a Panel Schedule containing multiple assets → the `Export PDF` button on it.
- **Check:**
  1. Open a Panel Schedule containing multiple assets; note every asset row shown on screen.
  2. Click `Export PDF`, download and open the generated PDF.
  3. Compare asset-for-asset: every asset displayed in the Panel Schedule is present in the PDF, with no missing records.
- **Endpoints:** none named.

---

### ZP-3677 — Web : In Add Assets Service Field Does Not Allow Multiple Service Selections.
- **Surface:** a `5-Year Shutdown` schedule → `Add Assets` dialog → the `Service` dropdown.
- **Check:**
  1. Open a `5-Year Shutdown` schedule and click `Add Assets`.
  2. Open the `Service` dropdown and select `Clean, Tighten, Torque`.
  3. Select a second service (`Infrared Thermography` or `NETA Testing`) — it must be selectable, not replace the first.
  4. Confirm all four of `Clean, Tighten, Torque` / `Infrared Thermography` / `NETA Testing` / `De-Energized Visual Inspection` can be held at once.
  5. Confirm the selected services are displayed together and saved correctly for the asset (re-open to verify).
- **Endpoints:** none named.

---

### ZP-4030 — [Web] Deep Linking Email Notifications - Backend & Web Routing
- **Surface:** an email-notification link → the web app's work-order route; plus the two static well-known files.
- **Check:**
  1. Click an email notification link: the web app opens with the **correct site auto-selected** and the specific work order displayed (no manual site/WO hunting).
  2. Confirm the deep link URL carries site ID, work order ID and entity type as URL parameters or path segments.
  3. Click the link while logged out: you are redirected to login and, after login, land on the preserved deep-link destination.
  4. Open a link with an invalid work order ID → a clear user-facing error message (not a blank page).
  5. Open a link to a work order the user has no access to → permission handling / clear message.
  6. Fetch `apple-app-site-association` and `assetlinks.json` and confirm both are served correctly.
- **Endpoints:** `apple-app-site-association`, `assetlinks.json`.
- **Negative control:** invalid/expired/inaccessible links must show a clear message rather than opening the wrong work order.

---

### ZP-4038 — Site Overview single-site redesign: Open Issues by Type card + Maintenance nav restructure
- **Surface:** `/dashboard` (Site Overview); left nav `Maintenance` section; `/maintenance/*`; `Engineering > Dashboards`; `Site Data > Dashboards`.
- **Check:**
  1. Open `/dashboard`: it opens on your last-picked site with a real sites-only picker (**no** `All Facilities`, no gray placeholder). Pick a site → go to Assets/Engineering → return: the pick survives.
  2. The issues card reads `Open Issues by Type` (not the `Open Issues by Site` donut), styled like `Assets by Type`; clicking a row lands on the Issues list filtered to that type.
  3. The `Open Issues by Type` total matches the site's open-issues count, and a `Resolved`/`Closed` issue on that site is NOT counted as open.
  4. As a Project/Facility Manager, `Maintenance Program` loads (not `Access Denied`). Section order: Dashboards → `Condition Assessment`; Program → `Maintenance Program` + `Program Compliance`; Reporting → `Reports`.
  5. `Arc Flash Readiness` appears under `Engineering > Dashboards` for Electrical Engineers; a seat with the permission but not the owning role still sees it as a tile under `Site Data > Dashboards`.
- **Endpoints:** `get_site_overview` / site-overview `issues_breakdown`; `/features/access`.
- **Negative control:** a stored `all` scope falls back to the first site on Site Overview; the Sales/Ops dashboards must STILL show the original `Open Issues by Site` widget.

---

### ZP-4039 — Preserve condition-evidence answers through bulk COM calculator applies + condition-assessment workflow tools
- **Surface:** COM calculator dialog on an asset; `Condition Assessment` tab (search box, shutdown-rule filter, `Not declared` / `Not assessed` / `No calculator` chips, Findings tab edit pencil); Maintenance plan editor `Stated history` bulk action.
- **Check:**
  1. On an asset with an open urgent issue (sweep set Condition 3), open the COM calculator: system evidence answers show as checked-and-locked rows with an `Evidence` chip; the chip tooltip explains the open / urgent / overdue counts.
  2. Complete a clean human checklist and Apply: the asset stays Condition 3 (no dip), the response/UI shows `evidence_preserved` > 0, `system_answers`/`system_evidence` are retained, and `maintenance_value` equals the union of human + system answers.
  3. Enter a human non-serviceable verdict → it still wins outright.
  4. Resolve the open issue via the Findings-tab edit pencil (issue drawer) → its evidence is withdrawn on the next calculator view.
  5. Condition Assessment tab: use the search box, shutdown-rule filter (incl. `Not declared`) and `Not assessed` / `No calculator` chips to isolate a gap, then bulk-fix it.
  6. Maintenance plan editor: `Stated history` sets one service + one date across a multi-asset selection; skipped-ineligible assets are named in the toast. Beamer's floating `Z` launcher is hidden globally while the custom trigger tiles still work.
- **Endpoints:** `POST /asset-maintenance/condition`, `/asset-maintenance/history`.
- **Negative control:** an asset with NO system answers applies with unchanged behaviour (single bulk update, no `Evidence` chip); Maintenance Program → asset → back restores preset, axis, grouping, filters and page.

---

### ZP-4040 — Work orders with a NULL service wo_view silently rendered as General - fall back to the service type's UI
- **Surface:** work-order detail for services whose `wo_view` is NULL — the `Infrared Thermography` service (type `IR Checklist`) and 3 `PM Forms` services, on the demo tenant.
- **Check:**
  1. Open the `Infrared Thermography` work order (type `IR Checklist`, `wo_view` NULL): the per-asset check-off column appears instead of the `General` view.
  2. Open a work order for each of the 3 `PM Forms` services that had NULL `wo_view`: each renders its type's tabs/columns.
  3. A service that DOES have its own `wo_view` is unchanged — its own contract still wins over the type default.
  4. Method-line readiness masks and completion `svc_meta` agree with the tabs shown (scorer gate and evidence lookup use the same fallback).
- **Endpoints:** none named in the description beyond the internal readers `get_service_registry` / `svc_meta`.
- **Negative control:** a service of an unknown/unmapped type still resolves to NO extra UI (renders `General`) rather than inventing tabs/columns.

---

### ZP-4041 — Maintenance surfaces (report spinner, condition dots, stated-history service picker) showed less than they knew
- **Surface:** `Maintenance Reports` page (report cards + work-order row generate buttons); `Condition Assessment` rows (Crit/Env/Maint dots + Condition chip); the stated-history dialog's service picker.
- **Check:**
  1. Generate one report: ONLY that card shows the spinner + a `Generating` chip + a primary border; other live cards become un-clickable/dimmed with no spinner. Repeat on a work-order row's generate button.
  2. Each Condition Assessment row's Crit/Env/Maint dot colour matches its calculator answer (1 green, 2 amber, 3 red, non-serviceable deep red) and agrees with the Condition chip; hover shows the rating tooltip.
  3. On the demo tenant (site with nothing scheduled/performed), open the stated-history service picker: the full company service catalog loads, with a loading state first and an honest empty message only when truly empty.
- **Endpoints:** `fetchServicesV2` (company service catalog).
- **Negative control:** generating a report must not spin unrelated cards; the picker must no longer read `No options` on sites that have services in the catalog.

---

### ZP-4042 — Maintenance Portal: customer-facing read-only view, license gating, and recorded report history
- **Surface:** `/maintenance-portal/*` rail section (Site Health, Maintenance Program, Program Compliance, Reports); `Site Data` > new `Maintenance` subgroup; the license picker under the site selector.
- **Check:**
  1. As T1 staff: the `Maintenance Portal` rail section appears below Builder with those four entries; `Site Data` has a `Maintenance` subgroup, `Condition Assessment` is back, `Site Health` is gone from Site Data.
  2. Switch the license picker `Free` / `read_only` / `interactive`: Free shows only Site Health + Reports with Reports capped to the single latest document; `read_only` shows the whole portal with nothing editable; `interactive` unlocks it. Locked items render **locked, not hidden**.
  3. Open the portal panel by clicking the rail tile before navigating: the `License` picker is present in the open panel, and also on a direct `/maintenance-portal` URL.
  4. Replay a mutating request (PUT/PATCH/DELETE) carrying `X-EG-Portal` → the API refuses it (not merely a hidden button).
  5. Portal Site Health loads: `POST /v2/issues/list` (a read declaring `issues.view`) returns JSON, not HTML / `Unexpected token <`.
  6. Generate a report → a `report_generations` row is written `pending` then finalized storing the S3 key; the portal's Reports view serves the existing PDF via `GET /reporting/history` and `/reporting/history/<id>/download` without re-running the pipeline.
- **Endpoints:** `GET /reporting/history?sld_id=`, `GET /reporting/history/<id>/download`, `POST /v2/issues/list`; header `X-EG-Portal`; migration `rptgen_a1`.
- **Negative control:** a report id from another tenant is refused; an open work order is never offered as a downloadable report; `Arc Flash Readiness` is absent in the portal but present for staff; an interactive T2 seat still writes normally (client omits the header); the same mutation from OUTSIDE the portal still works.

---

### ZP-4043 — Condition Assessment: expired-assessment health bucket + deeper Maintenance Portal read-only
- **Surface:** equipment health bar + `Expired` filter chip on Condition Assessment; Maintenance Portal's Maintenance Program / Compliance / Condition Assessment; the portal license picker.
- **Check:**
  1. An asset assessed (`com_ready`) more than a year ago, or with a null `com_calculation_modified_at`, falls in the new `Expired` band, not `Healthy`. Boundary: 364 days old is NOT expired, 366 days old IS expired.
  2. An asset genuinely at risk stays at risk even when its assessment is over a year old.
  3. The `Expired` filter chip isolates the re-assessment worklist and every listed row carries `assessment_expired = true`.
  4. In the portal, Maintenance Program no longer shows `Record completion` / `Defer`; Compliance no longer shows `Acknowledge` / `Add to program` / per-row defer-record-withdraw / acknowledgment edit-delete — while score, deviations and the acknowledgment record stay fully readable.
  5. Condition Assessment inside the portal has no header actions, no findings edit pencil, no calculator on the condition cell, no bulk ops, no row drill-in.
  6. The portal license picker labels `read_only` as `Premium` and does NOT offer `Interactive`.
- **Endpoints:** none named (fields `com_ready`, `com_calculation_modified_at`, `assessment_expired`).
- **Negative control:** the working (Site Data) copies of Maintenance Program, Compliance and Condition Assessment still have ALL their edit controls.

---

### ZP-4044 — Maintenance Program: server-side asset search (?q=), horizon sub-nav, Site Health header cleanup
- **Surface:** `Maintenance Program` grid search box (working copy + read-only portal copy); sidebar sub-nav under `Maintenance Program`; `Site Health` header; condition dashboard.
- **Check:**
  1. On a multi-page site, type an asset label into the search box and confirm it finds an asset NOT on the currently loaded page (server-side). Class-name search works — `transformer` returns the transformers.
  2. An underscore in an asset tag is matched literally: searching `T_R1` does not also match `TXR1`.
  3. The search box works on BOTH the working Maintenance Program surface and the read-only portal copy.
  4. The horizon (`History` / `2026` / `3 year` / `5 year` / `All`) appears as sidebar sub-nav under `Maintenance Program`; each entry deep-links the correct preset; a pasted URL carrying `?preset=` selects the right horizon and highlights the sidebar entry.
  5. The `Site Health` header is a single-line provenance bar that still shows the condition index.
  6. The condition dashboard no longer shows a duplicate `EMP Compliance` status card; the eleven NFPA 70B requirements read from the Compliance area only.
- **Endpoints:** `/asset-maintenance/program?q=<text>`, `?preset=`.

---

### ZP-4045 — Compliance: real coverage bars, refuse future service dates, pre-acknowledge impossible de-energized deviations
- **Surface:** stated history (single + bulk), the program apply, compliance `Performed on` picker, asset dialog `Last performed` picker, `Next due` picker, Compliance coverage bars, left nav panel.
- **Check:**
  1. Record a completion with a **future** last-serviced date through each entry point (single stated history, bulk stated history, the apply's `last_completed`, per-service `service_dates`) — every one is refused by the backend.
  2. The date pickers (bulk stated history, compliance `Performed on`, asset dialog `Last performed`) do not offer a future date; a `Next due` picker still allows future dates.
  3. On the demo site, a previously-future schedule (e.g. a CTT dated `2028`) no longer paints a `Completed` marker in a future quarter and its next due is recomputed sanely.
  4. Coverage bars: `by_service` and `by_class` show a real covered segment instead of a solid red `62 open / 62`, and `sum(by_service.total)` reconciles to expectations.
  5. De-energized procedures prescribed for an asset that can never be shut down arrive **pre-acknowledged**, attributed to `Shutdown rule`, with the reason stated.
  6. The left nav panel is wide enough that `Maintenance Program` no longer truncates (240px → 264px).
- **Endpoints:** compliance `by_service` / `by_class`; program apply `last_completed`, `service_dates`.
- **Negative control:** a real human acknowledgment on the same pair still wins over the automatic one, and a human can still withdraw the automatic one.

---

### ZP-4060 — Compliance coverage bars: split into in-program vs acknowledged
- **Surface:** `Program Compliance` tab → `Coverage by service` and `Coverage by class` breakdown bars.
- **Check:**
  1. Each bar draws three distinct segments — in-program (dark blue `#1e63c4`), acknowledged (light blue `#b6cdeb`), needs attention (error red) — matching the split on the top score bar.
  2. Hover each segment: the tooltip count is correct and the legend labels both blues.
  3. A service carried entirely by acknowledgements shows a light-blue `acknowledged` segment (it previously read as fully covered).
  4. Segment widths sum to 100%; `in_program` = total − open − acknowledged.
- **Endpoints:** by-service / by-class breakdowns gain an `acknowledged` counter alongside `total` and `open`.
- **Negative control:** a service fully in-program shows NO light-blue segment.

---

### ZP-4061 — Maintenance Portal: read-only Site Data + Work Orders, license tiers, portal CORS & assessment-timestamp fixes
- **Surface:** `/maintenance-portal/*` → new Site Data subsection (`Assets` → `Locations` → `Panel Schedules` → `SLD`), `/maintenance-portal/sld`, Work Orders in the Maintenance section, the Work Order create dialog's `Facility` field.
- **Check:**
  1. Portal pages load with no `CORS Error: Failed to fetch`; the GET/POST follows the OPTIONS preflight with `X-EG-Portal` present in `Access-Control-Allow-Headers`.
  2. Site Data on `Read-Only`: `0/5` asset toolbar buttons, 0 row edit icons, no `Add Floor` — yet all rows still visible. On `Interactive`: `5/5` toolbar buttons, row edit icons, `Add Floor` present.
  3. Portal pages render with a header bar; Condition Assessment tabs are clickable; `/maintenance-portal/sld` loads; back/refresh/deep-link stay on `/maintenance-portal/*`.
  4. The COM calculator opens read-only showing the same answer as `Interactive` for the same asset — inputs locked, `Close` only.
  5. `Work Orders` appears in the Maintenance menu at every tier; below `Interactive` it renders locked (disabled, lock icon, no link) and is unreachable from nav; on `Interactive` it loads with `Create Work Order` present, the `Facility` field disabled and following the site picker (moving the picker refetches the list).
  6. Create a new asset with a COM calculation → it does NOT read `Assessment expired`. Copy an asset → it inherits the source's assessment age. Import a spreadsheet moving a COM factor → no `NameError`.
- **Endpoints:** header `X-EG-Portal` in CORS `allow_headers`; field `com_calculation_modified_at`.
- **Negative control:** existing pre-fix nodes with NULL `com_calculation_modified_at` STILL read expired (known gap, not fixed here); staff routes `/assets`, `/locations`, `/sld`, `/maintenance/*` render with correct titles and populated rows, and `/sld` keeps its lock toggle for staff.

---

### ZP-4062 — Bulk "Mark As..." on the location-grouped assets grid silently posted grid row ids instead of node ids
- **Surface:** location-grouped assets grid inside an IR session → `Bulk Ops` → `Mark As...` → `"{service} - done"`.
- **Check:**
  1. Select multiple assets on a location-grouped grid, run `Bulk Ops` → `Mark As...` → `"{service} - done"`: the selected line items are actually checked (the gesture previously did nothing).
  2. The `Mark As...` menu offers only services that have registrations for the selected nodes — not the full service-menu fallback.
  3. Per-line checkboxes still work.
  4. Seeded demo work orders now show completions after a successful bulk mark.
- **Endpoints:** `PUT /ir_session/<id>/line-checks` (previously 400 `invalid id` on composite `loc-<n>-node-<uuid>` row ids).
- **Negative control:** force a failing bulk call and confirm a **toast** now appears instead of a silent `console.error`. (Ticket notes the fix was not verified end-to-end — "worth one click on dev".)

---

### ZP-4066 — SLD Issues: false "upstream can't deliver" phase-config errors from an undirected topology walk
- **Surface:** the SLD Issues panel on an SLD with a 3P4W → 3P3W chain (e.g. `MDP 3P4W` → feeder breaker → cable → disconnect → `3P3W` load).
- **Check:**
  1. On that SLD, confirm NO `Upstream ... can't deliver ... downstream` phase-config issue is raised.
  2. Repeat with terminal-less edges between the same two panels — still no false issue.
  3. A transformer between a 3P3W and a 3P4W resets the chain (no false flag).
  4. Because the file ships in the OTA viewer bundle, the fix appears in the iOS SLD viewer only after the per-env OTA republish.
- **Endpoints:** none (frontend-only; backend `derive_constraining_topology` is dead code).
- **Negative control:** a genuine `3P3W` panel feeding a `3P4W` panel MUST still be reported as an incompatible phase configuration, both with terminals set and terminal-less; a `3P3W -> node-bus -> 3P4W` path is still flagged.

---

### ZP-4067 — AI pipeline: site-walk journal interpretation - per-location fold engine + journal_fold runner mode
- **Surface:** no web surface — an AI-pipeline change (`journal_engine.py`, `journal_local.py`, agent-runner mode `journal_fold`, Step Function `journal-interpretation.asl.json`). Verified from a shell/AWS console, not the browser.
- **Check:**
  1. Run `journal_local.py` against the `BI Building #3` seed walk (7 rooms / 17 entries / 44 real photos): output is `22 rows` and `38 units`, with the ATS appearing as the single multi-fed target.
  2. Confirm phantom boards are merged into one once implied assets must carry their name.
  3. Trigger `journal_fold` via the journal-interpretation Step Function in dev: one Fargate task runs and `states/` plus `preview.json` land in S3.
  4. Run `python .github/scripts/check_vendored.py` → reports OK (vendored copy matches root `journal_engine.py`).
- **Endpoints:** none (Step Function `journal-interpretation`).
- **Negative control:** the removed `read_entry` doc-extraction step and the journal prompts no longer execute in the pipeline.

---

### ZP-4068 — Site Walk Journal mode: voice/photo field capture, continuous interpretation, and review UI
- **Surface:** `New Site Walk` dialog (Count/Journal choice), the site-walk registry (`JOURNAL` chip), the Journal Review page (locations rail, field-order timeline, `Interpret` / `Re-interpret` / `Accept N`, network graph).
- **Check:**
  1. `New Site Walk`: the `Count` / `Journal` choice appears (default `Count`); a Journal walk shows a `JOURNAL` chip beside its name in the registry.
  2. In a Journal walk add entries per location (transcript, notes, photos in field order): photos hang off their entry, edit and delete work, and the whole-document PUT persists after reload.
  3. Trigger `Interpret`; the sweeper also auto-starts ~8s after edits go quiet; watch the `Read n of m` progress and the LIVE fold-by-fold preview render in place of proposals.
  4. `Accept N`: the walk becomes an ordinary count walk (proposed rows, assets, feed connections with evidence) with no journal-aware pricing.
  5. Network graph: locations colour-coded, feeds as arrows (structural/containment edges lighter), implied nodes hollow/dashed, `Show entry` scrolls and flashes the timeline entry.
- **Endpoints:** `POST /site-walk/<id>/interpret`, `GET .../interpret/status`, `POST .../proposals/accept`; migrations `swjrnl_a1..a5` on `rptgen_a1`, `alembic heads` = `swjrnl_a5` only.
- **Negative control:** `walk_type` cannot be changed after creation (immutable).

---

### ZP-4082 — [Backend] Plan-released work orders read "Missed" and re-offered Release after workorder ids became deterministic, creating duplicate sessions
- **Surface:** a v2 plan released before the deterministic-id change and later regenerated — e.g. the `kochinc` `ICT Campus` EMP plan, workorder `Infrared Thermography - Tower / 0 - August 2026`; plus `/planned-workorders`, the plan nav badge, the dashboard.
- **Check:**
  1. On that plan, a completed work order shows its correct execution status instead of `Missed`, and the plan no longer offers `Release` on it.
  2. `/planned-workorders`, the plan nav badge and the dashboard all reflect the recovered plan→session link.
  3. Re-releasing the affected work order no longer produces a duplicate session.
  4. Sessions still linked by exact id are unaffected.
- **Endpoints:** `/planned-workorders`; internal `execution_status_for_plan`. Release-name match format: `"<workorder label> - <plan title>"` within ±30/60-day window tolerance.
- **Negative control:** a custom-named session that matches no release name stays unmatched — it must NOT be mis-credited to a plan. (Note: the fix currently lives on cicd/prod only, absent from dev/qa.)

---

### ZP-4084 — Eaton-1: Retire nodes.serviceability - COM is the single condition vocabulary
- **Surface:** asset condition toggle (Condition section); Assets page `Condition` filter; the COM calculator dialog.
- **Check:**
  1. Pick `Non-Serviceable` on the asset condition toggle + add a note: the PUT sends `com=4` with **no** `serviceability` key. Re-tap the lit level → clears to not assessed.
  2. With calculator answers present, directly pick a level → a confirm dialog appears; `Cancel` keeps the old level and breakdown, `Confirm` sets the new level and drops `com_calculation`.
  3. The Assets page `Condition` filter offers `1` / `2` / `3` / `Non-serviceable` / `Not assessed`; the old separate COM and Serviceability filters are gone; the `Not rated (0)` option is gone.
  4. API: lookup with `com=unassessed` and `com=4` return results; `com=9` and `com=abc` return **400**. Bulk `POST /asset-maintenance/condition` accepts `{node_ids, com:1..4|null, com_calculation}`.
  5. On the dev deploy, the Run Database Migrations step shows `com4_a1`: existing `non_serviceable` rows become `4`, `com=0` rows become NULL, CHECK `nodes_com_range` added.
- **Endpoints:** `POST /asset-maintenance/condition`; migration `com4_a1` (parent `swjrnl_a5`).
- **Negative control:** an old iOS payload with `serviceability:'non_serviceable'` + `com:null` is stored as `com=4` via the shim; compliance coverage and EMP preview EXCLUDE `com==4` assets.

---

### ZP-4086 — Eaton-2: Condition-section attributes - per-attribute Listed/Required flags gate COM readiness
- **Surface:** the node-class editor (per-attribute `Condition section` = `Listed` + Required/Optional control); the asset form's Condition section (`Condition attributes` caption); readiness gauges / PM Readiness dashboard.
- **Check:**
  1. Class editor: flag an attribute `Condition section` = `Listed`; the Required/Optional control appears only when Listed, and unlisting clears Required. Reserved attributes allow only these two flags; globals are disabled.
  2. Open an asset of that class: the listed attribute renders under `Condition attributes` (after the shutdown constraint, before Condition notes) and leaves the Engineering grid unless also arc-flash required.
  3. Leave a listed + required attribute blank: the asset must NOT be COM-ready and the attribute name must appear in the `missing` list — including for a non-serviceable asset.
  4. Readiness gauges / PM Readiness dashboard show a `Condition attributes` bucket and per-asset column; non-serviceable assets no longer count the three calculator inputs.
- **Endpoints:** coverage bucket `condition_attrs`; flags `condition_section`, `condition_required`.
- **Negative control:** an attribute set Required but NOT Listed must NOT gate readiness (backend stores `condition_required=false` when `condition_section` is false).

---

### ZP-4109 — Eaton-3: Bulk method editor - edit N service methods in one applied version
- **Surface:** a custom v2 service detail page (e.g. the acme `Eaton PM + IR` service) → `Asset classes` section → `By class / All methods` toggle → flat methods grid → `Edit N selected` → the bulk edit dialog.
- **Check:**
  1. Switch to the `All methods` view: a flat grid of every implementation method with checkbox selection and a text filter. A **global** service shows only `Customize` — no toggle, no grid.
  2. Select 3 methods, choose `Edit 3 selected`, set a description + upsert a labor line + add test equipment, Apply: exactly ONE new service version is created, its note names the changed fields (e.g. `Bulk edit - 3 methods: description, labor, test equipment`), and only the 3 selected methods changed.
  3. Run a second bulk edit removing that test equipment: one more version, unselected methods untouched across both passes.
  4. Filter the grid and confirm the earlier selection is preserved.
  5. Stale: change/remove a targeted method underneath the dialog then Apply → a `stale` message listing the missing keys plus a reload. Unresolved: an unknown catalog/form name → an `unresolved` **400** listing the names.
- **Endpoints:** `POST /procedures-v2/services/<id>/methods/bulk-edit`.
- **Negative control:** a cross-tenant service returns **404**; bulk-edit against a global service returns **403**; a user without `procedures.manage` is refused.

---

### ZP-4110 — [Web] Service method rules could not key on the specific device, and per-device overrides had no admin home
- **Surface:** `Admin` → `Config` → `Devices` (server-paged grid + filter tray); the service/method editor's `Device specific overrides` dialog; the rules editor's device / frame-name / frame-amps pickers.
- **Check:**
  1. `Admin > Config > Devices`: the grid loads server-paged with the filter tray (`Manufacturer`, `Asset class`, `Service`, `Frame`, `Company/Global rules`) and a search box; a device with only global rules shows a lock.
  2. Add a device rule: service → asset class → device (library scoped by the chosen class), write e.g. `SQUARE D FA`, frame `FA`, amps `>= 60`, and save.
  3. Open `Device specific overrides` for a class row; edit ONE device's rules (focused save) and confirm every other device rule and all class rules are carried through untouched.
  4. A device rule sits first and REPLACES the class default: a matching 60/70/100 A `FA` picks the device procedure; below the frame-amps threshold it falls through to the class rules.
  5. Device / frame / amps chips render on the procedure detail; `DevicePicker` fetches nothing until a manufacturer is set or 2+ characters typed, and flags when the first 100 matches are truncated.
- **Endpoints:** `GET /procedures-v2/device-rules` (filtered, paged, faceted); equipment-catalog picker / quick-search / resolve.
- **Negative control:** a node with no SKM device binding, or a rule with no device, must NEVER match as a wildcard; site-walk pricing must NOT apply device rules.

---

### ZP-4112 — [Web] Compliance had no forward view of the standard's prescription, and re-dating an assessment forced a full recalculation
- **Surface:** `Compliance` > `Visualizer` tab; `Condition Assessment` > `Asset Details` (Condition/Assessment dropdowns); `Bulk Ops` > `Edit` > `Assessment date`; Program PDF export.
- **Check:**
  1. `Compliance > Visualizer` on a site with a chosen standard: each asset row shows service due-dots across `Year 1..Year 6` (annual every year, 36-month at Y3 and Y6, 60-month at Y5, `x2` when due twice), with pinned `Asset` and `Condition` (`C1`/`C2`/`C3`/`NS`) columns.
  2. Toggle `Group by` none / asset class / location; apply location, class, service and condition filters; enter full-screen and `Esc` out.
  3. Export PDF, Excel and CSV — each matches the on-screen view; a grouped PDF repeats column and group-band headers across pages with a `Page N of M` footer; the Maintenance Program PDF also pages correctly (no single oversized sheet).
  4. `Condition Assessment > Asset Details`: `Condition` and `Assessment` dropdowns replace the nine chips, with counts shown inside the menus.
  5. `Bulk Ops > Edit > Assessment date`: re-date a selection — picker bounded to today, calculator answers unchanged, unassessed assets skipped and reported in the toast.
- **Endpoints:** `GET /program-compliance/<sld_id>/expected`, `POST /asset-maintenance/assessment-date` (date bounded `2000-01-01 <= assessed_on <= today`, stored at 12:00 UTC, capped at `MAX_APPLY_NODES`).
- **Negative control:** `assessment-date` rejects malformed / future / pre-2000 dates with **400** and no DB write; returns **404** when nothing in the selection belongs to the caller; the `expected` endpoint returns **403** on another tenant's standard.

---

### ZP-4113 — Issue Report: dedicated report card and a separate Condition Assessment export (Eaton-6, web)
- **Surface:** `Maintenance Reports` page (`Issue Report` card); `Condition Assessment` page header (`Export assessment report` and `Export issue report`). Precondition: seed an Issue Report config on an SLD-typed site.
- **Check:**
  1. Maintenance Reports: an `Issue Report` card appears and the Issue Report config is no longer in the `EMP Lite` leftover bucket.
  2. Open the `Issue Report` card: the Report Generation modal opens on the Issue Report with its three options (include resolved/closed, thermal photos, issue photos).
  3. Condition Assessment page shows two header buttons: `Export assessment report` and `Export issue report`.
  4. Press `Export issue report`: the modal opens scoped to Issue Report configs and the picker is populated (not empty — `sldId` is now passed).
  5. The toast on each export names the specific report being generated.
- **Endpoints:** none (frontend only; `NAME_MATCH["issue-report"] = /issue report/i`).
- **Negative control:** `Export assessment report` must NOT list the Issue Report config, and the issue export must NOT list assessment-only configs.

---

### ZP-4127 — Eaton-8: SKM continuous setting dials and LO/HI label parity (backend, frontend, asset agent)
- **Surface:** the library device page AND the asset form's `Engineering` drawer, on a device with a continuous SKM segment (e.g. `Powerpact J-Frame`, `INST (5-10 x Trip)`); `FXD6-A Sentron` INST picker.
- **Check:**
  1. The continuous segment renders as a bounded number field (dial), not a 2-item pick-list, on both surfaces.
  2. Enter `7.4` → accepted; `12.5` → snaps to max `10`; `4` → snaps to min `5`; `7.375` → rounds to `7.38`; clear the field → keeps the previous value.
  3. On a segment carrying an OFF position, the `Set`/`OFF` toggle appears and `OFF` is accepted; a non-OFF sentinel value is not offered.
  4. LO/HI parity: `FXD6-A Sentron` INST picker reads `LO, 2, 3, 4, 5, 6, 7, HI` (labels), not resolved to numbers.
  5. Asset agent: `get_trip_unit_settings` reports `continuous: true` with a range (min, max, step, off) for the J-Frame INST; `7.4` validates; `12` and `"abc"` are refused with the range quoted.
- **Endpoints:** web payload `segment_to_dict` gains `domain` and `pickup_in_amps`; `skm_config_validator`. OFF is matched at exactly `9999` or a label of `OFF`.
- **Negative control:** compound INST bands STILL keep their two pick-lists; export still writes the stored setting string verbatim (e.g. `INST (5-10 x Trip)=7.38;`).

---

### ZP-4128 — Eaton-9: ABB Ekip DIP-switch trip units always get the review flag (asset agent)
- **Surface:** no web UI change — an asset-agent (AI pipeline) rule. It surfaces through the existing `eqp_lib.warnings` well/chip and the existing `Mark reviewed` flow.
- **Check:**
  1. Run the asset agent against an `ABB Emax 2 E1.2 Ekip DIP` breaker with emitted settings: it carries an `Incomplete - needs review` warning (amber well/chip) and confidence is `medium`, not `high`.
  2. The same device with NO settings emitted (frame-only designation): no warning added, confidence unchanged.
  3. An honest no-match described as an Ekip Dip device (e.g. type `"XT2, Ekip Dip LSIG"`): the warning is still applied.
  4. Idempotency: run the audit twice on the same config → exactly one warning entry, no duplicate.
- **Endpoints:** none. Match regex `_DIP_TRIP_UNIT_RE = ekip[^,]*\bdip\b`.
- **Negative control:** an `XT2 Ekip Hi-Touch` / `Touch` LCD device must get NO review warning; a `kind=cable` result is untouched; `critical_warnings` is not populated by this rule while the existing kA-mismatch critical warning still fires.

---

### ZP-4131 — Settings Verifier: download EG Utils installer from the SKM and SLD export menus, with an audit-steps dialog
- **Surface:** Assets page `SKM` dropdown (item after `Export`); SLD `Export` menu (item after `Export Engineering XML`); the shared `SettingsVerifierDialog`.
- **Check:**
  1. Assets page: open the `SKM` dropdown after `Export` and confirm a `Settings Verifier` item appears.
  2. Click it: the dialog opens FIRST, listing the three audit steps (export the Engineering XML → import into SKM PowerTools → run EG Utils' SKM Import Audit for an Excel report of the differences) and the Windows / PTW32 requirement, before any download.
  3. Click `Download EG Utils` and confirm `EG-Utils.msi` downloads as an attachment from the `eg-pz-downloads-ohio` bucket.
  4. SLD `Export` menu, after `Export Engineering XML`: the same `Settings Verifier` item and dialog on desktop/web.
  5. Switch locale between `en` and `fr` and confirm the label renders in both (`sld.settingsVerifier`).
- **Endpoints:** installer URL in `src/constants/downloads.js`, bucket `eg-pz-downloads-ohio`.
- **Negative control:** inside the iOS WebView the SLD `Settings Verifier` entry is hidden (`isIOSBridgeAvailable`) — no `.msi` option offered.

---

### ZP-4138 — Portal Sales role gates the Maintenance Portal - hide the customer view from internal staff seats
- **Surface:** the left rail `Maintenance Portal` section (pinned below Builder) and the `/maintenance-portal/*` routes; role assignment for the new global system role `Portal Sales`.
- **Check:**
  1. Sign in as a user holding staff roles (e.g. Admin + PM + AM + Electrical Engineer) WITHOUT `Portal Sales`: the `Maintenance Portal` section is absent from the rail and direct navigation to `/maintenance-portal/*` is blocked (`AccessDenied`).
  2. Assign `Portal Sales` to that same user — even as a non-active `user_roles` row — and confirm the section reappears and the routes load.
  3. A Super Admin without `Portal Sales` also LOSES the Maintenance Portal (intended).
  4. A Tier-2 (T2) portal seat still sees the portal via the tier gate, with no `Portal Sales` role assigned.
  5. Backend: `alembic heads` reports a single head after `portalsales_a1`.
- **Endpoints:** none; migration `portalsales_a1` (parent `com4_a1`); role name constant `PORTAL_SALES_ROLE`.
- **Negative control:** `Portal Sales` grants NO extra access on its own — a user whose only role is `Portal Sales` gains no `platform.web` or `opportunities.*` capabilities (0 `role_permissions` rows for the role).

---

### ZP-4149 — Report configs follow a forked service's family, and EG form V2 definitions survive the envelope-less shape
- **Surface:** work-order report generation (`ReportGenerationModal`) on a company that forked a global service and renamed the fork's type to `IR Checklist`; the EG form builder canvas + Accept/Discard bar.
- **Check:**
  1. On such a company, open a work order on the fork and generate a report: the shared typed report config (e.g. `Infrared Thermography Report`) now appears as the **primary** config.
  2. In the form builder, have the AI agent produce a V2 `form.json` in the `{version: 2, blocks, option_sets}` shape: the canvas updates to show the agent's form BEFORE `Accept`, `Accept` saves what is shown, and a **saved** snackbar appears.
  3. Trigger a failing definition save and confirm a **failed** snackbar shows the backend's reason.
  4. Open a form previously stored without `version: 2` and confirm it no longer renders blank.
- **Endpoints:** form write/validate routes running `coerce_v2_definition`; config fields `work_type_service_ids`, `work_type_types`; `ReportGenerationModal` takes `workTypeServiceId` (`session.work_type_id`).
- **Negative control:** a plain `Infrared Thermography` work order and a general work order still resolve their correct configs; the legacy `work_type`-only fallback still matches on older data; genuine V1 lists are untouched.

---

### ZP-4150 — [Web] Issue Suggestion - In the list provide an option to add content directly from the issues if matching information not found
- **Surface:** the Issue Suggestion **list**. The description names no route, button label or field — the only stated acceptance criterion is "Provide an option in the list to add content directly from the issues when matching information is not found."
- **Check:**
  1. Open the Issue Suggestion list and search/scroll for a case where no matching information is found.
  2. Confirm an option is offered in that list to add content directly from the issues (present vs absent — this is the only falsifiable check the ticket supports).
- **Endpoints:** none named.
- Note: description is near-empty — Context reads "No additional background information was provided." Expect to confirm the exact control's label with the reporter.

---

### ZP-4152 — [Web] Rename a service without the AI round-trip, and mark a company's default PM standard by identity not name
- **Surface:** the service detail page `Rename` menu item (`ServiceRenameDialog`); the `PM Standards` list star; `Maintenance Compliance`; the Report modal's `Company default` option.
- **Check:**
  1. On a company-owned service, use the `Rename` menu item: a one-word rename saves instantly with no AI `Update service` flow.
  2. A rename is rejected on a **global** service, and returns **409** while a spec build is running.
  3. Rename a company's `NFPA 70B 2026` mirror (e.g. to add ` (ET)`): on `PM Standards` and `Maintenance Compliance` the star STAYS on that company mirror and does not jump to the global.
  4. Set a different company-owned standard as default via the star: the previous default's star clears and the Report modal's `Company default` option names the newly starred standard.
- **Endpoints:** `PATCH /procedures-v2/services/<id>` (name ≤200 chars), `POST /procedures-v2/pm-plans/standards/<id>/default`, `PUT /pm-standards/<id>` (accepts `is_default`); migration `pmstddefault_a1`. Resolution order: `is_default` → NFPA mirror → any standard with plans → global.
- **Negative control:** the set-default action is ABSENT on global standards, and a user from another company cannot rename or set-default a standard they do not own.

---

### ZP-4153 — [Web] Journal walk: a proposed unit names the photos that show it, and the interpretation sweeper stops idling
- **Surface:** a journal walk's Journal Review page — the location rail (add/rename/delete), the graph card's claimed-photo thumbnails, the poll status.
- **Check:**
  1. Capture an entry with several photos (board, its nameplate, the main's nameplate): the interpreted unit's graph card lists the claimed photos **by kind**, with thumbnails that open the viewer.
  2. Release the walk: each claimed journal photo lands on the correct node with the type its kind names (nameplate → `node_nameplate`, and likewise `node_panel_schedule`, `node_arc_flash_sticker`, `node_other`), while plain walk-asset photos stay `node_profile`.
  3. From the web, add / rename / delete (when empty) a location and confirm it matches the phone's two-field flow.
  4. Latency: make a change and confirm the review page's poll shows `queued` and refreshes within a couple of seconds (sweeper now every 2s, `QUIET_SECONDS = 0`), without waiting on the old 8s idle tick.
- **Endpoints:** migration `swjrnl_a6` adds `site_walk_assets.proposal_photos` JSONB; kinds = `profile | nameplate | panel_schedule | arc_flash_sticker | other`; `FOLD_VERSION` `2026-09-02.1`.
- **Negative control:** a claim referencing a photo id the model was not shown is dropped (surfaced as a repair problem), and the claim list never exceeds **20**.

---

### ZP-4159 — Web: Unauthorized Access: Facility Manager Can Open Work Orders for Unassigned Sites via Direct URL
- **Surface:** `https://acme.qa.egalvanic.ai` → `Work Orders` list, then the work-order details URL `https://acme.qa.egalvanic.ai/sessions/8d9aad65-fd8c-4fb7-bbc2-05f2e76cc688` (WO `test`, Site `Android Site 2`, Account `Test op`). Role: `Facility Manager` (also impacts the Client Portal role).
- **Check:**
  1. Sign in as a Facility Manager restricted to specific sites; open `Work Orders` and confirm only work orders from assigned sites are displayed.
  2. Paste the unassigned-site work-order URL above and open it.
  3. Confirm the page does NOT display Site name, Owning account information, Assets, Issues or Attachments for that unassigned site.
  4. Confirm the user instead gets `Access Denied (403)`, a redirect to an authorized page, or an appropriate authorization error message.
  5. Repeat for the Client Portal role, which the report says is also affected.
- **Endpoints:** route `/sessions/{id}` (work-order details).
- **Negative control:** a work order belonging to an **assigned** site must still open normally for the same Facility Manager.

---

### ZP-4167 — [Web] BCES engineering batch: bus-class field hide-flags, verbatim trip-setting text, GF I2t and relay settings
- **Surface:** the asset form for `Junction Box` and `Disconnect Switch`; the circuit-breaker trip-settings grid; the relay `Fixed / Has Settings` path.
- **Check:**
  1. `Junction Box` asset form: no `Mains Type`, no `Manufacturer` / `Panel Type` / `SCCR` block; readiness does not list mains type.
  2. `Disconnect Switch`: `Mains Type` present (`Fuse` / `None`), no `Manufacturer` / `Panel Type` block.
  3. Circuit breaker, `Has Settings`: type `10 x In` into `Long Time Pickup`, save, reload — the value persists **verbatim**, while a blank cell is still rejected.
  4. `Ground Fault I2t` (`On`/`Off`/`N/A`) appears after `Ground Fault Delay` in the breaker grid; the relay manual path shows `Pickup`, `Time Dial`, `Instantaneous`, `GF Pickup/Delay` and round-trips on save/reload.
  5. After deploy: `SELECT name, hide_manufacturer_and_type_for_bus, hide_mains_type_for_bus FROM node_classes` shows `Junction Box` true/true and `Disconnect Switch` true/false; republish the prod SLD viewer (OTA).
- **Endpoints:** migration `bceseng_a1` (parent `nodedrift_a1`); flags serialized in `NodeClass.to_dict`; trip settings stored in `trip_settings` JSONB.

---

### ZP-4170 — [Web] BCES engineering: seed manufacturer picker enum rows, aliases and placeholder models (KROON nameplate data)
- **Surface:** the transformer manufacturer picker and the bus/panel maker picker.
- **Check:**
  1. Confirm migration `bceseng_a2_manufacturers` applied cleanly on the target env.
  2. Transformer manufacturer picker: `HAMMOND POWER SOLUTIONS`, `MARCUS`, `REX`, `BEMAG`, `DELTA` and `DELTA STAR` are all selectable.
  3. Alias resolution: a nameplate reading `Crouse-Hinds` resolves to `EATON/CUTLER-HAMMER`, `Commander` to `SCHNEIDER/SQUARE D`, `HPS` to `HAMMOND POWER SOLUTIONS`.
  4. Bus/panel maker picker: `CEB` is selectable.
  5. Selecting a newly seeded transformer maker shows the placeholder model with no kVA entries (expected, not a bug).
- **Endpoints:** tables `enum_skm_manufacturers`, `enum_skm_manufacturer_aliases`, `skm_transformer_models` (placeholder oids `990000000+`); migration `bceseng_a2` (parent `bceseng_a1`).
- **Negative control:** re-running the migration makes no duplicate rows and raises no error (idempotency).

---

### ZP-4171 — [Web] Core attributes on switch subtypes: mask class props by subtype key, and expose AIC Rating for the switch library type
- **Surface:** the class editor's per-attribute `Subtypes (empty = all)` multi-select; the asset form's subtype-aware fields; `Integral Disconnect Switch` (Switch library type) form.
- **Check:**
  1. Class editor: mask a core attribute to a single subtype, save and reopen — the selection persists and is stored as **keys** (not ids).
  2. Asset form: pick that subtype → the attribute appears; switch to another subtype → the attribute is hidden and its value cleared.
  3. Arc-flash readiness ignores a masked attribute on a node of a non-listed subtype and still counts it on a listed subtype.
  4. `GET /lookup/node-subtypes/<class>` items include a `key` field.
  5. `Integral Disconnect Switch` (Switch library type): `AIC Rating` renders below `Pole Count` and saves to the `aic_rating` column; a circuit-breaker form is unchanged. The nameplate AI catalog no longer proposes a masked attribute.
- **Endpoints:** `GET /lookup/node-subtypes/<class>`; definition prop `subtype_keys`; column `aic_rating`. OTA bundle republish required.
- **Negative control:** a class prop with NO `subtype_keys` still applies to every node of the class; a node with no subtype does not receive a masked prop.

---

### ZP-4174 — bceseng_a3: Bus Duct hides mains type and the panel manufacturer / type block
- **Surface:** the component core-attributes form for a `Bus Duct` (`Busduct`) part.
- **Check:**
  1. Open a Bus Duct component's core attributes: the mains-type selector is hidden and the panel manufacturer / type block no longer appears, matching `Junction Box` behaviour.
  2. Check both the global `Busduct` definition and a component that carries an override — the flags are set on the global default plus every override.
  3. Confirm alembic migration `bceseng_a3` (parent `bceseng_a2`) applies cleanly and leaves a single alembic head.
- **Endpoints:** none (data-only migration setting `hide_manufacturer_and_type_for_bus = true` and `hide_mains_type_for_bus = true`).
- **Negative control:** other component types (e.g. switchboard / panel) STILL show mains type and the manufacturer/type block — the hide is scoped to Bus Duct only.

---

### ZP-4176 — [Branch Merge] All Dev FE/BE to marge to QA
- **Surface:** no web surface — a release/branch-management task (git branches + PRs), not a product change.
- **Check:**
  1. Confirm Prod has been pulled into Dev, QA and Stage.
  2. Confirm a PR was raised from Dev and from QA.
  3. Confirm all Dev **frontend** branches are merged to QA.
  4. Confirm all Dev **backend** branches are merged to QA.
- **Endpoints:** none.
- Note: acceptance criteria are branch-state facts only; nothing here is checkable in the browser.

---

### ZP-4183 — Exception: An internal error occurred.
- **Surface:** no web surface — a Sentry-generated issue (`EGALVANIC-PZ-5S`) with only a Kotlin stack trace from the **Android** app: `FlushSyncQueueUseCase.processEGFormInstance` (`FlushSyncQueueUseCase.kt:1795`), `Result.failure(Exception(errorMsg))`.
- **Check:**
  1. Description is a bare stack trace with no steps, no expected result, no environment. The only possible check: open the Sentry issue, confirm whether new events are still arriving on the current build for the EG Form instance sync-queue flush path, and confirm the `errorMsg` body carried in a recent event.
- **Endpoints:** none named (sync-queue flush of an EG Form instance).
- Note: description too thin to write an acceptance test from — needs a repro from the reporter.

---

### ZP-4185 — Error: HTTP Client Error with status code: 400
- **Surface:** no user-facing surface named — a Sentry issue (`EGALVANIC-REACT-APP-2SE`) whose only content is a JS stack: `APIClient._doRefresh (src/utils/apiClient.js:396:28)` ← `initializeAuth (src/store/auth.js:562:15)`. That is the **token refresh on app init**.
- **Check:**
  1. Open the Sentry issue and confirm whether 400s on the auth refresh path are still occurring on the current bundle.
  2. Exercise app load with a stale/expired refresh token and observe whether `_doRefresh` returns 400 and whether the user is cleanly sent to login rather than left in an error state.
- **Endpoints:** none named (the token-refresh call inside `APIClient._doRefresh`).
- Note: near-empty description — a Sentry stack trace only, with no steps, actual or expected result.

---

### ZP-4186 — Error: HTTP Client Error with status code: 500
- **Surface:** no page named, but the stack points at the extract action in the Core Attributes editor: `handleExtract (src/components/CoreAttributesEditorDialog.jsx:1296:24)` → `APIClient._executeRequest`. Sentry issue `EGALVANIC-REACT-APP-18H`.
- **Check:**
  1. Open an asset's Core Attributes editor dialog and run the extract action; confirm whether it returns HTTP 500.
  2. Open the Sentry issue and confirm whether new 500 events are still arriving on the current bundle for `handleExtract`.
- **Endpoints:** none named (the extract call issued from `CoreAttributesEditorDialog`).
- Note: near-empty description — a Sentry stack trace only, with no steps, actual or expected result.

---

### ZP-4190 — [AI Pipeline] PR #112 Review — Journal Interpretation Engine, COM 4, Continuous Settings, DIP-Switch Flag
- **Surface:** mostly no web surface — an AI-pipeline PR review (`eg-pz-engineering-ai-pipeline` PR #112, Dev → QA). The COM 4 and continuous-dial halves are observable in the product; the rest is S3 / Step Function / CI.
- **Check:**
  1. Journal fold end-to-end: trigger a site walk interpretation — `walk.json` is read, folds checkpoint to S3 (`states/{entry_id}.json`), `preview.json` updates live, `assembly.json` ships on completion.
  2. Caching: re-run a walk with 1-2 new entries — only the new entries are folded, cached entries reused.
  3. COM 4: create an asset with `COM=4` via the extraction workbook — it saves as non-serviceable; the quote-editor filter `com=[4]` returns non-serviceable assets only. Workbook dropdown reads `1,2,3,4`.
  4. Continuous settings: on an Eaton trip unit with a continuous dial, any value in range is accepted, not just the printed labels.
  5. DIP-switch flag: run the asset agent on an ABB Ekip DIP-switch trip unit — the review warning is always added regardless of agent confidence.
  6. Open question to answer, not assume: confirm the `asset_agent` mode removal from `modes.py` was intentional and all asset-agent workloads now route through the Lambda path only.
- **Endpoints:** Step Function `journal-interpretation.asl.json` (2-hour timeout, single Fargate task, 1 ECS retry); S3 `journal-jobs/{job_id}/walk.json`; CI `check_vendored.py`.
- **Negative control:** existing `form_fill`, doc-extraction and report modes still work — the `driver.py` refactor (`direct_artifact`/`direct_attrs`) must not change their behaviour.

---

### ZP-4207 — Z | FE | 502 Error When Re-interpreting Journal Site Walk Created on Mobile
- **Surface:** the web Site Walk view for a Journal Site Walk **created on mobile** → the `Re-interpret` option.
- **Check:**
  1. Create a Journal Site Walk from the mobile application.
  2. Open the same Site Walk from the web application.
  3. Select `Re-interpret`.
  4. Confirm the re-interpretation completes successfully and **no 502 error** is displayed.
- **Endpoints:** none named in the ticket (the re-interpret action; see ZP-4068 for `POST /site-walk/<id>/interpret`).

---

### ZP-4216 — Alembic revision graph left inconsistent after the qa-to-dev merge brought the prod hotfix chain across
- **Surface:** no web surface — a database migration-graph change (revision parents only; no upgrade/downgrade body altered).
- **Check:**
  1. Run `alembic upgrade head` on a database at the pre-merge revision: it resolves to a single head and completes without a `multiple heads` error.
  2. `alembic heads` reports exactly ONE head.
  3. The revisions apply in the new order: `bceseng_a3`, then `xfmrgen_a1`, then `swjrnl_a1` onward through `swjrnl_a6`.
  4. `bceseng_m1` is gone and nothing still references it as a `down_revision`.
  5. Because this is a graph-only change, the resulting schema is identical to a database migrated before the fix.
- **Endpoints:** none.
- **Negative control:** on a database already fully upgraded before this change, the deployment must NOT attempt to re-run or fail on the re-parented revisions.

---

### ZP-4218 — Quotes: RFQ button does not generate when a labor line item is marked subcontracted
- **Surface:** a quote → a labor line item → the `subcontracted` setting → the `RFQ` button/action on the quote.
- **Check:**
  1. Open a quote.
  2. Set a labor line item to `subcontracted`.
  3. Confirm an `RFQ` action now appears on the quote (it previously did not).
  4. Confirm the subcontractor request can actually be generated from the quote via that action.
- **Endpoints:** none named (DevRev ref `ISS-5238`).
- Note: the ticket also carries an open question (not an acceptance criterion): how to enter a subcontractor's lump-sum fee in the pricing workflow.

---

### ZP-4261 — Photo actions recorded duplicate activity-ledger rows (every record_event call was doubled)
- **Surface:** adding/updating a photo on a work order, then the activity ledger / per-user activity counts / per-session photo counts / audit exports. **Verify on QA or later** — dev still carries the duplicate.
- **Check:**
  1. On QA, add a photo to a work order: exactly ONE `PHOTO_ADDED` row is written to the activity ledger (previously two).
  2. Update an existing photo: exactly ONE `PHOTO_UPDATED` row.
  3. Exercise the create-with-date and upsert paths as well — each records a single event.
  4. Per-user activity counts and per-session photo counts reflect single events rather than double.
- **Endpoints:** none (`record_event` call sites in `app/controllers/photo_controller.py`).
- **Negative control:** no photo action is dropped entirely — each create/update must still produce exactly one row, never zero.

---

### ZP-4266 — Edit with AI on a report config does nothing and the button changes to "Try again" — the save is rejected (Newkirk Electric, production)
- **Surface:** **PRODUCTION** `https://newkirk-electric.egalvanic.ai` → `/reporting/config/{configId}` (the observed config id starts `09…-1ec1-4491-bccf-…`) → the `Edit with AI` dialog. Not yet reproduced on QA.
- **Check:**
  1. Open a report config for editing and click `Edit with AI`.
  2. Type a change request and submit it.
  3. Confirm the change IS applied and saved — or, if it cannot be, that the dialog says in plain words what went wrong and what to do (e.g. "this report config was changed since you opened it, please reload and try again"). A silent `Try again` button is not an error message.
  4. In DevTools, confirm `POST /api/reporting/configs/{configId}` no longer returns `409 (Conflict)` — and that if a 409 does occur, its body is surfaced to the user rather than swallowed.
  5. Confirm whether the same request succeeds after a page reload (distinguishes version clash from a duplicate/in-flight AI job).
- **Endpoints:** `POST /api/reporting/configs/{configId}` → `409 Conflict`.
- **Negative control:** ignore the ~60 other red console lines — `"A listener indicated an asynchronous response by returning true, but the message channel closed before a response was received"` is Chrome-extension noise, not the app. Only the single 409 is the defect.

---

### ZP-4272 — Column arrangement retention and customization
- **Surface:** the work-order asset list column arrangement, and the `manage columns` control on it.
- **Check:**
  1. In a work order's asset list, drag the columns into a non-default order (the reporter's preferred arrangement puts the QR Code column where it is quickly searchable).
  2. Leave the work order, then return to it: confirm whether the column arrangement is retained or has reverted to the default (reported: it reverts).
  3. Open `manage columns` and confirm whether any option exists to retain/lock the arrangement (reported: it does not; the reporter asks for a `lock current setting` button).
  4. Confirm the existing behaviour the reporter values still works: clicking the QR code number in the column opens the IR scan section of the `edit asset` box.
- **Endpoints:** none named.
- Note: this is a customer-feedback email pasted verbatim — it states the current behaviour and a feature request, not acceptance criteria. Checks 2 and 3 are the falsifiable parts.

---
