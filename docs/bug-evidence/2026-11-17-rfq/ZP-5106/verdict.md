# ZP-5106 · Task #218: Forking a global service repoints its open work orders and matches Set Up Forms (QA, 17 Nov 2026)

**Verdict: PASS → recommend READY TO RELEASE.**

Builds:
- Full run on `index-LGhrn-48.js` (live from about 14:05 IST). The fork itself ran at 14:19 IST.
- Smoke re-check on `index-CkkAeA9o.js` (live from about 14:45 IST), at 14:49 IST. All results are the same.

Seat: Super Admin test seat (staff). The fork and Build Quote need service and quote edit rights.

What changed since 12 Nov: the owner said "complete that too". So I forked a real global service on safe data:
- The service was Insulation Resistance Testing (IRT, global `d9c9efef`).
- EG-ACME had never forked it.
- It had **0 open work orders** in EG-ACME today (175 open WOs checked through the Work Orders list API) and 9 closed ones.
- UPS Maintenance was not touched.

## Test data (all mine, QA-DEMO)
Site "QA-DEMO ZP-5108 ZP-5109 12 Nov" (https://acme.qa.egalvanic.ai/sld/e1aeadfa-6b58-484a-acf1-1ef09496b8b9). Work orders, each with 5 assets and 5 IRT forms:
- A, open: "QA-DEMO ZP-5106 17 Nov IRT open A". Made in the Create Work Order wizard. https://acme.qa.egalvanic.ai/sessions/c26b2c5c-9a00-4bcc-9699-47c7b67aecf4
- B, open: "QA-DEMO ZP-5106 17 Nov IRT open B". Made with the same create call. https://acme.qa.egalvanic.ai/sessions/42df4b9a-0b94-4452-8a42-d9a32887071d
- C, closed control: "QA-DEMO ZP-5106 17 Nov IRT closed control C". I closed it with Close Work Order before the fork. https://acme.qa.egalvanic.ai/sessions/f853c27a-3dbf-4249-8ec4-6331e986174c
- EG-ACME's copy of IRT, made by the fork: https://acme.qa.egalvanic.ai/services/86d15fef-7b1a-40d6-befc-847e97e0b387 (`forked_from_id` = `d9c9efef`).

## Checked
| Row (QA Review) | How | Result |
|---|---|---|
| Fork a global service that has open WOs → the open WOs move onto the fork | Builder › Services › Insulation Resistance Testing (global) › **Customize** › confirm **Customize**. Recorded on video. | **PASS**. `POST /procedures-v2/services/d9c9efef…/fork` returned 201 and the copy opened at `/services/86d15fef…`. In `GET /ir_session/<id>/service-registry`: WO A went from global `d9c9efef` to copy `86d15fef`, and so did WO B. The list's `work_type_id` moved to the copy too. The method lines of A and B now carry `service_id` = copy. All 5 form instances per WO are kept |
| Closed WOs stay on the global | Control WO C (closed before the fork), plus 3 older closed IRT WOs from other users | **PASS**. C still points at global `d9c9efef` (registry, `work_type_id` and method lines). The 3 older closed WOs are unchanged |
| Set Up Forms matches a forked service to its global | The 12 Nov QA-DEMO WO that still carries the **global** Infrared Thermography `3b732d14`: https://acme.qa.egalvanic.ai/sessions/c3791ea4-f421-465e-9c16-61b37eecdd6d. Assets › Actions › Add New Asset › Panelboard "QA-DEMO ZP-5106 PNL-4 17 Nov" | **PASS**. Set Up Forms lists "Infrared Thermography · On 1 of 9 assets already carrying services", pre-ticked, "Create Forms (1)". The global was matched to EG-ACME's IR copy. Closed with Not Now; no forms were created (screenshot 2) |
| Build Quote: no false "no implementation" warning on forked services | Quote "QA-DEMO ZP-5106 12 Nov build quote" (https://acme.qa.egalvanic.ai/plans/5fbf0dfb-7a2f-4fd8-8d6c-609725a6f9fb) › Build Quote › All assets | **PASS**. No warning for today's new IRT copy, the IR copy or the NETA copy (screenshot 3). Control: UPS Maintenance (global, no implementation for these classes) still warns "9 matched assets have no UPS Maintenance implementation". On the newest build it says 10, because PNL-4 was added (screenshot 4). The dialog was cancelled |
| Negative: a fork with no open WOs repoints nothing and errors nowhere | Global **Shutdown (Composite)** `f9fb8d4a`: 0 open WOs, 8 closed. Customize › Customize | **PASS**. 201; the copy `5a9c3f6b…` opened. Open WOs stayed at 0. All 8 closed WOs kept the global in their registry (same as before). There was no error alert and no new console error |
| Clean-up of the negative copy ("Delete your copy later and the original comes back") | Builder › Services › Shutdown (Composite) row › Delete service › Delete Service | **PASS**. `DELETE /procedures-v2/services/5a9c3f6b…` returned 200. The services list shows the global `f9fb8d4a` again, and the copy returns 404 |
| Re-fork an already-forked global | `POST /procedures-v2/services/d9c9efef…/fork` after the fork | 409 `already_forked` with `existing_service_id` = `86d15fef…`, as the UI expects. Nothing moved: A is still on the copy and C on the global |
| Forking a non-global service is refused (12 Nov) | — | Not repeated; 400 on 12 Nov |
| Smoke on `index-CkkAeA9o.js` | API and Build Quote | A and B are on the copy and C on the global. The services list has the IRT copy only, plus the Shutdown global. Build Quote: no warning for IRT; the UPS control warns |
| Flags | — | No ticket flag |

## Bugs found
None.

## Enhancement ideas
- The Customize dialog could say how many **open** work orders will move to the copy ("2 open work orders will now use your copy"). Today the one-way side effect is invisible.
- After a fork, open and closed work orders show the same service name but carry different ids. Filters and reports that group by service id would split "Insulation Resistance Testing" in two. A small "(original)" hint on closed WOs would help.

## Not covered
- **Deleting a copy that open WOs already point at** (copy text: "existing work orders and quotes keep their data"). I did not run it: it would leave my two WOs on a deleted service.
- **Planned work orders** (Planned Work rows that are not yet sessions) carrying the global at fork time. QA has none for IRT, and creating one needs a quote flow.
- **Other roles.** Fork and Build Quote need `procedures.manage` and quote edit rights; only the staff SA seat was used.

## Company data changed
- EG-ACME now has its own copy of **Insulation Resistance Testing** (`86d15fef`). It has the same content as the global, which EG-ACME no longer lists. Only my 2 QA-DEMO open WOs moved; no one else had an open IRT WO.
- The **Shutdown (Composite)** copy was deleted again. The global is back as it was.

## Cross-platform
- **Twins:** none. A Jira search for "Task #218", "fork" and "forked" finds only this web ticket. The fix is backend plus web; there is no mobile twin.
- **Web:** PASS (above).
- **iOS:** not checked on a device. The source (`eg-pz-mobile-iOS`, branch `release/qa`, commit 34bd67e, 16 Nov, build 1.75) reads a WO's services from `GET /ir_session/<id>/service-registry` (`SessionServiceRegistryDTO`). It names them from a local cache of `GET /procedures-v2/services` (`Service.swift`). After the fork, the registry returns the copy's id `86d15fef`, which is in the services list now, so the work order should show the service and its forms. A device must confirm this, especially a phone that cached WO A **before** 14:19 IST.
- **Android:** not checked. The source is not readable here; same expectation as iOS.

### MOBILE-CHECKS
1. **iOS, SA seat** (TECH is busy on Android). Site "QA-DEMO ZP-5108 ZP-5109 12 Nov" › Work Orders.
   - Open "QA-DEMO ZP-5106 17 Nov IRT open A".
   - Expected: the service shows as "Insulation Resistance Testing" (not blank or "Unknown"), with 5 assets each carrying the IRT form.
   - Open one form, enter one value, save and sync.
   - Expected on web: WO A › Forms shows the value (https://acme.qa.egalvanic.ai/sessions/c26b2c5c-9a00-4bcc-9699-47c7b67aecf4).
2. **iOS, same seat:** WO "QA-DEMO ZP-5106 17 Nov IRT open B" shows the same.
   - The closed WO "…closed control C" is not in the open list. If it shows under Closed, its service still reads "Insulation Resistance Testing".
3. **iOS, cached before the fork:** if a device already had WO A or B synced before 14:19 IST, pull to refresh.
   - Expected: no error, no duplicate service, and the forms are still there.
4. **iOS, Add asset in WO A** (Panelboard, or any IRT class such as Cable or Transformer).
   - Expected: the IRT service is offered and applied (the method-first `available-for-node` call returns the copy).
5. **Android (Z Platform QA), any free seat with site access:** repeat checks 1–4.
