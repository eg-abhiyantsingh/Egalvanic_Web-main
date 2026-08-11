# TEGG/SKM Arc-Flash Inputs — full QA pass on QA V1.36

**Date:** 2026-08-11 · **Ticket:** TEGG/SKM Arc-Flash Inputs (14 PRs: BE #895–#906, FE #1075–#1077, AI #11)
**Report:** [`docs/bug-reports/2026-08-11-tegg-skm-arc-flash-inputs-QA.md`](../bug-reports/2026-08-11-tegg-skm-arc-flash-inputs-QA.md)
**Evidence:** `docs/bug-evidence/tegg-arc-flash/` (3 screenshots + 3 SKM XML files)

## Outcome

6 of 8 QA items PASS, 1 partial, 1 not-reproducible. Three findings raised — one new platform
defect (TEGG-1), one confirmation of an open reviewer concern (TEGG-2), one lower-severity
bulk-import gap (TEGG-3).

The headline: the feature works. `UI field (kA) → node.aic_rating → SKM ShortCircuitRating` is
verified with real exported XML, and the restored `n >= 0` sign bound from #1076 genuinely holds.

## What I actually did

**Found where attributes live.** `/api/node_classes/user/{uid}` returns 43 classes, and the
attribute definitions are inside each class's `definition[]` array — not a separate endpoint.
Each entry has `key`/`type`/`options` plus an **`af_required`** flag. My first two attempts
(`node_attributes`, `/api/node_classes/{id}`) were masked 404s, which is why I went to the UI
and read the Core Attributes tab's network calls instead of guessing further.

**Audited all 43 classes at once** rather than spot-checking the six the ticket names. The AF set
is `electrodeConfig` + `enclosureHeight/Width/Depth`, present on exactly the 7 `device_role_code=bus`
classes and absent from all six named classes. `sections` is on MCC and Switchboard.

**Bounds-tested the live input** character by character, including progressive typing, because the
reviewer's original complaint was about mid-typing blanking. All nine values behaved per #1076.

**Then tested the same bounds server-side** — which is where the interesting result was.

## The finding worth reading (TEGG-1)

`POST /api/node/create` with `aic_rating: -5` returns **HTTP 200**, echoes `"aic_rating": -5`
back, and reports `"_mutation": {"status": "received"}` with a real UUID. The asset is then
**never created**. Same for `2147483648` and `"abc"`. Only 3 of 6 posted assets existed
afterwards.

### How I avoided reporting this wrong, twice

**First**, my immediate re-read after the POST showed `aic_rating: null` for *all five* cases —
including the two that actually succeeded. Writes are async (`status: received`), so reading
immediately proves nothing. Re-reading after a 9 s settle gave the real picture. Had I stopped at
the first read I would have reported "the API accepts and then loses every value", which is false.

**Second**, before writing it up I ran a control with a *different* bad field. `com: "xyz"` and
`width: "big"` are silently dropped the same way, and a valid payload is created. So this is a
**general property of the mutation pipeline**, not an AIC bug — reporting it as "the AIC feature
is broken" would have sent the wrong team chasing the wrong file.

That control also turned up a bonus: a **non-existent `node_class` UUID is accepted** and produces
an asset rendering as "No Class".

Why it matters: the web UI is insulated (it validates client-side), but **iOS has an unbounded AIC
field** and `node.aic_rating` is a **bulk-import column**. Those callers get a success response for
an asset that does not exist. Good news alongside it — no invalid AIC became readable through any API
surface I checked, so the negative `ShortCircuitRating -5.000` the reviewer feared did not materialise
on this path.

**Third — a mechanism error caught in review, not by me.** I first wrote the rule as "any type-invalid
field drops the asset". My own results disprove it: `65.5` *is* type-invalid for an int column and was
**coerced** to `65`; `-5` and `2147483648` are *valid integers* rejected on **range**; a dangling
`node_class` UUID is **accepted**. What fits every row is that there is **no validation layer** — the
worker attempts the write and a swallowed storage-level exception aborts the transaction. Naming a
validator that the evidence gives no reason to believe exists would have pointed dev at the wrong
layer. The report now states the corrected rule and flags the untested cases (over-long strings,
unique/FK violations).

## Where I deliberately stopped

- **Did not process a bulk import** that would have rewritten 18 connections on a shared SLD. The
  preview claimed those changes for a Connections sheet I never edited, and I could not confirm
  from the API whether they were real or a preview artefact. Logged as an observation needing dev
  confirmation, not a bug.
- **Did not run a 25-asset AI extraction job** to reach `BulkExtractionJobDialog`. It is billable
  and mutates data, and the target assets have no nameplate photos, so the job would have been
  empty and still would not have exercised the apply → X-close path.

## Cleanup

Seven probe assets created, seven deleted (`DELETE /api/node/delete/{id}` → 200). "Test without
location" verified back to `total: 0`; ZTest_28_07 unmodified; row selection cleared.

## Depth notes — techniques worth reusing

- **The masked-404 trap bit repeatedly.** Roughly a dozen guessed endpoints returned 200 +
  `text/html`. Every probe in this session checked `content-type` before parsing. It also means
  "the endpoint isn't there" is never a safe conclusion from a guess — which is exactly why item 7
  was reported as partial rather than broken.
- **Learn contracts from the UI, not from guessing.** `POST /api/node/create`,
  `DELETE /api/node/delete/{id}`, `POST /api/sld/{id}/export-skm` and
  `GET /api/extraction/signatures/donors/{id}` were all discovered by recording what the app itself
  called. Guessing found none of them.
- **`page.goto` destroys an injected `window.fetch` recorder.** Install after load, or use
  Playwright's own `page.on('request')` — which is what finally caught the asset-detail call.
- **SKM export is two-stage**: the POST returns JSON with a presigned S3 URL; the XML must be
  fetched from S3 (curl, since CSP blocks it from the page).
