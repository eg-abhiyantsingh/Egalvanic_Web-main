"""
Batch benchmark — runs the pipeline against every PDF in data/raw_pdfs/
and compares each output to the building's slice of KSTAR_SITE_merged_v3.xlsx.

Produces a per-building accuracy table plus a "what went wrong" sample for
each building so we can see the actual failure patterns instead of guessing.

Usage:
    python -m scripts.benchmark_all
    python -m scripts.benchmark_all --building B01   # one building only
    python -m scripts.benchmark_all --details        # show missed names
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src.pipeline import run_pipeline

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "data" / "raw_pdfs"
GT = ROOT / "data" / "ground_truth" / "KSTAR_SITE_merged_v3.xlsx"
OUT = ROOT / "data" / "exports"

# Map building → PDF filename (the most recent revision of each)
BUILDING_PDFS = {
    "B01": "M1-E600-B01-300_ ELECTRICAL SINGLE LINE DIAGRAM - NEW WORK Rev.0 markup.pdf",
    "B02": "M1-E600-B02-300_ ELECTRICAL SINGLE LINE DIAGRAMS & SCHEDULES Rev.0 markup.pdf",
    "B03": "M1-E600-B03-300_ ELECTRICAL SINGLE LINE DIAGRAM - NEW WORK Rev.1.pdf",
    "B04": "M1-E600-B04-300_ ELECTRICAL SINGLE LINE DIAGRAM -NEW WORK Rev.3 markup.pdf",
    "B09": "M1-E605-B09-300_ ELECTRICAL SINGLE LINE DIAGRAM - B09-MCC-07 Rev.2 markup.pdf",
    "B12": "M1-E605-B12-300_ ELECTRICAL SINGLE LINE DIAGRAM - B12-1.0-NSWGR-01 Rev.4 markup.pdf",
    "B13": "M1-E600-B13-300_ ELECTRICAL SINGLE LINE DIAGRAM -NEW WORK I GAN ENGIN EER P ROFESSIONAL Rev.1 markup.pdf",
    "B30": "M1-E605-B30-300_ ELECTRICAL SINGLE LINE DIAGRAM - B30-1.0-NSWGR-01 Rev.2 markup.pdf",
    "B32": "M1-E600-B32-300_ ELECTRICAL SINGLE LINE DIAGRAM Rev.0 markup.pdf",
    "B33": "M1-E605-B33-301_ ELECTRICAL PHASE II SINGLE LINE DIAGRAM - B33-1.0-NSWGR-01 Rev.0 markup.pdf",
    "B34": "M1-E605-B34-300_ ELECTRICAL SINGLE LINE DIAGRAM - B34-1.0-NSWGR-01 Rev.1 markup.pdf",
    "DC13": "M1-E605-DC13-301_ ELECTRICAL PARTIAL SINGLE LINE DIAGRAM DATA CENTER 13 Rev.2 markup.pdf",
}


def benchmark_one(building: str, pdf_path: Path, gt_assets: pd.DataFrame,
                  gt_conns: pd.DataFrame) -> dict:
    """Run pipeline on one PDF and compare to its slice of the merged GT."""
    out_xlsx = OUT / f"{building}_bench.xlsx"
    OUT.mkdir(parents=True, exist_ok=True)

    try:
        result = run_pipeline(pdf_path, building=building, out_xlsx=out_xlsx)
    except Exception as e:
        return {"building": building, "error": str(e)}

    auto_assets = {a.asset_name for a in result.workbook.assets if a.asset_name}
    auto_conns = {
        (c.source_asset_name, c.target_asset_name)
        for c in result.workbook.connections
    }

    gt_b = gt_assets[gt_assets["building"] == building]
    gt_names = set(gt_b["asset_name"].dropna())
    gt_pairs = {
        (r["source_asset_name"], r["target_asset_name"])
        for _, r in gt_conns.iterrows()
        if r["source_asset_name"] in gt_names or r["target_asset_name"] in gt_names
    }

    matched_a = gt_names & auto_assets
    matched_c = gt_pairs & auto_conns

    # Entity breakdown by class
    cls_gt = gt_b["asset_class"].value_counts().to_dict()

    # Structural metric — how close is the count of each class, regardless
    # of whether the names match? This shows real detection progress.
    auto_cls = pd.Series(
        [a.asset_class for a in result.workbook.assets]
    ).value_counts().to_dict()
    struct_score = 0.0
    struct_total = 0
    for cls, gt_n in cls_gt.items():
        auto_n = auto_cls.get(cls, 0)
        struct_score += min(auto_n, gt_n)
        struct_total += gt_n

    # What's missing
    missed_a = sorted(gt_names - auto_assets)
    extra_a = sorted(auto_assets - gt_names)
    missed_c = sorted(gt_pairs - auto_conns)

    return {
        "building": building,
        "n_lines": result.geometry.stats()["lines"],
        "n_text": result.geometry.stats()["text_spans"],
        "n_breakers": sum(1 for e in result.entities
                          if e.type in ("BREAKER", "MAIN_BREAKER")),
        "auto_assets": len(auto_assets),
        "gt_assets": len(gt_names),
        "matched_assets": len(matched_a),
        "asset_pct": (len(matched_a) / len(gt_names) * 100) if gt_names else 0.0,
        "auto_conns": len(auto_conns),
        "gt_conns": len(gt_pairs),
        "matched_conns": len(matched_c),
        "conn_pct": (len(matched_c) / len(gt_pairs) * 100) if gt_pairs else 0.0,
        "struct_pct": (struct_score / struct_total * 100) if struct_total else 0.0,
        "gt_class_breakdown": cls_gt,
        "auto_class_breakdown": auto_cls,
        "low_confidence": sum(1 for m in result.matches
                              if m.load is not None and m.confidence < 0.5),
        "missed_assets": missed_a,
        "extra_assets": extra_a,
        "missed_conns": missed_c,
    }


def categorize_missed_asset(name: str) -> str:
    """Cluster a missed asset name into a failure-mode bucket."""
    if not isinstance(name, str):
        return "other"
    if re.match(r"^CB-MAIN-", name):
        return "main-breaker-naming"
    if re.match(r"^CB-", name):
        return "feeder-breaker"
    if re.search(r"-B\d+$", name) or re.search(r"^B\d+-", name):
        return "cross-building-ref"
    if any(k in name.upper() for k in ("VAV", "RTU", "AHU")):
        return "load-tag"
    if "XFMR" in name.upper() or re.search(r"[NG]TLV|NTMV", name):
        return "transformer"
    if any(k in name.upper() for k in ("NPNLB", "GPNLB", "NSWBD", "NSWGR", "GSWBD")):
        return "panel-switchboard"
    if any(k in name.upper() for k in ("UTILITY", "OVERHEAD", "FUTURE")):
        return "site-level"
    if any(k in name.upper() for k in ("PUMP", "COMPRESSOR", "FAN", "HEATER",
                                       "CHILLER", "BOILER")):
        return "named-load"
    return "other"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--building", help="Run one building only")
    parser.add_argument("--details", action="store_true",
                        help="List missed asset names per building")
    args = parser.parse_args()

    if not GT.exists():
        print(f"ERROR: Ground truth not found: {GT}", file=sys.stderr)
        return 1

    gt_assets = pd.read_excel(GT, sheet_name="Assets")
    gt_conns = pd.read_excel(GT, sheet_name="Connections")

    items = (BUILDING_PDFS.items()
             if not args.building
             else [(args.building, BUILDING_PDFS[args.building])])

    results = []
    for b, fname in items:
        pdf = RAW / fname
        if not pdf.exists():
            print(f"  ✗ {b}: PDF missing — {fname}")
            continue
        print(f"  → {b} ...", end=" ", flush=True)
        r = benchmark_one(b, pdf, gt_assets, gt_conns)
        results.append(r)
        if "error" in r:
            print(f"ERROR: {r['error']}")
        else:
            print(f"assets {r['matched_assets']}/{r['gt_assets']} ({r['asset_pct']:.0f}%)  "
                  f"conns {r['matched_conns']}/{r['gt_conns']} ({r['conn_pct']:.0f}%)")

    print("\n=== SUMMARY ===")
    print(f"{'Bldg':<6} {'brk':>4} {'GT-A':>5} {'auto':>5} "
          f"{'name%':>6} {'struct%':>8} {'GT-C':>5} {'conn%':>6} {'lowC':>5}")
    print("-" * 60)
    for r in results:
        if "error" in r:
            continue
        print(f"{r['building']:<6} {r['n_breakers']:>4} "
              f"{r['gt_assets']:>5} {r['auto_assets']:>5} "
              f"{r['asset_pct']:>5.1f}% {r['struct_pct']:>7.1f}% "
              f"{r['gt_conns']:>5} {r['conn_pct']:>5.1f}% "
              f"{r['low_confidence']:>5}")

    # Aggregate accuracy
    total_gt_a = sum(r['gt_assets'] for r in results if 'error' not in r)
    total_match_a = sum(r['matched_assets'] for r in results if 'error' not in r)
    total_gt_c = sum(r['gt_conns'] for r in results if 'error' not in r)
    total_match_c = sum(r['matched_conns'] for r in results if 'error' not in r)
    # Aggregate structural metric across all buildings
    total_struct = sum(r.get('struct_pct', 0) * r['gt_assets'] / 100
                       for r in results if 'error' not in r)
    print("-" * 60)
    print(f"{'ALL':<6} {'':>4} {total_gt_a:>5} {'':>5} "
          f"{total_match_a/total_gt_a*100 if total_gt_a else 0:>5.1f}% "
          f"{total_struct/total_gt_a*100 if total_gt_a else 0:>7.1f}% "
          f"{total_gt_c:>5} "
          f"{total_match_c/total_gt_c*100 if total_gt_c else 0:>5.1f}%")

    # Failure-mode clustering across all missed assets
    print("\n=== MISSED-ASSET FAILURE MODES (across all buildings) ===")
    buckets: dict[str, int] = {}
    for r in results:
        if "error" in r:
            continue
        for name in r["missed_assets"]:
            b = categorize_missed_asset(name)
            buckets[b] = buckets.get(b, 0) + 1
    for cat, n in sorted(buckets.items(), key=lambda x: -x[1]):
        print(f"  {cat:<25} {n}")

    if args.details:
        print("\n=== PER-BUILDING MISSED LISTS ===")
        for r in results:
            if "error" in r or not r["missed_assets"]:
                continue
            print(f"\n--- {r['building']} (missed {len(r['missed_assets'])} assets, "
                  f"{len(r['missed_conns'])} connections) ---")
            print(f"  Missed assets: {r['missed_assets'][:20]}")
            if len(r['missed_assets']) > 20:
                print(f"  ...and {len(r['missed_assets']) - 20} more")

    return 0


if __name__ == "__main__":
    sys.exit(main())
