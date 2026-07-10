# Plain-English RBAC test names in the client report

**Date:** 2026-07-10 UTC · **Branch:** main

## Ask
Manager review: the RBAC results (415 pass / 0 fail / 191 skip — correct) used engineer-speak like
"Project Manager · company_data.manage · false" and "Backend enforces *.manage ... permission_denied".
Rewrite in language a client understands.

## Changes
- `consolidated-report.py`: RBAC permission cells (params role, permission-key, true/false) now render as
  plain English — e.g. "Project Manager — should NOT be able to Manage company data",
  "Technician — should be able to View accounts". Added `_humanize_perm()` (company_data.manage →
  "Manage company data", features.assets.view → "View assets", platform.web → "Access the web app") and
  `_rbac_cell_name()`; non-RBAC tests are unaffected (fall back to prior formatting).
- Rewrote 6 technical @Test descriptions into client language:
  - RoleActionEnforcement → "Each role can create/edit records only when its permissions allow it (unauthorised roles are blocked)"
  - RoleCrudContract → "Each role can create, edit and delete records only when allowed (others are refused)"
  - WorkOrderEditEnforcement → "Only roles allowed to edit Work Orders can edit them (others are blocked)"
  - RoleBasedPermissionContract → "Each role's live access matches its approved permission list"
  - matrix integrity → "The approved permission list is complete and covers every expected role"
  - RolePermissionMatrixCell → "Each role has exactly the access it should — and nothing it shouldn't"
- Per-cell detailed-report label: "perm [denied]" → "should NOT have 'perm'".

No test logic changed — naming/reporting only. 415/0/191 result is unchanged and correct.
