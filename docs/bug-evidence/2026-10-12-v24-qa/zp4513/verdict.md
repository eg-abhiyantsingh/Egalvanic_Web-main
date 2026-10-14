# ZP-4513 — Add a global Motor Overload node class and an 'MC' symbol for Motor Controller (migration movl_a1)

**Verdict:** PASS (all web-testable rows). The migration re-run and downgrade rows are backend-only (NOT WEB-TESTABLE).
**Build:** QA `index-C14cwR0U.js` (live 17:32 and 18:11 IST, 13 Oct).
**Tested:** 13 Oct 2026, 17:35–18:10 IST. Project Manager (main), Super Admin, Client Portal.
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`): QA G-Motor, QA G-MotorController, QA G-MotorOverload, QA G-MotorStarter, QA MC1, QA MOL1. New today: "QA-DEMO ZP-4514 MC (delete me)", "QA-DEMO ZP-4514 MOL (delete me)".
**Ticket re-read live:** In QA, 0 comments.
**Flags:** none. The symbols come from the database.

## Checked

| Check | How | Result |
|---|---|---|
| "Motor Overload" class exists and can be placed | + Asset picker; class list | PASS. AC tab › PROTECTION › "Motor Overload" with the two-arc symbol (shot 3). Class key `motor-overload`: device_role 2 (protective), OCP on, 1 in / 1 out, not library-backed (`use_eqp_lib:false`), no core attributes, icon `motorOverload`. Placing one creates the asset (`POST /node/create` 201, 7 created today). |
| Motor Overload draws two opposed arcs, not a motor | Canvas | PASS (shot 1). The SVG is "terminals + two opposed interlocking arcs". |
| Motor Controller draws "MC" in a square; Motor keeps the M in a circle | Canvas, PM and Super Admin | PASS (shots 1, 2). Icons `motorController` and `motor` are different. New Motor Controller assets get "MC" too (ZP-4514 shot 1). |
| SKM export: Protection with ProtectionType "Motor Overload" | Export › Export Engineering XML on my site | PASS. `<s:Protection s:name="QA G-MotorOverload"> … <s:Field s:name="ProtectionType" s:value="Motor Overload"/>` (QA MOL1 the same). |
| SKM import of a Motor Overload | Import preview of a crafted file | PASS. A `Protection` with ProtectionType "Motor Overload" maps to class Motor Overload. |
| One override per company | Acme class list | PASS for Acme (override row present, `is_override:true`). Other companies: NOT WEB-TESTABLE. |
| Roles | Super Admin; Client Portal | Super Admin sees the same symbols (shot 2). Client Portal: no SLD (Access Denied, checked on ZP-4800). |
| Migration re-run is a no-op; downgrade | — | NOT WEB-TESTABLE. Dev to confirm in a rolled-back transaction. |

## Bugs found
None.

## Enhancement suggestions
1. Motor Starter still uses the Motor icon (M in a circle). Give it its own symbol, as was done for Motor Controller.
2. Motor Controller exports to SKM as a plain `Load` (no `skm_config`). An SKM `MotorController` mapping would keep it a controller after a round trip. Today a Motor Starter comes back as a Motor Controller (see ZP-4603 out-of-scope bug).

## Mobile part for Nency
No app change is needed (the viewer draws `icon.svg` from the database). On iOS and Android, open site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)". QA G-MotorController must show "MC" in a square, QA G-Motor the M in a circle, and QA G-MotorOverload the two arcs. Also check that "Motor Overload" can be picked as a class when adding an asset on mobile.

## Not covered
Migration re-run and downgrade (backend only). Overrides for other companies.

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C14cwR0U.js (QA): PASS

# + Asset > AC tab > PROTECTION offers "Motor Overload". It is protective, OCP on, 1 in / 1 out, and has no library.
# Motor Overload draws two opposed arcs. Motor Controller draws "MC" in a square. Motor still draws the M in a circle (Project Manager and Super Admin).
# SKM export writes it as a Protection with ProtectionType "Motor Overload". The SKM import maps it back to Motor Overload.

!QA-13Oct-ZP-4513-1-PASS-motor-vs-motor-controller-vs-motor-overload.png|width=720!
!QA-13Oct-ZP-4513-3-PASS-picker-motor-overload-under-protection.png|width=720!
!QA-13Oct-ZP-4513-2-PASS-super-admin-sees-same-symbols.png|width=720!

*Not checked from the web:* migration re-run and downgrade (backend).
*Suggestion:* Motor Starter still shares the Motor symbol.

*Status:* moved to READY TO RELEASE
```

## Recommended status
**READY TO RELEASE.**
