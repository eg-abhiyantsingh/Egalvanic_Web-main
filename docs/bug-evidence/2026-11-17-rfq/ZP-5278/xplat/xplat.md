# ZP-5292 / ZP-5249 / ZP-4892 / ZP-5284 / ZP-5278 / ZP-5285 cross-platform (Android)
- Platform/build: Android 1.40 (54). Seats: Super Admin (EG-ACME) and Technician (home and guest). 17 Nov 2026.
- Settings sections on Android: Account, Security (Passkeys, Two-Factor Authentication, Screen Lock Login), Language, Sync & Network (Sync Queue Analyzer, Network Mode), Diagnostics (Photo Storage Diagnostics), Session Analytics (Session Recording), Device Library, Storage (Clear Image Cache), About, Switch Experience (Super Admin only), Logout. There is no help/user-guide entry elsewhere (the Z support button opens Support).
- Result: **no email-preferences, no User Guide, no Upload Anything, no pricing settings, no SKM import/export screen** on Android. The APK strings agree (apk-string-check.txt: 0 hits for each; "SKM" appears only as library-matching text).
- Parity: **not on Android**, matching the iOS source. All six stay web-only.
- Evidence: android-settings-bottom-sa.png, android-settings-bottom-guest.png, apk-string-check.txt.
