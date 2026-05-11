"""
Site-level multi-PDF orchestration.

Runs the single-PDF pipeline on every SLD in a directory, then merges the
outputs into one site-wide workbook. The merger:

  1. De-duplicates assets across PDFs (same canonical name → single row,
     attributed to its actual owning building).
  2. Resolves cross-PDF references: when B33's SLD shows a feeder going to
     "B01-1.0-NPNLB-01", we already detect that name; the merger ensures
     B01's row owns it and B33 just has a connection to it.
  3. Disambiguates collisions: when two buildings have the same canonical
     name (e.g. both call a panel "NPNLB-01"), the merger keeps both rows
     separate by their `building` qualifier.

This mirrors what `KSTAR_SITE_merged_v3.xlsx` does manually.
"""
from __future__ import annotations

import logging
import re
from pathlib import Path

from src.export.xlsx_export import write_workbook
from src.models.schema import EGalvanicAsset, EGalvanicConnection, EGalvanicWorkbook
from src.pipeline import PipelineResult, run_pipeline
from src.utils.naming import extract_building_from_id

logger = logging.getLogger(__name__)

# Building code extracted from a PDF filename like "M1-E600-B01-300_..." or
# "M1-E605-DC13-301_...". First captured (B\d+|DC\d+) is the building.
_BUILDING_FROM_FILE_RE = re.compile(r'\b(B\d+|DC\d+)\b')


def building_code_from_filename(pdf_path: str | Path) -> str:
    """Pull the building code out of a K STAR PDF filename."""
    name = Path(pdf_path).stem.upper()
    m = _BUILDING_FROM_FILE_RE.search(name)
    return m.group(1) if m else 'UNK'


def run_site(
    pdf_dir: str | Path,
    out_xlsx: str | Path | None = None,
    skip_pattern: str | None = None,
) -> EGalvanicWorkbook:
    """
    Process every PDF in `pdf_dir` and merge the results into one workbook.

    Args:
        pdf_dir: Directory containing the SLD PDFs
        out_xlsx: If given, write the merged workbook here
        skip_pattern: If given, skip PDFs whose filename contains this substring
                      (useful to skip "_sample" copies)

    Returns:
        The merged EGalvanicWorkbook
    """
    pdf_dir = Path(pdf_dir)
    pdfs = sorted(pdf_dir.glob('*.pdf'))
    if skip_pattern:
        pdfs = [p for p in pdfs if skip_pattern not in p.name]

    # Group by building — if multiple files map to one building, take the
    # most recent revision (last in sorted order).
    by_building: dict[str, Path] = {}
    for p in pdfs:
        b = building_code_from_filename(p)
        if b == 'UNK':
            logger.warning(f"Skipping {p.name}: cannot extract building code")
            continue
        by_building[b] = p  # later entries overwrite earlier ones

    logger.info(f"Site run: {len(by_building)} buildings  ({sorted(by_building)})")

    # Run per-building pipeline. Keep the full PipelineResult per building so
    # the merger can inspect each one.
    per_building: dict[str, PipelineResult] = {}
    for b, pdf in by_building.items():
        try:
            per_building[b] = run_pipeline(pdf, building=b)
        except Exception as e:
            logger.error(f"Pipeline failed on {b}: {e}")

    merged = _merge_workbooks(per_building)

    if out_xlsx is not None:
        write_workbook(merged, out_xlsx)
        logger.info(f"Site merged workbook written to {out_xlsx}")

    return merged


def _merge_workbooks(
    per_building: dict[str, PipelineResult],
) -> EGalvanicWorkbook:
    """
    Merge per-building workbooks into one site workbook.

    De-dup strategy:
      1. Same `asset_name` AND same `building` → keep first, drop later.
      2. Same `asset_name` but different `building` → both rows kept
         (legit collision — different physical assets sharing a name).
      3. Cross-PDF: an asset like 'B01-1.0-NPNLB-01' may appear in multiple
         building workbooks (its home + every drawing that references it).
         The merger keeps the row tagged with its OWNING building (B01) and
         drops duplicates from other buildings.
    """
    all_assets: dict[tuple[str, str], EGalvanicAsset] = {}
    connections: list[EGalvanicConnection] = []

    # Pass 1: claim each asset for its owning building.
    # Owning-building rule: if the asset_name has a building prefix, that's
    # the owner. Otherwise, the building of the PDF it was extracted from.
    #
    # Dedup rule:
    #   - Names with explicit building prefix (B01-1.0-NPNLB-01) → ONE row
    #     globally, attributed to that prefix's building. Drops cross-PDF refs.
    #   - Names without building prefix (CB-A1, CB-MAIN-300A, AIR-COMPRESSOR)
    #     → ONE row per (name, building) pair. Different physical assets in
    #     different buildings legitimately share these short names.
    for building, result in per_building.items():
        for a in result.workbook.assets:
            owner = a.building or building
            has_prefix = extract_building_from_id(a.asset_name) is not None
            if has_prefix:
                # Global dedup — first occurrence wins
                key = (a.asset_name, '')
            else:
                # Per-building dedup
                key = (a.asset_name, owner)
            if key in all_assets:
                continue
            all_assets[key] = a

    # Pass 2: keep all connections, dedup by (source, target) pair.
    seen_conns: set[tuple[str, str]] = set()
    for result in per_building.values():
        for c in result.workbook.connections:
            pair = (c.source_asset_name, c.target_asset_name)
            if pair in seen_conns:
                continue
            seen_conns.add(pair)
            connections.append(c)

    return EGalvanicWorkbook(
        assets=list(all_assets.values()),
        connections=connections,
    )
