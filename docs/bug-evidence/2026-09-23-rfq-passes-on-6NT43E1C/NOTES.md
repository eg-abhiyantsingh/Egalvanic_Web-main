# 2026-09-23 (evening) — the ten Ready-for-QA passes re-verified on `index-6NT43E1C.js`

Third rebuild of the day (CPjC9Hwo → CH4p1H5S → **6NT43E1C**). Seat `+admin@`, site Android Site 2 (366 assets today).
Purpose: the owner asked for passing tickets to be moved to READY TO RELEASE — none may move on stale evidence.

| Ticket | Re-verified today | Capture |
|---|---|---|
| ZP-4039 | Condition Assessment tabs Overview / Findings 36 / Asset Details 366; filter row Location / Asset class / Shutdown rule / Condition / Assessment + **Bulk Ops**; Findings rows each carry the edit pencil | 02, 03 |
| ZP-4041 | "**8 assets** had their condition revised automatically in the last 30 days" renders under Condition Distribution | 01 |
| ZP-4043 | Equipment Health band = Healthy · **Assessment expired** · At risk · Not assessed | 01 |
| ZP-4068 | 3 JOURNAL chips on /site-walks; New Site Walk offers **Count** ("Tally assets by class and location…") vs **Journal** ("Talk and shoot as you go…") | 04 |
| ZP-4109 | Service *Infrared Thermography* → **All methods** = flat grid, **30 methods**, checkbox per row, filter "Class, method, labor, equipment", Per device, Edit selected. Columns Class / Method / Description / Labor / Est. / Forms / Test equip. / Kits / Materials / Per-unit / De-en. / Sched. / Check | 11, 12 |
| ZP-4110 | /devices tray = Manufacturer / Asset class / Service / Frame / Rules (Company and global); Add device = Service → Asset class → Device with "Pick the asset class first — it decides which part of the library to search." | 06, 07 |
| ZP-4112 | Compliance tabs incl. **Visualizer**; Group by / Filters / Export / Full screen present | 05 |
| ZP-4149 | IR-typed WO → Report → config list: "ABBOTT PM IR — sample-fix proof (Custom)", **"Infrared Thermography Report"**, "Other configurations (39)" | 15 |
| ZP-4167 | **Junction Box**: System Voltage only — no Mains Type, no Panel Type, no Phase Configuration, and no Schedule/OCP tabs. **Panelboard** (control): Mains Type, Manufacturer, Panel Type, System Voltage all present, Schedule + OCP tabs present | 13, 14 |
| ZP-4171 | Asset Classes → Circuit Breaker → Core Attributes → **Add** → row header `Name * / Type / Subtypes (empty = all) / Default Value / Description`; an Asset Subtypes section lists the subtypes | 10 |

## Correction: one unfiled finding is now FIXED — do not report it
The "**all three Admin class grids open at 0–0 of 0 until you press refresh**" regression (seen on BOaMecwk,
C9NJAR1x and CPjC9Hwo) **no longer reproduces on 6NT43E1C**. Cold navigation AND a hard reload both render
populated: `/asset-classes` 1–25 of 49, `/connection-classes` 1–3 of 3, `/issue-classes` 1–8 of 8. → 08, 09.
It must be dropped from the "findings awaiting a ticket" list.

## Other corrections
- The custom service "**corrective IR**" used as the ZP-4109 example on 21–22 Sep **no longer exists** on the
  tenant (search returns nothing). Today's proof uses *Infrared Thermography*, which is a better one: 30 methods,
  so the multi-select the ticket is about is genuinely exercisable (the bulk apply itself was still not run —
  it mints a service version).
- Nothing was written during this sweep. Every dialog was cancelled.

## Adversarial re-check of every published DEFECT (13-agent workflow + live re-walks on 6NT43E1C)
The owner asked to be sure every issue on the board is real. Six of my own claims were wrong or overstated:

1. **ZP-4272 — my claim was wrong.** "Nothing persists, no grid state in localStorage, onColumnOrderChange wired
   to nothing" is false for the shared grid. Live today: `/assets` → hide QR Code → writes
   `gridLayout_v1_assets = {"widths":{},"visibility":{"qr_code":false},"version":1}`; navigate to /locations and
   back → **still hidden** (16). The work-order Assets tab is a DIFFERENT component: hiding QR Code there writes
   **no key at all** and the column is **back** after navigating away and returning (17). Its column menu is also
   different (Pin to left/right vs Sort ASC/DESC). Correct claim: **partial fix — retention shipped on the shared
   grid, not on the work-order Assets grid.**
2. **ZP-4151 — my ✘ was invalid.** The secondary account's "Assets 0" tile is the **licence-entitlement meter**
   (`useEntitlementStore(s => s.accountUsage(accountId))` in AccountDetails), not an asset roll-up. Reading it as
   "the shared site's 68 assets are missing" was wrong. The linkage half passes; acceptance line 2 is *unverified*,
   not failing.
3. **ZP-4322 — cannot reproduce.** Today's bundle carries the tooltip `wrapperStyle:{zIndex:1}` (PR #1508), and on
   this viewport the Assets-by-Type card renders as a centre total + legend with **no donut arcs to hover** (18).
   Downgraded from open defect to "fix in build, not reproducible today".
4. **ZP-4189 — not re-measured.** PR #1506 merged after the 22 Sep 7,176 ms reading. I could not re-measure:
   the automated tab runs backgrounded, so Chrome records no paint/LCP entries at all (`supportedEntryTypes`
   includes LCP, but 0 entries and `first-contentful-paint` null). The number stays labelled 22 Sep, not "current".
5. **ZP-4042 — priority softened.** The ticket is **Medium**, not a blocker; and the client always sends `sld_id`.
   The 14 Sep evidence used that shape from three seats, so the fault is real — but "release blocker" was my label,
   not Jira's.
6. **ZP-4218 — downgraded to "not verified".** PR #1467 shipped an RFQ affordance for rows already subcontracted;
   the Convert path was never re-walked after it. Not a proven defect today.
Also restated: **ZP-4138** mechanism is a tier-2 inversion (`requiresTier2: !(seat holds one of five staff roles)`),
not a role allowlist; **ZP-4181** is an iOS Sentry ticket whose web mitigation is dead code (not a "web regression");
**ZP-3919** keeps one defect (email not carried) — the Google / passkey items are tenant-config absence, not defects,
and backup codes are an unimplemented acceptance item; **ZP-4030**'s AASA half rests on a developer comment, not an
acceptance criterion, so only the message half is a defect.
