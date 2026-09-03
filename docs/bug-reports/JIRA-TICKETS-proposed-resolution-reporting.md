# Jira ticket texts — proposed_resolution / Report Builder (3 tickets)

Source investigation: [2026-09-03-proposed-resolution-field-gap.md](2026-09-03-proposed-resolution-field-gap.md)
Artifact: https://claude.ai/code/artifact/867b7806-25d5-44a2-a4f4-07bda67860c9
All evidence gathered live on acme.qa.egalvanic.ai (V1.36), 2026-09-03. Read-only.

---

## Bug 1 — [Reporting] `proposed_resolution` is missing from the issue report field catalogue / render context

**Description**
Customer reports that `proposed_resolution` does not appear as a valid option in the Report
Builder, even though the value is present in issue data and visible in the data viewer.

Confirmed. The field is not exposed to issue report templates anywhere:

- 0 of 8 issue report templates declare it in `available_fields.issue_details`
- The string `proposed_resolution` appears **zero times across all 106 report configs**
- The data viewer shows it because the viewer displays the raw query row; the template
  context is built from the declared catalogue — two surfaces, two sources

The catalogue is hand-curated and extending it is routine: the Quote Issue Report template
already carries five extra issue fields (`immediate_hazard, customer_notified, workorder_name,
session_name, session_type`) that the other six issue templates lack. `proposed_resolution`
was simply never added.

**Steps to Reproduce**
1. (UI) Reporting → Report Builder → open any Issue-type config (e.g. "Default Issue Report",
   via the row's actions button → `/reporting/config/{id}`).
2. Observe the VARIABLES panel: it offers only user-defined variables ("Include IR Photos",
   BOOL) — no issue data fields are offered anywhere in the editor.
3. (API, definitive) `GET /api/reporting/configs/{id}` → inspect
   `config.templates.issue.available_fields.issue_details` =
   `id, title, description, severity, status, issue_class_name`.

**Actual:** `proposed_resolution` is not available to issue report templates.
**Expected:** it is offered in the field catalogue and reaches the render context.

**Where the fix goes — two cases (both needed):**
- **Old-generation configs** (`config.templates{}` + `available_fields` — all 8 current issue
  templates, DOCX): add `proposed_resolution` to `available_fields.issue_details`, same edit
  as the Quote Issue Report precedent. All eight, not just one tenant's.
- **New-generation configs** (`pages[]` + `page_template_id` + `render_query` — the customer's
  HTML template is this kind, rendered per-issue with `render_scope: "issue"`): the context is
  the output of the issue-detail query (`get_issue_details` /
  `eg_get_work_order_issue_details`), so the column must be in that query's projection.

**Open question for the implementer:** whether `available_fields` also filters the render
context or is builder-display-only could NOT be confirmed on QA (both issue-scoped HTML
templates there are unwritten stubs; all substantive issue templates are DOCX, which
`preview-html` refuses). Confirm which layer gates the context before shipping.

**Also required:** label the new option distinctly. "Proposed Resolution" already exists as an
issue-class property inside `details[]` holding a DIFFERENT value on the same issue (enum-ish,
e.g. "Cable") than the top-level free-text column (e.g. "Install 1.25\" bushing."). Two
identically-named picker entries would recreate this confusion.

**Impact:** authored issue resolutions cannot be included in reports. No workaround — the
class property in `details_by_key` is a different field with a different value.

**Evidence:** artifact page (screenshots of the config editor incl. the VARIABLES panel);
631/1,041 issues on QA carry a non-empty value, so the field is real and populated.

---

## Bug 2 — [Reporting] Recommendations section never renders: template gates on a key that does not exist

**Description**
The customer's issue page template wraps the Recommendations/Resolution table inside:

```jinja
{% if "Recommendations" in details_by_key %}
  ...
  {% if proposed_resolution %}<td>{{ proposed_resolution }}</td>{% endif %}   ← unreachable
  ...
{% endif %}
```

On the QA tenant, **no issue-class property is named "Recommendations"** — 0 of 84 property
definitions across all 50 classes; nothing matches /recommend/i. The gate is therefore false
for every issue, and the whole section — including the `proposed_resolution` cell — is
skipped. Fixing Bug 1 alone will NOT surface the column while this gate stands.

**Tenant scoping (important):** class definitions are per-tenant. The 0/84 result is proven
for acme QA; run the same one-call check on the affected customer tenant before closing:
`GET /api/issue_classes` → search `definition[].name` for "Recommendations". The nearest
historical property name on record (ZP-1404) is "Recommendations For Repair", which still
fails the exact-match test.

**Steps to Reproduce** (on the affected tenant — NOT reproducible on QA, where both
issue-scoped HTML templates are placeholder stubs and all substantive issue templates are DOCX)
1. Take an issue with a populated `proposed_resolution`.
2. Render the affected report/template for it.
3. Recommendations/Resolution section is absent.

**Actual:** the section never renders, so resolution data appears missing even when present.
**Expected:** the resolution renders whenever the issue has one, independent of an unrelated
Recommendations detail.

**Why "it appears after I edit the issue" (the customer's observation):**
`proposed_resolution` itself is never stale — on QA, 530 of 1,041 issues were never edited
after creation and still hold a value. What CAN change on save is the KEY SET of
`details_by_key`: (a) auto-fill rules fire on save and write other class properties, creating
detail rows/keys that didn't exist; (b) detail rows with empty/stale names are re-joined to
current property names on save (the ZP-40 family), materialising a key. Either makes the gate
start passing after an edit — which reads exactly like "the field appeared after refresh".
**Discriminating ask for the customer:** dump `details_by_key`'s keys immediately before and
after their repro edit; the diff settles the mechanism in one observation.

**Fix**
1. Move the `proposed_resolution` cell out of the Recommendations gate (render it whenever
   non-empty).
2. Establish the intended key and correct the gate; on QA no candidate key exists at all.
3. Product hardening (optional but cheap): the template editor could warn when a
   `details_by_key` gate references a name that matches no property definition on the tenant —
   this class of dead gate is invisible today.

**Ownership note:** this is template *content* (report authoring), distinct from Bug 1
(platform field catalogue / query projection).

---

## Bug 3 — [Platform] `details_by_key` keys are unnormalised: empty, whitespace- and case-variant property names silently break template lookups

**Description**
`details_by_key` is keyed on issue-class property NAMES — free text typed by whoever
configured the class. Harvesting every distinct property name across all 1,041 issues' live
`details[]` on QA shows the keyspace is already polluted:

| Observed key | Collides with |
|---|---|
| `""` (empty string) | — (the "Test data" class also defines a property with no name) |
| `Current Draw (A)` | `Current Draw (A)␣` |
| `Problem Temp` | `Problem Temp␣␣␣` |
| `Reference Temp` | `Reference Temp␣␣` |
| `Replacement check 1/2/3` | `replacement check 1/2/3` (case-only) |

A template keying `details_by_key["Reference Temp"]` silently misses every issue stored under
`"Reference Temp  "` — blank cell, no error, no warning. This is the same failure class as
Bug 2 and will keep generating "field randomly missing from report" tickets until normalised.

**Fix**
- Normalise lookup keys (trim + case-fold), or key `details_by_key` on property **id** with a
  name alias map.
- Validation: reject empty property names and names that collide after trim/case-fold when
  saving an issue-class definition.

**Evidence:** live key harvest, 2026-09-03, acme QA; class-definition scan
(`GET /api/issue_classes`, 50 classes / 84 properties) confirms one unnamed property.
