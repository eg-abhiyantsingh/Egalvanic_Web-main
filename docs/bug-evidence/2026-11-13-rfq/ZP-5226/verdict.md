# ZP-5226 · Transformer X/R in the SKM export: typical X/R + "X/R Override" reserved attribute — QA 13 Nov 2026

**Verdict: PASS** on QA, build **index-C6DvLko9.js**. The exporter is backend-only; the earlier build of the day had the same frontend. The ticket says "dev only", but the code is live on QA: the fingerprint is the `xrOverride` attribute and the 720948 field in the export.

**Test site:** my own QA-DEMO site, "QA-DEMO ZP-5226 13 Nov transformer XR" (`8027ad52-d39d-4460-995e-5edd88ebf156`). None of its transformers has an equipment library, so there is no library R/X split:

| Asset | kVA | %Z | X/R Override |
|---|---|---|---|
| TX-A | 500 | 5.75 | — |
| TX-B | 1500 | 5.75 | **8.5** |
| TX-C | 75 | 3.5 | — |
| TX-D | 2500 | 6 | — |
| TX-E | 300 | — (no %Z) | — |

## Checked
| Check | How | Result |
|---|---|---|
| Reserved attribute exists | `GET /api/node_classes/user/<id>` | `xrOverride` "X/R Override", type number, **reserved: true, exclude_ai: true**, on **Transformer** (Acme override class, `is_override:true`) and **Transformer (3-Winding)**. Same attribute id on both; exactly one per class, so the migration did not run twice ✅ |
| Locked in the class editor | Admin › Asset Classes › Transformer › Core Attributes | X/R Override shows the padlock tooltip "Reserved system attribute — managed by the platform (pricing, SKM export, imports). It can't be edited or deleted…". The delete icon is disabled. **"Exclude from AI" is selected and disabled** ("Include in AI" is disabled too) ✅ (screenshots 1, 2) |
| Value on an asset | Asset TX-B › Engineering | X/R OVERRIDE = 8.5 ✅ (3) |
| Typical X/R when kVA + %Z and no library R/X | `POST /api/sld/<id>/export-skm`, plus the UI: SLD › Export › Export Engineering XML › "Before you export to SKM" › Export | Field **720948 X/R** is filled. TX-A 500 kVA → **4.6944**; TX-C 75 kVA → **3.1562**; TX-D 2500 kVA → **10.4732**. The value rises with kVA ✅ (4, 5) |
| Rpos/Xpos derived; Rzero/Xzero = pos | Arithmetic on the exported values | TX-A: Rpos 1.1980, Xpos 5.6238 → √(R²+X²) = **5.750 = Z%**, X/R = 4.694 ✅. TX-C: R 1.0571, X 3.3365 → 3.500 ✅. TX-D: R 0.5703, X 5.9728 → 6.000 ✅. Rzero = Rpos and Xzero = Xpos on every transformer ✅. ImpedanceBase = NominalKVA |
| Override wins | TX-B with X/R Override 8.5 | X/R **8.5000**, Rpos 0.6718, Xpos 5.7106 → √ = 5.750 ✅ |
| Only when kVA **and** %Z | TX-E: kVA, no %Z | No X/R, Rpos or Xpos fields (Nominal/FullLoad kVA only) ✅ |
| Re-import must not populate X/R Override | Unchanged native export → `POST /api/skm/import-xml/preview` (5 × "update") → `/commit` (updated_nodes 5) | **X/R Override stays empty on TX-A, C and D**. TX-B keeps its own 8.5. A second export gives identical X/R values ✅ |
| UI export | The UI downloads the same XML | Same values (`QA-13Nov-ZP-5226-skm-export-from-UI.xml`); no warnings, no unconfigured classes ✅ |
| Flags | `feature-skm-export` (live ON) gates the menu item | SIMULATED OFF: "Export Engineering XML" is hidden; the other export items remain ✅ (`flag-skm-export-OFF-SIMULATED.png`). No new flag for this ticket |
| Roles | The class editor route is Admin / Super Admin only (bundle route roles) | Checked as SA. The export was not re-run per role (unchanged existing gate) |
| Performance | Export API | 0.5–1.0 s for 5–7 components; XML about 5 KB |

