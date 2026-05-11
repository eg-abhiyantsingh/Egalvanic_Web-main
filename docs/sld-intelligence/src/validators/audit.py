"""Format audit — catches the four kinds of inconsistency the Mac codebase audit
flagged on v3:

    1. KVA Rating column has mixed types (str / int / float)
    2. CB-MAIN naming uses multiple formats site-wide
    3. Column headers mix snake_case and Title Case
    4. Site-level assets have a blank `building` field

Per Mukul's guidance, these are *not* upload-blockers — eGalvanic accepts the
file either way. But fixing them makes the dataset queryable in Python/pandas
without surprises, and keeps the format aligned with the canonical
`Memorial_Hospital_bulk_upload.xlsx` template.

This module reports the issues. Fixing them is a separate concern
(see `scripts/normalize_merged.py` once written, or apply manually).
"""
from __future__ import annotations

import re
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

import pandas as pd

from src.rules.naming import detect_main_breaker_format

# --- Canonical template column sets ---
# Mukul's spec — 15-column base; the five property columns are optional
# but should be snake_case if present.
CANONICAL_BASE_COLUMNS: tuple[str, ...] = (
    "asset_id", "asset_name", "asset_class", "asset_subtype", "building",
    "floor", "room", "com", "criticality", "operating_conditions",
    "maintenance_state", "suggested_shortcut", "qr_code",
    "parent_asset_name", "delete",
)

CANONICAL_PROPERTY_COLUMNS_SNAKE: tuple[str, ...] = (
    "voltage", "ampere_rating", "kva_rating",
    "primary_voltage", "starting_voltage",
)

# The mapping of "what we see" → "what it should be"
TITLE_TO_SNAKE = {
    "Voltage": "voltage",
    "Ampere Rating": "ampere_rating",
    "KVA Rating": "kva_rating",
    "Primary Voltage": "primary_voltage",
    "Starting Voltage": "starting_voltage",
}


@dataclass
class AuditFinding:
    code: str               # e.g. "KVA_MIXED_TYPES"
    severity: str           # "error" | "warning" | "info"
    description: str
    details: list[str] = field(default_factory=list)
    fix_hint: str = ""


@dataclass
class AuditReport:
    file: Path
    findings: list[AuditFinding] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not any(f.severity == "error" for f in self.findings)


def audit(xlsx_path: str | Path) -> AuditReport:
    """Run the 4-finding format audit."""
    xlsx_path = Path(xlsx_path)
    assets = pd.read_excel(xlsx_path, sheet_name="Assets")
    report = AuditReport(file=xlsx_path)

    # --- Finding 1: KVA Rating mixed types ---
    if "KVA Rating" in assets.columns or "kva_rating" in assets.columns:
        col = "KVA Rating" if "KVA Rating" in assets.columns else "kva_rating"
        kva = assets[col].dropna()
        types = Counter(type(v).__name__ for v in kva)
        str_vals = [v for v in kva if isinstance(v, str)]
        if "str" in types and ("int" in types or "float" in types):
            report.findings.append(AuditFinding(
                code="KVA_MIXED_TYPES",
                severity="warning",
                description=(
                    f"`{col}` mixes types ({dict(types)}); "
                    f"sorting/aggregation will fail."
                ),
                details=sorted({str(v) for v in str_vals})[:20],
                fix_hint=(
                    "Strip unit suffix: df[col] = df[col].astype(str)"
                    ".str.replace(r'\\s*kVA$', '', regex=True).astype(float)"
                ),
            ))

    # --- Finding 2: CB-MAIN naming inconsistency ---
    breakers = assets[assets["asset_class"] == "Circuit Breaker"]
    mains = breakers[breakers["asset_name"].str.contains("CB-MAIN", na=False)]
    if len(mains) > 0:
        fmts = Counter()
        for n in mains["asset_name"]:
            fmt = detect_main_breaker_format(n) or "unknown"
            fmts[fmt] += 1
        if len(fmts) > 1:
            report.findings.append(AuditFinding(
                code="MAIN_BREAKER_NAMING_MIXED",
                severity="warning",
                description=(
                    f"CB-MAIN-* uses {len(fmts)} formats across {len(mains)} mains"
                ),
                details=[f"{fmt}: {n}" for fmt, n in fmts.items()],
                fix_hint=(
                    "Standardize on one format (bldg_panel recommended). "
                    "See src/rules/naming.py::main_breaker_name"
                ),
            ))

    # --- Finding 3: Column case inconsistency ---
    cols = list(assets.columns)
    snake = [c for c in cols if c.islower() and ("_" in c or c.isalnum())]
    title = [c for c in cols if c[0].isupper()]
    if snake and title:
        offenders = [
            f"{c} → should be {TITLE_TO_SNAKE.get(c, c.lower().replace(' ', '_'))}"
            for c in title
        ]
        report.findings.append(AuditFinding(
            code="COLUMN_CASE_MIXED",
            severity="warning",
            description=(
                f"Column headers mix snake_case ({len(snake)}) and "
                f"Title Case ({len(title)}); canonical template is all snake_case."
            ),
            details=offenders,
            fix_hint=(
                "df.rename(columns={'Voltage': 'voltage', "
                "'Ampere Rating': 'ampere_rating', ...}, inplace=True)"
            ),
        ))

    # --- Finding 4: Site-level assets with blank building ---
    if "building" in assets.columns:
        blank_bldg = assets[assets["building"].isna()]
        if len(blank_bldg) > 0:
            # Try to infer the intended building from the name
            implied = []
            for _, r in blank_bldg.iterrows():
                m = re.search(r"\bB\d+\b", str(r["asset_name"]))
                tag = f" (name implies {m.group(0)})" if m else ""
                implied.append(f"{r['asset_name']} [{r['asset_class']}]{tag}")
            report.findings.append(AuditFinding(
                code="MISSING_BUILDING",
                severity="info",
                description=(
                    f"{len(blank_bldg)} site-level asset(s) have no `building` value. "
                    f"Per Mukul this is acceptable, but for queryability consider "
                    f"using 'SITE' or 'default' instead of NaN."
                ),
                details=implied,
                fix_hint="df['building'] = df['building'].fillna('SITE')",
            ))

    return report


def print_report(report: AuditReport) -> None:
    """Human-friendly printer for an AuditReport."""
    print("=" * 70)
    print(f"Format audit for {report.file.name}")
    print("=" * 70)
    if not report.findings:
        print("  ✓ No format issues found — file matches canonical conventions.")
        return
    for f in report.findings:
        icon = {"error": "✗", "warning": "⚠", "info": "ℹ"}.get(f.severity, "·")
        print(f"\n  {icon}  [{f.code}] {f.description}")
        for d in f.details[:10]:
            print(f"      • {d}")
        if len(f.details) > 10:
            print(f"      ... +{len(f.details) - 10} more")
        if f.fix_hint:
            print(f"     Fix: {f.fix_hint}")
