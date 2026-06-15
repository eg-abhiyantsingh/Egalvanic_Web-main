# RBAC Permission-Matrix — Test Case Catalog

Source of truth: `testcase/prod_permissions-by-role_202606151113.csv` (7 roles, 113 distinct
permissions, 555 grants). Automated by `RolePermissionMatrixCellTest` (per-cell) +
`RoleBasedPermissionContractTest` (role-level) — see `testng-rbac-permissions.xml`.

**Full machine-readable list of every test case: [`RBAC_TEST_CASES.csv`](RBAC_TEST_CASES.csv)** (799 rows).

## Totals

| Layer | Test cases | What it asserts |
|-------|-----------:|-----------------|
| Per-cell (`cell`) | 791 | Each role×permission: granted ⇒ present in `/auth/me`; denied ⇒ absent |
| Role contract (`role-contract`) | 7 | Whole live set == matrix (identity + no-escalation + completeness + `has_web_access`/`is_admin`) |
| CSV integrity (`csv-integrity`) | 1 | Matrix file parses to the pinned snapshot (7 roles / 555 grants / 113 perms / role_ids) |
| **Total** | **799** | |

Per-cell split: **555 granted** (must be present) + **236 denied** (must be absent).

## Current live status (acme.qa)

**682 PASS / 117 SKIP / 0 FAIL.** Skips = Electrical Engineer (113 cells + 1 contract — no QA
account yet) + 3 documented prod-vs-QA drift cells (Admin/PM/FM).

| Role | role_id (matrix) | granted | denied | live status |
|------|------------------|--------:|-------:|-------------|
| Admin | `b60006dd-…` | 98 | 15 | live ✓ |
| Project Manager | `242dbe6a-…` | 93 | 20 | live ✓ (1 drift skip) |
| Technician | `e84a0fbb-…` | 90 | 23 | live ✓ |
| Electrical Engineer | `fd6b624e-…` | 83 | 30 | SKIP — no QA account |
| Facility Manager | `54021b71-…` | 77 | 36 | live ✓ (1 drift skip) |
| Account Manager | `92f38105-…` | 77 | 36 | live ✓ (QA id 392a2233…) |
| Client Portal | `2a85145f-…` | 37 | 76 | live ✓ |

## Assertion types (the 3 ways a cell/role can be checked)

- **GRANTED cell** → permission MUST appear in the role's live `/auth/me` `permissions[]`. Absent ⇒ FAIL (missing grant), unless it is documented prod-vs-QA drift ⇒ SKIP.
- **DENIED cell** → permission MUST NOT appear. Present ⇒ FAIL (privilege escalation).
- **Role contract** → the entire live set equals the matrix, the `roles[0].id/name` match, and `has_web_access`/`is_admin` agree with the grants.

## Documented exceptions (not product bugs)

- **Prod-vs-QA drift** (granted in prod CSV, absent on QA tenant) → those 3 GRANTED cells SKIP: Admin `features.equipment_insights.view`, Project Manager `accounts.view`, Facility Manager `features.locations.view`.
- **QA role_id override** — Account Manager's QA UUID (`392a2233-…`) differs from the prod CSV (`92f38105-…`); same name + identical 77 permissions. Identity check expects the QA id.
- **Unprovisioned** — Electrical Engineer has no QA login (401); its 113 cells + contract SKIP until the account exists.

## Run

```
mvn -o test -DsuiteXmlFile=testng-rbac-permissions.xml
```

## Per-role granted permissions (exactly what each role is tested to HAVE)

### Admin (98 granted)

