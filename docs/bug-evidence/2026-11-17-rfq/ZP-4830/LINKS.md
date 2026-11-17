# ZP-4830 company links on QA (17 Nov 2026), for the ZP-4833 tester

Status updated: 15:32 IST, build index-CkkAeA9o.js

## Link B: EG-ACME = prime, Demo = sub. ACTIVE since 15:30 IST
- Unblocked when `feature-guest-access` for EG-ACME turned **true** (first read true at 15:26 IST; earlier reads at 14:44, 14:48 and 15:09 were false).
- Link id `c5d50283-62d5-4783-9d98-216e4a0db6aa`, status `active`. Accepted 2026-11-17 10:00:31Z (15:30 IST).
- Prime: EG-ACME `d59d449b-09d8-45d6-8f0a-ef70024b1293`.
  - Uses your vendor "QA-DEMO ZP-4833 17 Nov sub (Demo Company)", vendor id `9767989d-abac-4a3b-8de4-47c3cff70feb`.
  - Sent from SA through Collaborators › Company Links › New company link.
- Sub: Demo Company `93611164-13e6-47da-b2cd-a150e73173f6`. The Demo admin accepted it in Collaborators › Requests.
- **Heads-up for ZP-4833: Demo Company has NO Teams** (`GET /api/crews` in Demo returns `[]`).
  - So on EG-ACME the vendor shows `linked_teams: []`, and `/api/crews/assignable` lists the vendor with no crews.
  - A linked sub's Team can't appear on an EG-ACME schedule until a Team exists in Demo (Demo admin › Admin › Teams).
  - I haven't created one: the Demo login rules only allow links, WO ticks and SLD sharing. The lead decides.
- Where to see it:
  - EG-ACME: https://acme.qa.egalvanic.ai/collaborators?tab=links, row "Demo Company · Your subcontractor".
  - Demo: https://demo.qa.egalvanic.ai/collaborators?tab=links, row "EG-ACME · You subcontract for them".

## Link A: Demo = prime, EG-ACME = sub. ACTIVE since 14:55 IST
- Link id `14d77ee3-ee52-4b5d-92dc-4c6105bb01ea`, status `active`. Accepted 2026-11-17 09:25:16Z (14:55 IST).
- Prime: Demo Company `93611164-13e6-47da-b2cd-a150e73173f6` (https://demo.qa.egalvanic.ai).
  - Its subcontractor record is "QA-DEMO ZP-4830 17 Nov sub (EG-ACME)", vendor id `7ea55eda-2f37-46f2-accf-95ac838525b3`.
- Sub: EG-ACME `d59d449b-09d8-45d6-8f0a-ef70024b1293`.
  - Invited and accepted with the EG-ACME ADMIN seat (adminqa, staff).
  - The accept worked even though EG-ACME's `feature-guest-access` is false.
- In Demo, all 7 EG-ACME Teams now read as crews of that vendor (`GET /api/crews/assignable` → `vendors[0].crews`, each with `linked: true`).
  - "QA-DEMO ZP-4755 in-house crew (delete me)" holds 1 member: the PM seat. I'm ticking PM onto a Demo WO for ZP-4830.
  - "QA-DEMO ZP-4583 r8 team" holds 2 members: PM and FM.
- Where to see it:
  - Demo: https://demo.qa.egalvanic.ai/collaborators?tab=links, row "EG-ACME · Your subcontractor".
  - EG-ACME: https://acme.qa.egalvanic.ai/collaborators?tab=links, row "Demo Company · You subcontract for them".
- I'm leaving it in place. Please don't Unlink it.

## Customer link: EG-ACME = provider, Demo = customer (facility). ACTIVE since 15:20 IST
- Link id `a3a138ef-4863-4697-b0e1-669ee828eb6c`, status `active`.
- EG-ACME side:
  - Customer record "QA-DEMO ZP-4561 customer 16 Oct", account `3cb8a94f-d652-4b15-947c-07777e6c4465`.
  - Sending a customer link works with `feature-guest-access` OFF (201). Only the subcontractor link is gated.
- Demo shares ONE SLD: "test" `24eb08b1-bb88-4f47-8ad0-f5b09326cf8d`. Not shared: "Demo" and "Riverside Plant".
- EG-ACME's chosen person: the AM seat, with role "Electrical Engineer" in Demo.
- Where to see it:
  - Demo: https://demo.qa.egalvanic.ai/collaborators?tab=links, row "EG-ACME · Service provider in your SLDs".
  - EG-ACME: https://acme.qa.egalvanic.ai/collaborators?tab=links, row "Demo Company · Your customer", then Manage.

## Demo work orders created for ZP-4830 (site "Demo")
- WO 1, created in the UI with the linked Team: https://demo.qa.egalvanic.ai/sessions/38f48f6f-0be4-4d3b-becf-edae3a3114b6
- WO 2–6, created through the API, with the same staffing:
  - https://demo.qa.egalvanic.ai/sessions/e93116a5-99ce-4dd7-a86c-108532f669ec
  - https://demo.qa.egalvanic.ai/sessions/d99ac331-4bbb-46bf-877c-923041fee9f2
  - https://demo.qa.egalvanic.ai/sessions/5f4d7f08-35c0-46b6-9e12-4f8526d80a4f
  - https://demo.qa.egalvanic.ai/sessions/e07576c0-fcd1-4990-85d5-3b2ae4e6f44c
  - https://demo.qa.egalvanic.ai/sessions/16cec4d1-e769-45dc-98fd-8b326c308c9d

Alternative for ZP-4833 while Demo has no Teams: Demo has `feature-planned-work-v2` = true and Link A gives it EG-ACME's 7 linked Teams. A Demo planned-work schedule can use them with the roles reversed (Demo as prime).
