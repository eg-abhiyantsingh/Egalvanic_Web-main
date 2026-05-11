# Architecture

## The core insight

Most SLD-to-Excel tools fail because they treat PDFs as **images** and apply
OCR + computer vision. This pipeline starts from a different premise:

> Modern engineering PDFs are **vector documents**. Every wire, breaker symbol,
> and text label has exact coordinates in the PDF content stream. We can
> extract those coordinates directly with zero OCR uncertainty.

This single decision changes everything downstream:

| Property | Raster + OCR pipeline | Vector pipeline (ours) |
|---|---|---|
| Coordinate accuracy | ~5px @ 300 DPI | exact (PDF native) |
| Text accuracy | ~95% | 100% |
| Determinism | depends on model | deterministic |
| Speed per page | 5–30s | <100ms |
| Hardware needed | GPU helpful | CPU only |
| Failure mode | silent (wrong char) | loud (no vector content) |

## The data flow

```
                              ┌──────────────────┐
                  ┌──────────▶│  PageGeometry    │  Stage 1
                  │           │  (Pydantic)      │  pdf/vector_extract.py
                  │           └────────┬─────────┘
        PDF ──────┤                    │
                  │                    ▼
                  │           ┌──────────────────┐
                  └──────────▶│  TextCluster[]   │  Stage 2A
                              │  (Pydantic)      │  vision/text_cluster.py
                              └────────┬─────────┘
                                       │
                                       ▼
                              ┌──────────────────┐
                              │  Entity[]        │  Stage 2B
                              │  (typed)         │  vision/classifier.py
                              └────────┬─────────┘
                                       │
                                       ▼
              ┌──────────────────────────────────────┐
              │                                      │
              ▼                                      ▼
       ┌────────────┐                       ┌────────────────────┐
       │  Bus[]     │                       │ Spatial matching   │ Stage 3
       │ (vision/   │                       │ graph/             │
       │ bus_detect)│                       │ spatial_match.py   │
       └─────┬──────┘                       └─────────┬──────────┘
             │                                        │
             └────────────────┬───────────────────────┘
                              ▼
                     ┌─────────────────────┐
                     │ ConnectionChain[]   │
                     └─────────┬───────────┘
                               ▼
                     ┌─────────────────────┐
                     │ EGalvanicWorkbook   │  Stage 5
                     │ (Pydantic)          │  export/xlsx_export.py
                     └─────────┬───────────┘
                               ▼
                            output.xlsx
```

## Why Pydantic everywhere

Every inter-stage boundary is a Pydantic model. Three reasons:

1. **Contract enforcement** — Stage 3 cannot accept a malformed Entity from
   Stage 2 by accident; the schema check fires immediately.
2. **JSON serialization** — every intermediate output can be saved as
   `data/extracted_json/B01.json` and inspected, diffed, or replayed.
3. **Self-documenting** — `src/models/schema.py` is the entire data contract
   in one file. Read it first when joining the project.

## Why no AI in the hot path

For ~85% of K STAR-style SLDs, spatial geometry is enough. Adding an LLM call
would:

- Add 1–5 seconds per page (vs <100ms for the deterministic path)
- Introduce non-determinism (same input → different output on different days)
- Cost money (API tokens)
- Make debugging harder (why did it pick that load? "ask the LLM")

**Where AI helps:** the remaining 5–15% of ambiguous cases. The pipeline
already computes confidence scores; we can route the low-confidence matches
to an LLM later (see `src/ai/` placeholder directory).

## Naming conventions (K STAR-specific)

These rules come from the K STAR project knowledge and are encoded in
`src/utils/naming.py`:

- Drop building prefix from breaker names: `B01-1.0-NPNLB-01` → `NPNLB-01`
- Main breakers: `CB-MAIN-{amps}`
- Feeder breakers: `CB-{load}-{amps}`
- Drop `M1B{nn}` prefix for compact breaker names
- VAVs / RTUs / motor-driven loads → eGalvanic class `Motor`
- Other loads → `Load`
- Fuses → `Disconnect Switch` (Rule 9: never use the `Fuse` class)

## Confidence scoring

Each `BreakerLoadMatch` carries a 0–1 confidence based on:
- `dx_score = 1 − dx/80` (vertical alignment)
- `dy_score = 1 − dy/500` (proximity)
- `confidence = 0.7 * dx_score + 0.3 * dy_score`

Matches with confidence < 0.5 should be flagged for human review. The
benchmark CLI will surface these.

## Failure modes

| Symptom | Likely cause | Fix |
|---|---|---|
| `RuntimeError: PDF has no vector content` | Raster-only PDF | Add OCR fallback (PaddleOCR) |
| Breaker found but no load matched | Wire jog > 80 pt | Increase `max_dx` in spatial_match |
| Wrong load matched (visually offset breaker) | Label-symbol offset | Use wire-graph BFS instead |
| Missing transformer in chain | Transformer x-offset from chain | Loosen `tol_x` in build_connection_chain |
| Asset count too low | Too many clusters classified `OTHER` | Add patterns to classifier |
| Asset count too high | Paragraphs not rejected | Tighten paragraph detection in classifier |

## Where to extend next

In order of impact-to-effort ratio:

1. **Panel schedule parser** — page 2 of most SLDs has a precise schedule
   table. `camelot` or `pdfplumber` can extract these. This gives ground
   truth for every breaker.
2. **Multi-PDF orchestration** — process all 12 K STAR PDFs in one pass,
   resolve cross-building references (CB-EXDP1-300A-B01 → CB-MAIN-300A).
3. **Wire-graph BFS** — when spatial matching is ambiguous, walk the actual
   wire graph from breaker to nearest entity. Already prototyped, needs
   robustness work on near-coincident line endpoints.
4. **LLM ambiguity resolver** — pass the low-confidence cropped region to
   Claude Vision with the candidate options. Use rarely.