accounts.manage,accounts.view attachments.manage,attachments.manage_internal attachments.manage_public,attachments.view attachments.view_internal,attachments.view_public bulk_operations.manage,bulk_operations.view company_data.manage,company_data.view contacts.manage,contacts.view data.export,data.import devices.manage,devices.view edge_classes.manage,edge_classes.view edges.manage,edges.view eqp_lib.approve,features.arc_flash.view features.assets.view,features.attachments.view features.audit_log.view,features.condition_assessment.view features.connections.view,features.equipment_insights.view features.goals.manage,features.issues.view features.jobs.view,features.locations.view features.nfpa70e.view,features.opportunities.view features.panel_schedules.view,features.planning.view features.scheduling.view,features.settings.classes.view features.settings.forms.view,features.settings.pm.view features.settings.sites.view,features.settings.users.view features.settings.view,features.site_overview.view features.site_visits.view,features.slds.view features.tasks.view,form_instances.manage form_instances.view,forms.manage forms.view,issue_classes.manage issue_classes.view,issues.manage issues.view,jobs.manage jobs.view,locations.manage locations.view,mappings.manage mappings.view,mutations.view node_classes.manage,node_classes.view nodes.manage,nodes.view opportunities.manage,opportunities.view photos.manage,photos.view platform.mobile,platform.web procedures.manage,procedures.view quotes.approve,quotes.manage quotes.view,reports.generate reports.manage,reports.view sessions.manage,sessions.view settings.manage,settings.view shortcuts.manage,shortcuts.view slds.manage,slds.share slds.view,tasks.assign tasks.manage,tasks.view users.manage,users.view workorders.manage,workorders.view

### Project Manager (93 granted)

accounts.manage,accounts.view attachments.delete,attachments.manage attachments.manage_internal,attachments.manage_public attachments.move,attachments.update attachments.upload,attachments.view attachments.view_internal,attachments.view_public bulk_operations.manage,bulk_operations.view company_data.view,contacts.manage contacts.view,data.export data.import,devices.manage devices.view,edge_classes.manage edge_classes.view,edges.manage edges.view,features.arc_flash.view features.assets.view,features.attachments.view features.audit_log.view,features.condition_assessment.view features.connections.view,features.goals.manage features.issues.view,features.jobs.view features.locations.view,features.nfpa70e.view features.opsdb.view,features.panel_schedules.view features.scheduling.view,features.settings.pm.view features.settings.view,features.site_visits.view features.slds.view,features.tasks.view folders.manage,form_instances.manage form_instances.view,forms.manage forms.view,ir_photos.delete ir_photos.upload,issue_classes.manage issue_classes.view,issues.manage issues.view,jobs.manage jobs.view,locations.manage locations.view,mappings.manage mappings.view,node_classes.manage node_classes.view,nodes.manage nodes.view,opportunities.manage opportunities.view,photos.manage photos.view,platform.mobile platform.web,procedures.manage procedures.view,quotes.approve quotes.manage,quotes.view reports.generate,reports.manage reports.view,sessions.manage sessions.view,shortcuts.manage shortcuts.view,slds.manage slds.share,slds.view tasks.assign,tasks.manage tasks.view,users.manage users.view,workorders.manage workorders.view

### Technician (90 granted)

accounts.manage,accounts.view attachments.delete,attachments.manage attachments.manage_internal,attachments.manage_public attachments.move,attachments.update attachments.upload,attachments.view attachments.view_internal,attachments.view_public bulk_operations.manage,bulk_operations.view company_data.view,contacts.manage contacts.view,data.export data.import,devices.manage devices.view,edge_classes.manage edge_classes.view,edges.manage edges.view,features.accounts.view features.arc_flash.view,features.assets.view features.attachments.view,features.condition_assessment.view features.connections.view,features.equipment_insights.view features.issues.view,features.jobs.view features.locations.view,features.nfpa70e.view features.opportunities.view,features.planning.view features.scheduling.view,features.site_overview.view features.site_visits.view,features.slds.view features.tasks.view,folders.manage form_instances.manage,form_instances.view forms.manage,forms.view ir_photos.delete,ir_photos.upload issue_classes.manage,issue_classes.view issues.manage,issues.view jobs.manage,jobs.view locations.manage,locations.view mappings.manage,mappings.view node_classes.manage,node_classes.view nodes.manage,nodes.view opportunities.manage,opportunities.view photos.manage,photos.view platform.mobile,procedures.manage procedures.view,quotes.approve quotes.manage,quotes.view reports.generate,reports.manage reports.view,sessions.manage sessions.view,shortcuts.manage shortcuts.view,slds.manage slds.share,slds.view tasks.assign,tasks.manage tasks.view,users.view workorders.manage,workorders.view

### Electrical Engineer (83 granted)

