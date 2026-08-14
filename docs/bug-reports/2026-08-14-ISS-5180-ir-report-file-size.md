# ISS-5180 — "Is there a way to reduce document size in an IR report?"

**Investigated:** 2026-08-14 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**Source:** DevRev ISS-5180 — customer cannot email IR reports; recipients on stricter mail
servers bounce the attachment while others accept it.
**Work order used:** `fcc37c67` *Infrared Thermography (I.R)* — 1,856 assets, **23 IR photo pairs**
**Config:** *Infrared Thermography Report* (`16c55d5d`), PDF, "Include IR thermography photos" **on**

---

## Answer to the customer's question

**Today: no.** The Generate Report dialog offers Output Format and two *asset-inclusion* toggles
(*Include child/subcomponent assets*, *Exclude assets with no tasks and no issues*). There is **no
size, quality, resolution, compression or DPI control anywhere** in the dialog or in the
`generate_simple` API payload.

**But the fix is much easier than expected**, because the size is not coming from where everyone
assumes.

## The measurement

A real report generated on QA:

| | |
|---|---|
| **File size** | **19.23 MB** (20,168,189 bytes) |
| Pages | 957 |
| Generation time | ~7.5 minutes |
| **Over 10 MB corporate cap** | **YES** — this is the customer's bounce |
| Over 20 MB Exchange default | No (marginal — 19.23 of 20) |
| Over 25 MB Gmail | No |

That straddling is exactly the reported symptom: *"some of those peoples emails get rejected due to
size and it goes through with others."* A 19.23 MB file passes Gmail and a default Exchange, and
bounces off any server with the very common 10 MB cap.

## Root cause — it is **not** the photos. It is duplicated fonts.

The intuitive assumption (mine included, before measuring) is that thermal imagery is the problem.
It is not:

| Component | Bytes | Share of file |
|---|---|---|
| **Embedded font programs** | **11.08 MB** | **58%** |
| All other stream data | ~3.0 MB | 15% |
| Images | **0.29 MB** | **2%** |
| PDF structure / xref / overhead | remainder | ~25% |

**5,397 font programs are embedded for 10 distinct typefaces** — a **540× duplication**:

```
/BaseFont /AAAAAA+GeistMono-500   × 3,524 objects
/BaseFont /BAAAAA+Geist-600       × 2,646 objects
/BaseFont /DAAAAA+GeistMono-600   × 1,766 objects
…10,795 font objects in total, 13 distinct names, 10 real typefaces
```

Median embedded font program: **1.6 KB**; largest ~13 KB. The renderer is emitting a font subset
**per page** (957 pages × ~5.6 fonts) instead of once per document, and not merging the subsets.

### What fixing it is worth

Deduplicating to one subset per typeface recovers **~11.06 MB**, taking this report from
**19.23 MB → ~8.2 MB — under the 10 MB cap the customer is bouncing off**, with **no loss of
content or image quality**. That is a bigger win than any image compression could deliver, because
images are only 2% of the file.

## Second finding — the IR photos are missing from the IR report

Worth raising on its own: **the report contains no thermal photos at all.**

* "Include IR thermography photos" was **checked**.
* The work order has **23 IR photo pairs**, all healthy: 23/23 have a `node_id`, 23/23 have both
  `ir_photo_key` and `visual_photo_key`, each linked to a named asset (*Switch 1*, etc.).
* The PDF contains **0 JPEG streams** (`\xff\xd8\xff` markers: 0, `DCTDecode`: 0) and only **6
  images total** — two 353×220 and four 612×150, i.e. logo/header art. For 23 pairs there should be
  ~46 photos.

So a report whose entire purpose is thermal documentation shipped **without a single thermal image**,
while still costing 19 MB and 7.5 minutes. This is either the option not being honoured or the
photos not being resolved into the template.

> **Note on the bigger picture:** if this is fixed and photos start embedding, size will grow
> sharply — a single source IR photo measured **1,270 KB** straight from S3 (`H59.jpg`), full
> resolution and un-downscaled. 46 photos at that size is **~57 MB** of imagery. So the font fix
> should land *and* photos should be downscaled/recompressed on embed, or fixing the photo bug will
> turn a 19 MB problem into a 60 MB one.

## Recommendations, in order of value

1. **Deduplicate embedded fonts** (one subset per typeface per document, not per page).
   *~11 MB saved, 19.23 → ~8.2 MB, no quality loss.* Highest value, no user-visible trade-off.
2. **Fix the missing IR photos** — the report is not doing its job without them.
3. **Downscale + JPEG-recompress photos on embed** *before* (2) ships. Fitting the page at ~150 DPI
   (roughly 1000 px wide) and quality ~75 typically cuts a 1.2 MB FLIR frame to 80–150 KB — 46
   photos then cost ~5 MB rather than ~57 MB.
4. **Give the user a size control** — the customer literally asked for one. A "Compact / Standard /
   High quality" selector, or simply stating the size before download, would let them self-serve.
5. **Consider the 957 pages.** The report enumerated all 1,856 assets with *Include child assets*
   on. "Exclude assets with no tasks and no issues" exists and would cut this dramatically — worth
   telling the customer today as an immediate workaround.

## Immediate workaround for the customer

Until the font fix ships: tick **"Exclude assets with no tasks and no issues"** and untick
**"Include child/subcomponent assets"** in the Generate Report dialog. On this job that is the
difference between a report covering 1,856 assets and one covering only those with findings.

## Method notes

* A first attempt used the *ABBOTT PM IR — sample-fix proof* config, which produced 0.44 MB / 93
  pages / 5 images — a text report that embeds no photos. Measuring that would have concluded
  "reports are small, no issue". The customer's document is the *Infrared Thermography Report*
  config; picking the wrong one gives the wrong answer.
* Byte attribution was done by parsing the PDF's own stream dictionaries (`/Length`, `/Filter`,
  `/FontFile`, `/Length1`, `/Subtype /Image`), not by estimation.
* Generation is slow: **~7.5 minutes** for this report. Worth noting alongside the size complaint.
