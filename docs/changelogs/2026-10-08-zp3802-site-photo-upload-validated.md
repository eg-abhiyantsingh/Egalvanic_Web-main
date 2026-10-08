# ZP-3802: "Upload my own" site photo validated on QA (the part left open on 6 Oct)

- **Date:** 2026-10-08, 05:25–05:45 UTC · **Prompt:** "I have disabled the launch directly from for your side photo upload. You can validate that now. That was left."
- **Build:** QA `index-rqyDlfQa.js`, admin seat abhiyant.singh+admin@.

## Flag check
LaunchDarkly evalx (read in the browser, 05:30 UTC): `feature-address-verification` = **true** for acme on QA (env 6a34e78f…)
and on stage (env 6a34e797…). The owner's switch-off is not visible for acme. Stage does not have the ZP-3802 UI yet
(Create Customer has no Address verification / Site photo block). Flag-off behaviour was checked by rewriting ONLY that
flag in the test browser's evalx response (and blocking clientstream). It is labelled "simulated" everywhere.

## Results (all PASS)
- Verify → Google photo offered with "Use this photo" / "Upload my own" → Upload my own (JPG) → preview "Uploaded" + Change.
- Create: `POST /api/account/v2` 201, `PUT /api/account/<id>` 200, `POST /api/sld/` 201, `POST /api/s3/url` 200,
  `PUT eg-pz-qa-s3-sld-photos-ohio/<uuid>.jpg` 200, `POST /api/photo/create` 201. No error.
- Site page shows the uploaded photo in "Site photo" and keeps it after a reload ("No Google view", because our own photo was chosen).
- ⋮ › Edit Site › Change › Upload my own (PNG) › Save Changes: `PUT /api/sld/update/<id>` 200 + the same upload chain. The new photo shows after a reload.
- GIF → "Choose a JPG, PNG or WebP image."; 11 MB → "Photos must be 10 MB or smaller."; accept=image/jpeg,image/png,image/webp (limit LB=10).
- The photo URL without sign-in → 403; bucket listing → 403.
- Flag off (simulated): Create Customer has no suggestions, no verification block and no "Use current location"; the Site
  photo block shows "Upload a photo of the site" only. Site pages hide the map and Google view. A site with only a Google
  photo shows "No site photo".

## Noticed (not filed)
- The bad-type / too-big message renders only at the top of the dialog, about 770 px above the photo section (D1/D2 screenshots).
- Picking a suggestion no longer auto-verifies; it needs the "Verify address" click (on 6 Oct it auto-verified). Re-check under ZP-4675.

## Jira
ZP-3802 comment **44967**, screenshots embedded. Status unchanged (READY TO RELEASE).
Evidence: `docs/bug-evidence/2026-10-08-site-photo-upload-flag-off-qa/`.
Test data: account "QA-DEMO site photo upload 8 Oct (delete me)", site https://acme.qa.egalvanic.ai/sites/bd598806-f1cf-4189-91a7-e43dbc237161.
