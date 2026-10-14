# Bug audit B — 13 Oct 2026 (skeptical developer review)

Bugs audited: ZP-4849, ZP-4850, ZP-4851, ZP-4852, ZP-4853, ZP-4854, ZP-4855, ZP-4865, ZP-4866.
Live QA build for every re-run: **index-C14cwR0U.js** (the tickets 4849–4855 were filed on index-COeCBn0z.js).
Environment: QA only (https://acme.qa.egalvanic.ai). Nothing was changed in Jira. Fresh screenshots are in
`docs/bug-evidence/2026-10-13-bug-audit/`. Scripts and raw output are in `.playwright-mcp/v24/audit-b/` (`out_r1.txt` … `r5`).

Seats used (is_eg_admin printed at run time): Project Manager `+project@` (false, 116 sites), Electrical Engineer `+electric@`
(false, 4 sites), Client Portal `+clientportal@` (false), Super Admin `+admin@` (true, used only as a control and for UI flows
that are not about tenancy).

Data written by this audit (QA-DEMO only):
- Bus "QA-DEMO 13Oct audit-B PM probe (delete me)" `af69c4f3-cb0c-487b-8815-3c6464ab496c`, project `QA-DEMO-13OCT-AUDITB`,
  on the QA-DEMO site `cfc07c0d…` (this is the ZP-4849 re-proof). Left in place, labelled.
- ZP-4865 re-proof: connection `30fcae76…` and asset `525225bf…` on "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" were changed by
  the EE and **restored by the PM** in the same run (`restoredOK: true`, edge back to `{"hidden":true}`, node_manips back to `null`).
- ZP-4851 re-import of G.xml into `QA-DEMO-13OCT-G`: created 0 (no change). ZP-4852 probe used an EMPTY study (nothing written).
- ZP-4854: Process Import was **not** clicked.

## Summary

| Bug | Verdict | One-line reason |
|---|---|---|
| ZP-4849 | VALID-NEEDS-EDIT | Reproduced; PM's own read of that site is 422, so it is not "PM covers all sites". But it is API-only (UI cannot pick the site), needs a copy-paste repro, an unproven "update" claim removed, and High vs ZP-4865's Medium for the same class of gap. |
| ZP-4850 | VALID-NEEDS-EDIT | Code proves the SLD item has no flag check, so the simulation does not weaken the finding; but step 1 claims a real flip that never happened, the assignee should be frontend, and it must be framed as a product decision (by-design risk). |
| ZP-4851 | DOUBTFUL | Reproduced, but the outcome (0 created) is correct, the helper line and the result warning both explain it, and "3 Assets" is the file's component list. Cosmetic button label at most → Low improvement or close. |
| ZP-4852 | DOUBTFUL | API-only; the frontend deliberately treats the project name as optional (`o && (h.skm_namespace = o)`), and adopting unnamed rows is the documented ZP-4582 back-compat. A dev can close as by design. |
| ZP-4853 | VALID-NEEDS-EDIT | Tooltip text confirmed stale (suffix removed by ZP-4760). The "keep existing label" half is really a ZP-4746 (In QA) spec failure and must be raised there; wrong assignee (text is frontend); export claims have no repro steps. |
| ZP-4854 | VALID-NEEDS-EDIT (small) | Reproduced with the exact numbers (20 assets / 4 connections, Generator-1 240 → 200). Add the proof that the untouched file stores "56" as text and that Generator-1 carries a conflicting `voltage` = 200 attribute. |
| ZP-4855 | VALID-CLEAR | Reproduced exactly (3 × 500 with trace ids, SKM export gives 400). API-only and Low is stated honestly; the same class (ZP-4820/ZP-4824) was accepted and fixed today. |
| ZP-4865 | VALID-CLEAR | Reproduced and restored; EE is non-staff, read refused 422, both writes 200 and visible to the PM. API-only, Medium is fair. |
| ZP-4866 | VALID-NEEDS-EDIT (small) | Reproduced (typed /arc-flash opens the full page; CP lacks features.arc_flash.view; /slds and /equipment-designations are denied). Pre-empt the obvious push-back: the CP already sees the headline "Arc Flash Readiness 13 %" card on Assets. |

No duplicates found. JQL text searches run: "skm import", "import-xml", skm_namespace, "bulk-edit", "Bulk Import", "arc-flash",
"Arc Flash Readiness", "node/update", "edge/update", "af_label", "label numbers", "skm-export", "not assigned", accessible_sld_ids,
"unassigned site", "require_sld_access", "arc_flash.view", "56.0", "round trip", "Invalid SLD ID format", "Process Import".
Nearest neighbours: ZP-4848 (SKM export of another company's site), ZP-4709 (CP reads unassigned assets; fix PR #1564 merged
today — its `nodes/sld` gate is now live: PM gets 422 on `/api/nodes/sld/{unassigned}`, but the SKM commit is still open),
ZP-4741 (CP opens an unassigned issue), ZP-4723 (CP SLD data), ZP-4876 (SKM re-import changes classes — different feature from
ZP-4854), ZP-4621 (batch-positions, the ZP-4865 addendum route). None covers the same endpoint + behaviour.

---

## ZP-4849 — SKM Import lets a Project Manager add equipment to a site they are not assigned to

**Verdict: VALID-NEEDS-EDIT.** Build index-C14cwR0U.js.

Reproduced (PM `+project@`, is_eg_admin false, role Project Manager, 116 assigned sites, has `data.import`):

| Check | Result |
|---|---|
| QA-DEMO site `cfc07c0d…` in PM's assigned sites | false |
| Site picker, typed "QA-DEMO ZP-4582" | "No sites available" (A1) |
| `GET /api/sld/{own site}/graph` (control) | 200 |
| `GET /api/sld/{QA-DEMO site}/graph` | **422 permission_denied** (the read is refused) |
| `GET /api/nodes/sld/{QA-DEMO site}` | 422 (ZP-4709 fix now live) |
| `POST /api/skm/import-xml/preview` own site (control) | 200, token |
| `POST …/preview` QA-DEMO site | **200, token, will create 1** |
| `POST …/commit` QA-DEMO site | **200 `created_nodes: 1`** → bus `af69c4f3…` now on the site |
| Preview on another company's site (control) | refused (masked HTML) |
| Force `activeSiteId` = QA-DEMO site in local storage, open /assets | app ignores it and falls back to "Android Site 2" → **not reachable in the UI** |

Evidence: `A1-4849-pm-site-picker-qademo-not-offered.png`, `A2-4849-pm-import-commit-on-unassigned-site.png`.

By-design check: the owner asked whether a PM legitimately covers all company sites for imports. No: the same PM is refused the
diagram read and the node list for that site, and the site is not in the picker. The server already treats the PM as
site-scoped; only the SKM write path skips the check.

Reasons a dev could reject or push back:
1. **API-only.** The ticket does not say so plainly; the UI cannot select the site. Severity "High" is inconsistent with ZP-4865
   (same class of gap, also API-only, filed Medium). Recommend Medium for both.
2. **Not reproducible from the ticket alone:** step 2 says "send an SKM XML" with no file, no FormData field names and no snippet.
3. **Unproven claim:** "(or, through re-import matching, update) equipment" was not tested. Remove it or prove it.
4. Could be folded into ZP-4865 as one "write routes skip the site-assignment check" fix; keep both but link them (already linked).

Replacement description (Jira wiki markup):

```
h3. What happens
A Project Manager who is *not* assigned to a site cannot open that site in the app, and the server refuses to show them its diagram (422). But the SKM import server calls still accept that site and create equipment on it.
This is reachable only by calling the API directly (the site is not offered in the site picker). Same kind of gap as ZP-4865 (SLD edits), different endpoints.

h3. Steps (QA, https://acme.qa.egalvanic.ai)
# Sign in as the Project Manager test user abhiyant.singh+project@egalvanic.com (a customer user, is_eg_admin = false).
# Open the "Current site" box and type *QA-DEMO ZP-4582*. The app says "No sites available": this site is not assigned to this user.
# Open the browser console (F12) on any app page and paste:
{code:javascript}
const SITE = 'cfc07c0d-71a2-40bc-bd14-76ad2372ffbd'; // "QA-DEMO ZP-4582 SKM namespace (delete me)"
const xml = `<?xml version="1.0" encoding="UTF-8"?><s:SKM xmlns:s="http://www.skm.com/schemas/powersystem" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><s:PowerSystem><s:Components><s:Bus s:action="create" s:id="961" s:name="QA-DEMO PM scope test (delete me)" s:altId="" s:altName="" xsi:ID="skm961"><s:Field s:id="37552142" s:name="NodeBus" s:value="False"/></s:Bus></s:Components><s:Topology></s:Topology></s:PowerSystem></s:SKM>`;
console.log('read', (await fetch(`/api/sld/${SITE}/graph`)).status);           // 422 - read refused (correct)
const fd = new FormData(); fd.append('xml_file', new File([xml], 'probe.xml', {type: 'text/xml'})); fd.append('sld_id', SITE);
const pv = await (await fetch('/api/skm/import-xml/preview', {method: 'POST', body: fd})).json();
console.log('preview', pv.summary.will_create_nodes);                           // 1 - should be refused
const cm = await fetch('/api/skm/import-xml/commit', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({token: pv.token, sld_id: SITE, skm_namespace: 'QA-DEMO-PM-SCOPE-TEST'})});
console.log('commit', cm.status, await cm.json());                             // 200, created_nodes: 1 - should be refused
{code}
# Sign in as an admin, pick site "QA-DEMO ZP-4582 SKM namespace (delete me)", open *Site Data > Assets* and search *PM scope*.

h3. Actual
The diagram read is refused (422), but preview answers 200 ("will create 1") and commit answers 200 with created_nodes: 1. The bus "QA-DEMO PM scope test (delete me)" is now on a site this user was never given.

h3. Expected
Preview and commit are refused (422 permission_denied) for a site that is not in the user's assigned sites, exactly like the diagram read.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Earlier proof: bus "QA-DEMO 13Oct audit-B PM probe (delete me)" af69c4f3-cb0c-487b-8815-3c6464ab496c.
* Control: another company's site is refused, so this is inside one company only. The user's own site works (200).
* The Project Manager has data.import; the check that is missing is the site assignment (accessible_sld_ids), not the permission.
* For the developer: /api/skm/import-xml/preview and /commit check the company but not caller_can_reach_sld(sld_id). Please cover them in the same audit as ZP-4865 and the ZP-4709 follow-up ("about 25 more sld_id routes").
```
Suggested priority: Medium (to match ZP-4865). The snippet's plain `fetch` (cookie auth, no extra headers) was verified from
the PM seat on index-C14cwR0U.js with an empty study: read 422, preview 200 with a token (`r6` run).

---

## ZP-4850 — Turning off SKM export hides it on Assets but not on the SLD page

**Verdict: VALID-NEEDS-EDIT.** Build index-C14cwR0U.js.

Reproduced: real QA value for Acme `feature-skm-export` = **true** (also `skm-import` true, `skm-import-legacy` true). With the
evalx answer rewritten to OFF in one test browser (PM, "(s) Wild Goose Brewery"): Assets › SKM = Import (Full) | Import
(XML→Excel) | Settings Verifier (Export hidden); SLD › Export = Export PDF | Export DXF | **Export Engineering XML** | Settings
Verifier | Export JSON | Export Ground Fault JSON. Evidence: `B4-4850-simulated-flag-off-sld-menu.png` (labelled SIMULATED).

Is the simulation acceptable? **Yes, for the conclusion** — the shipped code decides it without any flag: in
`GoJSGraphEditor-uQAC6bM7.js` the menu entry is `{key:"xml",label:sld.exportSkm,onClick:…,hidden:k}` where `k` is the PDF-only
mode; there is no flag read anywhere in that chunk, while `Assets-k7wjvXay.js` computes `le=ot("skm-export")` and gates Export on
it. So a real flip will give the same menus. It should still be confirmed with a real flip before a dev sees it, and the
ticket must not say a real flip was done.

Reasons a dev could reject:
1. **By design:** ZP-4482 describes "both entry points (the SLD export menu and Assets > SKM)" as one feature, but the SLD-page
   item existed long before (memory: ZP-3836 testing on 3 Sep used /slds › Export › Export Engineering XML). The flag may have been
   added only for the new Assets entry point. This is a product decision, not an obvious defect.
2. **Step 1 is not what was done** ("Set feature-skm-export OFF in LaunchDarkly") — the flag was simulated.
3. **Wrong assignee:** the fix is in the frontend SLD export menu (QA's own ZP-4482 comment suggested Krunal); Avani is backend.

Replacement description:

```
h3. What happens
The flag feature-skm-export controls the "Export" item under *Site Data > Assets > SKM*. The same export is also offered on the SLD page (*Export > Export Engineering XML*), and that item does not look at the flag. So turning the flag OFF does not turn SKM export off.

h3. Steps (QA, https://acme.qa.egalvanic.ai)
# Turn feature-skm-export OFF for Acme in LaunchDarkly (QA). (QA simulated this in its own browser because the flag is ON for Acme; a real flip is requested.)
# Sign in as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery".
# Open *Site Data > Assets > SKM*: the menu shows Import (Full), Import (XML to Excel), Settings Verifier. No Export (correct).
# Open *Site Data > SLD* (https://acme.qa.egalvanic.ai/slds) and click *Export*.

h3. Actual
The SLD Export menu still lists "Export Engineering XML", and it still downloads the SKM file.

h3. Expected (please confirm with product)
If the flag is meant to switch SKM export off, the SLD-page item is hidden too. If the SLD-page item is meant to stay, close this as by design.

h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026. The OFF state was *simulated* (LaunchDarkly answer rewritten in the test browser only).
* The code shows the result does not depend on the simulation: the SLD export menu item is hidden only in PDF-only mode and reads no flag; the Assets item reads skm-export.
* Raised first as a gap on ZP-4482.
```
Suggested assignee: Krunal (frontend). Real flip needed: `feature-skm-export` → OFF, env QA, company Acme, briefly, to confirm both
menus (then back ON).

---

## ZP-4851 — SKM Import review says "Import 3 assets" when re-importing into the same project, but creates nothing

**Verdict: DOUBTFUL** (cosmetic; likely "works as designed"). Build index-C14cwR0U.js.

Reproduced (Super Admin, QA-DEMO site, G.xml, existing project `QA-DEMO-13OCT-G`):
- Before picking: helper "3 SKM ids are also used by another project here and will be created as separate assets", button
  "Import 3 assets".
- After picking QA-DEMO-13OCT-G: helper "**3 components already on this SLD from this project will be linked, not duplicated.**",
  tile "3 Assets", list of the 3 buses, button "**Import 3 assets**" (`B1-4851-reimport-same-project-review.png`).
- Result: "Import successful. Created 0 assets and 0 connections … 1 warning: 3 component(s) already exist on this SLD (same SKM id
  in 'QA-DEMO-13OCT-G' from a previous import) and were not re-created; new connections were pointed at the existing assets."
  (`B2-4851-reimport-result-0-created.png`). Commit: `created_nodes 0, skipped_existing_nodes 3`.

Why a dev will likely reject it:
1. The behaviour (no duplicates) is exactly what ZP-4582 asks for, and the title "but creates nothing" reads as if that were wrong.
2. The screen tells the truth twice: the helper line before import and the warning after.
3. "3 Assets" heads a table of the file's 3 components — it is the study's content, not a promise of 3 new rows. The button text
   is built from `will_create_nodes` / `total_components` of the preview (code: `` `Import ${W.total_components} assets` ``),
   which is computed before the project is chosen; changing it is a UX polish request.
4. The ticket says the tile shows "3 new assets"; the tile actually says "3 Assets".

Recommendation: close, or keep as **Low improvement** with a fixed title, e.g.:

```
h3. [Web] SKM Import: when an existing project is picked, the Import button should say what will happen ("Link 3 existing")
Re-importing a study into the project it came from correctly creates nothing (the 3 buses are linked, not duplicated), and the helper line says so. But the button still says "Import 3 assets", which reads as if 3 new assets will be added.
# Admin, site "QA-DEMO ZP-4582 SKM namespace (delete me)", *Site Data > Assets > SKM > Import (Full)*.
# Upload a study that was already imported into project QA-DEMO-13OCT-G and pick that project in "SKM project".
# Helper: "3 components already on this SLD from this project will be linked, not duplicated." Button: "Import 3 assets". Result: "Created 0 assets".
Suggestion: after a project is picked, label the button "Link 3 existing assets" (or "Import 0 new, link 3"). QA build index-C14cwR0U.js.
```

---

## ZP-4852 — An SKM import saved without a project name is later taken over by a different project

**Verdict: DOUBTFUL** (API-only, matches documented back-compat). Build index-C14cwR0U.js.

Re-checked: `POST /api/skm/import-xml/commit` with no `skm_namespace` (EMPTY study, nothing can be written) → **200**,
`"skm_namespace": null` — the server still accepts it. The earlier H-buses are still in project `QA-DEMO-13OCT-I` (state from the
original test, unchanged).

Why a dev will likely reject it:
1. **Not reachable by a user.** The web dialog requires a name whenever the preview offers projects.
2. **The client treats the field as optional on purpose:** the shipped commit helper only adds it when present
   (`o && (h.skm_namespace = o)`), and the Import button is disabled only when `summary.skm_namespace` exists and the name is
   empty. That is a deliberate back-compat contract, not an oversight.
3. **The "taken over" part is the documented design:** ZP-4582 says "legacy rows with a NULL namespace still match and are
   adopted". Rows created through step 1 are indistinguishable from legacy rows, so adoption is expected.
4. The only real harm needs someone to call the API by hand with a study whose SKM ids collide. Hard to justify even as Low.

The one user-visible nit (reachable with genuine legacy data): the helper says "N components already on this SLD **from this
project** will be linked" even when the N are unnamed legacy rows (code: count = chosen-project matches + null-namespace matches).
If the owner wants to keep a ticket, reword it to that wording nit, Low; otherwise close as by design.

---

## ZP-4853 — SKM Export tooltip still promises a "-1, -2" suffix and that existing label numbers are kept

**Verdict: VALID-NEEDS-EDIT.** Build index-C14cwR0U.js.

Reproduced:
- Tooltip text today (role=tooltip, Wild Goose, Assets › SKM › Export): "Each bus gets Building/Floor/Room plus -1, -2, … as its
  SKM arc-flash label number. Buses that already have a label number keep it; buses with no location are left blank."
  (`B3-4853-tooltip-text.png` — the bubble had closed before the shot; the ticket's own screenshot 6 shows it.)
- `POST /api/sld/cfc07c0d…/export-skm?af_label_from_location=true` (QA-DEMO site): QDA-BUS-MAIN, QDA-BUS-LOAD and QDB-BUS-MAIN all
  get **"QA Bldg-North/Fl 1-2/Rm A-B"** (same room, no suffix — the intended ZP-4760 behaviour). QDA-BUS-MAIN was imported from
  QA-DEMO-PROJ-A.xml with `AF_LabelNumber = QA-PRESET-LABEL-A`; the export writes the location label instead, and the node has no
  stored AF label field at all (so with the option OFF the imported label is simply gone).

Reasons a dev could reject or bounce it:
1. **Two different problems in one ticket.** (a) The "-1, -2" text is stale because ZP-4760 removed the suffix (clear, trivial,
   frontend text). (b) "Buses that already have a label number keep it" is a requirement of **ZP-4746** (In QA: "A label already on
   the bus (e.g. from a prior SKM import) is preserved"). That not working is a ZP-4746 failure; a dev will say "fix the backend, not
   the tooltip" or "already covered by ZP-4746". It should be reported as a FAIL comment on ZP-4746 (no comment exists there yet).
2. **Wrong assignee:** tooltip strings (`labelFromLocationHint`, EN + FR) live in the frontend → Krunal, not Avani.
3. The export claims have no steps (which site, which call) — a dev cannot check them from the ticket.

Replacement description (scoped to the text only):

```
h3. What happens
The help text next to "Generate arc-flash label numbers from location" (SKM Export dialog) still describes the old numbering. Since ZP-4760 the export writes the location label exactly as it is, with no -1, -2 suffix.

h3. Steps (QA, https://acme.qa.egalvanic.ai)
# Sign in as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery".
# *Site Data > Assets > SKM > Export*.
# Hover the (i) icon next to "Generate arc-flash label numbers from location".

h3. Actual
"Each bus gets Building/Floor/Room plus -1, -2, ... as its SKM arc-flash label number. Buses that already have a label number keep it; buses with no location are left blank."
Today's export (site "QA-DEMO ZP-4582 SKM namespace (delete me)", option ON) gives three buses in the same room the same label "QA Bldg-North/Fl 1-2/Rm A-B", with no suffix.

h3. Expected
The text matches the export, e.g. "Each bus gets its Building/Floor/Room as its SKM arc-flash label number; buses with no location are left blank." Same change in the French text (labelFromLocationHint).

h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026.
* The sentence "Buses that already have a label number keep it" is a ZP-4746 requirement. QA found it does not work today (an imported AF_LabelNumber "QA-PRESET-LABEL-A" is replaced by the location label, and is dropped when the option is OFF). That is reported on ZP-4746. Keep or drop that sentence depending on the ZP-4746 outcome.
```
Suggested assignee: Krunal (frontend). Action for the owner: add the "imported label not kept" FAIL to ZP-4746 (evidence already
in `.playwright-mcp/v24/skm/evidence/zp4746-xml-imported-label-not-kept.html`, re-confirmed today).

---

## ZP-4854 — Bulk Import of an unchanged Bulk Export shows 20 asset changes and would overwrite values

**Verdict: VALID-NEEDS-EDIT (small additions only).** Build index-C14cwR0U.js.

Reproduced exactly (PM, "(s) Wild Goose Brewery"): Bulk Export → Bulk Import of the same file → skip the one ambiguous name
("Panelboard-1" matches 2 assets) → Review: **Assets 20 to update, Connections 4 to update**; e.g. "abhiy ant": voltage (empty) →
240.0, ampere_rating (empty) → 800, Catalog Number 56 → 56.0, Size 67 → 67.0, Serial Number 89 → 89.0; **Generator-1: voltage 240.0
→ 200.0**; connections Length (ft) 89 → 89.0; "testv": asset_name testv → testv. The Review also raises "Issues Found" on rows
51, 38, 39 of the untouched file. Process Import not clicked. Evidence: `C1-4854-unchanged-export-reimport-review.png`,
`C2-4854-generator-1-change.png`.

Root cause evidence from the downloaded file (`.playwright-mcp/v24/audit-b/bulk_export_unchanged_auditB.xlsx`):
- "abhiy ant": `Catalog Number` = "56", `Size` = "67", `Serial Number` = "89" are stored as **text** cells, `Voltage` = "240V",
  `Ampere Rating` = "800A", and there is no node voltage. So "56 → 56.0" is the importer reading a text cell as a number, and
  "(empty) → 240.0" is the importer copying the attribute into the node field.
- "Generator-1": `node.voltage` = "240V" and a separate attribute column `voltage` = "200"; the server node has voltage 240 /
  "240V". The importer lets the attribute overwrite the node value.

Is "normalising numbers" by design? Nothing in ZP-4592 or the dialog ("Upload an edited bulk edit export…") suggests it, and it
cannot be for text fields: Serial Number / Catalog Number "89" → "89.0" is a real data change. A dev may argue Generator-1 is
pre-existing conflicting data; the ticket should say that the round trip, not the data, is the problem.

Text to add under Notes:

```
* Proof from the untouched file: Catalog Number, Size and Serial Number are text cells ("56", "67", "89"), so "56 -> 56.0" comes from the import reading text as a number. Text fields such as Serial Number would really change on Process Import.
* Generator-1 row: node.voltage = "240V" and a separate attribute column "voltage" = "200". The import lets the attribute overwrite the node value, so an untouched sheet lowers the generator's voltage from 240 to 200.
* Most other rows: node voltage is empty and the attribute "Voltage" = "240V", so the import fills node voltage with 240.0. Even if that sync is wanted, it should not appear as a change for a file the user did not edit.
* Re-checked on QA build index-C14cwR0U.js, 13 Oct 2026: same 20 / 4 changes. Process Import was not clicked.
```

---

## ZP-4855 — Bulk Edit export/validate/process return 500 for a malformed site id

**Verdict: VALID-CLEAR.** Build index-C14cwR0U.js.

Reproduced (PM): `GET /api/bulk-edit/export?sld_id=not-a-uuid` → 500 `internal_error` (trace a7344ebb…); `POST validate` → 500
(8da24f04…); `POST process` (dummy file) → 500 (1fdfa51c…); empty `sld_id=` → 400 "sld_id query parameter is required"; own site →
200; comparison `POST /api/sld/not-a-uuid/export-skm` → 400 "Invalid SLD ID format". Evidence: `A3-4855-bulk-edit-malformed-id.png`.

Dev push-back risk is low: the ticket already says "no screen hits this" and is Low; ZP-4820 and ZP-4824 (same class) were accepted,
fixed and moved to READY TO RELEASE today. Optional: add "Re-checked on index-C14cwR0U.js".

---

## ZP-4865 — A user not assigned to a site can still change that site's SLD connections and assets

**Verdict: VALID-CLEAR.** Build index-C14cwR0U.js.

Reproduced (EE `+electric@`, is_eg_admin false, role Electrical Engineer, 4 assigned sites, site not among them; has
nodes.manage / edges.manage): site picker "No sites available" (`A4-4865-ee-site-picker-not-offered.png`); `GET
/api/sld/363d5350…/graph` → 422 permission_denied; `PUT /api/edge/update/30fcae76…` `{"edge_manips":null}` → 200; `PUT
/api/node/update/525225bf…` with a `qa_probe` key → 200. After ~9 s the PM (assigned) reads `edge_manips: null` (was
`{"hidden":true}`) and `node_manips.qa_probe = "EE-not-assigned-auditB-13Oct"` (`A5-4865-ee-writes-on-unassigned-site.png`). Both
restored by the PM, verified.

Dev review: steps are click-by-click, the requests and bodies are in the ticket, the attacker is shown as non-staff, a read-refused
control is included, restoration is documented, related tickets are named. API-only is implicit (step 3 shows the app refuses
to load the diagram); one could add the words "only by calling the API directly". Medium is fair. The batch-positions addendum
(ZP-4621 route) was not re-run in this audit (it moves an asset); it was proven earlier today.

Note on my screenshot A5: the "Current site" box shows "QA-DEMO ZP-4800" only because that is the text I typed into the picker
search; the EE did not open the site (A4 shows "No sites available").

---

## ZP-4866 — A Client Portal user can open the Arc Flash Readiness page by typing /arc-flash

**Verdict: VALID-NEEDS-EDIT (small).** Build index-C14cwR0U.js.

Reproduced (CP `+clientportal@`, is_eg_admin false, role Client Portal, 36 permissions, **no** `features.arc_flash.view`; the PM
control has it): menu has no Arc Flash Readiness link (`D1-4866-cp-menu.png`); typed /slds → "Access Denied"
(`D2-4866-cp-typed-slds-denied.png`); typed /equipment-designations → "Access Denied"; typed **/arc-flash → full page** with the
gauges (13 % / 33 % / 0 %), per-class cards, building filter, Engineering Mode toggle and "Export readiness report"
(`D3-4866-cp-typed-arc-flash-opens.png`). The page loads `/api/lookup/nodes/{site}`, `/api/sld/{site}/edges` and
`/api/sld/{site}/library-designations` — all 200 for the CP.

Code check: the bundle's route-guard table has `{path:"/arc-flash",permission:"features.arc_flash.view"}`, but /arc-flash is also
in the layout's direct-render list (`Ko.includes(pathname)`), which is why the guard does not run.

Reason a dev could push back: **the CP already sees "Arc Flash Readiness 13 % — 261 of 2048 fields • 4 unclassified" as a card on
Site Data › Assets** (`D4-4866-cp-assets-page-cards.png`); the card is not clickable for the CP (`D5-…`). So the headline number is
not secret. The real gap is the full page (per-class breakdown, Engineering Mode, report export) behind a permission the role does
not have — a nav-vs-route mismatch. Severity: Medium is arguable; Low-Medium is safer.

Text to add under Notes:

```
* The Client Portal user does see the headline "Arc Flash Readiness 13%" card on Site Data > Assets (not clickable). This ticket is about the full page: per-class breakdown, Engineering Mode, building filter and "Export readiness report", which need features.arc_flash.view.
* While the page is open as the Client Portal user, /api/lookup/nodes/{site}, /api/sld/{site}/edges and /api/sld/{site}/library-designations all answer 200.
* Re-checked on QA build index-C14cwR0U.js, 13 Oct 2026 (also /equipment-designations is correctly denied, so /arc-flash is the odd one out).
```

---

## Owner hints (tickets reassigned to the owner today)

- **ZP-4849** — keep, but edit (API-only wording, copy-paste repro, drop the unproven "update" claim, Medium).
- **ZP-4851** — weakest of the batch. Close, or turn into a Low wording improvement.
- **ZP-4852** — API-only and consistent with the documented back-compat; close as by design unless the owner wants a Low
  "require skm_namespace server-side" hardening task.
- **ZP-4854** — solid; add the file-cell proof so "56 → 56.0" cannot be waved away as a display artefact.

## Real flag flips needed
- `feature-skm-export` → OFF, QA, company Acme (key d59d449b-…), short window, to confirm ZP-4850 without simulation; then back ON.
