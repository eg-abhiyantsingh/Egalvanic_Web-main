"""New sub-commands for the sld-intelligence CLI.

These plug into the existing argparse setup in `src/cli/main.py`. To wire
them up, add the following inside the existing `main()` function's subparser
setup:

    from src.cli.commands import register_subcommands
    register_subcommands(subparsers)

Then in the dispatcher block:

    elif args.command == "triage":
        from src.cli.commands import cmd_triage; cmd_triage(args)
    elif args.command == "audit":
        from src.cli.commands import cmd_audit; cmd_audit(args)
    elif args.command == "validate":
        from src.cli.commands import cmd_validate; cmd_validate(args)
    elif args.command == "compare":
        from src.cli.commands import cmd_compare; cmd_compare(args)

Run examples:
    python -m src.cli.main triage data/raw_pdfs/B33.pdf
    python -m src.cli.main triage data/raw_pdfs/               # whole dir
    python -m src.cli.main audit data/ground_truth/v3.xlsx
    python -m src.cli.main validate data/ground_truth/v3.xlsx
    python -m src.cli.main compare data/ground_truth/v3.xlsx data/exports/v4.xlsx
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def register_subcommands(subparsers: argparse._SubParsersAction) -> None:
    """Register the four new commands on an existing argparse subparsers obj."""
    # triage
    p = subparsers.add_parser(
        "triage",
        help="Classify a PDF as vector / raster / procore-zip",
    )
    p.add_argument("path", help="PDF file OR directory of PDFs")
    p.add_argument("--json", action="store_true", help="Output JSON instead of text")

    # audit
    p = subparsers.add_parser(
        "audit",
        help="Run the format-consistency audit on a K STAR bulk-upload XLSX",
    )
    p.add_argument("xlsx", help="Path to the Assets/Connections workbook")
    p.add_argument("--json", action="store_true")

    # validate
    p = subparsers.add_parser(
        "validate",
        help="Run the 10-stage structural validation",
    )
    p.add_argument("xlsx", help="Path to the Assets/Connections workbook")
    p.add_argument("--json", action="store_true")

    # compare
    p = subparsers.add_parser(
        "compare",
        help="Diff two K STAR bulk-upload XLSX files (baseline → new)",
    )
    p.add_argument("baseline", help="The previous version (e.g. v3.xlsx)")
    p.add_argument("new", help="The current version (e.g. v4.xlsx)")
    p.add_argument(
        "--max-per-section", type=int, default=25,
        help="Cap how many items to print per change category",
    )


# --- Command handlers ---

def cmd_triage(args: argparse.Namespace) -> int:
    from src.pdf.triage import triage, triage_directory
    path = Path(args.path)
    if path.is_dir():
        results = triage_directory(path)
    else:
        results = [triage(path)]

    if args.json:
        print(json.dumps([r.to_dict() for r in results], indent=2))
    else:
        for r in results:
            print(f"  {r.path.name:60s}  {r.kind.value:14s}  "
                  f"(confidence={r.confidence:.2f}) — {r.notes}")
    return 0


def cmd_audit(args: argparse.Namespace) -> int:
    from src.validators.audit import audit, print_report
    report = audit(args.xlsx)
    if args.json:
        print(json.dumps({
            "file": str(report.file),
            "findings": [
                {"code": f.code, "severity": f.severity,
                 "description": f.description, "details": f.details,
                 "fix_hint": f.fix_hint}
                for f in report.findings
            ],
        }, indent=2))
    else:
        print_report(report)
    return 0 if report.ok else 1


def cmd_validate(args: argparse.Namespace) -> int:
    from src.validators.structural import print_report, validate
    report = validate(args.xlsx)
    if args.json:
        print(json.dumps({
            "file": str(report.file),
            "asset_count": report.asset_count,
            "connection_count": report.connection_count,
            "ok": report.ok,
            "issues": [
                {"stage": i.stage, "severity": i.severity,
                 "message": i.message, "affected": i.affected}
                for i in report.issues
            ],
        }, indent=2))
    else:
        print_report(report)
    return 0 if report.ok else 1


def cmd_compare(args: argparse.Namespace) -> int:
    from src.compare import compare, print_report
    report = compare(args.baseline, args.new)
    print_report(report, max_per_section=args.max_per_section)
    return 0 if not report.has_changes else 0  # diff is informational, never fails
