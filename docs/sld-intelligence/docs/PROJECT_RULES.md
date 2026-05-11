# K STAR Project Rules

Single source of truth for all conventions used in the K STAR / Cytiva SLD
digitization work. Every rule below is enforced somewhere in `src/rules/` or
`src/validators/`.

If you change a rule here, update the corresponding code module **and** its
test in `tests/unit/`.

---

## 1. PDF formats

K STAR ships SLDs in three distinct file formats — pick the right extractor.

| Format | Magic bytes | Detection | Extractor |
|---|---|---|---|
| **Real vector PDF** | `%PDF-` | `src.pdf.triage.PdfKind.VECTOR_PDF` | `src.pdf.vector_extract` |
| **Procore Markup ZIP** | `PK\x03\x04` | `PdfKind.PROCORE_ZIP` | `src.pdf.procore_zip` |
| **Raster-only PDF** | `%PDF-` (but no geometry) | `PdfKind.RASTER_PDF` | OCR fallback (not wired) |

**Always call `triage()` first**, then route. The Procore ZIPs in K STAR's
deliverables look like `.pdf` but are actually ZIPs containing pre-extracted
text (`1.txt`) and a rasterized drawing (`1.jpeg`). Treating them as real
PDFs returns zero content.

---

## 2. eGalvanic class taxonomy

Three groups, with strict membership rules. See `src/rules/classes.py`.

### OCP classes — *must* have a `parent_asset_name`

`3-Pole Breaker`, `Circuit Breaker`, `Disconnect Switch`, `Fuse`,
`Fused Disconnect Switch`, `Integrated Transformer`, `MCC Bucket`,
`Other (OCP)`, `PLCs`, `Relay`

### Box container classes — *can be* a `parent_asset_name`

`DC Bus`, `Disconnect Switch`, `Loadcenter`, `MCC`, `Motor Starter`, `Other`,
`Panelboard`, `PDU`, `PLCs`, `Switchboard`, `VFD`

### Leaf / link classes — connected via Connections only

`ATS`, `Battery`, `Busway`, `Capacitor`, `Generator`, `Inverter`,
`Junction Box`, `Load`, `Meter`, `Motor`, `Oil Transformer`, `Reactor`,
`Rectifier`, `Temperature Sensor`, `Transformer`, `Transformer (3-Winding)`,
`UPS`, `Utility`

`Disconnect Switch` and `PLCs` appear in both OCP and Box — they can serve
either role depending on context.

---

## 3. ATS dual-input rule

Every ATS has exactly **two** target handles representing its two power sources:

| Handle | Source role | Typical source class |
|---|---|---|
| `top-target-0` | Normal (utility-side) | Utility, Transformer |
| `top-target-1` | Emergency (backup-side) | Generator |

**Both handles must be populated and must be distinct.** If both inputs end
up on the same handle, eGalvanic's UI flattens them and the topology displays
incorrectly. Validator: `src.validators.structural` stage 6.

If the E/N labels on the SLD are unclear, **stop and ask** — don't guess.

---

## 4. Upstream placeholder rule

Don't try to model the full utility chain (substation → primary transformer →
meter cabinet → service entrance → main breaker). Use a single `Utility`-class
placeholder that connects to the building's first real breaker.

| Situation | Placeholder name |
|---|---|
| Building has its own utility tap | `UTILITY-{BLDG}` (e.g. `UTILITY-B33`) |
| Building fed from another building's DP | `EXISTING-FEEDER` or the literal DP name from the SLD |

Internal ATSes (ATS-02, GATS-01, etc.) downstream of this placeholder still
get full E/N modeling per rule 3. Helpers: `src.rules.chains.upstream_placeholder_for_building`.

---

## 5. B30+ chain pattern

