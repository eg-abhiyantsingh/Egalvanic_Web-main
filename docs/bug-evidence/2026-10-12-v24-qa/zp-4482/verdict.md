# ZP-4482 — Engineering XML export ships as a zip bundle with READ THIS BEFORE IMPORTING.pdf

**Verdict:** PASS — build `index-D1IWZ6SI.js` (QA redeployed from `index-BdXGlAq5.js` ~19:35 IST; the main bundle + every SKM source is byte-identical between the two builds, source-map diff = 0 differing sources, and backend XML is byte-identical before/after, so both carry this code).

## Checked
| Check | How | Result |
|---|---|---|
| Both entry points download ONE zip | Assets > SKM > Export, and SLD > Export > Export Engineering XML, on Richmond, CA | both download a `.zip` holding `skm_export_*.xml` + `READ THIS BEFORE IMPORTING.pdf` + `uncategorized-assets.csv`. PASS |
| XML is byte-identical to the bare export | sha256 of zip XML vs the bare `POST /sld/{id}/export-skm` presigned XML, both entry points | identical (693779…/66e7b0…). PASS |
| PDF content | pdftotext of the README | Facts block + "Issues standing at export" (45 err/27 warn, each with asset count + "List in this folder" CSV name) + "Verify … Settings Verifier" (model has breakers) + "Exported as generic loads" table (6 classes) + "Exporter messages" + "Files in this folder". PASS |
| One CSV per worklist issue | large site (Addtioanl Site) | `uncategorized-assets.csv` + `3awg-redbook-cables.csv`, both scoped. PASS |
| View-scoped narrows issues/counts/CSV | SLD view (5 nodes) export | PDF scope "View: …", 5 comps/2 conns, 3 errors (vs 4 whole-SLD), rows scoped. PASS |
| Acknowledgement gate | model with errors + breakers | "Before you export to SKM" dialog; **Export Engineering XML button disabled** until the error-ack (and Settings-Verifier-ack when breakers) is ticked; enabled after. PASS |
| "SKM will reset trip settings" → Settings Verifier link | ack dialog | for any model with a circuit breaker, the Settings-Verifier ack row appears with a "(download EG Utils)" link; Learn more → "How the Settings Verifier works" opens the Settings Verifier dialog. PASS |
| Negative: issue check fails | aborted the preflight `/sld/{id}/graph` | XML still ships in a zip; PDF prints "The issue check did not run … Reason reported: Failed to fetch"; toast "Exported, but the issue check could not run." PASS |
| Negative: bundle can't be built | aborted the `jspdf` chunk | **bare `.xml`** downloads (no zip); toast "The export summary could not be built, so only the XML was downloaded." PASS |
| zip integrity | python `zipfile.testzip()` | None (valid), store-only (compress_type 0) as described. PASS |
| Roles | SuperAdmin/Admin/PM/AM/EE/FM/CP/Tech/PortalSales | SKM dropdown present SA/Admin/PM/AM/EE/FM; **absent for CP** (no `sld_diagrams.view`); Tech "Web Access Restricted"; Portal Sales no sites. PASS |
| Flags OFF (simulated) + LD unreachable | rewrote evalx so `feature-skm-export/import/import-legacy`=false; and blocked launchdarkly.com | Assets SKM button disappears (fails closed). BUT the **SLD page Export menu still lists "Export Engineering XML"** in both states — see B2. |
| Performance | Addtioanl Site (1818 assets) full UI export | graph fetch 3.5 s, export-skm 3.0 s, S3 XML 1.0 MB in 3.3 s, zip assembled client-side; whole flow ~10.7 s. No duplicate graph fetch (preflight result reused). Acceptable. |

## Bugs found
**B1 (Low, pre-existing) — Data State stays "Incomplete" on exported components** — not in scope of this ticket (export-time downgrade), noted only so it isn't re-raised.

**B2 (Medium) — the SLD-page "Export Engineering XML" item is NOT behind `feature-skm-export`.**
- Steps: simulate `feature-skm-export=false` (or block LaunchDarkly). (1) Assets page — the SKM dropdown's Export item is correctly hidden. (2) Engineering > SLD > Export — "Export Engineering XML" is **still offered** and still works.
- Actual vs expected: the two entry points disagree under the flag; `ExportMenu` gates the item only on `pdfOnly`, not on `feature-skm-export`. Flag OFF should hide both.
- Severity: Medium (a killswitch that only half-works). Evidence: `QA-12Oct-ZP-4482-3`, and the role/flag sweep. Assignee: Krunal (frontend). No existing ticket.

## Enhancement suggestions
- The zip is named `<xml-basename>.zip`; include the site name so a folder full of exports is scannable.
- README PDF "Issue check 45 errors…" has no link back to the SLD Issues page — add the site/SLD URL so the importer can jump to the fix.

## Mobile part for Nency
The ticket note says files under `src/sld/` changed, so **iOS needs the SLD viewer OTA bundle republished per environment**; the PDF's Settings-Verifier link is plain text in the embedded iOS viewer (no .msi download). Nency: confirm the iOS SLD viewer renders the new export flow and that the PDF/zip handoff works from the app.

## Not covered
Did not open the XML in SKM PowerTools (desktop tool, not web-testable). Did not exercise a real customer-authored PTW file.

## Proposed Jira comment
h3. QA — ZP-4482 PASS (QA build index-D1IWZ6SI.js)

Both entry points (Assets > SKM > Export, and SLD > Export > Export Engineering XML) download one zip = the XML (byte-identical to the bare export) + READ THIS BEFORE IMPORTING.pdf + one CSV per worklist issue. The PDF lists standing SLD issues, exporter messages, generic-load classes, Settings Verifier steps and the file list. A view export narrows the issues/counts/CSV rows.

!QA-12Oct-ZP-4482-2-PASS-acknowledge-dialog-before-download.png|width=720!
!QA-12Oct-ZP-4482-5-PASS-readme-pdf-files-list.png|width=720!

Graceful degradation works: forcing the issue check to fail still ships the XML in a zip with a PDF that says the check didn't run; forcing the bundle to fail downloads the bare XML with a UI message.

!QA-12Oct-ZP-4482-10-PASS-bundle-failed-bare-xml-message.png|width=720!

*One gap:* with the {{feature-skm-export}} flag OFF (or LaunchDarkly unreachable) the Assets entry point hides correctly, but the *SLD-page* "Export Engineering XML" item is still offered — it is gated only on pdfOnly. Suggest gating it on the flag too (Krunal).

## Recommended status
**READY TO RELEASE.** File B2 (Medium, flag-gating) as a separate frontend ticket.
