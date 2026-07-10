# EG Admin super-admin verification test

**Date:** 2026-07-10 UTC · **Branch:** main

## Ask
EG Admin is the super-admin role — add a positive test proving it genuinely grants complete access
(turn "EG Admin = super admin" from assumption into a verified guarantee).

## What
New `EgAdminSuperAdminContractTest` (in testng-rbac-permissions.xml). Logs in as the first account
holding EG Admin (all 5 base accounts currently carry it) and asserts the super-admin contract:
- HARD: roles[] include "EG Admin"; is_admin=true; has_web_access=true.
- HARD: live permissions are a SUPERSET of the full 113-perm prod matrix (RolePermissionMatrix.allPermissions()),
  with KNOWN_QA_DRIFT tolerance (a perm absent from the whole QA tenant warns, not fails).
- HARD: the exclusive Reporting + Forms module present (any reports.* and any forms.*).
- SOFT: has_web_access ⇔ platform.web consistency; logs super-admin extras (count never upper-bounded —
  a super-admin is expected to gain perms as features ship).
- SKIP (distinct messages): no account provisioned at all vs provisioned-but-none-carry-EG-Admin.
Added RbacFixtures.holdsEgAdmin(LiveAuth).

## Design
Assertion set was adversarially designed via a 2-agent review workflow (proposer + adversarial critic)
to be meaningful yet drift-robust (superset over exact count; role-name over UUID; drift tolerance).

## Validation
Live: 1 run, 0 fail — confirmed EG Admin (via +project account) = is_admin true, web access, all 113
matrix perms (115 live), Reporting+Forms present.
