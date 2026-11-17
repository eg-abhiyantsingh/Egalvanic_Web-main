# Cross-platform backfill, 17 Nov 2026 (owner: "what you lag sometimes … cross platform checking … mobile web checking")

This morning, 15 web tickets went to READY TO RELEASE with "mobile not checked" or "web-only".
The lead read the iOS source (eg-pz-mobile-iOS, release/qa 34bd67e = #826, the build under test). Some of those "web-only" claims are wrong:
- iOS has a site **Quotes** tab: `Views/Quotes/QuotesView.swift`. It lists quotes with a total value and status counts.
- iOS can **create and update quotes**: `APIClient+SnapshotPayload.swift` has `snapshotQuoteCreateBody` / `snapshotQuoteUpdateBody`.
- iOS has **SLD assemblies and layout groups**: `AssemblyPlacementService`, `AssemblyCacheService`, `SLDAssemblyLocal`, node_group code.
- iOS has no pricing inputs, IR delta tool, Upload Anything, User Guide or email preferences code.

Run these when your current tickets are done.
- Evidence goes to `docs/bug-evidence/2026-11-17-rfq/<KEY>/xplat/` (MP4 + PNG, outline boxes only, no burned-in text).
- Add a short `xplat.md` per ticket: platform, build, seat, steps, result, and parity verdict (same / differs).
- A real parity gap or mobile defect gets its own BUG-<short>.txt, one per platform.

## iOS tester (lane A, Z Platform-QA 1.75 sim build 16 Nov)
1. **ZP-4636** created_by / modified_by on commercial records.
   - Create a QA-DEMO quote on iOS, if the Quotes tab allows it, then edit it.
   - On web/API (GET the quote), created_by and modified_by must be the iOS user, not null, not "system", and not another user.
   - Compare with a quote created and edited on web.
2. **ZP-5279 / ZP-5280 / ZP-5281** quote pricing.
   - Open site "QA-DEMO ZP-5108 ZP-5109 12 Nov" › Quotes on iOS (EG-ACME, SA seat).
   - Compare each QA-DEMO ZP-5279 / 5280 / 5281 quote's amount and status on iOS with web. Expected: identical. In particular, no $0 where web now shows a price.
   - Check that the iOS total equals the sum of the web totals for the listed quotes.
3. **ZP-5264 / ZP-4942** assemblies on iOS (EG-ACME, flag ON):
   - place an assembly twice (on the SLD with an existing QA4942 assembly);
   - check the second placement's suggested name is the next free name (not "L1 CB-3");
   - check handles and elbows are wired like on web.
   - In Demo Company (flag really OFF), no assembly controls should show.
4. **ZP-5274** company role permissions, Technician seat.
   - EG-ACME: what the Technician can open on iOS.
   - Demo (guest): the same.
   - Compare with the web permission set from ZP-5274 (89 vs 88 permissions; platform.web on EG-ACME only). Note any mobile screen the removed permissions should hide: opportunities, planning, scheduling, site_overview, tasks, data.export.
5. **ZP-5218** Add Existing by Service: confirm iOS has no equivalent picker in WO › Add Assets. If it has one, check that the "Downstream of" filter keeps only matching rows.
6. **ZP-5115 / ZP-5119** (Android tickets, RTR this morning): run the same flow on iOS.

## Android tester (emulator, 1.40 (54))
1. Does Android have a site **Quotes** tab? If yes, run the same checks as iOS items 1–2.
2. **ZP-5264 / ZP-4942:** assembly placement naming (shared with ZP-5112).
3. **ZP-5274:** Technician permissions on EG-ACME vs guest in Demo, compared with web (as iOS item 4).
4. **ZP-4937 IR delta:** confirm there is no delta / "create issue from delta" tool on Android IR photos. If there is one, check that negative deltas keep hot/cold in the right order.
5. **ZP-5292 / ZP-5249 / ZP-4892 / ZP-5284:** confirm Android has no email-preferences, user-guide or Upload Anything screen. If it has one, check parity.
6. **ZP-5218:** as iOS item 5.

## Web-only, confirmed by iOS source (Android still to confirm in its UI)
- ZP-5278: pricing settings.
- ZP-5285: SKM import/export.
- ZP-4937: IR delta.
- ZP-5284: Upload Anything.
- ZP-5249 / ZP-4892: User Guide.
- ZP-5292: email preferences.

## Added ~15:45 IST: mobile checks from Eric's web tickets (posted RTR today)
Do these on BOTH iOS and Android, in this order (most valuable first). Use the SA seat unless noted; TECH is busy with the guest checks.
1. **ZP-4942 / ZP-5107, assembly placement fidelity.** iOS repro is already requested.
   - Android: EG-ACME, SLD "QA-DEMO v2.6 SLD editing 12 Nov", place assembly QA4942-FDR named AND17.
   - On web: is "FUSE nosub → Load-1" still hidden, and does "CB-2 → PNL-2" keep its elbow (edge_manips)?
2. **ZP-5106, forked service on mobile.**
   - Site "QA-DEMO ZP-5108 ZP-5109 12 Nov": WO "QA-DEMO ZP-5106 17 Nov IRT open A" shows "Insulation Resistance Testing" (EG-ACME's copy) with 5 assets and their forms.
   - Fill one harmless form value and sync; web shows it. WO B is the same; closed WO C is not in the open list.
3. **ZP-4831, install status survives a mobile edit.**
   - First, on web as SA, type https://acme.qa.egalvanic.ai/safety/install-statuses (SA can open it by typed URL), pick site "QA-DEMO ZP-5211 13 Nov power scheme bypass" and set CB-TIE to Tested. It is In Service now.
   - On the device, edit a harmless field on CB-TIE and sync.
   - Web must still show Tested, with no extra install-status history event. Then set CB-TIE back to In Service. No Safety screens on mobile.
4. **ZP-4942, company code `demo` (flag really OFF):** no Assembly control on mobile.
5. **ZP-5100, data on device:**
   - QA5100C-XF-PAD-LIB: Type Pad Mount, windings Delta / Wye-Ground.
   - QA5100C-PNL208: Electrode Configuration VCBB.
   - QA5100-CBL-NEC on the 12 Nov site: insulation "N/A".
6. **ZP-4943, mobile SLD viewer:**
   - in group "QA-DEMO ZP-4943 badge grp 17 Nov", tapping the badge jumps to Load-1;
   - on a locked SLD, long-pressing a connection offers only "Select Entire Run".
