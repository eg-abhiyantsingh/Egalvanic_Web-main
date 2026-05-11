"""
Command-line interface for the SLD intelligence pipeline.

Usage:
    python -m src.cli.main extract PATH.pdf --building B01 --out output.xlsx
    python -m src.cli.main batch DIRECTORY/ --out-dir exports/
    python -m src.cli.main probe PATH.pdf
    python -m src.cli.main benchmark generated.xlsx truth.xlsx --building B01
"""
from __future__ import annotations

import argparse
import json
import logging
import sys
from pathlib import Path

# Make the src package importable when running as a script
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from src.pdf.vector_extract import extract_page, is_vector_pdf
from src.pipeline import run_pipeline
from src.site import run_site


def cmd_probe(args: argparse.Namespace) -> int:
    pdf = Path(args.pdf)
    if not pdf.exists():
        print(f"ERROR: not found: {pdf}", file=sys.stderr)
        return 1
    geometry = extract_page(pdf)
    stats = geometry.stats()
    is_vec = geometry.stats()['lines'] >= 100
    print(json.dumps({
        'pdf': str(pdf),
        'page_size': [geometry.page_width, geometry.page_height],
        'stats': stats,
        'verdict': 'VECTOR' if is_vec else 'RASTER/EMPTY',
    }, indent=2))
    return 0


def cmd_extract(args: argparse.Namespace) -> int:
    pdf = Path(args.pdf)
    if not pdf.exists():
        print(f"ERROR: not found: {pdf}", file=sys.stderr)
        return 1
    if not is_vector_pdf(pdf):
        print(f"WARNING: {pdf.name} is not vector-based. Results will be poor.",
              file=sys.stderr)

    out = Path(args.out) if args.out else Path(f"data/exports/{args.building}_generated.xlsx")
    result = run_pipeline(pdf, building=args.building, out_xlsx=out, page_number=args.page)
    print(f"\n✅ Saved: {out}")
    print(f"  {result.workbook.stats()}")

    # Flag low-confidence spatial matches for human review
    low = [m for m in result.matches if m.load is not None and m.confidence < 0.5]
    if low:
        print(f"\n⚠️  {len(low)} low-confidence matches — review these:")
        for m in low[:10]:
            print(f"    conf={m.confidence:.2f}  "
                  f"{m.breaker.text[:30]!r:32} → {m.load.text[:30]!r}")
        if len(low) > 10:
            print(f"    ...and {len(low) - 10} more")
    return 0


def cmd_batch(args: argparse.Namespace) -> int:
    src_dir = Path(args.dir)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    pdfs = sorted(src_dir.glob('*.pdf'))
    if not pdfs:
        print(f"ERROR: no PDFs in {src_dir}", file=sys.stderr)
        return 1

    print(f"Processing {len(pdfs)} PDFs...")
    for pdf in pdfs:
        try:
            # Extract building code from filename (e.g. B01, B30, DC13)
            stem = pdf.stem.upper()
            import re
            m = re.search(r'\b(B\d+|DC\d+)\b', stem)
            building = m.group() if m else 'UNK'
            out_path = out_dir / f"{building}_{pdf.stem}.xlsx"
            run_pipeline(pdf, building=building, out_xlsx=out_path)
            print(f"  ✓ {pdf.name} → {out_path.name}")
        except Exception as e:
            print(f"  ✗ {pdf.name}: {e}", file=sys.stderr)
    return 0


def cmd_benchmark(args: argparse.Namespace) -> int:
    """Compare generated XLSX vs hand-built ground truth."""
    import pandas as pd
    auto = pd.read_excel(args.generated, sheet_name='Assets')
    auto_c = pd.read_excel(args.generated, sheet_name='Connections')
    truth = pd.read_excel(args.truth, sheet_name='Assets')
    truth_c = pd.read_excel(args.truth, sheet_name='Connections')

    if args.building:
        truth = truth[truth['building'] == args.building]

    truth_names = set(truth['asset_name'].dropna())
    auto_names = set(auto['asset_name'].dropna())
    truth_pairs = set(
        (r['source_asset_name'], r['target_asset_name'])
        for _, r in truth_c.iterrows()
        if r['source_asset_name'] in truth_names or r['target_asset_name'] in truth_names
    )
    auto_pairs = set(
        (r['source_asset_name'], r['target_asset_name'])
        for _, r in auto_c.iterrows()
    )

    print(f"\n=== Benchmark{' for ' + args.building if args.building else ''} ===")
    print(f"  Assets — truth: {len(truth_names)}, auto: {len(auto_names)}, "
          f"matched: {len(truth_names & auto_names)}")
    print(f"  Connections — truth: {len(truth_pairs)}, auto: {len(auto_pairs)}, "
          f"matched: {len(truth_pairs & auto_pairs)}")
    if truth_names:
        print(f"  Asset accuracy: {len(truth_names & auto_names) / len(truth_names) * 100:.1f}%")
    if truth_pairs:
        print(f"  Connection accuracy: {len(truth_pairs & auto_pairs) / len(truth_pairs) * 100:.1f}%")
    return 0


def cmd_site(args: argparse.Namespace) -> int:
    """Run the site-wide orchestrator on a directory of PDFs."""
    workbook = run_site(args.dir, out_xlsx=args.out, skip_pattern=args.skip)
    print(f"\n✅ Site workbook saved: {args.out}")
    print(f"  {workbook.stats()}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(prog='sld-intel')
    parser.add_argument('-v', '--verbose', action='store_true')
    sub = parser.add_subparsers(dest='cmd', required=True)

    p_probe = sub.add_parser('probe', help='Check if a PDF is vector-based')
    p_probe.add_argument('pdf')
    p_probe.set_defaults(func=cmd_probe)

    p_ext = sub.add_parser('extract', help='Extract one PDF to XLSX')
    p_ext.add_argument('pdf')
    p_ext.add_argument('--building', required=True)
    p_ext.add_argument('--out', help='Output XLSX path')
    p_ext.add_argument('--page', type=int, default=0)
    p_ext.set_defaults(func=cmd_extract)

    p_batch = sub.add_parser('batch', help='Process all PDFs in a directory')
    p_batch.add_argument('dir')
    p_batch.add_argument('--out-dir', default='data/exports')
    p_batch.set_defaults(func=cmd_batch)

    p_bench = sub.add_parser('benchmark', help='Compare auto-generated vs truth')
    p_bench.add_argument('generated')
    p_bench.add_argument('truth')
    p_bench.add_argument('--building', help='Filter truth to this building')
    p_bench.set_defaults(func=cmd_benchmark)

    p_site = sub.add_parser(
        'site', help='Run pipeline on all PDFs in a directory and merge')
    p_site.add_argument('dir')
    p_site.add_argument('--out', required=True, help='Output merged XLSX path')
    p_site.add_argument('--skip', help='Skip filenames containing this substring')
    p_site.set_defaults(func=cmd_site)

    args = parser.parse_args()
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format='%(asctime)s [%(levelname)s] %(name)s: %(message)s',
        datefmt='%H:%M:%S',
    )
    return args.func(args)


if __name__ == '__main__':
    sys.exit(main())