accounts.manage,attachments.manage attachments.manage_internal,attachments.manage_public attachments.view,attachments.view_internal attachments.view_public,bulk_operations.manage bulk_operations.view,company_data.view contacts.manage,contacts.view data.export,data.import devices.manage,devices.view edge_classes.manage,edge_classes.view edges.manage,edges.view features.arc_flash.view,features.assets.view features.attachments.view,features.audit_log.view features.connections.view,features.equipment_designations.view features.equipment_library.view,features.goals.manage features.issues.view,features.locations.view features.nfpa70e.view,features.panel_schedules.view features.settings.classes.view,features.settings.view features.site_overview.view,features.slds.view features.tasks.view,form_instances.manage form_instances.view,forms.manage forms.view,issue_classes.manage issue_classes.view,issues.manage issues.view,jobs.manage jobs.view,locations.manage locations.view,mappings.manage mappings.view,node_classes.manage node_classes.view,nodes.manage nodes.view,opportunities.manage opportunities.view,photos.manage photos.view,platform.mobile platform.web,procedures.manage procedures.view,quotes.approve quotes.manage,quotes.view reports.generate,reports.manage reports.view,sessions.manage sessions.view,shortcuts.manage shortcuts.view,slds.manage slds.share,slds.view tasks.assign,tasks.manage tasks.view,users.manage users.view,workorders.manage workorders.view

### Facility Manager (77 granted)

attachments.manage,attachments.manage_public attachments.upload,attachments.view attachments.view_public,bulk_operations.manage bulk_operations.view,company_data.view contacts.manage,contacts.view data.export,data.import devices.manage,devices.view edge_classes.manage,edge_classes.view edges.manage,edges.view features.arc_flash.view,features.assets.view features.attachments.view,features.condition_assessment.view features.connections.view,features.equipment_designations.view features.issues.view,features.jobs.view features.locations.view,features.nfpa70e.view features.planning.view,features.scheduling.view features.site_overview.view,features.site_visits.view features.slds.view,features.tasks.view folders.manage,form_instances.manage form_instances.view,forms.manage forms.view,ir_photos.upload issue_classes.manage,issue_classes.view issues.manage,issues.view jobs.manage,jobs.view locations.manage,locations.view mappings.manage,mappings.view node_classes.manage,node_classes.view nodes.manage,nodes.view photos.manage,photos.view platform.mobile,platform.web procedures.manage,procedures.view quotes.approve,quotes.manage quotes.view,reports.view sessions.manage,sessions.view shortcuts.manage,shortcuts.view slds.manage,slds.share slds.view,tasks.assign tasks.manage,tasks.view users.view,workorders.manage workorders.view

### Account Manager (77 granted)

accounts.manage,accounts.view attachments.manage,attachments.update attachments.upload,attachments.view bulk_operations.manage,bulk_operations.view company_data.view,contacts.manage contacts.view,data.export data.import,devices.manage devices.view,edge_classes.manage edge_classes.view,edges.manage edges.view,features.accounts.view features.assets.view,features.condition_assessment.view features.goals.manage,features.goals.view features.jobs.view,features.opportunities.view features.salesdb.view,features.settings.pm.view features.settings.view,features.site_visits.view features.slds.view,form_instances.manage form_instances.view,forms.manage forms.view,issue_classes.manage issue_classes.view,issues.manage issues.view,jobs.manage jobs.view,locations.manage locations.view,mappings.manage mappings.view,node_classes.manage node_classes.view,nodes.manage nodes.view,opportunities.manage opportunities.view,photos.manage photos.view,platform.mobile platform.web,procedures.manage procedures.view,quotes.approve quotes.manage,quotes.view reports.generate,reports.manage reports.view,sessions.manage sessions.view,shortcuts.manage shortcuts.view,slds.manage slds.share,slds.view tasks.assign,tasks.manage tasks.view,users.manage users.view,workorders.manage workorders.view

### Client Portal (37 granted)

attachments.view,attachments.view_public contacts.view,devices.view edge_classes.view,edges.view features.arc_flash.view,features.assets.view features.attachments.view,features.condition_assessment.view features.connections.view,features.issues.view features.locations.view,features.nfpa70e.view features.site_overview.view,features.slds.view features.tasks.view,form_instances.view forms.view,issue_classes.view issues.view,jobs.view locations.view,node_classes.view nodes.view,notes.manage notes.view,opportunities.view photos.view,platform.web reports.view,sessions.view settings.view,shortcuts.view slds.view,tasks.view workorders.view
