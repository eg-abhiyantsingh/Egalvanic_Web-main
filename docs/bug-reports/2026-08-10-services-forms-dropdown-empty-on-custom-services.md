# [Services] Forms dropdown is permanently empty on any company-created service

**Env:** QA V1.36 · **Found:** 2026-08-10 · **Severity:** Medium · **Priority:** Medium
**Surface:** `/services/{id}` → Asset class → procedure → Edit → **Forms**
**Reported case:** `/services/e40d05ec-9108-42fa-96a2-392b9e537e95` ("abhiyant cortniess"),
procedure "ats Procedure" — Forms dropdown shows "No options".

## Verdict on the reported case: behaving as specified

Eric's rule — *"It should only be forms owned by the service (eg form service id)"* — is being
applied correctly. **Zero forms on this tenant carry this service's id**, so there is nothing to
list. Not a filter bug.

## The real issue: only GLOBAL services can own forms

| Measure | Value |
|---|---|
| EG Forms on tenant | 344 |
| …carrying a `service_id` | **42** |
| …carrying **no** `service_id` | 302 |
| Of those 42, how many are `is_global: true` | **42 (all of them)** |
| Distinct services owning any form | **4** |
| Services on tenant | 17 (**13 global**, **4 company-created**) |
| Company-created services owning any form | **0** |

The four services that own forms are all global catalog entries with **deterministic UUID v5**
ids: **Cleaning** (21 forms), **NETA Testing** (16), **Insulation Resistance Testing** (4),
**UPS Maintenance** (1).

The reported service is **company-created** — `is_global: false`, `company_id` set, **UUID v4**.

**Consequence:** every company-created service shows an empty Forms dropdown, and there is no
visible in-product path to attach a form to one. Creating a custom service is the normal
workflow, so this is the default experience, not an edge case. Even among *global* services,
only 4 of 13 have any forms.

## Secondary UX issue (visible in the report screenshot)

The Forms field renders with a **red/error border** and the placeholder **"No forms recorded"**
while the open listbox reads "No options". An empty-by-design state is styled as a validation
failure, which is why this looked broken rather than simply empty. Suggested: neutral styling
plus explanatory text — e.g. *"No forms are attached to this service"* — and, if custom services
genuinely cannot own forms, hide or disable the field with that reason.

## Questions for the author

1. Should a **company-created** service be able to own forms? If yes, the attach path is missing
   (or not surfaced) — that is the actual defect.
2. If form→service ownership is intended to exist only on the global catalog, should the Forms
   field appear at all on custom services?
3. Is the red border intentional here, or is the control being treated as invalid-when-empty?

## Reproduce

```js
const forms = await (await fetch('/api/eg-forms', {credentials:'include'})).json();
const rows  = forms.data || forms;
rows.filter(f => f.service_id === 'e40d05ec-9108-42fa-96a2-392b9e537e95').length; // 0
rows.filter(f => f.service_id).every(f => f.is_global);                            // true
```

> Note: use the **unpaginated** endpoint — the paginated shape omits fields (see
> `2026-08-10-eg-forms-paginated-list-omits-definition.md`).

## Prediction worth confirming manually (2 min)

Open a procedure on a **global** service that owns forms — e.g. NETA Testing
(`0d914f81-a750-5833-8c46-5c71064f676e`, 16 forms). If the dropdown lists those 16, the filter
is confirmed working end-to-end and this is purely a data/product-model question. I verified the
form→service data but did not complete this UI control.
