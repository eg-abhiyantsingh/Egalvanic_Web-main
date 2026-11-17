# ZP-4636 [BE] created_by / modified_by on commercial records (Tier 3): verdict

**Result: PASS (backend, API).** All 7 tables in the ticket now carry `created_by` and `modified_by`, and the API payloads include both. A create stamps the acting user. An edit by a different user changes `modified_by` and leaves `created_by` alone. Values sent by the client are ignored. Rows from before the change are still NULL (no backfill).

- **Build:** QA `index-oFAZQ3rK.js`, 17 Nov 2026, about 11:30–11:40 IST. Fix: eg-pz-backend PR #1812 (merged to QA 13 Nov). I could not read the PR because the backend repo is not visible to the QA GitHub account.
- **Seats:** Account Manager (creates the quote), Project Manager (edits it), Super Admin (creates the config records), Admin (edits them). All of them are QA test seats. `users.id` is the same value as the seat's `sub` in `/api/auth/v2/me`, and that is how the ids were matched to seats.
- **Flag:** none. The ticket names no LaunchDarkly flag.

## What was run (same API calls the web app makes)
| Table | Create (user) | Update (other user) | After create: created_by / modified_by | After the edit |
|---|---|---|---|---|
| quotes | `POST /quote/create-standard` (AM), the call behind Opportunities › New Quote | `PUT /quote/update/{id}` (PM) | AM / AM | AM / **PM** |
| opportunities | created along with the quote (AM) | `PUT /opportunity/{id}` name (PM) | AM / AM | AM / **PM** |
| quote_labor | `POST /quote-labor/create` (AM) | `PUT /quote-labor/update/{id}` (PM) | AM / AM | AM / **PM** |
| quote_labor_lines | `POST /workorder/{pwo}/labor-lines` manual line (AM) | `PUT /workorder/{pwo}/labor-lines/bulk-save` (PM) | AM / AM | AM / **PM** |
| labor_rates | `POST /labor-rates` (SA) | `PUT /labor-rates/{id}` (Admin) | SA / SA | SA / **Admin** |
| materials_library | `POST /materials-library` (SA) | `PUT /materials-library/{id}` (Admin) | SA / SA | SA / **Admin** |
| vendors | `POST /vendors` (SA) | `PUT /vendors/{id}` (Admin) | SA / SA | SA / **Admin** |

The values are the same when read back through the list endpoints (`GET /labor-rates`, `/materials-library`, `/vendors`) and the detail endpoints (`GET /quote/{id}`, `/opportunity/{id}`, `/workorder/{pwo}/labor-lines`).

## Edge cases
- **Spoofing on create:** `POST /labor-rates` as SA, with `created_by`/`modified_by` set to the PM's real id. Stored: SA / SA. PASS.
- **Spoofing on update:** `PUT /labor-rates/{id}` as Admin, with both fields set to the AM's real id. Stored: SA / Admin. PASS.
- **No backfill:** the old quote "test new - Rev 1" (12 Jan 2026) and its opportunity are null / null. The older labor rates (Feb 2026), materials (Aug–Sep) and vendors (Oct) are null / null too. PASS.
- **Records made by real users since the deploy are stamped:** opportunity 77fd4b8c… (13 Nov 13:53 UTC) carries a created_by.

## Observations (not defects by the ticket's wording)
1. **A save with nothing changed:** Admin saved a labor rate with identical values. `modified_at` moved (06:05:05Z), but `modified_by` stayed SA. The stamp only fires on a real change, so `modified_at` can show a later time than the change `modified_by` describes. This is minor. If "who last saved" is wanted, the stamp should follow `modified_at`.
2. **The quote list doesn't include these fields:** list rows from `POST /company/{id}/quotes/v2` don't carry `created_by`/`modified_by`. The detail call does. This matters only if a future list column wants them.
3. **No web screen shows these fields yet.** The ticket only asks that `to_dict()` exposes them. Showing them in the UI is a later frontend change.

## Not covered
- System writes (CLI, migration, pipeline API key) saving NULL. These can't be triggered from QA.
- The FK constraint itself (a DB check).
- Bulk imports (`/labor-rates/process`, `/materials/bulk-process`). An authenticated import should also stamp the user. Not run.
- Other money endpoints, such as quote materials (`quote_materials` is not in the ticket's list).

## Mobile
Not applicable. The mobile apps don't create or edit these sales/pricing records (mobile not checked).

## Test data (QA, all named "QA-DEMO ZP-4636 17 Nov …")
- Quote: https://acme.qa.egalvanic.ai/quotes/2cc081bd-752a-4946-872f-57c529b08cda (opportunity 11f4e310-44d9-4de3-920d-44df02f6382b, quote labor 7485c01f-fba5-425e-ab8c-aa41044d65e9, labor line d97658fc-824b-4799-9fda-92bc06986c9b on planned work order 95d643a8-3e3e-46a0-a8a2-2984ab564301)
- Labor rates: https://acme.qa.egalvanic.ai/labor (404c82a2-5044-4e7f-b444-b099cc8fca4b, 8a3fc74e-f85c-4f6a-ae49-4714cbd5d517, 4ccdd942-6ad3-40ad-a593-40b963c70bed, c7297e1d-699e-4c45-8d7d-8c1a4657fded)
- Material: https://acme.qa.egalvanic.ai/materials (bda70d09-bc89-47ac-ac7b-3da5adab7307)
- Subcontractor (vendor): https://acme.qa.egalvanic.ai/subcontractors (149f57ae-3277-42f7-8156-e5c31f2d8b48; contact email is an example.com address with no login)

## New defects
None.

## Evidence
- `QA-17Nov-ZP-4636-created-modified-by-video.mp4` (37 s)
- `QA-17Nov-ZP-4636-1-seven-tables-create-and-update.png`, `-2-quote-in-app-with-live-api-values.png` (app page, outline only; the API values are in the caption), `-3-edge-cases-spoof-old-rows.png`
- Raw API results: `raw/main.json`, `raw/part2.json`, `raw/part3-labor-lines.json`, `raw/recon.json`
- ZP-4636: screenshots re-saved without text tags (17 Nov, owner rule); outlines only, PASS/FAIL wording only in the .txt captions.
