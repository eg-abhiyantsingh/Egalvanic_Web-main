# Jira ticket texts — proposed_resolution / Report Builder

Source investigation: [2026-09-03-proposed-resolution-field-gap.md](2026-09-03-proposed-resolution-field-gap.md)
Artifact: https://claude.ai/code/artifact/867b7806-25d5-44a2-a4f4-07bda67860c9
Evidence on acme.qa.egalvanic.ai (V1.36) and **proven end-to-end on acme.egalvanic.ai (prod, V2.0)** 2026-09-03.

> **ROOT CAUSE — PROVEN (updated 2026-09-03).** The customer's report is an HTML/Jinja issue
> report. For that template kind the render context comes from the issue-detail query, and
> `eg_get_work_order_issue_details` **does** carry `proposed_resolution`. Proven on prod: binding
> `{{ proposed_resolution }}` on the Issues page **outside** the Recommendations gate makes the
> value render immediately — "clean tighten torque" and "Throw her into the ocean" both printed for
> issues that have them. **The customer's entire bug is Bug A below (the dead gate). It is a
> one-line template fix; no backend/query change is needed.** The `available_fields` whitelist gap
> (old Bug 1, now Bug C) is a real but SEPARATE issue affecting only the old DOCX templates — it is
> NOT the customer's cause. Keep it as a lower-priority hardening ticket.

---

## Bug A — [Reporting] Proposed Resolution never renders: template gates it behind a "Recommendations" key that never exists  ← THE CUSTOMER'S BUG

**Priority:** the actual fix. One-line template change.

**Description**
The customer's issue page template only outputs `proposed_resolution` from inside:

```jinja
{% if "Recommendations" in details_by_key %}
  ...
  {% if proposed_resolution %}<td>{{ proposed_resolution }}</td>{% endif %}   ← unreachable
  ...
{% endif %}
```

`details_by_key` is keyed on issue-class property NAMES, and **no issue class has a property named
"Recommendations"** — 0 of 577 property definitions on prod (0 of 84 on QA); nothing matches
`/recommend/i`. So the outer `if` is false for every issue, the whole block is skipped, and the
resolution never prints — even though the issue has one and the field is in the render context.

This also explains the customer's "appears only after I refresh the issue": the field itself is
never stale (verified — 530 never-edited issues on QA carry a value), but editing an issue can add
a KEY to `details_by_key` (auto-fill rules write other class properties on save; and empty/stale
detail names get re-joined to current names on save — the ZP-40 family). A newly-present key can
make the gate pass, which reads as "the resolution appeared after I changed something."

**Proven fix (verified live on prod, config `f592f046`):**
Move the resolution out of the Recommendations gate — output it directly on the Issues page:

```jinja
{% if proposed_resolution %}
  <div class="field"><span class="label">Proposed Resolution</span>{{ proposed_resolution }}</div>
{% endif %}
```

Rendered against work orders whose issues have resolutions, this prints the value every time
("Proposed Resolution: clean tighten torque", "Proposed Resolution: Throw her into the ocean").

**Steps to Reproduce**
1. Issue report (HTML) whose issue template references `proposed_resolution` only inside a
   `{% if "Recommendations" in details_by_key %}` block.
2. Render for an issue that has a `proposed_resolution` but whose class has no "Recommendations"
   property (i.e. every issue on acme).
3. The resolution is absent.

**Actual:** resolution never renders. **Expected:** renders whenever the issue has one.

**Product hardening (optional, cheap):** the template editor could warn when a `details_by_key`
gate references a name that matches no property definition on the tenant — this class of dead gate
is invisible today.

---

## Bug B — [Platform] `details_by_key` keys are unnormalised: empty, whitespace- and case-variant property names silently break template lookups

**Description**
`details_by_key` is keyed on issue-class property NAMES — free text typed when a class is
configured. Harvested across all live issues, the keyspace is already polluted:

- Empty-string key `""` (and classes that define a property with no name at all — 1 on QA)
- `Current Draw (A)` vs `Current Draw (A)␣` · `Problem Temp` vs `Problem Temp␣␣␣` ·
  `Reference Temp` vs `Reference Temp␣␣`
- `Replacement check 1/2/3` vs `replacement check 1/2/3` (case-only)
- On prod: `Problem Location` vs `Problem Location␣`, `Approved for Repair at Time of Visit` vs
  `…Visit?`

A template keying `details_by_key["Reference Temp"]` silently misses every issue stored under
`"Reference Temp  "` — blank cell, no error. Same failure class as Bug A; will keep producing
"field randomly missing from report" tickets.

**Fix**
- Normalise lookup keys (trim + case-fold), or key `details_by_key` on property **id** with a name
  alias map.
- Validation: reject empty property names and names that collide after trim/case-fold when saving a
  class definition.

---

## Bug C — [Reporting] DOCX issue templates' `available_fields` whitelist omits `proposed_resolution` (lower priority; NOT the customer's cause)

**Description**
The OLD-generation issue report templates (`config.templates{}` with `s3_key: *.docx` +
`available_fields`) declare a hand-curated field whitelist. `proposed_resolution` is in none of
them — 0 of 8 issue templates across 106 configs. The whitelist is maintained by hand (the Quote
Issue Report variant already had `immediate_hazard, customer_notified, workorder_name,
session_name, session_type` added), so extending it is routine.

**Important scope note:** this whitelist governs the OLD DOCX generation only. The customer's report
is the NEW HTML/Jinja generation, whose context comes from the query (which carries the field — see
Bug A). So this ticket does NOT fix the customer's report and must not be conflated with it. File it
only so DOCX issue reports can also surface the field; low priority.

**Fix:** add `proposed_resolution` to `available_fields.issue_details` on the DOCX issue templates.
