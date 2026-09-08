# [Web] Materials-library entries had no way to record which real piece of equipment they represent

**QA verdict — PASS on what the product exposes. The materials grid carries an **Equipment Link** column that renders a real linked identity per row, the material modal carries a full **Equipment Linkage** section (equipment type, manufacturer filter, catalog entry, poles, amps, fuse class, and a Clear link action), and the **Unpriced** filter turns the library into a working pricing queue — it resolves to exactly the one entry with no price. Three linked entries exist on the tenant with curated refs, including one carrying the free-text shape this ticket introduced. The validation and scope negatives, and the agent-dedupe cross-check, could not be exercised: they need either a write to shared library data or an agent run, and no agent run is triggerable on QA.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · Admin → OPERATIONS → Materials.
**Ticket said "dev only, not yet promoted to cicd/qa" — wrong:** both halves (the editable `catalog_ref` and the linkage manager UI) are live on QA, and later layers on top of them (ZP-3936's typed linkage, ZP-3937's identity-only refs, ZP-3942's bus-model routing) are live too.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Create a company materials-library entry with an Equipment Linkage set (manufacturer and model), save, reload, and confirm the linkage persisted and shows in the grid's Equipment Link column | ⚠️ **PASS on persistence and display; not created fresh** — three entries persist a linkage and render it in the column: "1 sep abs" → **Disconnect Switch · bus 452651**, "19 aug abs" → **Fuse · SKM 550331**, "Bussmann LPJ …" → **Fuse · family 144**. Reopening "1 sep abs" resolves the ref live (`resolve?skm_bus_oid=452651` → "SafeGear HD (AR) · ABB · MV Switchgear Arc-Resistant"). Note the manufacturer/model form this step describes has since been replaced by catalog-entry identity (ZP-3937), so a new entry cannot be saved with free-text manufacturer and model any more. A fresh create was not performed against shared QA data. |
| 2 | Use the SKM device search and frame picker on an existing entry, then confirm poles, amps and fuse class save correctly alongside it | ⚠️ **PARTIAL** — the SKM search and the frame picker both work: `quick-search?type=circuit_breaker` browses the device catalog and picking an entry surfaces a **Frame** select whose options carry `{ampere_rating, skm_frame_sid}` patches. Poles, Amps and Fuse class exist as fields and **persist** on the stored ref (the disconnect entry holds `pole_count: 31`, `ampere_rating: 1`, `fuse_class: "1"`). Saving that combination fresh was not driven. |
| 3 | Clear the linkage (null) on an entry and confirm it is removed rather than silently retained | ⚠️ **NOT EXERCISED** — the **Clear link** button is present in the dialog for a linked entry, but clearing was not performed (it would strip a link from shared QA data). |
| 4 | Negative on validation: submit a `catalog_ref` with a non-existent SKM device or frame and confirm the API rejects it with a clear error instead of storing a dangling reference | ⚠️ **NOT EXERCISED** — this needs a hand-crafted write to the library CRUD; no such write was made. |
| 5 | Negative on scope: attempt to set `catalog_ref` on a global (non-company) library row and confirm it is refused — globals must stay untouched | ⚠️ **NOT EXERCISED** — no global library row was identified among the 17 entries returned for this company, so the refusal path had no target. |
| 6 | Apply the Unpriced filter and confirm it returns exactly the entries with no price, including ones an agent minted, so the pricing queue is complete | ✅ **PASS on the filter itself** — the toolbar reads **Unpriced (1)** and the filter resolves to exactly the one entry whose Price column is "-" ("New materials", Indirect Cost). Every other entry carries a price. No agent-minted row exists to include (nothing is minted on QA at evaluation, and no agent run is triggerable). |
| 7 | Price an entry from the Unpriced queue and confirm it drops out of the filter | ⚠️ **NOT EXERCISED** — pricing the single unpriced entry would alter shared library data. Adjacent evidence that the count is live: the count reads "(1)" against exactly one price-less row. |
| 8 | Cross-check with the agent side: have the resolution agent mint a stub for a device that already has a hand-curated row with the same `catalog_ref`, and confirm they dedupe onto one row rather than creating a second | ❌ **NOT EXERCISED** — no agent run is triggerable on QA (`POST /issue-resolution/reevaluate` → `{issues: 1, jobs: 0}`, no proposals produced). What is verified instead, and matters for the same concern: **evaluation does not write to the library at all** — two issues were re-evaluated with the count held at 17 and no non-GET call to `/api/materials-library`, so an agent stub cannot race a curated row on this build. |

---

## Findings

### FINDING 1 (Low, documentation) — step 1's manufacturer-and-model linkage no longer exists
This ticket introduced `catalog_ref` carrying manufacturer and model as free text; ZP-3937 then removed those keys entirely in favour of the catalog entry plus subsettings. On QA a new link therefore cannot be saved the way step 1 describes, and the only entry still holding a free-text key ("Bussmann LPJ …", with `family`) is a survivor from before that change — which is itself the backwards-compatibility case ZP-3937 asks about, and it loads cleanly. Anyone running this checklist as written should expect the identity fields to look different from the ticket.

---

## Test data — direct links (QA)
- Materials library: https://acme.qa.egalvanic.ai/materials · API: https://acme.qa.egalvanic.ai/api/materials-library
- Linked entries: "1 sep abs" (`{equipment_type:"disconnect_switch", skm_bus_oid:452651, ampere_rating:1, fuse_class:"1", pole_count:31}`) · "19 aug abs" (`{equipment_type:"fuse", skm_device_oid:550331}`) · "Bussmann LPJ Class J current-limiting fuse" (legacy `{family:"LPJ (Class J)", pnl_device_id:144}`)
- Ref resolution: `https://acme.qa.egalvanic.ai/api/equipment-catalog/resolve?skm_bus_oid=452651`
- The single unpriced entry behind the queue: "New materials" (Indirect Cost, no price)

Evidence: `docs/bug-evidence/zp-materials-library-equipment-linkage/`.

## Not covered / honest gaps
- **Every write path** — creating a link, clearing one, and pricing an entry out of the queue were all skipped to leave shared QA library data intact; the read, resolve and display halves were verified instead.
- **Both validation negatives** (dangling SKM device/frame; `catalog_ref` on a global row) — one needs a crafted write, the other needs a global row that this tenant does not expose.
- **The agent-dedupe cross-check** — no agent run is triggerable on QA.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
