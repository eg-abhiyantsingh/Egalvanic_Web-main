# SLD Intelligence

Convert electrical Single-Line Diagram (SLD) PDFs into structured XLSX asset
lists for the eGalvanic platform — deterministically, from vector geometry.

## What this does

Given a vector PDF of an SLD, this tool extracts:
- Every breaker with its amperage
- Every panel, transformer, MCC, ATS, generator
- Every load (motors, RTUs, VAVs, compressors, etc.)
- The connection graph between them (breaker → load chains)

And emits a properly formatted eGalvanic bulk-upload `.xlsx` file.

**Accuracy (B01 sample vs `KSTAR_SITE_merged_v3.xlsx` ground truth):**
- Asset accuracy: **77.8%** (28 of 36 ground-truth assets matched)
- Connection accuracy: **75.0%** (12 of 16 connections matched)
- Deterministic spatial reasoning, no AI required, <100ms per page

The gap to 100% is dominated by (a) breakers labeled with circuit prefixes the
matcher can't yet disambiguate, and (b) cross-PDF references that need
multi-PDF orchestration (see Roadmap).

## Why this works

Most SLD-to-Excel tools fail because they treat PDFs as images and use OCR.
This pipeline starts from a different premise: **modern engineering PDFs
contain vector geometry** — every wire, every breaker symbol, every text label
has exact coordinates in the PDF content stream. We extract that geometry
directly and reason about spatial alignment.

This gives us:
- **Sub-pixel coordinate accuracy** — no OCR uncertainty
- **Determinism** — same PDF always produces same output
- **Speed** — milliseconds per page
- **Explainability** — every decision traces back to a coordinate

## Quick start

```bash
# 1. Set up venv
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate

# 2. Install
pip install -r requirements.txt

# 3. Probe a PDF to check if it's vector-based
python -m src.cli.main probe data/raw_pdfs/B01.pdf

# 4. Extract one PDF to XLSX
python -m src.cli.main extract data/raw_pdfs/B01.pdf \
    --building B01 --out data/exports/B01.xlsx

# 5. Batch process a directory
python -m src.cli.main batch data/raw_pdfs/ --out-dir data/exports/

# 6. Benchmark output against a known-good ground truth
python -m src.cli.main benchmark \
    data/exports/B01.xlsx data/ground_truth/v2.xlsx --building B01
```

## Project structure

```
sld-intelligence/
├── data/
│   ├── raw_pdfs/          # input PDFs
│   ├── rendered_pages/    # debug renders (300 DPI)
│   ├── extracted_json/    # geometry.json per PDF
│   ├── ground_truth/      # hand-built XLSXs for benchmarking
│   └── exports/           # generated XLSXs
│
├── src/
│   ├── models/
│   │   └── schema.py       # Pydantic data models (the contract)
│   ├── pdf/
│   │   ├── vector_extract.py   # Stage 1: vector geometry from PDF
│   │   └── renderer.py         # Render PDF → image (for debug)
│   ├── vision/
│   │   ├── text_cluster.py     # Stage 2A: cluster spans into labels
│   │   ├── classifier.py       # Stage 2B: classify entities
│   │   └── bus_detect.py       # Stage 2C: find bus bars
│   ├── graph/
│   │   └── spatial_match.py    # Stage 3: breaker → load matching
│   ├── ai/                     # (optional) LLM reasoning for ambiguity
│   ├── export/
│   │   └── xlsx_export.py      # Stage 5: write eGalvanic XLSX
│   ├── utils/
│   │   └── naming.py           # naming conventions (K STAR / eGalvanic)
│   ├── cli/
│   │   └── main.py             # command-line interface
│   └── pipeline.py             # orchestrator (all stages)
│
├── tests/
│   ├── unit/                   # fast per-module tests
│   ├── integration/            # end-to-end pipeline tests
│   └── fixtures/               # small PDF samples for tests
│
├── notebooks/                  # exploratory analysis
├── scripts/                    # one-off scripts
└── docs/                       # design notes
```

## The pipeline

```
PDF (vector)
    │
    ├── Stage 1: pdf/vector_extract.py
    │           23k+ line segments, 500+ text spans w/ bboxes
    │
    ├── Stage 2A: vision/text_cluster.py
    │            Merge multi-line labels into compound entities
    │
    ├── Stage 2B: vision/classifier.py
    │            Tag each cluster: BREAKER, LOAD, PANEL, TRANSFORMER, …
    │
    ├── Stage 2C: vision/bus_detect.py
    │            Find horizontal bus bars
    │
    ├── Stage 3: graph/spatial_match.py
    │            Match each breaker to its parent bus + downstream entity
    │            Build connection chains, insert transformers/disconnects
    │
    └── Stage 5: export/xlsx_export.py
                 Generate eGalvanic-format .xlsx
```

## Design principles

1. **Vector first.** If the PDF is raster-only, the pipeline degrades
   gracefully but accuracy drops sharply. Probe before processing.
2. **Pydantic everywhere.** Every stage's input and output is a typed model.
   This catches schema errors at the boundary instead of in production.
3. **Pure functions.** Each module is stateless. Same input → same output.
4. **Confidence scores.** Spatial matches carry a 0–1 confidence so
   downstream stages (or humans) can flag the low-confidence ones.
5. **No AI in the hot path.** The pipeline runs deterministically. LLMs are
   reserved for ambiguity resolution on the ~5–10% of edge cases where
   geometry alone is insufficient.

## Known limitations

- **Multi-page schedules**: a transformer's downstream sub-panel breakers are
  often on a separate schedule sheet (page 2). Not yet handled.
- **Cross-building connections**: the chain `B33 → CB-EXDP1-300A-B01 → CB-MAIN-300A → B01-1.0-NPNLB-01` requires processing the B33 PDF and B01 PDF together. Not yet handled.
- **Raster PDFs**: pipeline will warn and produce sparse output. Add OCR
  (PaddleOCR) to recover.
- **Phase-shifted labels**: rarely, a breaker label is offset from its wire
  by > 80 PDF points. Current matcher will miss these.

## Roadmap

- [ ] Multi-page PDF support (panel schedules)
- [ ] Cross-PDF stitching for site-level connections
- [ ] Optional LLM reasoner for ambiguous matches (Claude or GPT-4 Vision)
- [ ] PaddleOCR fallback for raster PDFs
- [ ] Validation rules engine (BFS reachability, load math)
- [ ] Web UI (Streamlit) for non-CLI users

## License

MIT (decide before publishing)
