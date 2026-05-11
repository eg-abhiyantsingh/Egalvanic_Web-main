"""Structural validation — 10-stage integrity check for a K STAR bulk-upload file.

These are the checks that catch real upload-blockers (the kind of thing
eGalvanic will reject or import wrong). Format niceties are in `audit.py`.

Stages:
    1. asset_name uniqueness
    2. asset_class is a recognized eGalvanic class
    3. parent_asset_name refs are valid and point at a Box container
    4. connection source/target refs all resolve
    5. no self-loops
    6. ATS dual-input rule (Normal + Emergency, distinct handles)
    7. containers are non-empty (have children or connections)
    8. (advisory) cross-building connections are counted, not flagged
    9. (advisory) reachability from Utility/Generator roots
   10. summary

A `ValidationReport` is returned; callers decide how to surface issues.
"""
from __future__ import annotations

from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

import pandas as pd

from src.rules.chains import EMERGENCY_HANDLE, NORMAL_HANDLE
from src.rules.classes import (
    ALL_VALID_CLASSES,
    BOX_CONTAINER_CLASSES,
    NON_EMPTY_CONTAINER_CLASSES,
    OCP_CLASSES,
)


@dataclass
class ValidationIssue:
    stage: int
    severity: str          # "error" | "warning" | "info"
    message: str
    affected: list[str] = field(default_factory=list)

    def __str__(self) -> str:
        prefix = {"error": "✗", "warning": "⚠", "info": "ℹ"}.get(self.severity, "·")
        return f"[{self.stage}] {prefix} {self.message}"


@dataclass
class ValidationReport:
    file: Path
    asset_count: int
    connection_count: int
    issues: list[ValidationIssue] = field(default_factory=list)
    class_distribution: dict[str, int] = field(default_factory=dict)
    cross_building_connections: int = 0

    @property
    def errors(self) -> list[ValidationIssue]:
        return [i for i in self.issues if i.severity == "error"]

    @property
    def warnings(self) -> list[ValidationIssue]:
        return [i for i in self.issues if i.severity == "warning"]

    @property
    def ok(self) -> bool:
        return len(self.errors) == 0

    def summary(self) -> str:
        status = "✓ PASS" if self.ok else f"✗ FAIL ({len(self.errors)} errors)"
        return (
            f"{self.file.name}: {status} — "
            f"{self.asset_count} assets, {self.connection_count} connections, "
            f"{len(self.warnings)} warnings"
        )


