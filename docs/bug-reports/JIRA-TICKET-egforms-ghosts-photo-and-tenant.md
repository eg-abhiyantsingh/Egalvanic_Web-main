# [Web] EG Forms blue-ghosts: the previous-submission lookup ghosts photos/signatures and isn't tenant-scoped

**Severity:** High · **Priority:** High
**Environment:** QA (Project Z), V1.36 · `acme.qa.egalvanic.ai` + `demo.qa.egalvanic.ai`
**Backend PR:** eg-pz-backend #999 (the `GET /eg-form-instance/previous/<form>/<node>` lookup)

Two of this ticket's own QA-review requirements fail on QA. The ghost UX otherwise works.

---

## Defect 1 — signature & photo fields ghost (they must never)
The ticket says: *"signature and image_capture never ghost … materializing them into a new submitted record would forge both."* They do come through.

**Repro:** on a Bolted-Connections asset whose newest submitted instance has photos in its Verdict:
```
GET /api/eg-form-instance/previous/66d01aa2-…/fd75fab7-…
→ 200; data.form_submission.verdict.photos = [ { "_s3": true, "bucket": "attachments",
    "photo_id": "2c50a959-…", "key": "eg_forms/<priorInstanceId>/photos/….jpg", … } ]
```
The endpoint strips the presign query (the bare URLs 403 when fetched, so it's a **reference leak, not a directly-viewable image**), but it leaves the whole photo/signature field object in the payload — so the renderer will ghost, and an untouched Submit will **materialize a prior technician's photos/signature into a new record.** Same seen on an ATEST1 `signature`-typed field.

**Fix:** in the `previous` projection, drop any field whose **definition type** is `signature` or `image_capture` (by type, not a field-name heuristic).

## Defect 2 — `previous` is not tenant-scoped (cross-company read)
The ticket says: *"a node belonging to another company must return 404, not that company's submission."* It returns the submission.

**Repro:** as a **demo-tenant** user (company B), on the **acme host**, request an **acme** form+node:
```
GET https://acme.qa.egalvanic.ai/api/eg-form-instance/previous/<acmeForm>/<acmeNode>   (demo token)
→ 200 + acme's form_submission  (e.g. line 80/58) — expected 404
```
Auth *is* enforced (no token → 401), but the route resolves form/node by id **without scoping to the caller's company**. On the demo host the same call returns the masked-404, so a normal demo user's browser doesn't hit it — this is **request-tamper** (aim the request at the other tenant's host). Filed High, not Critical, for that reason, but it's a real violation of the ticket's negative case and it exposes another tenant's inspection readings.

**Fix:** scope the node→sld→company check to the caller's company (JWT `company_id`), return 404 on mismatch. This is the same per-route `eg-form-instance` tenancy gap seen on the NETA-3 by-session, the pin route, and the covered-services picker — worth fixing across the family at once.

## Note on my earlier QA pass
An earlier QA run marked "signature/photo never ghost" and "tenancy 404" as PASS. That was wrong: the photo-exclusion pass rested on a prior instance that happened to carry **no** photos (a data-luck false negative), and the tenancy pass saw only the demo-host masked-404, not the acme-host leak. Both corrected here.

## Evidence
Independently reproduced twice (adversarial API panel + hand re-verify): photo `_s3` objects in the `previous` payload on the literal Bolted target; demo→acme-host `previous` returning acme's form_submission with a JSON body (not a masked-404). Distinct companies confirmed via `/auth/me` (JWT-resolved, host-independent).
