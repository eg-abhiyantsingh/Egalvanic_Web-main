# Android 1.40 (54) round, 17 Nov 2026: brief for the Android tester

Read ../2026-11-17-rfq/BRIEF.md first (same hard rules: QA only, no Jira/Chat/git writes, never DevRev, evidence format, no text drawn on screenshots, outline boxes only, PASS/FAIL wording in captions). This file adds the Android specifics.

## State when you start
- Emulator `emulator-5554` (quacky_test, 1080×2400, density 420), Appium on 4725. App `com.egalvanic.pz.qa` **1.40 (54)**, installed from Firebase App Tester at 12:46 IST over 1.39 (53) with real data. Signed in as the Super Admin QA seat in the Field Technician experience, site **Android Site 2**. App language English (keep it English at the end).
- **ZP-5227 already proven by the lead:** the 1.39 (DB 77) → 1.40 upgrade on first launch had no crash, no "duplicate column" or SQLite error in logcat, still signed in. Home 632 assets / 249 connections = web Site Overview 632 after a full Refresh (the earlier 638 was server-side change). Evidence in ZP-5227/: two screenshots + upgrade-logcat-excerpt.txt. The build is not debuggable (no run-as), so the DB version can't be read directly.

## Your tickets (all In QA; read each with getJiraIssue incl. every comment; Rajat's comments have device test steps)
1. **ZP-5227** (finish): after the upgrade, confirm custom manufacturers work (add a custom manufacturer to an asset's Engineering section and save) and layout groups work (Android SLD: assembly/layout-group placement or Add to Group, if the 1.40 app offers it; LaunchDarkly feature-sld-assemblies is ON for EG-ACME on QA). Write the verdict including the lead's upgrade evidence.
2. **ZP-5120**: New Location sheet: pick ONLY an existing building → Create closes the sheet and the location is created; negative: nothing picked → Create disabled or warns.
3. **ZP-5197**: Create Asset screen gets section links like the asset page (tap each link → jumps to the section).
4. **ZP-5257**: issue/asset pages must not crash when a table attribute (e.g. "Current Draw (A)", columns A/B/C/N) holds numbers or Python-style quotes instead of text. Make the data on a QA-DEMO asset/issue you create, via the web API (lib.js `s.api`) — e.g. write the table JSON with numeric cells — then open it on Android: no crash, values shown. Restore/delete your test record. Never touch real customer records.
5. **ZP-5230**: EG form Sections list beside the form on a tablet in portrait: follow Rajat's steps (`adb shell wm density 216` → Work order → Forms → a form → asset → Edit Form; portrait = list beside the form; rotate = stays; `wm density 240` portrait = drawer (expected); phone landscape (density 420) = drawer (expected)). **Always end with `adb shell wm density reset` and rotation back to portrait** (`settings put system user_rotation 0`).
6. **ZP-3021**: Equipment Library picker when creating OCP assets: "Create a Main Breaker?" dialog (when adding a panel), Create New Child and Create Multiple. Spec (Abhiyant's Jira comments 46126/46198): Use library switch → Manufacturer → search list → linked device card with Unlink; optional; only for companies with the engineering library on; Pole/Fuse Count on Child/Multiple; Create Multiple = one pick for the batch, applies only to rows of the same type, with a line like "Applies to 4 breakers". Verify the link is saved (the asset's Engineering section opens with the device linked). Name test assets "QA-DEMO ZP-3021 17 Nov …".

## Android tooling (in .playwright-mcp/qrmob/)
- `and.js`: `const A=require('./and.js'); const s=await A.open();` → `s.texts()`, `s.exists(sel,ms)`, `s.waitFor(sel,ms)`, `s.findAll(sel)`, `s.click(el)`, `s.tap(x,y)`, `s.tapText(t)`, `s.type(el,text)`, `s.clear(el)`, `s.scroll(dir,pct)`, `s.scrollTo(sel,dir,max)`, `s.rect(el)`, `s.source()`, `s.shot(file)`, `s.back()`. Selectors: `{text}`, `{contains}`, `{desc}`, `{xpath}`, `{ui:'new UiSelector()…'}`. Run scripts with `node x.js` from qrmob/. Examples from today: fr10.js…fr19.js, ir3.js…ir5.js, loc*.js, wo*.js, seatsignin.js.
- adb: `~/Library/Android/sdk/platform-tools/adb -s emulator-5554 …`. Long-press = `adb shell input swipe x y x y 1200`. Screenshot = `adb exec-out screencap -p > file.png`.
- Video: `adb shell screenrecord --time-limit ≤170 /sdcard/x.mp4` (spawn detached, stop with `adb shell pkill -INT screenrecord`, then `adb pull`). It drops idle frames (timeline shorter than wall time) and a static screen can give a broken file, so place captions by looking at frames, and use screenshot stills where needed. Captions: render PNG bars with Playwright (see .playwright-mcp/acctset/caps4.js) and overlay with ffmpeg `overlay=0:H-h:enable='between(t,a,b)'` (this ffmpeg has no drawtext). One MP4 per ticket, 20–90 s.

## Traps (all hit today)
- `s.hideKeyboard()` presses Back when no keyboard is up: it closes bottom sheets and can exit the app to App Tester. Don't call it on sheets; if the app vanishes, relaunch with `adb shell monkey -p com.egalvanic.pz.qa -c android.intent.category.LAUNCHER 1`.
- Switches/toggles: tap the switch element (clickable/checkable View right of the label), not the label text.
- Long-press menus (Modifier/Supprimer style) for edit/delete on list rows.
- Opening a work order may ask "Start Work Order Session?" → Start Session is fine on QA test WOs.
- The site list search: tap the EditText, type, then tap the row (no hideKeyboard).
- Android Site 2 has many test WOs; "Job - Nov 6, 12:03 PM" has 2 IR photos; "QA-DEMO ZP-5244 13 Nov check-off" has forms/assets (Condition Assessment + Ultrasonic).
- If a sign-in is ever needed: `SEAT_E=VALID_EMAIL SEAT_P=VALID_PASSWORD node seatsignin.js` (never print passwords).

Return per ticket: PASS / FAIL / PARTLY, one-line reason, evidence paths, any new bug drafts (BUG-*.txt with video), and anything you changed and restored (density, rotation, test records).
