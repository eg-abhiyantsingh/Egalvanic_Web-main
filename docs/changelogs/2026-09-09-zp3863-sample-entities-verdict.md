# ZP-3863 — sample-entity picker for report authoring

**Date:** 2026-09-09
**Prompt:** ZP-3863 ticket text (`GET /reporting/sample-entities`) + the ticket's QA Review steps
**Env:** `acme.qa.egalvanic.ai` · V1.36 · tenant acme · read-only (GET only)

## What was asked

Test the new sample-entity picker behind ZP-3863 — *"[Web] Report page authoring sampled the newest
session, so IR pages rendered empty and looked like broken templates"* — against the eight QA Review
steps in the ticket.

## Verdict

**Six of eight steps pass. Two fail**, and one fails in exactly the case the ticket is named after.

| Step | Result |
|---|---|
| 1 · candidates carry work type + per-entity signal | PASS |
| 1b · `name_contains` case-insensitive | PASS |
| 2 · `work_type` nudges the sort without hard-filtering | **FAIL** (Defect A) |
| 3 · `query_name` changes the reported signal | **FAIL** (Defect B) |
| 4 · zero-signal candidate called out, not hidden | PASS |
| 5 · no entity from an unmapped site appears | not runnable as written; substitutes pass |
| 6 · negatives fail cleanly | PASS |
| 8 · `tests/test_company_scope.py` on dev | blocked — no backend repo access |

### Defect A — the IR request returns no IR work orders, and claims none exist

`GET /reporting/sample-entities?entity_type=session&work_type=IR` at the **documented default limit of
10** returns **zero IR work orders**, plus a note: *"None carry work type 'IR' — most entities have no
work type set at all. Judge from the names instead…"*.

The note is false. The identical request at `limit=25` lifts **4 IR work orders** to the top (stable over
5 runs; same via `config_type=session`). The match count grows monotonically with `limit` — IR first
appears at 16, IR Checklist at 13 — so the preference lifts only matches that fall inside a signal-ranked
window whose depth scales with `limit`. Every IR work order on this tenant has signal 0–1, so a shallow
window never reaches them.

Not IR-specific: `COM` returns 1 match at limit 10 and 6 at limit 25. `PM Forms`/`AF` mask the bug
because they have high-signal members.

Impact: an author asks for IR, is told IR work orders don't exist, picks a non-IR session, and gets an
empty IR page that looks like a broken template — verbatim the failure this ticket was filed to remove.

### Defect B — 4 of 7 real session queries silently ignore `query_name`

`_with_issues` → `issue`, `_with_tasks` → `task`, `_with_eg_forms` → `eg_form` all work. But
`_with_nodes`, `_with_forms`, `_with_neta_forms` and `_with_neta_forms_for_class` (all real v2 queries per
`list_report_queries`) fall back to the default `tasks+issues` ranking, returning signals identical to an
invented query name. `_with_nodes` is the engine's own *"Work orders with at least one asset"* — the
asset-page case named in the ticket's step 3.

### Step 5 — why it could not be run as written

No QA seat is both site-restricted and able to call the endpoint: FM (11 sites) and Electrical Engineer
(4 sites) both get a clean **422 `permission_denied`, `required_permission: reports.view`**; the seats
that hold `reports.view` (PM 114, Technician 113, Account Manager 113, Client Portal 234) are all
effectively unrestricted. Substitute checks, all passing:

- all 25 candidates for each of those four seats resolve to a site inside that seat's own
  `accessible_sld_ids` — 0 out-of-scope, 0 unresolved (all on QA);
- `company_id` / `company` / `subdomain` / `sld_id` / `mapping_user_sld` are ignored — returned id list
  *identical* to the unparameterised baseline, not merely the same length. No foreign tenant contacted;
- the staff-seat comparison originally cited here is retracted (see Corrections) — it was prod data.

Closing step 5 properly needs a seat with `reports.view` and a 2–3 site mapping. No user was created or
modified to manufacture one.

## Adjacent observation (not a ZP-3863 defect)

Client Portal reports **234 mapped sites — more than PM's 114 — and holds `reports.view`**, so it can call
this authoring endpoint. Possibly correct for this tenant's portal mapping; recorded as a lead for
whoever owns portal scoping.

## Corrections made during the run

- An earlier probe concluded `work_type=IR` returned **zero rows at every limit**. That was wrong — it
  came from probing only small limits. The behaviour is limit-dependent, and the corrected finding
  (threshold at 16) is what the verdict reports.
- An earlier hypothesis that matching keys off `session.type` vs `work_type_id` was refuted: both matching
  and non-matching entities have `type = None` and only a `work_type_id`.
- **Retracted an evidence claim.** I first read the staff seat's 6 IR work orders vs the PM seat's 4 as a
  per-caller *scope* difference and cited it as step-5 evidence. The staff MCP is actually pointed at
  **production** (acme `0a61e613…` + `eg-pz-prod-s3-branding-ohio`, vs QA's `d59d449b…`), so those are two
  different environments and the comparison proves nothing about scoping. Removed from step 5. It does
  establish something better: the Defect A behaviour reproduces on prod as well.

## Deliverables

- Verdict — `docs/bug-reports/2026-09-09-QA-ZP-3863-sample-entities-verdict.md`
- Artifact — https://claude.ai/code/artifact/4406f190-0c4c-4183-abf3-eb50225d13a9
- Evidence — `docs/bug-evidence/zp-3863-sample-entities/` (raw responses for every step + 2 UI screenshots)

## Footprint

Read-only. Every call a GET; no work order, user, site or report config created, modified or deleted, and
no foreign tenant contacted. The Work Orders grid was filtered in the UI only.
