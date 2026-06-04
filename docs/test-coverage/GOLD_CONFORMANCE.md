# Gold-vs-Tests Conformance — why the class-attribute tests fail (2026-06-04)

Source of truth: [`testcase/node_classes_gold.json`](../../testcase/node_classes_gold.json)
(generated from `node_classes_template (12) - updated.xlsx` — 38 classes, 163 core
attributes, 65 subtypes). Produced by a 9-file diff workflow (Asset/Connection/Issue
class-attribute tests vs the gold). **89 mismatches** found.

## Root cause — four kinds of drift

The class-attribute tests hardcode their own assumptions about each class's attributes
(control type, option strings, attribute names, which class owns an attribute) instead of
deriving them from the gold, and those assumptions have drifted:

1. **Type drift (wrong-handler).** Gold says `textfield` but the test drives a dropdown
   (`selectFirstDropdownOption` first), or the reverse (`editTextField` typed into a
   `select`). The primary handler can never bind the real control; it only "works" because a
   fallback fires — so saves silently don't persist. e.g. ATS Manufacturer (select, tested as
   textfield), Generator/Motor Voltage + Configuration (textfield, tested as dropdown),
   Motor/UPS Manufacturer, Transformer Type.
2. **Mis-assigned / renamed options (invalid-option-value).** Hardcoded values aren't literal
   gold options: `"65kAIC"`→`"65 kA"`, `"480"`→`"480V"`, `"GE"`→`"General Electric (GE)"`, MCC
   subtype `"(<=1000V)"`→`"(<= 1000V)"` (missing space).
3. **Renamed attribute labels (spaced acronyms).** Compact labels miss gold's spaced names
   under starts-with matching, binding nothing and logging `val=null` as a pass:
   `"KVAR Rating"`→`"K V A R Rating"`, `"PCB Labeled"`→`"P C B Labeled"`, `"UF Rating"`→
   `"U F Rating"`, `"RPM"`→`"R P M"`. Also 17 attrs are lowercase/camelCase in gold
   (`Generator:configuration/voltage/manufacturer`, `Fuse:fuseAmperage`, `Relay:model`).
4. **Attributes / classes that moved or don't exist.** `Configuration` is tested on
   Switchboard/Motor/MCC/Loadcenter but exists only on **Generator**; **UPS** is given
   `Ampere Rating`/`Voltage` it lacks; the class `"Load Center"` is **`Loadcenter`** in gold
   (and has only `Size`+`Mains Type`); `"Power Distribution Unit"` is **`PDU`**; Fuse
   `Fuse Amperage`/`Fuse Manufacturer` are Capacitor labels (gold uses camelCase
   `fuseAmperage`/`fuseManufacturer`, options UPPERCASE `BUSSMANN`).

> ⚠️ **The gold itself is partly corrupted.** Many `textfield` attributes carry option
> arrays copied from unrelated fields (Generator `Ampere Rating`→voltages, `manufacturer`→
> amperes, `Serial Number`→brands; Capacitor `Style`/`Type`; Transformer `Primary Voltage`
> mis-split `13`/`800V`). 28 such rows. So only **handler-type, attribute-name, and
> existence** mismatches are fully reliable; option-value fixes must wait for a gold repair.

## Highest-confidence edits (certain to flip / correctness-critical)

1. **AssetPart2 · testATS_EAD_17_SaveAllRequired** — `editTextField("Manufacturer",…)` →
   `selectDropdownValue("Manufacturer","Siemens")`. Hard-asserts `saved==true` (L869);
   free-typing into the Autocomplete can't persist → guaranteed red.
2. **AssetPart3 · ALL LC_* tests** — class `"Load Center"` → **`"Loadcenter"`** (phantom class).
3. **AssetPart4 · testPDU_01/_12/_13/_AST_01** — class `"Power Distribution Unit"` → **`"PDU"`**.
4. **AssetPart2 · testCAP_EAD_14/_18/_23** — labels `"K V A R Rating"`, `"P C B Labeled"`, `"U F Rating"`.
5. **AssetPart3 · testMCC_AST_02/_03/_04** — spaced subtype `"Motor Control Equipment (<= 1000V)"` / `"(> 1000V)"`.
6. **AssetPart5 · testSWB_07 / testUPS_05 / testUPS_12** — `Configuration` (SWB), `Ampere Rating`/`Voltage` (UPS) don't exist on those classes — remove/retarget.
7. **AssetSmoke · testUpdateAsset** — `editModel` writes the QR Code field, not Model — rename/add a real Model handler.
8. **AssetPart3 · testFUSE_EAD_10/_11** — retarget to gold selects `fuseAmperage`/`fuseManufacturer`; assert `30A`/`BUSSMANN`.
9. **AssetPart3 · testGEN_04 / testJB_AST_01 / testMCCB_AST_01** — relax `assertNotNull` on Subtype (no subtypes in gold for these).
10. **ConnectionPart2 · testCONN_081b/_081c** — hard-assert six Cable attrs that are `[]` in gold → gate behind gold-regeneration (gold likely stale for Cable/Busway) or downgrade to log-only.

## Full fix table

(89 rows across AssetPart1-5, AssetSmoke, ConnectionPart2, ConnectionTestNG; IssuePart2 is
Issue-schema, not gold-governed — locator-stability only.) The per-file breakdown — wrong-handler
and invalid-option first — is preserved verbatim in the workflow output; the highest-confidence
subset above is what to apply first.

## Recommendation — convert to data-driven from the gold

Nearly every finding is a hardcoded assumption that drifted from the gold. A gold-driven
`@DataProvider` that per (class, attribute): (1) uses `type` to pick the handler (combobox for
`select`, text input for `textfield`) — killing every wrong-handler defect and the masking
dropdown-then-textfield fallback; (2) selects/asserts only a real gold `option`; (3) uses the
exact gold attribute name + class key; (4) reads `required`/`subtypes`/`ocp` for counters and
presence — would close almost the entire finding set and make the suite self-updating.

**Two prerequisites:** (a) **repair the gold first** (the 28 mis-optioned `textfield` rows +
add the missing Cable/Busway Core Attributes, regenerated from the live app); (b) keep
Issue/Connection-CRUD suites **out** of the asset harness — their schema isn't governed by the
gold; their issues are locator-stability + swallowed-exception, not conformance.

## Applied this pass

The mechanical, gold-reliable edits (class names + handler types — independent of the corrupted
options) are applied to the asset suites; see the commit. Option-value alignment and the full
data-driven migration are gated on a gold repair (tracked above).
