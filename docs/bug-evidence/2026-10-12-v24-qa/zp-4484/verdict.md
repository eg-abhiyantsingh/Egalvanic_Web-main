# ZP-4484 — SKM XML export: emit GF_* ground-fault library fields on Protection

**Verdict:** PASS (field-by-field in XML; the "import into PTW" row is not web-testable) — build `index-D1IWZ6SI.js`.

## Checked
| Check | How | Result |
|---|---|---|
| GF_* block on a SST+GF breaker | Richmond, CA CB 451 & CT 450 (both have a paired GF device in the app) | both emit ids 99000100-99000109: GF_FunctionName="Ground", GF_Manufacturer=ABB, GF_Type, GF_Description, GF_CatalogNo, GF_TccNo, GF_Frame, GF_Sensor, GF_Plug, GF_Settings. PASS |
| Values match the stored pick | compared XML vs `eqp_lib.skm_settings.ground_fault` from `/sld/{id}/graph` | GF_Type/GF_Description/GF_Sensor(250)/GF_Plug(100)/GF_Settings all match (CB 451 `Function=Ground;GFD=0.1 (I^s T Off);`, CT 450 `GFPU=0.1;GFD=0.1 (I^s T On);`). PASS |
| Negative: breaker with NO GF pair | 82 of 84 Protection nodes on Richmond | zero GF_* fields emitted (no empty GF block). PASS |
| Frame/sensor/plug fallback to parent chassis | own site: set GF frame_id/sensor_id to non-existent ids | GF_Frame still "E1.2 S-A" (resolved via the parent breaker chassis). PASS |
| Resilience: GF device cannot be resolved | own site: GF cdevice/frame/sensor/trip_unit = 999999999 | GF_* omitted, the rest of the breaker exports intact, `POST /sld/{id}/export-skm` → 200, valid XML (ElementTree parse OK); warning "N assets exported as Incomplete — SKM cannot import the ground-fault settings". PASS |
| Synthetic id range | field ids | 990001xx, matching the EG-format ImpEx export per the ticket. PASS |
| Tenant/site scope | PM (customer) seat vs staff | export-skm leaks cross-tenant/cross-site — see **cross-cutting finding** below (not caused by this ticket; rides on the same endpoint). |

## Bugs found
No bug *introduced by this ticket*. See the cross-cutting SKM-export IDOR (filed separately; references `QA-12Oct-ZP-4484`-adjacent tenant evidence under zp-4582).

## Enhancement suggestions
- The GF breaker still exports Data State = "Incomplete"; a one-line note in the README PDF ("ground-fault attached but data state Incomplete") would stop engineers re-adding it by hand.

## Mobile part for Nency
Backend-only export change; no iOS/Android surface. Nothing for Nency beyond the shared SLD-viewer OTA note on ZP-4482.

## Not covered
Could not import the XML into SKM PowerTools (desktop) to confirm the Ground function attaches — verified every GF_* field in the XML instead. The standalone single-device library export (`_PROTECTION_FIELD_IDS`, which deliberately omits GF) is not reachable from the web UI; not tested.

## Proposed Jira comment
h3. QA — ZP-4484 PASS (QA build index-D1IWZ6SI.js)

On Richmond, CA the two breakers that carry a paired ground-fault device (CB 451, CT 450) now export a GF_* block (ids 99000100-109): Function "Ground", the GF library Type/Description, Frame/Sensor/Plug and the GF settings string — all matching the stored {{eqp_lib.skm_settings.ground_fault}} pick. The other 82 Protection components emit no GF fields.

!QA-12Oct-ZP-4484-2-PASS-xml-gf-block-richmond.png|width=720!

On a controlled test site: a valid GF pick emits the block; a GF whose frame/sensor id doesn't resolve falls back to the parent breaker chassis; and a wholly unresolvable GF device is omitted best-effort — the export still returns 200 and a valid file with the breaker intact.

!QA-12Oct-ZP-4484-3-PASS-xml-gf-fallback-and-unresolvable.png|width=720!

(Not web-testable: opening the file in PTW. Verified field-by-field in the XML.)

## Recommended status
**READY TO RELEASE** on feature correctness. The separate cross-tenant export IDOR is filed on its own.
