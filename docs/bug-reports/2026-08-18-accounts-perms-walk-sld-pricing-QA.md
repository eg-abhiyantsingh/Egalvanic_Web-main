# Accounts permissions & quotes · site-walk grid/taxonomy · SLD terminal relief · walk pricing explanations — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-backend **#962** · eg-pz-frontend **#1131** (both merged to cicd/dev 2026-08-05)
**Author caveat on the ticket:** *"verified by SQL and through the real controllers but not click-tested end to end — a full manual pass is worth the time."*

---

## Verdict

**Deployed to QA and the backend/RBAC core is solid.** The headline — the accounts permission split — is verified precisely at the live permission resolver, along with active-role site visibility and the pricing-explanation endpoint. Several items are blocked by missing fixtures on acme (no accounts; no overcrowded terminal) or are UI-only (grid layout, the four render defects). **No defects found in what was exercised.**

| # | QA item | Result |
|---|---|---|
| 1 | PM keeps list + create, but cannot open account detail or edit | ✅ **PASS** |
| 2 | A role that held `accounts.view` retains detail access (grants derived, PM excluded) | ✅ **PASS** |
| 3 | Site visibility follows the active role; hidden site not openable by direct URL | ✅ **PASS** |
| 4 | Account value/status/Legacy-Latest match the Quotes grid; v2 plans visible | ⚠️ **BLOCKED** — 0 accounts on acme |
| 5 | Non-AM owner refused on assignment; existing non-AM owner still echoes back | ⚠️ **BLOCKED** — 0 accounts on acme |
| 6 | Site-walk grid: one class open, roll-ups, Generic child row, photos 3rd column | ⏳ **UI — not exercised** |
| 7 | Deleting a walk confirms; an exported walk cannot be deleted | ✅ **PASS** (API side) |
| 8 | terminal.overcrowded → Fix All inserts a Node Bus preserving direction + edge ids | 🟡 **Endpoint deployed; behavior not exercised** |
| 9 | explain-pricing returns a paragraph, never fatal, books to ai_usage | ✅ **PASS** (paragraph) |
| 10 | Regression on the four UI defects | ⏳ **UI — not exercised** |

**4 PASS, 1 partial, 2 blocked (no fixture), 2 UI-only, 1 partial.**

---

## ✅ Verified

### Item 1 & 2 — Accounts permission split (the headline)
The new `accounts.view_detail_page` permission (migration `acctperm_a1`) is live. I assumed each role via `x-active-role-id` and read `/api/auth/v2/me` permissions — the live resolver, not a static grant table:

| Role | `accounts.view` | `accounts.view_detail_page` | `accounts.manage` |
|---|---|---|---|
| Super Admin | ✅ | ✅ | ✅ |
| Admin | ✅ | ✅ | ✅ |
| Account Manager | ✅ | ✅ | ✅ |
| Electrical Engineer | ✅ | ✅ | ✅ |
| **Project Manager** | ✅ | **❌** | ✅ |
| Technician / Facility Manager / Client Portal | ❌ | ❌ | ❌ |

- **Item 1 PASS:** Project Manager keeps the list (`view`) and create (`manage`) but is the one role denied `view_detail_page` — so it cannot open an account detail page. (Editing an existing account is reached through the detail page, so losing detail access removes edits too, matching "lose the detail page and account edits.")
- **Item 2 PASS:** the derivation holds — **every** role that has `accounts.view` retains `view_detail_page` **except** Project Manager. Non-admin *Electrical Engineer* kept detail access, showing the grant isn't hardcoded to admins but derived from existing `view` grants minus PM. (No purpose-built "custom" role exists on acme to test by name, but EE demonstrates the mechanism.)

### Item 3 — Site visibility scoped to the active role
Accessible SLDs change with the acting role (same user, `x-active-role-id` varied): Super Admin **210**, Account Manager **4**, Project Manager **1**, Technician/Facility Manager **0**. And the direct-URL hole is closed: as **Technician** (0 accessible SLDs), `GET /api/sld/{hidden_id}` returned **HTTP 422 with no document** — a hidden site cannot be opened by direct URL under a role that lacks it.

### Item 9 — Walk pricing explanation
`POST /api/plans/{id}/workorders/{wo}/explain-pricing` → **200** with a single coherent paragraph:
> *"This quote takes the 1.45 hours this cleaning work requires across the four assets walked, multiplies by the 150.0 dollar per hour blended rate for the trades involved, and arrives at 217.5 dollars. …Labor hours directly drive the final price."*

Returns a real Haiku-generated paragraph. *(The "never fatal on failure" and "books to the ai_usage ledger" halves are backend/DB behaviors I can't observe from the client — worth a backend confirm.)*

### Item 7 — Exported walk cannot be deleted
Confirmed earlier this session: `DELETE /api/site-walk/{id}` on an **exported** walk is refused with **`409 walk_exported`**; it succeeds only after reverting status to draft. The delete-confirmation dialog is a UI affordance (not separately click-tested).

## 🟡 Deployed but not fully exercised

### Item 8 — Overcrowded-terminal relief
`POST /api/edge/resolve-overcrowded-terminals` **exists** (returns `400 edge.missing_required_fields` without the terminal/edge payload — it's a targeted fix, not a bulk scan). I could not exercise the actual Node-Bus insertion because it needs a real overcrowded terminal (a terminal carrying >1 connection) plus its edge ids, which I couldn't construct on demand. Confirming it "preserves direction and edge ids" needs that fixture or the SLD Issues → Fix All flow on a diagram that has the issue.

## ⚠️ Blocked — no fixture on acme

- **Items 4 & 5** — the Accounts list (`POST /api/account/by-company/{company}/v2`) returns **0 accounts** on acme right now, so I can't verify account value/quote-badging (item 4) or owner-must-be-Account-Manager enforcement (item 5). Both are testable once an account with a quote exists (I can create one if you want it, per your "leave test data" note).

## ⏳ UI-only — not exercised
- **Item 6** (site-walk count grid: one class open at a time, roll-up rows, Generic child row, photos as an aligned third column) and **Item 10** (the four render defects: hrs/unit em-dash, separator over new unit rows, Asset column alignment, Subbed-chip overpaint) are pixel/layout behaviors best confirmed with a visual pass on a populated walk + a walk-sourced quote's Pricing/WorkOrderDetail dialogs.

## Observation (not a defect)
`GET /api/company/alliance-config/acme.egalvanic` returned **500** on one load and **200** on the next — intermittent, consistent with the known flaky branding/alliance-config API. Flagging only because it briefly errors on page load.

## Method notes
- Permissions verified against the **live resolver** (`/auth/v2/me` under each `x-active-role-id`), role ids from `GET /api/users/roles/company/{company}`.
- No accounts/walks were created for this pass; no data changed.
