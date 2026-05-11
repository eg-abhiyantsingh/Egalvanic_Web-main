.PHONY: help install dev test lint format probe extract benchmark clean

help:
	@echo "SLD Intelligence — common tasks"
	@echo ""
	@echo "  make install      Install package"
	@echo "  make dev          Install with dev extras (testing, linting)"
	@echo "  make test         Run all tests"
	@echo "  make lint         Lint with ruff"
	@echo "  make format       Auto-format with ruff"
	@echo "  make probe PDF=path/to.pdf"
	@echo "                    Check if a PDF is vector-based"
	@echo "  make extract PDF=path/to.pdf BUILDING=B01"
	@echo "                    Extract one PDF to XLSX"
	@echo "  make benchmark    Run benchmark on B01 reference"
	@echo "  make clean        Remove build artifacts"

install:
	pip install -e .

dev:
	pip install -e ".[dev]"

test:
	pytest -v

lint:
	ruff check src/ tests/
	mypy src/ || true

format:
	ruff format src/ tests/
	ruff check --fix src/ tests/

probe:
	@if [ -z "$(PDF)" ]; then echo "Usage: make probe PDF=path/to.pdf"; exit 1; fi
	python -m src.cli.main probe $(PDF)

extract:
	@if [ -z "$(PDF)" ] || [ -z "$(BUILDING)" ]; then \
		echo "Usage: make extract PDF=path/to.pdf BUILDING=B01"; exit 1; fi
	python -m src.cli.main extract $(PDF) --building $(BUILDING) \
	    --out data/exports/$(BUILDING)_generated.xlsx

benchmark:
	@python -m src.cli.main extract data/raw_pdfs/B01_sample.pdf \
	    --building B01 --out data/exports/B01_bench.xlsx
	@python -m src.cli.main benchmark data/exports/B01_bench.xlsx \
	    data/ground_truth/v2.xlsx --building B01

clean:
	rm -rf build/ dist/ *.egg-info __pycache__/ .pytest_cache/ .mypy_cache/ .ruff_cache/
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name '*.pyc' -delete