Medium-voltage buildings (B30 and later, plus B12's left side) use the full
in-series modeling pattern. Order from utility down to MAIN:

1. **300E** primary fuse — `Fuse`
2. **NLIS** primary load interrupter switch — `Disconnect Switch`
3. **K-interlock** kirk-key relay — `Relay`
4. **NTMV** medium-voltage transformer — `Transformer`
5. **CBB** busway — `Busway` (always keep, never collapse)
6. **Neutral connection box** — `Junction Box`
7. **F-TVSS** protection fuse — `Fuse`
8. **TVSS** surge suppressor — `Other`
9. **EMM** energy meter — `Meter`
10. **LDU** load distribution unit — `Meter`
11. **MAIN** breaker (child of NSWGR) — `Circuit Breaker`

Skip from this chain: lightning arresters (LAs), NGR, ammeters, RCM/CBCT/CTs,
differential relay (87). See rule 7.

---

## 6. Fuse handling

Don't use the bare `Fuse` class for normal feeders. Use:

| SLD shows | eGalvanic class |
|---|---|
| Fuse + switch in one enclosure | `Fused Disconnect Switch` (combine into ONE asset) |
| Disconnect/switch without integrated fuse | `Disconnect Switch` |
| Standalone primary fuse (e.g. F-300E) | `Fuse` (keep — but only for documented primary fuses) |
| Standalone non-primary fuse | `Disconnect Switch` (rename for clarity) |

Naming: when renaming, strip the `F-`/`FUSE-` prefix and use `DS-` (e.g.
`FUSE-OVERHEAD-80A` → `DS-OVERHEAD-80A`). Helpers: `src.rules.fuses`.

---

## 7. Skip list

Items that appear on SLDs but are **not** modeled in eGalvanic:

- Lightning arresters (LA, LIGHTNING ARRESTER)
- Neutral grounding resistors (NGR)
- Ammeters / panel meters (simple instruments)
- Residual current monitors (RCM), core balance CTs (CBCT), bare CTs
- Differential protection relays (ANSI 87, DIFFERENTIAL RELAY)

Helpers: `src.rules.fuses.should_skip(label)`.

---

## 8. Naming conventions

### Container assets

- New equipment with level: `{BLDG}-{LEVEL}-{KIND}-{NUM}` (e.g. `B33-1.0-NSWGR-01`)
- Existing equipment: `{BLDG}-EX-{KIND}-{NUM}` (e.g. `B33-EX-NLIS-01`)

### Main breakers (three accepted formats)

| Format | When to use | Example |
|---|---|---|
| `CB-MAIN-{BLDG}-{PANEL}` | **Preferred for new work** (site-wide collision-free) | `CB-MAIN-B04-NPNLB01` |
| `CB-MAIN-{PANEL}` | Legacy — kept for B33-side panels | `CB-MAIN-NPNLB03` |
| `CB-MAIN-{AMPS}` | When panel name is implicit (B01/B02 only) | `CB-MAIN-300A` |

### Feeder breakers (children of a panel)

- `CB-{TARGET-SHORTNAME}-{AMPS}` — preferred (e.g. `CB-VAV105-20A`)
- `CB-CKT{N}-{AMPS}` — when target isn't labeled (e.g. `CB-CKT1-125A`)

### Motors and loads

Match the actual SLD tag verbatim. Never invent. Examples:
`M1B01VAV105`, `M1B4ACT118`, `M1B4HP317`.

Helpers: `src.rules.naming`.

---

## 9. Column schema for bulk upload

Canonical template (`Memorial_Hospital_bulk_upload.xlsx`) has 15 base columns,
all snake_case:

```
asset_id, asset_name, asset_class, asset_subtype, building, floor, room,
com, criticality, operating_conditions, maintenance_state, suggested_shortcut,
qr_code, parent_asset_name, delete
```

Optional property columns (snake_case in the spec):

```
voltage, ampere_rating, kva_rating, primary_voltage, starting_voltage
```

Per Mukul: blank `building` is acceptable for site-level utility/overhead
equipment. eGalvanic accepts the file regardless of case (v1/v2/v3 uploaded
fine with Title Case), but snake_case is the spec.

Defaults: `floor = 1`, `room = "default"` unless overridden.

---

## 10. Workflow rhythm

Section-by-section, one container at a time:

```
Cropped SLD region → assets+conns rows → validate → upload to eGalvanic → next
```

If a label or value is unreadable on the PDF, **stop immediately** and ask
for a zoomed screenshot — don't silently blank or guess.

---

## Quick reference — which validator to run

| Stage | Tool | Catches |
|---|---|---|
| Before merge | `src.pdf.triage` | Wrong PDF extractor for this file |
| After merge | `src.validators.structural.validate` | Upload-blocking issues |
| Before publish | `src.validators.audit.audit` | Format drift |
| Reviewing a change | `src.compare.compare` | What actually changed |
