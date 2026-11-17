# ZP-5230 [Android] EG form: Sections list beside the form on a tablet in portrait

**Verdict: PASS** (against Rajat's rule in comment 46187: side-by-side on screens ≥ 744 dp wide; phones and narrower tablets keep the drawer)

- Build: Android Z Platform (QA) **1.40 (54)**, emulator 1080x2400. Screen sizes set with `adb shell wm density`, as in Rajat's steps.
- Seat: Super Admin QA seat, EG-ACME, site Android Site 2, work order "new demo 2nov" (the WO Rajat used) › Forms › Trip Setting Verification › 14N-H1-2-Circuit Breaker 1 › Edit Form. 17 Nov 2026, ~14:30-14:40 IST.
- Fix: eg-pz-mobile-android PR #606.

## What was run (Rajat's steps 1-7)
| # | Step | Result |
|---|------|--------|
| 1-3 | density 216 (800 dp wide), portrait | The Sections list sits on the left beside the form, which is not dimmed. Tapping "Short Time Element" switches the form at once and the list stays open. Tapping a Range cell opens the keyboard, so the form can be edited while the list is open. PASS |
| 4 | Rotate to landscape (216) | The list stays beside the form and "Short Time Element" stays selected. PASS |
| 5 | density 240 (720 dp), portrait | Only the Sections button; tapping it opens a drawer over the dimmed form. Expected. PASS |
| 6 | Phone (density 420) in landscape | Sections opens the drawer. Expected. PASS |
| 7 | `wm density reset` | Done. Portrait restored (user_rotation 0). |
| ctrl | Phone (420) portrait | Drawer. Expected. |

## Observation (minor, test-method only)
- The running app does **not** re-layout when the display density is changed live (`adb shell wm density …` while the app is open). The app content stayed at the old size, and only the system bars changed. After the app was reopened, the new size applied and every row above passed. Rotation is handled live. Real tablets don't change density at run time, but a user who changes "Display size" in Android settings would need to reopen the app. I'm noting this for the dev, not filing it.
- After each relaunch the app showed the 2FA enrolment prompt again ("Set up later"). That is the existing MFA behaviour.

## Not covered
- A real tablet (emulator density only). Densities between 240 and 216 (the 744 dp cut-off itself).

## New defects
None.

## Cross-platform
- **Web** (index-CkkAeA9o.js): the same WO "new demo 2nov" › Forms › Trip Setting Verification › 14N-H1-2-Circuit Breaker 1 shows the Sections list beside the form on a desktop-size window. Same layout. PASS (parity).
- **iOS:** side-by-side is iPad-only on iOS (ZP-4864, Done, iOS v1.75); Android follows the same rule (≥ 744 dp = smallest iPad). Not checked on iOS today.
- MOBILE-CHECKS (iOS): iPad simulator (portrait and landscape), the same WO › Forms › Trip Setting Verification › Edit Form: the list sits beside the form in both orientations. iPhone landscape: drawer.

## Restored
- `wm density reset` (420) and `user_rotation 0` (portrait) after the run; accelerometer rotation was set off for the test.
- The WO session "new demo 2nov" was started for the test and later replaced/deactivated. No form was saved (Close; nothing typed into the cell).

## Evidence
- QA-17Nov-ZP-5230-sections-list-tablet-portrait.mp4 (40 s; mostly full-size stills, because the screen recorder keeps a portrait frame and makes landscape tiny)
- QA-17Nov-ZP-5230-1-density216-portrait-list-beside-form.png, -2-density216-landscape-list-stays.png, -3-density240-portrait-drawer-expected.png, -4-phone-landscape-drawer-expected.png, -5-web-eg-form-sections.png
