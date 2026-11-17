# ZP-4831 cross-platform (Android): install status survives a mobile edit — NOT RUN
- Not run on 17 Nov. It needs a web-side change first (web SA › /safety/install-statuses › site "QA-DEMO ZP-5211 13 Nov power scheme bypass" › CB-TIE → Tested), then a device edit on CB-TIE, a sync, a web check (still Tested, no extra history event) and a web revert to In Service. I ran out of time after the other Android items and did not want to leave CB-TIE half-changed.
- Known from today: Android has no Safety / install-status screens. The asset Engineering and More Options menus show none (Super Admin, Android Site 2 and the ZP-5100 sites).
- To run it: the steps above, in that order, with the revert at the end.
