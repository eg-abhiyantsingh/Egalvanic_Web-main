"""Compare two K STAR bulk-upload XLSX files and report every difference.

Useful for:
    - Verifying a v3 → v4 merge applied the intended patches
    - Auditing what changed between two team members' edits
    - Building a change log for project stakeholders

Outputs a structured `DiffReport` you can render as text, markdown, or JSON.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import pandas as pd


@dataclass
class AssetChange:
    asset_name: str
    field: str
    old_value: object
    new_value: object


@dataclass
class DiffReport:
    file_a: Path
    file_b: Path
    # Assets
    assets_added: list[str] = field(default_factory=list)
    assets_removed: list[str] = field(default_factory=list)
    assets_changed: list[AssetChange] = field(default_factory=list)
    # Connections (represented as (source, source_handle, target, target_handle) tuples)
    connections_added: list[tuple] = field(default_factory=list)
    connections_removed: list[tuple] = field(default_factory=list)

    @property
    def has_changes(self) -> bool:
        return bool(
            self.assets_added or self.assets_removed or self.assets_changed
            or self.connections_added or self.connections_removed
        )

    def summary_line(self) -> str:
        return (
            f"{self.file_a.name} → {self.file_b.name}: "
            f"+{len(self.assets_added)} / -{len(self.assets_removed)} assets, "
            f"{len(self.assets_changed)} field changes, "
            f"+{len(self.connections_added)} / -{len(self.connections_removed)} conns"
        )


def _conn_key(row) -> tuple:
    return (
        row.get("source_asset_name"),
        row.get("source_handle"),
        row.get("target_asset_name"),
        row.get("target_handle"),
    )


def compare(
    file_a: str | Path,
    file_b: str | Path,
    ignore_fields: set[str] | None = None,
) -> DiffReport:
    """Compare file_a (the baseline) to file_b (the new version).

    Assets are matched on `asset_name`. Connections are matched on the full
    tuple (source, source_handle, target, target_handle).

    `ignore_fields` lets you skip noise like `asset_id` or `connection_id`
    which are generated server-side and shouldn't affect the diff.
    """
    file_a, file_b = Path(file_a), Path(file_b)
    ignore = ignore_fields or {"asset_id", "connection_id"}

    a_assets = pd.read_excel(file_a, sheet_name="Assets")
    b_assets = pd.read_excel(file_b, sheet_name="Assets")
    a_conns = pd.read_excel(file_a, sheet_name="Connections")
    b_conns = pd.read_excel(file_b, sheet_name="Connections")

    report = DiffReport(file_a=file_a, file_b=file_b)

    # --- Asset adds/removes ---
    a_names = set(a_assets["asset_name"])
    b_names = set(b_assets["asset_name"])
    report.assets_added = sorted(b_names - a_names)
    report.assets_removed = sorted(a_names - b_names)

    # --- Asset field changes (intersection) ---
    common = a_names & b_names
    a_idx = a_assets.set_index("asset_name")
    b_idx = b_assets.set_index("asset_name")
    shared_cols = [c for c in a_idx.columns if c in b_idx.columns and c not in ignore]
    for name in sorted(common):
        a_row = a_idx.loc[name]
        b_row = b_idx.loc[name]
        for col in shared_cols:
            a_val = a_row[col]
            b_val = b_row[col]
            # Treat NaN == NaN as equal
            if pd.isna(a_val) and pd.isna(b_val):
                continue
            if a_val != b_val:
                report.assets_changed.append(AssetChange(
                    asset_name=name, field=col, old_value=a_val, new_value=b_val,
                ))

    # --- Connection adds/removes ---
    a_keys = {_conn_key(r) for _, r in a_conns.iterrows()}
    b_keys = {_conn_key(r) for _, r in b_conns.iterrows()}
    report.connections_added = sorted(b_keys - a_keys)
    report.connections_removed = sorted(a_keys - b_keys)

    return report


def print_report(report: DiffReport, max_per_section: int = 25) -> None:
    """Human-friendly text rendering of a DiffReport."""
    print("=" * 70)
    print(f"Diff: {report.file_a.name} → {report.file_b.name}")
    print("=" * 70)
    print(report.summary_line())
    print()

    if report.assets_added:
        print(f"+ Assets added ({len(report.assets_added)}):")
        for n in report.assets_added[:max_per_section]:
            print(f"    + {n}")
        if len(report.assets_added) > max_per_section:
            print(f"    ... +{len(report.assets_added) - max_per_section} more")
        print()

    if report.assets_removed:
        print(f"- Assets removed ({len(report.assets_removed)}):")
        for n in report.assets_removed[:max_per_section]:
            print(f"    - {n}")
        if len(report.assets_removed) > max_per_section:
            print(f"    ... -{len(report.assets_removed) - max_per_section} more")
        print()

    if report.assets_changed:
        print(f"~ Field changes ({len(report.assets_changed)}):")
        for c in report.assets_changed[:max_per_section]:
            print(f"    ~ {c.asset_name}.{c.field}: {c.old_value!r} → {c.new_value!r}")
        if len(report.assets_changed) > max_per_section:
            print(f"    ... +{len(report.assets_changed) - max_per_section} more")
        print()

    if report.connections_added:
        print(f"+ Connections added ({len(report.connections_added)}):")
        for c in report.connections_added[:max_per_section]:
            s, sh, t, th = c
            print(f"    + {s} ({sh}) → {t} ({th})")
        if len(report.connections_added) > max_per_section:
            print(f"    ... +{len(report.connections_added) - max_per_section} more")
        print()

    if report.connections_removed:
        print(f"- Connections removed ({len(report.connections_removed)}):")
        for c in report.connections_removed[:max_per_section]:
            s, sh, t, th = c
            print(f"    - {s} ({sh}) → {t} ({th})")
        if len(report.connections_removed) > max_per_section:
            print(f"    ... -{len(report.connections_removed) - max_per_section} more")
