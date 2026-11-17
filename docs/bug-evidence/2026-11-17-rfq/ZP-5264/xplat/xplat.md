# ZP-5264 cross-platform (Android)
- Platform/build: Android 1.40 (54), SLD Viewer 90, emulator. Seat: Super Admin, EG-ACME (flag ON). 17 Nov 2026 ~15:20-15:35 IST. Web build index-CkkAeA9o.js.
- Steps: v2.6 SLD (L1 and L2 exist) > unlock > Assembly > QA4942-FDR-{name}. Also on ZP-4800 after a custom-named copy.
- Result: Android pre-fills **"L1 CB-3"** (v2.6 SLD), while the server's place-preview for the same SLD returns **"L3"**. On ZP-4800 it pre-fills "QA-DEMO ZP-5112 17Nov PNL-3".
- Parity: **differs**. Web is fixed (ZP-5264 RTR; the server suggests L3). The Android viewer still shows the bug. → BUG-assembly-name-suggestion.txt (+ video, 2 PNGs).
- iOS: not checked (iOS tester item 3).