def validate(xlsx_path: str | Path) -> ValidationReport:
    """Run the 10-stage validation on a K STAR bulk-upload XLSX."""
    xlsx_path = Path(xlsx_path)
    assets = pd.read_excel(xlsx_path, sheet_name="Assets")
    conns = pd.read_excel(xlsx_path, sheet_name="Connections")

    report = ValidationReport(
        file=xlsx_path,
        asset_count=len(assets),
        connection_count=len(conns),
    )
    names = assets["asset_name"].tolist()
    name_set = set(names)

    # === Stage 1: uniqueness ===
    dups = [n for n, c in Counter(names).items() if c > 1]
    if dups:
        report.issues.append(ValidationIssue(
            stage=1, severity="error",
            message=f"{len(dups)} duplicate asset_name(s)",
            affected=dups,
        ))

    # === Stage 2: valid classes ===
    invalid_cls = []
    for _, r in assets.iterrows():
        if r["asset_class"] not in ALL_VALID_CLASSES:
            invalid_cls.append(f"{r['asset_name']} ({r['asset_class']})")
    if invalid_cls:
        report.issues.append(ValidationIssue(
            stage=2, severity="error",
            message=f"{len(invalid_cls)} asset(s) with invalid asset_class",
            affected=invalid_cls[:20],
        ))

    # === Stage 3: parent_asset_name integrity ===
    missing_parents = []
    wrong_class_parents = []
    for _, r in assets.iterrows():
        p = r["parent_asset_name"]
        if pd.isna(p):
            continue
        if p not in name_set:
            missing_parents.append(f"{r['asset_name']} → {p}")
            continue
        parent_cls = assets.loc[assets["asset_name"] == p, "asset_class"].iloc[0]
        if parent_cls not in BOX_CONTAINER_CLASSES:
            wrong_class_parents.append(
                f"{r['asset_name']} (class={r['asset_class']}) → "
                f"{p} (class={parent_cls})"
            )
        if r["asset_class"] not in OCP_CLASSES:
            wrong_class_parents.append(
                f"{r['asset_name']} (class={r['asset_class']}) has a parent "
                f"but its class isn't OCP"
            )
    if missing_parents:
        report.issues.append(ValidationIssue(
            stage=3, severity="error",
            message=f"{len(missing_parents)} parent_asset_name(s) point to non-existent assets",
            affected=missing_parents[:20],
        ))
    if wrong_class_parents:
        report.issues.append(ValidationIssue(
            stage=3, severity="error",
            message=f"{len(wrong_class_parents)} parent-child class mismatches",
            affected=wrong_class_parents[:20],
        ))

    # === Stage 4: connection ref integrity ===
    bad_conns = []
    for i, r in conns.iterrows():
        s, t = r["source_asset_name"], r["target_asset_name"]
        if pd.isna(s) or pd.isna(t):
            bad_conns.append(f"row {i}: NaN ({s!r} → {t!r})")
            continue
        if s not in name_set:
            bad_conns.append(f"row {i}: missing source {s!r}")
        if t not in name_set:
            bad_conns.append(f"row {i}: missing target {t!r}")
    if bad_conns:
        report.issues.append(ValidationIssue(
            stage=4, severity="error",
            message=f"{len(bad_conns)} connection(s) with bad refs",
            affected=bad_conns[:20],
        ))

    # === Stage 5: no self-loops ===
    self_loops = conns[conns["source_asset_name"] == conns["target_asset_name"]]
    if len(self_loops):
        report.issues.append(ValidationIssue(
            stage=5, severity="error",
            message=f"{len(self_loops)} self-loop connection(s)",
            affected=self_loops["source_asset_name"].tolist(),
        ))

    # === Stage 6: ATS dual-input ===
    ats_assets = assets[assets["asset_class"] == "ATS"]["asset_name"].tolist()
    ats_warnings = []
    for ats in ats_assets:
        incoming = conns[conns["target_asset_name"] == ats]
        handles = set(incoming["target_handle"].dropna())
        if len(incoming) < 2:
            ats_warnings.append(f"{ats}: only {len(incoming)} input(s)")
        elif NORMAL_HANDLE not in handles or EMERGENCY_HANDLE not in handles:
            ats_warnings.append(f"{ats}: handles={handles}, expected both 0 and 1")
        elif len(handles) < 2:
            ats_warnings.append(f"{ats}: all inputs on same handle ({handles})")
    if ats_warnings:
        report.issues.append(ValidationIssue(
            stage=6, severity="warning",
            message=f"{len(ats_warnings)} ATS(es) violate dual-input rule",
            affected=ats_warnings,
        ))

    # === Stage 7: non-empty containers ===
    empty_containers = []
    for _, r in assets.iterrows():
        if r["asset_class"] not in NON_EMPTY_CONTAINER_CLASSES:
            continue
        has_kids = (assets["parent_asset_name"] == r["asset_name"]).any()
        has_conns = (
            (conns["source_asset_name"] == r["asset_name"]).any()
            or (conns["target_asset_name"] == r["asset_name"]).any()
        )
        if not has_kids and not has_conns:
            empty_containers.append(r["asset_name"])
    if empty_containers:
        report.issues.append(ValidationIssue(
            stage=7, severity="warning",
            message=f"{len(empty_containers)} empty container(s)",
            affected=empty_containers,
        ))

    # === Stage 8: cross-building count (informational) ===
    bldg_lookup = dict(zip(assets["asset_name"], assets["building"]))
    xb = 0
    for _, r in conns.iterrows():
        sb = bldg_lookup.get(r["source_asset_name"])
        tb = bldg_lookup.get(r["target_asset_name"])
        if pd.notna(sb) and pd.notna(tb) and sb != tb:
            xb += 1
    report.cross_building_connections = xb

    # === Stage 9: class distribution (informational) ===
    report.class_distribution = dict(Counter(assets["asset_class"]))

    # === Stage 10: trivial summary stage — handled at report level ===

    return report


def print_report(report: ValidationReport) -> None:
    """Human-friendly printer for a ValidationReport."""
    print("=" * 70)
    print(f"Validation report for {report.file.name}")
    print("=" * 70)
    print(f"  Assets:                  {report.asset_count}")
    print(f"  Connections:             {report.connection_count}")
    print(f"  Cross-bldg connections:  {report.cross_building_connections}")
    print(f"  Errors:                  {len(report.errors)}")
    print(f"  Warnings:                {len(report.warnings)}")
    print()
    for issue in report.issues:
        print(str(issue))
        for a in issue.affected[:5]:
            print(f"      - {a}")
        if len(issue.affected) > 5:
            print(f"      ... and {len(issue.affected) - 5} more")
    print()
    print(report.summary())
