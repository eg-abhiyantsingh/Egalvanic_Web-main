# 2026-09-08 — QA: ZP-3945 [Web] Issue photos were unreachable from the workbench drawer, and full-page back always dumped you on the register

**Prompt:** the ticket text with its 7-step QA review ("test all this ticket too").

## What was done
1. **Opened an issue with a photo in the workbench drawer** — header reads exactly "View Issue", with a Photos section between the problem and the resolutions.
2. **Followed the presign all the way to the bytes** — the batch presign returns 200, and the S3 object it hands back answers 200 `image/jpeg` decoding at its full 1080 × 2340. A real image, not a broken placeholder.
3. **Measured the hover-shift bug instead of eyeballing it** — read the image's bounding box before the cursor entered, moved diagonally across it twice, read it again: byte-identical. Fixed.
4. **Hunted for the full-page affordance and reported honestly that there is none** — searched the drawer for "View Full", "Full Page", "Open full"; its actions are Re-evaluate, Add manually, Accept, Add to Quote, Go to Quote. Navigating to the full page directly renders it correctly but exposes no Back control at all.
5. **Negative case on three photo-less issues** — no empty strip, and console output stayed at the page's pre-existing baseline with no new entries on drawer open.
6. Verdict, evidence, artifact page.

## Results (short)
- **PASS on the photo work** — drawer photos, presigning, the viewer, and the hover-shift fix.
- **FINDING 1 (Medium)** — there is no route from the drawer to the full page and the full page has no Back control, so both origin-aware back steps could not be posed at all.
- Not verified: prev/next and download (single-photo fixture, no labelled controls), the form-renderer regression, IR photos as distinct from field photos.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3945-issue-photos-drawer-lightbox-verdict.md`
- Evidence: `docs/bug-evidence/zp-3945-issue-photos-drawer/`
- Artifact: https://claude.ai/code/artifact/dc586391-7cae-4a23-a4d0-8249c04ff1d9

## Depth notes (learning)
- **A presigned URL that returns 200 is not proof the image loads.** Reading `naturalWidth`/`naturalHeight` off the decoded element is: a 403 placeholder and a broken image both still give you an `<img>`.
- **Measure "it doesn't move" numerically.** A hover-shift regression is invisible in a screenshot pair. Two bounding-box reads around a real cursor path is a claim with a number behind it.
- **"Not exercised" is a finding when the affordance is missing, not a gap in the testing.** Saying which controls the drawer *does* offer is what makes that distinction credible.
