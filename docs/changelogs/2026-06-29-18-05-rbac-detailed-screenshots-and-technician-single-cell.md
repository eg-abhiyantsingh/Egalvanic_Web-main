# RBAC matrix: per-cell screenshots in the detailed report + Technician collapsed to one login/logout cell

- **Date:** 2026-06-29 18:05
- **Prompts:** (1) "report format should be same as client report … two reports will show like client report and
  details report with proper screenshot." (2) "for technician there will be only one test case — check
  login/logout only."
- **Type:** Reporting evidence + matrix shaping.

---

## 1. Detailed report now has proper screenshots
The RBAC matrix is non-destructive and was all-green, so the detailed report had **no** screenshots
(screenshots were failure-only). Added evidence-screenshot capture to each cell so the **detailed report**
(`consolidated-detailed-report.py` output / the ExtentReports `Detailed_Report_RBAC_UI_*.html`) shows a
compressed screenshot per check:
- **View** cell → the nav state observed (route shown/hidden for that role).
- **Create/Edit/Delete** cell → the module page observed (the create/row-action control present/absent).

Capture is on by default; disable with `-Drbac.screenshots=false` for quick local runs. Uses the existing
compressed-base64 path so report size stays reasonable. Verified: a Client-Portal run embedded 39
screenshots across its cells.

> The two reports remain in the **standard framework format**: `consolidated-report.py` → the **client
> report** (now one row per role·module·action — see the report-accuracy changelog), and
> `consolidated-detailed-report.py` → the **detailed report** with these screenshots. The detailed report
> loads its ExtentReports theme from a CDN, so it's meant to be **downloaded from the run's Artifacts and
> opened in a browser** (it can't render inside a Claude Artifact, which blocks external CDNs).

## 2. Technician → a single login/logout web-access cell
Technician has **no `platform.web`** grant, so all 48 of its module×action cells trivially confirmed
"everything hidden" — noise. The matrix now gives any **web-restricted** role (provisioned but lacking
`platform.web`) a **single** cell: log in, assert the web app correctly **withholds all navigation**
(logout via session teardown). The DataProvider probes the role's live `/auth/me` (cached) to decide, and
falls back to the full matrix on any error so coverage is never silently dropped.

**Effect on counts:** the RBAC front-end group is now **≈303** (6 web/at-the-API roles × 48 + 1 Technician
cell = 289 matrix + 14 login/gating journeys), down from 350. Catalog + dashboard counts updated. Validated:
a Technician-only run produces **exactly 1 test** (passed — web access correctly restricted).
