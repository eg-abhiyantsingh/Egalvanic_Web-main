# Work order types and the EG Forms instance API

**Verified:** 2026-08-11 against QA **V1.36**, tenant `acme.qa.egalvanic.ai`.
Origin: eg-pz-frontend **#1107** (EG Forms addable + fillable on every work order type).

## Work orders live at `/sessions`

The list page is `/sessions` ("Work Orders"), fed by **`POST /api/company/{companyId}/workorders/v2`**
with body `{page, page_size}`. Envelope is **`{data: {items: [...], total}}`** — note `data.items`,
not `data`. 1248 work orders on QA at time of writing.

A work order's type is **`work_type_id`**, a foreign key into `GET /api/procedures-v2/services`
(envelope `{data: [...]}`), where each service carries a **`type`** field. `work_type_id: null`
means **General**.

QA service-type inventory:

| `type` | Services | Example |
|---|---|---|
| PM Forms | 10 | NETA Testing, UPS Maintenance, Cleaning |
| AF | 3 | Arc Flash Data Collection |
| Checklist | 1 | Arc Flash Label Placement |
| COM | 1 | Condition Assessment |
| IR | 1 | Infrared Thermography |
| Schedule | 1 | Panel Schedule Updates |

Tabs differ per type, which is how you recognise one without looking up its service:

| Type | Distinguishing tab |
|---|---|
| General | Tasks + IR Photos |
| PM Forms | (none — Assets/Forms/Issues/Attachments only) |
| AF | SLD + Equipment Designations |
| IR | IR Photos |
| Checklist | Tasks + IR Photos |
| Schedule | Panel Schedules |
| COM | Condition Assessment |

### Trap: "no context menu" usually means "no assets"

A work order with **zero assets** has no data row to right-click, so the row context menu appears
empty and looks like a gating bug. **Check the Assets tab badge first** — types that work show a
count ("Assets 45"); an empty one shows a bare "Assets". Several AF and PM Forms work orders on QA
have no assets at all. This produced a false "context menu missing on AF/PM Forms" reading on the
first pass of #1107 testing.

## EG Forms instance API

| Purpose | Endpoint |
|---|---|
| Instances in a work order | `GET /api/eg-form-instance/by-session/{sessionId}` |
| Count badge | `GET /api/eg-form-instance/by-session/{sessionId}/count` → `{open, total}` |
| Per-asset status (drives the context menu) | `GET /api/eg-form-instance/by-session/{sessionId}/node-status` |
| Instances for one asset | `GET /api/eg-form-instance/by-session/{sid}/by-node/{nodeId}` |
| Forms attachable to an asset | `GET /api/eg-form-instance/available-for-node/{nodeId}` |
| Attach | `POST /api/eg-form-instance/create-for-asset` |
| Save answers | `PUT /api/eg-form-instance/{id}` |
| Delete | `PUT /api/eg-form-instance/{id}/delete` — **PUT, not DELETE** |

`DELETE /api/eg-form-instance/{id}` returns **405**. The instance payload embeds the whole form
`definition` (blocks/containers), so responses are large.

### The gating rules (post-#1107)

- **Tab visibility** — every work type. Disabled only when the company lacks `feature-eg-forms`.
- **Context menu** — `Add Form` on every work type; `Open Forms (n)` and `Copy Form Data To…`
  appear only when the asset's form count is non-zero (from `node-status`), *independent of work type*.
- **Forms column** (`data-field="forms_status"`) — appears on non-PM-Forms work orders **only when
  at least one asset carries forms**. Absent otherwise, which is correct, not a bug.
- **Row click** is NOT the forms affordance — it opens the asset drawer / IR photos. Forms open
  from the Forms cell or the context menu.

### Trap: the form viewer is a MuiDrawer, not a dialog

`EGFormInstanceDialog` renders as `.MuiDrawer-paper`, so `document.querySelector('[role="dialog"]')`
returns nothing and it looks like the viewer failed to open. Locate it by content instead —
`[...document.querySelectorAll('.MuiDrawer-paper')].find(p => /Save Draft|Submit/.test(p.innerText))`.
Note the sidebar nav is also a `.MuiDrawer-paper`, so always filter by content.

**Delete lives inside the viewer**, as an icon button with `aria-label="Delete this form"` — not in
the Forms-tab row menu (which offers only *Copy Data To…*) and not in the assets context menu.
Concluding "forms can't be detached" from those two menus alone would be wrong.

## Known defect — stale count after delete (EGF-1, 2026-08-11)

Attaching a form refreshes the tab badge (add fires `…/count` + `…/node-status`); **deleting does
not** (only its `PUT`). The grid empties but the tab still shows the old count, and stays stale
indefinitely — a page reload is required. Same family as #1077's Equipment Designations staleness.

## Feature flags come from LaunchDarkly, client-side

`POST /api/features/sync` is a *push* endpoint (it returned
`{"pushed": false, "reason": "ld_api_token_not_configured"}` on QA) — it is **not** the flag list.
The real evaluation is:

```
GET https://app.launchdarkly.com/sdk/evalx/{clientSideId}/contexts/{base64-context}
```

where the context base64-decodes to `{"kind":"organization","key":"<companyId>","name":"EG-ACME"}`.
EG-ACME has **35 flags, all true**, including `feature-eg-forms`.

**To test a flag-off state without a second tenant**, intercept that response and rewrite the value,
and abort `clientstream.launchdarkly.com` so the real value cannot be streamed back:

```js
await page.route('**/app.launchdarkly.com/sdk/evalx/**', async route => {
  const resp = await route.fetch();
  const json = await resp.json();
  json['feature-eg-forms'].value = false;
  await route.fulfill({ response: resp, body: JSON.stringify(json), contentType: 'application/json' });
});
await page.route('**/clientstream.launchdarkly.com/**', r => r.abort());
```

Always pair it with the un-intercepted control, or you have only shown that *something* changed.

## Build-identity ambiguity

The sidebar badge reads **V1.36** while the in-app release panel advertises **"Fixes in Web
v1.39.1"**. Do not trust either alone when deciding whether a PR has shipped — **test the behaviour
or the endpoint**. The #1107 ticket said "dev only, not yet in QA" and was wrong; the deciding
evidence was `SessionEGFormsTab` issuing `by-session/{id}` on an IR work order.
