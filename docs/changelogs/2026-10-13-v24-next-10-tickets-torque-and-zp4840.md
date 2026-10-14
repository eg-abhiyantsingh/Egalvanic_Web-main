# Web v2.4: next 10 tickets, Torque Rules question, bulk-service bug ZP-4840 (13 Oct 2026)

- **Build:** QA `index-COeCBn0z.js`.
- **Account:** all Jira writes from abhiyant.singh@; the account id was checked before every write.

## Tickets
| Ticket | Result | Jira |
|---|---|---|
| ZP-4565 Maintenance Portal gate | PASS with real LD OFF 12:57 → ON 13:06 IST, 9 seats | comment 45108 → READY TO RELEASE |
| ZP-4562 Hide Fill from Photos | PASS (re-checked) | 45109 → RTR |
| ZP-4614 Link adds no services | PASS (fresh link re-checked) | 45110 → RTR |
| ZP-4720 Cable/Busway in services + PM plans | PASS (re-checked) | 45111 → RTR |
| ZP-4680 Report preview site picker | PASS (281/281 sites searchable, tenancy OK) | 45112 → RTR |
| ZP-4777 Invalid UUID (view-link drag) | PASS (0 bad ids, 0 errors) | 45113 → RTR |
| ZP-3424 Asset table URL state | All 8 ACs pass; Search not re-applied after Back (Medium) | 45114, stays In QA |
| ZP-4559 WO list role ordering | core multi-role case needs an AM+PM non-staff seat; single roles pass | 45115, stays In QA |
| ZP-4684 Service apply keeps .docx | HTML path passes; .docx path not testable (no .docx form on QA) | 45116, stays In QA |
| ZP-4801 IR photos Tuned | FAIL: closed WO still saves Tuned; double-click desync; unassigned-site PUT 200; Space key; 500 on odd bodies | 45118 → To Do |
| ZP-4717 EG form boolean visible | PASS (backend normalize + renderer, simulated raw booleans) | 45119 → RTR |

ZP-3424, ZP-4680 and ZP-4777 moved Ready for QA → In QA (Start Testing) before testing.

## Torque Rules question (owner, site "14 augest abhiyant")
- **Class flag is retired.** `torque_eligible` on node classes is no longer read (swagger, `GET /node/{id}/record-sections`).
- **Actual rule:** an asset gets Torque only if it already has torque records, or a service whose form has a Torque Log block (or a TV/DGA-type service) evaluates to the asset.
- **QA has no such service.** There is no "Torque Verification" service, and Clean, Tighten, Torque carries no Torque Log. That is why every site shows "No torque-eligible assets". Across 116 PM sites, only 1 asset was listed: a QA-DEMO transformer that already had records.
- **Created on the owner's site:**
  - "Torque test Panelboard" (07482e06…) with one torque record (Line lug, 50 ft-lb). It now shows on Torque Rules as "Not read".
  - "Torque test Transformer" (19fd641b…, oil-filled). Not eligible: the DGA service feeds Fluid Samples, not torque.
- Evidence: `docs/bug-evidence/2026-10-13-torque-asset/`.

## ZP-4840 (new bug, owner request, not tested by QA)
- **What happens:** WO bulk edit › Services on more than 500 selected assets → 400 "at most 500 assets per request", and the error replaces the whole page.
- **Where:** production basselectric, WO 1029, 1,512 assets.
- **Ticket fields:** High, Krunal, Web v2.4, sprint Z-26-10-S1, To Do.
- **Attachments:** the video (re-encoded to 1.1 MB) and 2 frames. Evidence: `docs/bug-evidence/2026-10-13-bulk-service-500-cap/`.

## Bug batch filed 13 Oct (owner: "yes file issue … assign to avani only")
- **How they were filed:** all from abhiyant.singh@, assigned to Avani, Web v2.4, sprint Z-26-10-S1, status To Do. Each has screenshots and a "Relates" link to its parent ticket.
- **Re-verified first:** every finding from 12 Oct was re-checked on build `index-COeCBn0z.js` before filing.
- **ZP-4840 (bulk service, >500 assets):** moved from Krunal to Avani on the owner's "yes".

| Key | Pri | Title |
|---|---|---|
| ZP-4841 | High | Customer user reads another company's quotes (`quote/by-sld`) |
| ZP-4842 | Low | `quote/create-standard` malformed site id → 500 |
| ZP-4843 | Low | Guest access OFF but typed `/guests` opens the page |
| ZP-4844 | Medium | Assets search text kept after Back but list not filtered (from ZP-3424) |
| ZP-4845 | Low | Report config site picker: no-match shows the current site (from ZP-4680) |
| ZP-4846 | Low | `/reporting/config/<bad id>` blank page, API 500 (from ZP-4680) |
| ZP-4847 | Low | `GET /api/opportunity/<bad id>` → 500 (from ZP-4777) |
| ZP-4848 | High | Customer user downloads another company's SKM file (`export-skm`) |
| ZP-4849 | High | SKM import writes to a site the PM isn't assigned to |
| ZP-4850 | Medium | SKM export flag OFF hides Assets item but not the SLD-page item (flag OFF simulated) |
| ZP-4851 | Medium | SKM re-import into same project says "Import 3 assets", creates 0 (from ZP-4582) |
| ZP-4852 | Low | Namespaceless SKM commit rows adopted by a later project (API-only trigger) |
| ZP-4853 | Low | SKM export tooltip promises "-1, -2" suffix / kept labels (from ZP-4746) |
| ZP-4854 | Medium | Unchanged bulk export re-import shows 20 changes (would overwrite) |
| ZP-4855 | Low | Bulk-edit export/validate/process 500 on a malformed site id |

- **Already filed earlier, not re-filed:** ZP-4823 (AWS details in report status) and ZP-4824 (Z University 500).
- **QA-DEMO data created on site `cfc07c0d…`:** probe node `13e0e9e3…`, plus G-BUS1..3 and H-BUS1..3.
- **Foreign-company data:** the other company's XML copies from 12 Oct are no longer on disk (only a 119-byte fingerprint file remains).
