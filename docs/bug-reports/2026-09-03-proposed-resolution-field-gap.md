# QA verdict — "proposed_resolution not appearing until Issue refresh" (DevRev)

**Date:** 2026-09-03 · **Environment:** acme.qa.egalvanic.ai V1.36 + **proven on acme.egalvanic.ai prod V2.0** · **Writes:** none (prod config is the owner's own)
**Artifact (the deliverable):** https://claude.ai/code/artifact/867b7806-25d5-44a2-a4f4-07bda67860c9
**Evidence scope:** 1,041 issues · 106 report configs · 866 page templates · 50 issue classes / 84 property definitions

---

## PROVEN ROOT CAUSE (correction added 2026-09-03, later same day)

An end-to-end test on prod settled the one question this verdict had left open, and it **corrects
the framing below.** The customer's report is an HTML/Jinja issue report; for that template kind the
render context comes from the issue-detail query, and **`eg_get_work_order_issue_details` carries
`proposed_resolution`.** Binding `{{ proposed_resolution }}` on the Issues page OUTSIDE the
Recommendations gate made the value render immediately — verified live on prod config `f592f046`
for two issues ("clean tighten torque", "Throw her into the ocean").

So:
- **The customer's entire bug is the dead `{% if "Recommendations" in details_by_key %}` gate.**
  It is a one-line template fix; no backend/query change is needed. The field was in the data all
  along.
- **The `available_fields` whitelist gap below is real but affects only the OLD DOCX templates**, a
  different mechanism from the customer's HTML report. It is NOT the customer's cause. Keeping it as
  a separate low-priority ticket.

Paste-ready tickets reflecting this: [JIRA-TICKETS-proposed-resolution-reporting.md](JIRA-TICKETS-proposed-resolution-reporting.md).
The rest of this doc is the original QA-only investigation, preserved for the evidence trail.

---

## One-line verdict (original — superseded by the correction above)

Two defects merged into one ticket: (1) `proposed_resolution` is missing from every issue report
template's field catalogue on the instance — confirmed; (2) the reporter's own template gates the
resolution column behind `{% if "Recommendations" in details_by_key %}`, and no issue-class
property named "Recommendations" exists (0 of 84 definitions, nothing matches /recommend/i), so the
block can never render — confirmed. The stated mechanism, "missing until the issue is refreshed",
is refuted for the field itself: 530 never-edited issues carry a value.

## The four claims, tested separately

| # | Ticket claim | Verdict | Evidence |
|---|---|---|---|
| 1 | Not a valid option in the report builder | **CONFIRMED** | `available_fields` whitelist on every issue template; 8 issue templates across 106 configs declare it **zero** times; full-text search of every config: zero hits |
| 2 | Report blank where resolutions should be | **Explained, not reproduced** | Fully accounted for by the dead gate (below). Not watchable on QA: both issue-scoped HTML templates are `<h1>New Template</h1>` stubs; every substantive issue template is DOCX → preview refuses ("not HTML; preview unavailable") |
| 3 | Appears only after the issue is refreshed | **MISATTRIBUTED** | Cross-tab of all 1,041 issues: **530 never-edited issues have a non-empty `proposed_resolution`** (631 non-empty total). The record is never stale. What *can* materialise on save is a `details_by_key` KEY (auto-fill rules / name re-join) — i.e. the gate, not the field |
| 4 | Appears in the data viewer | **CONSISTENT** | The viewer shows the raw query row (which has the field); the template context is built from the whitelist. Two surfaces, two sources |

## Root-cause detail

### Defect 1 — hand-maintained whitelist, never extended for this field

```
Default Issue Report (+5 more):  issue_details = id, title, description, severity, status, issue_class_name
Quote Issue Report (+ Copy):     ...the same six + immediate_hazard, customer_notified,
                                 workorder_name, session_name, session_type
```

The Quote variant proves the catalogue is curated by hand and extending it is routine — five extra
fields were added for that report. `proposed_resolution` was never added to any of the eight.

### Defect 2 — the gate that can never pass

```jinja
{% if "Recommendations" in details_by_key %}   ← 0/84 property definitions carry this name
  ...
  {% if proposed_resolution %}<td>{{ proposed_resolution }}</td>{% endif %}   ← unreachable
  ...
{% endif %}
```

Fixing defect 1 alone will not surface the column.

### Bonus defect — the `details_by_key` namespace is polluted (observed live)

Distinct property-name keys harvested from the 1,041 issues' `details[]`:

- `""` — an empty-string key; one class definition ("Test data") also defines a property with an empty name
- `"Current Draw (A)"` **and** `"Current Draw (A) "` (trailing space)
- `"Problem Temp"` **and** `"Problem Temp   "` (three trailing spaces)
- `"Reference Temp"` **and** `"Reference Temp  "`
- `"Replacement check 1"` **and** `"replacement check 1"` (case-only variants, ×3)

Any template keying `details_by_key` on these strings silently misses rows — same failure class the
reporter hit. Recommend trimming/case-folding keys or keying on property id.

### Naming collision

"Proposed Resolution" is BOTH an issue-class property in `details[]` (value e.g. `"Cable"`) AND the
top-level free-text `proposed_resolution` column (e.g. `"Install 1.25\" bushing."`). Different
values on the same issue. The builder gives an author no way to tell them apart.

## Reporting-engine facts established on the way (durable)

- Issue list is `POST /api/v2/issues/list` (`{company_id, page, page_size, filters, search}` →
  `data.items[]`); omit `sld_id` or the set silently shrinks. GET guesses land on the SPA-HTML 200.
- Builder: `/reporting/builder` → config editor `/reporting/config/{id}`. Grid rows open via the
  `data-field="actions"` cell buttons (name-cell clicks are inert).
- Query catalogue: `GET /api/reporting/test-query/queries?version=all` → `queries` (v1: 49) +
  `queries_v2` (124). `resolved_vars` = INPUT parameters, not output columns.
- Issue page shape: `{type: "loop", loop_query: get_issues_list, inner_query: get_issue_details,
  template: issue}`; v2 issue-detail queries are `return_scope: "issue"`, `return_type: "renderable"`.
- Render/preview: `POST /api/reporting/configs/{id}/preview-html` `{primary_id}` → per-page
  `html`/`render_error` inside a 200 `success:true`; a `type:"placeholder"` page with `more_count`
  is loop elision, not a failure.
- Two config generations: old (`templates{}` + `available_fields`, DOCX era) vs new
  (`page_template_id` + `render_query` + `template: *.html`).

## Limits

- The blank render itself was not witnessed (no real HTML issue template exists on this tenant).
- The refresh mechanism for `details_by_key` keys is reasoned (auto_fill_rules / ZP-40 empty-name
  join), not demonstrated. Asking the reporter to dump `details_by_key` keys before/after their
  repro edit would settle it in one step.
- Class definitions are per-tenant; "Recommendations doesn't exist" is proven for acme QA and needs
  the same one-call check on the reporting tenant.
- QA only; not checked on prod/staging.
