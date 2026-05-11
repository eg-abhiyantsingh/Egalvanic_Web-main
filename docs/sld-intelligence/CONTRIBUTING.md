# Contributing

## Development setup

```bash
git clone <repo>
cd sld-intelligence
python -m venv venv
source venv/bin/activate
pip install -e ".[dev]"  # editable install + dev extras
pre-commit install        # optional
```

## Running tests

```bash
pytest                                  # all tests
pytest tests/unit                       # unit only
pytest -k "test_air_compressor"         # one test
pytest --cov=src --cov-report=html      # coverage
```

## Code style

- **Type hints** required on all public functions and Pydantic fields.
- **Ruff** for linting and formatting: `ruff check src/ && ruff format src/`
- **Mypy** for type checks: `mypy src/`
- One module = one concern. If a module exceeds ~300 lines, split it.

## Adding a new entity type

1. Add the type to `EntityType` literal in `src/models/schema.py`
2. Add detection logic in `src/vision/classifier.py::classify`
3. Add mapping to `src/utils/naming.py::_CLASS_MAP`
4. Add a unit test in `tests/unit/test_classifier.py`
5. Run benchmark to verify accuracy doesn't regress

## The "one module → test → improve → integrate" loop

Don't paste the whole project into Claude. Work on one module at a time:

```
1. Open ONE file (e.g. src/vision/classifier.py)
2. Add ONE test case for the bug/feature
3. Watch it fail: pytest -k <name>
4. Fix the module
5. Watch it pass
6. Run full suite to check for regressions
7. Run benchmark to check end-to-end accuracy
8. Commit
```

## Benchmark before merging

Every change must run the benchmark and show the accuracy delta:

```bash
python -m src.cli.main extract data/raw_pdfs/B01_sample.pdf \
    --building B01 --out /tmp/B01_test.xlsx
python -m src.cli.main benchmark /tmp/B01_test.xlsx \
    data/ground_truth/v2.xlsx --building B01
```

Expected baseline (as of v0.1, measured against `KSTAR_SITE_merged_v3.xlsx`):
**75.0% connection accuracy, 77.8% asset accuracy on B01.** Any change that
drops either number is a regression.

## When to use Claude / AI

**Use AI for:**
- Algorithm design and architecture decisions
- Debugging confusing classification mistakes
- Generating new test cases from PDF screenshots
- Interpreting ambiguous SLDs (read the SLD → suggest entity)

**Don't use AI for:**
- Running the pipeline (it's deterministic — just run the code)
- Generating production data (use the CLI)
- Storing PDFs (use the filesystem)

## Roadmap priorities

In order of impact:

1. **Cross-PDF stitching** — handle B33 → B01 connections by processing
   multiple PDFs in one run
2. **Multi-page support** — read panel schedules from page 2+
3. **LLM-assisted ambiguity resolution** — for the ~5–10% of cases where
   spatial alignment is ambiguous, pass the cropped region to Claude Vision
4. **Validation rules engine** — BFS reachability, load math sanity checks
5. **Web UI** — Streamlit app for non-CLI users
6. **YOLO symbol detector** — only after we have 20+ labeled SLDs;
   premature before then
