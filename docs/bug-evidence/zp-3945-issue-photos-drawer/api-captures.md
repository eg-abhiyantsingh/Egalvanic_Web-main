# ZP-3945 — issue photos in the workbench drawer, shared lightbox, origin-aware back — captures (QA V1.36, 2026-09-08)

## Drawer, title and photos
Pull-Through Work → search "Repair Needed" → open the row. Drawer header reads exactly **"View Issue"**. Body order: title + Medium Priority + "Test asset · Android Site 2" → **The problem** → three issue-class questions → **Photos** → **Resolutions**.
Issues on the tenant carrying one photo each: `b83b932e-23d3-48d5-a3bb-48b90ce098e6` (Repair Needed, `photo_5438cc99-cefb-45fb-99fd-29afa54118da.jpg`), `b2a99a7f-e277-4a4a-a748-a9c038ffdc22` (Thermal Anomaly, `photo_c14d684b-…`), `a285e5fd-1b2b-4049-92e0-7a13dde06457` (SCCR on ATS-EM-L, `498.jpg`, with a `local_filepath` from the mobile capture).

## Hydration and presigning actually resolve
Opening the drawer fires **`POST /api/s3/urls/batch` → 200** (`application/json`), and the object it returns loads:
`GET https://eg-pz-qa-s3-asset-photos-ohio.s3.us-east-2.amazonaws.com/photo_5438cc99-….jpg?X-Amz-Al…` → **200 `image/jpeg`**.
The rendered `<img alt="issue photo">` reports `naturalWidth 1080 × naturalHeight 2340` — a real decoded image, so no 403 or broken placeholder. Thumbnail box in the drawer: **84 × 84**.

## Full-screen viewer and the hover-shift fix
Clicking the thumbnail opened a full-screen view of the same object rendered at **415 × 900**, over three stacked overlay layers (`.MuiModal-root` / backdrop containing an `img[src*="asset-photos"]`).
**Hover-shift check** — the large image's bounding rect, read before the cursor entered and again after moving diagonally across it twice:
| | x | y | w | h |
|---|---|---|---|---|
| before hover | 692 | 107 | 415 | 900 |
| after two moves across the image | 692 | 107 | 415 | 900 |
Identical — the image does not move under the cursor.
No prev/next or download control was found by label; the fixture carries a single photo, so the stepping controls may legitimately be hidden. Not verified.

## Navigation — could not be posed
No control in the drawer navigates to the issue's full page: searched the drawer for "View Full", "Full Page" and "Open full" — none present (its actions are Re-evaluate · Add manually · Accept · Add to Quote / Unaccept / Go to Quote). Navigating directly to `/issues/b83b932e-…` renders the full page correctly ("Repair Needed · Medium Priority · Test asset · The problem · No description…") but exposes **no Back control** (`button|a` matching /^Back|← / → 0). So neither the PTW-origin nor the register-origin back could be exercised.

## Empty state
The SCCR, OSHA and Thermal Anomaly drawers opened repeatedly through the session show no Photos strip and render cleanly; console stayed at the page's baseline (10 pre-existing unrelated errors) with no new entries on drawer open.
