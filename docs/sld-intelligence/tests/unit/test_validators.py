"""Tests for src.validators.* — using small in-memory XLSX files."""
from __future__ import annotations

from pathlib import Path

import pandas as pd

from src.validators.audit import audit
from src.validators.structural import validate


def _write_xlsx(path: Path, assets: pd.DataFrame, conns: pd.DataFrame) -> Path:
    with pd.ExcelWriter(path) as w:
        assets.to_excel(w, sheet_name="Assets", index=False)
        conns.to_excel(w, sheet_name="Connections", index=False)
    return path


def _minimal_clean_file(tmp_path: Path) -> Path:
    """A small, syntactically perfect bulk-upload file used as the baseline."""
    assets = pd.DataFrame([
        {"asset_name": "UTILITY-B01", "asset_class": "Utility",
         "building": "B01", "parent_asset_name": None},
        {"asset_name": "B01-1.0-NPNLB-01", "asset_class": "Panelboard",
         "building": "B01", "parent_asset_name": None},
        {"asset_name": "CB-MAIN-B01-NPNLB01", "asset_class": "Circuit Breaker",
         "building": "B01", "parent_asset_name": "B01-1.0-NPNLB-01"},
        {"asset_name": "GEN-B01", "asset_class": "Generator",
         "building": "B01", "parent_asset_name": None},
        {"asset_name": "ATS-1", "asset_class": "ATS",
         "building": "B01", "parent_asset_name": None},
    ])
    conns = pd.DataFrame([
        {"source_asset_name": "UTILITY-B01", "source_handle": "bottom-source-0",
         "target_asset_name": "ATS-1", "target_handle": "top-target-0"},
        {"source_asset_name": "GEN-B01", "source_handle": "bottom-source-0",
         "target_asset_name": "ATS-1", "target_handle": "top-target-1"},
        {"source_asset_name": "ATS-1", "source_handle": "bottom-source-0",
         "target_asset_name": "CB-MAIN-B01-NPNLB01", "target_handle": "top-target-0"},
    ])
    return _write_xlsx(tmp_path / "clean.xlsx", assets, conns)


# --- Structural validation ---

class TestStructuralValidation:
    def test_clean_file_passes(self, tmp_path):
        f = _minimal_clean_file(tmp_path)
        report = validate(f)
        assert report.ok, f"Expected pass; got: {[i.message for i in report.errors]}"

    def test_detects_duplicate_names(self, tmp_path):
        assets = pd.DataFrame([
            {"asset_name": "A", "asset_class": "Utility",
             "building": "X", "parent_asset_name": None},
            {"asset_name": "A", "asset_class": "Utility",
             "building": "Y", "parent_asset_name": None},
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "dup.xlsx", assets, conns)
        report = validate(f)
        assert not report.ok
        assert any("duplicate" in e.message.lower() for e in report.errors)

    def test_detects_invalid_class(self, tmp_path):
        assets = pd.DataFrame([
            {"asset_name": "X", "asset_class": "Not A Real Class",
             "building": "B", "parent_asset_name": None},
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "bad-class.xlsx", assets, conns)
        report = validate(f)
        assert not report.ok
        assert any("invalid asset_class" in e.message.lower() for e in report.errors)

    def test_detects_orphan_parent(self, tmp_path):
        assets = pd.DataFrame([
            {"asset_name": "CB-X-20A", "asset_class": "Circuit Breaker",
             "building": "B", "parent_asset_name": "NONEXISTENT-PANEL"},
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "orphan.xlsx", assets, conns)
        report = validate(f)
        assert not report.ok

    def test_detects_orphan_connection(self, tmp_path):
        assets = pd.DataFrame([
            {"asset_name": "A", "asset_class": "Utility",
             "building": "X", "parent_asset_name": None},
        ])
        conns = pd.DataFrame([
            {"source_asset_name": "A", "source_handle": "bottom-source-0",
             "target_asset_name": "GHOST", "target_handle": "top-target-0"},
        ])
        f = _write_xlsx(tmp_path / "ghost.xlsx", assets, conns)
        report = validate(f)
        assert not report.ok
        assert any("bad refs" in e.message.lower() for e in report.errors)

    def test_ats_dual_input_warning(self, tmp_path):
        """An ATS with only one input should generate a warning."""
        assets = pd.DataFrame([
            {"asset_name": "U", "asset_class": "Utility",
             "building": "X", "parent_asset_name": None},
            {"asset_name": "ATS-1", "asset_class": "ATS",
             "building": "X", "parent_asset_name": None},
        ])
        conns = pd.DataFrame([
            {"source_asset_name": "U", "source_handle": "bottom-source-0",
             "target_asset_name": "ATS-1", "target_handle": "top-target-0"},
        ])
        f = _write_xlsx(tmp_path / "one-input-ats.xlsx", assets, conns)
        report = validate(f)
        # ATS issue is a warning, not an error — file still passes
        assert report.ok
        assert len(report.warnings) >= 1


# --- Format audit ---

class TestFormatAudit:
    def test_audit_finds_kva_mixed_types(self, tmp_path):
        # Build a file with Title Case columns AND mixed KVA types
        assets = pd.DataFrame([
            {"asset_name": "T1", "asset_class": "Transformer", "building": "B",
             "Voltage": "480V", "KVA Rating": 75},
            {"asset_name": "T2", "asset_class": "Transformer", "building": "B",
             "Voltage": "480V", "KVA Rating": "45kVA"},  # string!
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "mixed-kva.xlsx", assets, conns)
        report = audit(f)
        codes = {fnd.code for fnd in report.findings}
        assert "KVA_MIXED_TYPES" in codes
        assert "COLUMN_CASE_MIXED" in codes

    def test_audit_finds_main_breaker_naming_drift(self, tmp_path):
        assets = pd.DataFrame([
            {"asset_name": "CB-MAIN-300A", "asset_class": "Circuit Breaker",
             "building": "B01", "parent_asset_name": None},
            {"asset_name": "CB-MAIN-NPNLB03", "asset_class": "Circuit Breaker",
             "building": "B03", "parent_asset_name": None},
            {"asset_name": "CB-MAIN-B04-NPNLB01", "asset_class": "Circuit Breaker",
             "building": "B04", "parent_asset_name": None},
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "naming.xlsx", assets, conns)
        report = audit(f)
        codes = {fnd.code for fnd in report.findings}
        assert "MAIN_BREAKER_NAMING_MIXED" in codes

    def test_audit_finds_missing_building(self, tmp_path):
        assets = pd.DataFrame([
            {"asset_name": "A1", "asset_class": "Utility",
             "building": "B01", "parent_asset_name": None},
            {"asset_name": "A2", "asset_class": "Utility",
             "building": None, "parent_asset_name": None},  # blank!
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "missing-bldg.xlsx", assets, conns)
        report = audit(f)
        codes = {fnd.code for fnd in report.findings}
        assert "MISSING_BUILDING" in codes

    def test_audit_clean_file_no_findings(self, tmp_path):
        """A file with no Title Case columns, consistent CB-MAIN, no blanks."""
        assets = pd.DataFrame([
            {"asset_name": "U", "asset_class": "Utility",
             "building": "B01", "parent_asset_name": None,
             "voltage": "480V", "ampere_rating": "300A"},
        ])
        conns = pd.DataFrame(columns=[
            "source_asset_name", "source_handle",
            "target_asset_name", "target_handle",
        ])
        f = _write_xlsx(tmp_path / "clean-audit.xlsx", assets, conns)
        report = audit(f)
        assert report.ok
        assert len(report.findings) == 0
