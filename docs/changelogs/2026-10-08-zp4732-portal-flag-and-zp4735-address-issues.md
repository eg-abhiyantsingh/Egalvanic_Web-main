# ZP-4732 (Maintenance Portal flag, hotfix v2.2.5) PASS on stage · ZP-4735 (address issues) reproduced on QA

- **Date:** 2026-10-08, 07:15–07:55 UTC
- **Prompt:** owner pasted the three address-validation issues sent to Dharmesh, and: "you miss this issue please dont miss this. also you miss to tell me to disable maintance protol from launch darly … in producation maintency porotl was disable for lots of company but still everyone can see that we miss that bug."

## What was missed, and why
- **ZP-4732:** while testing ZP-4572/ZP-4703 I never tested the `feature-maintenance-portal` OFF state. Prod (`index-C3h6wmBi.js`) builds the Maintenance Portal nav section with `requiresTier2` only and no `requiresFlag`, so the LD flag only greyed Site Data › Maintenance.
- **ZP-4735:** I passed ZP-3802's site photo because a photo appeared, without checking that it was the right property, that it could be hidden, or that search works across countries.
- Lesson saved: memory `feedback_test_flag_off_and_real_world_correctness`.

## ZP-4732 on stage `index-Chf-sAV5.js` (PR #1693 adds `requiresFlag: pe` to the section): PASS → READY TO RELEASE
| State | Result |
|---|---|
| Real flag OFF for acme stage | No Maintenance Portal for Admin, Super Admin, PM, AM, EE, FM, CP or the reporter; Technician restricted. Typed /maintenance-portal/overview → "Feature Not Available". Site Data › Maintenance locked (lock icon). |
| Flag ON (simulated by rewriting LD evalx in the test browser) | Portal back for every role except Technician; the typed URL opens Site Health; Program, Compliance and Reports enabled. |

- Comment **44970**; ZP-4732 moved In QA → READY TO RELEASE.
- Open question in the comment: a CP user at a flag-off company keeps only Site Data. Is that intended?
- Release v2.2.5 (14278, due 8 Oct) = ZP-4732 (RTR) + ZP-4731 (In Progress).

## ZP-4735 reproduced on QA `index-D6ybXb17.js` (ticket In Progress, Avani)
1. 47 W 13th St, New York → "Photo: Anna's massage and spa wellness · Google". Address place `ChIJ2zwdEThZwokRzB6cIH8qc4s`, photo place `ChIJYwPPeZZZwokRM6q3yrbMzxA`. `place-photos` is called with lat/long only.
2. Photo block: only "Use this photo" / "Upload my own"; Edit Site: only "Change". No "Hide site photo".
3. Country = US: "10 Downing Street London" → only "10 Downing Circle, London, OH, USA" (`country_code: "US"`). Country = UK → the real address (`"GB"`).

- Comment **44969** with screenshots, plus a re-test checklist for the fix.

## Evidence
- `docs/bug-evidence/2026-10-08-zp4732-stage/` (real-* and sim-on-* menu / typed-portal screenshots for 8 seats)
- `docs/bug-evidence/2026-10-08-zp4735-address-issues-qa/`
