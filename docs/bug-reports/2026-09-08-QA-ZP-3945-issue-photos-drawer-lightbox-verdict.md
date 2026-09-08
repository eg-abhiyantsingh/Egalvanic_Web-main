# [Web] Issue photos were unreachable from the workbench drawer, and full-page back always dumped you on the register

**QA verdict — PASS on the photo work, which is the substance of the ticket. The workbench drawer is titled **View Issue**, carries a **Photos** section, and hydrates and presigns the issue's images: `POST /api/s3/urls/batch` returns a presigned URL and the S3 object answers **200 `image/jpeg`**, rendering at its full 1080 × 2340. Clicking the 84 × 84 thumbnail opens a full-screen viewer showing the same photo at 415 × 900, and **the hover-shift bug is fixed** — moving the cursor across the image left its bounding box byte-identical. Not verified: prev/next and download (the fixture has a single photo and no labelled controls were found), and the origin-aware back, because no route from the drawer to the full page could be found and the full page shows no Back control.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · live UI with the photo and S3 responses captured.
**Ticket said "dev only, not yet promoted to cicd/qa" — wrong:** drawer photos, presigning and the extracted viewer are all live on QA.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Open an issue with attached photos in the workbench drawer: thumbnails render, and the drawer header reads "View Issue" | ✅ **PASS** — the drawer header is exactly **"View Issue"**, and the body carries a **Photos** section between "The problem" and "Resolutions". The Repair Needed issue on Test asset (one photo, `photo_5438cc99-….jpg`) renders an 84 × 84 thumbnail with `alt="issue photo"`. |
| 2 | Click a thumbnail — the full-screen lightbox opens. Step through with prev/next, download an image, and confirm the image does not shift when the cursor moves over it | ⚠️ **PASS on opening and on the hover fix; prev/next and download not verified** — clicking the thumbnail opened a full-screen view rendering the same photo at **415 × 900** over three stacked overlay layers. **Hover-shift: fixed** — the image's bounding box was `{x:692, y:107, w:415, h:900}` before the cursor entered and **identical** after moving diagonally across it twice. Prev/next and download controls were not found by label on this fixture, which has only one photo — so stepping and downloading remain unexercised. |
| 3 | Confirm presigned URLs actually resolve (images load rather than showing a broken/403 placeholder), including IR photos | ✅ **PASS** — opening the drawer fires `POST /api/s3/urls/batch` → **200** (`application/json`), and the resulting `https://eg-pz-qa-s3-asset-photos-ohio.s3.us-east-2.amazonaws.com/photo_5438cc99-….jpg?X-Amz-Al…` returns **200 `image/jpeg`** with `naturalWidth 1080 × naturalHeight 2340` — a real decoded image, not a placeholder. IR photos specifically were not distinguished from field photos on this fixture. |
| 4 | Reach an issue's full page from Pull-Through Work, press back, and confirm you land back in PTW — not on the issue register | ❌ **NOT EXERCISED** — no control in the drawer navigates to the issue's full page (searched for "View Full", "Full Page", "Open full" — none present; the drawer's own actions are Re-evaluate / Add manually / Accept / Add to Quote). Navigating directly to `/issues/{id}` renders the full page correctly (Repair Needed · Medium Priority · Test asset · The problem …) but **shows no Back control at all**, so there was nothing to press and no origin to return to. |
| 5 | Reach an issue's full page from the issue register, press back, and confirm you land on the register | ❌ **NOT EXERCISED** — same reason: no Back control was found on the full page. |
| 6 | Regression: open any form that uses EGFormRendererV2's photo viewer and confirm the extracted shared lightbox behaves as it did before — open, prev/next, download, close | ❌ **NOT EXERCISED** — no EG form with attached photos was opened in this session. |
| 7 | Negative: open an issue with no photos in the drawer and confirm it renders cleanly with no empty thumbnail strip or console error | ✅ **PASS** — the SCCR, OSHA and Thermal Anomaly issues opened repeatedly through this session carry no photo strip and render their Problem and Resolutions blocks cleanly. Console output stayed at the page's usual baseline (10 pre-existing errors, unrelated to photos) with no new entries on drawer open. |

---

## Findings

### FINDING 1 (Medium) — there is no route from the workbench drawer to the issue's full page, and the full page has no Back control
Steps 4 and 5 are the navigation half of this ticket, and neither could be posed. The drawer offers no "full page" affordance, and `/issues/{id}` reached directly renders without any Back button or breadcrumb. Either the affordance lives somewhere I did not find, or the origin-aware back has nothing to attach to on this build. Worth a developer confirming where the full-page entry point is meant to be, because as it stands the drawer is the only way in and there is no way out of the full page except the browser control.

---

## Test data — direct links (QA)
- Issue with a photo, opened in the drawer: https://acme.qa.egalvanic.ai/pull-through-work → search "Repair Needed" (issue `b83b932e-23d3-48d5-a3bb-48b90ce098e6`, photo `5438cc99-cefb-45fb-99fd-29afa54118da`)
- Its full page: https://acme.qa.egalvanic.ai/issues/b83b932e-23d3-48d5-a3bb-48b90ce098e6
- Two more issues carrying one photo each: `b2a99a7f-e277-4a4a-a748-a9c038ffdc22` (Thermal Anomaly, `photo_c14d684b-…`) · `a285e5fd-1b2b-4049-92e0-7a13dde06457` (SCCR on ATS-EM-L, `498.jpg`)
- Presigning: `POST https://acme.qa.egalvanic.ai/api/s3/urls/batch`

Evidence: `docs/bug-evidence/zp-issue-resolution-pricing-pull-through/api-captures.md` (same session) plus the drawer and viewer screenshots.

## Not covered / honest gaps
- **Prev/next and download** in the viewer — the fixture has a single photo and no labelled controls were located.
- **Both back-navigation steps** — see FINDING 1.
- **The EGFormRendererV2 regression** — no form with photos was opened.
- **IR photos specifically** — the photos on these fixtures were not identified as IR.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