## Bugs found (separate from this ticket's scope)

### 1. SKM round trip silently shortens asset names to 25 characters
**Steps:**
1. On a site, create a transformer named `QA-DEMO ZP-5226 TX-F long name round trip 13 Nov` (47 characters).
2. SLD › Export › Export Engineering XML. The file has `s:name="QA-DEMO ZP-5226 TX-F long"` (25 characters).
3. Assets › SKM › Import (Full) with that same, unchanged file, then commit.
4. Open the asset.

**Actual:** the asset's name in eGalvanic is now `QA-DEMO ZP-5226 TX-F long`. The preview lists the row as "update" and shows no warning that the name will change.

**Expected:** re-importing an unchanged native export keeps the full eGalvanic name, or at least warns before the rename.

**Control:** a short name (`QA-DEMO TX-G short`) is unchanged.

**Proof:** reproduced twice, first on TX-A…E, then on TX-F with the TX-G control, same SA seat.

| Field | Value |
|---|---|
| Severity | Medium (silent loss of user data) |
| Suggested assignee | Avani (backend import/export) |
| Evidence | `QA-13Nov-ZP-5226-flag-skm-export-OFF-SIMULATED.png` and screenshot 4: the SLD shows the shortened names "TX-A 500k", "TX-B 1500" and others. Raw data in `r06-out.json` and `r20` output |
| JQL run | `project = ZP AND text ~ "SKM" AND (text ~ "truncat*" OR text ~ "25 char*" OR text ~ "label overwritten" OR text ~ "name length")`. Nothing covers it. ZP-3836, the SKM round trip ticket (Done), does not mention names |

## Question for Eric
- The typical X/R values are **4.6944 @ 500 kVA, 3.1562 @ 75 kVA, 10.4732 @ 2500 kVA**.
- They rise with size, and the values are not round, which suggests interpolation from a table.
- I cannot press PTW's "Typical X/R" button on QA. Can you confirm these match PTW for the same kVA, or share the table source?

## Enhancement suggestions
- **Show the computed typical X/R next to the empty "X/R Override" field** (for example placeholder "typical 4.69 for 500 kVA"). Engineers would then see what will be exported before deciding to override.
- **Add a "Typical X/R used" note in the export summary**, listing the transformers that got a typical value, the same way unconfigured classes are listed.

## Mobile impact
- X/R Override is a reserved transformer attribute, so it should appear on the iOS and Android transformer asset forms.
- A mobile tester should check that it shows as a number field, saves a value, and is not offered to AI extract.
- The exporter itself is backend-only.

## Not covered
- **Exact equality with PTW's Typical X/R.** No PTW on QA; see the question above.
- **A transformer that HAS a library R/X split.** I did not build one with an SKM transformer library entry, so "library R/X wins over typical" is not proven.
- **Migration re-run (`xfmrxr_a1` NOT EXISTS guard).** Database-only and not web-testable. Indirect evidence: one `xrOverride` per class.

## Test data
- Created my own site "QA-DEMO ZP-5226 13 Nov transformer XR" with 7 transformers.
- The re-import renamed them to the 25-character SKM names (the bug above).
- No settings changed.

## Evidence
- `QA-13Nov-ZP-5226-video-steps.mp4` (85 s; contact sheet checked).
- Screenshots 1–5.
- Exported XML files: `-skm-export-1.xml`, `-2-after-reimport.xml`, `-from-UI.xml`.
- Scripts: `r06`, `r15`, `r16`, `r18`, `r20`.

## Recommended status
**READY TO RELEASE.** The name-shortening bug is separate and pre-existing, not caused by this ticket.
