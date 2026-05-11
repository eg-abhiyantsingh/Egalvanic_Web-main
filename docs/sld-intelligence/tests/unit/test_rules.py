"""Tests for src.rules.* — the encoded project conventions."""
from __future__ import annotations

import pytest

from src.rules import (
    EMERGENCY_HANDLE,
    # chains
    NORMAL_HANDLE,
    # classes
    FuseClassification,
    ats_input_handle,
    building_uses_b30_pattern,
    # fuses
    classify_fuse_or_switch,
    # naming
    container_name,
    detect_main_breaker_format,
    feeder_breaker_name,
    filter_skipped,
    fuse_rename,
    is_box_container,
    is_ocp_class,
    is_valid_breaker_name,
    is_valid_class,
    main_breaker_name,
    should_skip,
    upstream_placeholder_for_building,
    validate_ats_inputs,
    validate_parent_child,
)

# --- classes.py ---

class TestClasses:
    def test_circuit_breaker_is_ocp(self):
        assert is_ocp_class("Circuit Breaker")

    def test_panelboard_is_box(self):
        assert is_box_container("Panelboard")

    def test_motor_is_neither_ocp_nor_box(self):
        assert not is_ocp_class("Motor")
        assert not is_box_container("Motor")

    def test_disconnect_switch_is_both(self):
        # DS is in both lists — can act as OCP or container
        assert is_ocp_class("Disconnect Switch")
        assert is_box_container("Disconnect Switch")

    def test_unknown_class_invalid(self):
        assert not is_valid_class("Magic Breaker 9000")

    def test_valid_pairing(self):
        ok, _ = validate_parent_child("Circuit Breaker", "Panelboard")
        assert ok

    def test_invalid_pairing_motor_child(self):
        ok, reason = validate_parent_child("Motor", "Panelboard")
        assert not ok
        assert "OCP_CLASSES" in reason

    def test_invalid_pairing_motor_parent(self):
        ok, reason = validate_parent_child("Circuit Breaker", "Motor")
        assert not ok
        assert "BOX_CONTAINER_CLASSES" in reason


# --- naming.py ---

class TestNaming:
    def test_container_name_with_int(self):
        assert container_name("B33", "1.0", "NSWGR", 1) == "B33-1.0-NSWGR-01"

    def test_container_name_with_string(self):
        assert container_name("B33", "EX", "NLIS", "01") == "B33-EX-NLIS-01"

    def test_main_breaker_bldg_panel(self):
        n = main_breaker_name("NPNLB01", building="B04", fmt="bldg_panel")
        assert n == "CB-MAIN-B04-NPNLB01"

    def test_main_breaker_panel_only(self):
        n = main_breaker_name("NPNLB03", fmt="panel_only")
        assert n == "CB-MAIN-NPNLB03"

    def test_main_breaker_amps_only(self):
        n = main_breaker_name("", amps=300, fmt="amps_only")
        assert n == "CB-MAIN-300A"

    def test_main_breaker_missing_args(self):
        with pytest.raises(ValueError):
            main_breaker_name("NPNLB01", fmt="bldg_panel")  # building missing

    def test_detect_format(self):
        assert detect_main_breaker_format("CB-MAIN-300A") == "amps_only"
        assert detect_main_breaker_format("CB-MAIN-B04-NPNLB01") == "bldg_panel"
        assert detect_main_breaker_format("CB-MAIN-NPNLB03") == "panel_only"
        assert detect_main_breaker_format("CB-VAV105-20A") is None

    def test_feeder_breaker(self):
        assert feeder_breaker_name("VAV105", 20) == "CB-VAV105-20A"
        assert feeder_breaker_name("CKT7", 20) == "CB-CKT7-20A"

    def test_is_valid_breaker_name(self):
        assert is_valid_breaker_name("CB-VAV105-20A")
        assert not is_valid_breaker_name("VAV105-20A")


# --- chains.py ---

class TestATSDualInput:
    def test_handle_mapping(self):
        assert ats_input_handle("normal") == NORMAL_HANDLE
        assert ats_input_handle("emergency") == EMERGENCY_HANDLE
        assert NORMAL_HANDLE != EMERGENCY_HANDLE

    def test_validate_correctly_wired_ats(self):
        ok, errs = validate_ats_inputs("ATS-01", [
            {"source_asset_name": "UTILITY-1", "target_handle": NORMAL_HANDLE},
            {"source_asset_name": "GEN-1", "target_handle": EMERGENCY_HANDLE},
        ])
        assert ok
        assert errs == []

    def test_validate_single_input(self):
        ok, errs = validate_ats_inputs("ATS-01", [
            {"source_asset_name": "UTILITY-1", "target_handle": NORMAL_HANDLE},
        ])
        assert not ok
        assert any("only 1 input" in e for e in errs)

    def test_validate_same_handle_violation(self):
        ok, errs = validate_ats_inputs("ATS-01", [
            {"source_asset_name": "UTILITY-1", "target_handle": NORMAL_HANDLE},
            {"source_asset_name": "GEN-1", "target_handle": NORMAL_HANDLE},
        ])
        assert not ok


class TestUpstreamPlaceholder:
    def test_utility_for_direct_feed(self):
        p = upstream_placeholder_for_building("B33")
        assert p.name == "UTILITY-B33"
        assert p.asset_class == "Utility"

    def test_cross_bldg_feed_keeps_source(self):
        p = upstream_placeholder_for_building("B01", source_dp="EXISTING-DP-1")
        assert p.name == "EXISTING-DP-1"


class TestB30Pattern:
    def test_b30_uses_pattern(self):
        assert building_uses_b30_pattern("B30")
        assert building_uses_b30_pattern("B33")

    def test_b01_does_not(self):
        assert not building_uses_b30_pattern("B01")

    def test_dc13_does_not(self):
        # DC13 isn't a numeric B# building
        assert not building_uses_b30_pattern("DC13")


# --- fuses.py ---

class TestFuseClassification:
    def test_fuse_plus_switch_is_fds(self):
        assert classify_fuse_or_switch(True, True) == FuseClassification.FUSED_DISCONNECT

    def test_switch_only_is_disconnect(self):
        assert classify_fuse_or_switch(False, True) == FuseClassification.DISCONNECT

    def test_primary_fuse_alone_kept_as_fuse(self):
        assert classify_fuse_or_switch(True, False, is_primary=True) == FuseClassification.BARE_FUSE

    def test_non_primary_fuse_alone_becomes_disconnect(self):
        # The rule: prefer Disconnect Switch over bare Fuse unless explicitly primary
        assert classify_fuse_or_switch(True, False, is_primary=False) == FuseClassification.DISCONNECT


class TestFuseRename:
    def test_fuse_prefix(self):
        assert fuse_rename("FUSE-OVERHEAD-80A") == "DS-OVERHEAD-80A"

    def test_f_prefix(self):
        assert fuse_rename("F-OVERHEAD") == "DS-OVERHEAD"

    def test_non_fuse_untouched(self):
        assert fuse_rename("CB-MAIN-300A") == "CB-MAIN-300A"


class TestSkipList:
    @pytest.mark.parametrize("label", [
        "LA-1", "LIGHTNING ARRESTER", "NGR", "AMMETER",
        "RCM-1", "CBCT-3", "CT-1200/5", "87 RELAY",
        "DIFFERENTIAL RELAY",
    ])
    def test_should_skip(self, label):
        assert should_skip(label), f"Should skip: {label!r}"

    @pytest.mark.parametrize("label", [
        "B33-1.0-NSWGR-01", "CB-MAIN-300A", "VAV105", "PANEL",
    ])
    def test_should_not_skip(self, label):
        assert not should_skip(label), f"Should NOT skip: {label!r}"

    def test_filter_skipped(self):
        labels = ["B33-1.0-NSWGR-01", "LA-1", "CB-300A", "NGR", "VAV105"]
        kept = filter_skipped(labels)
        assert kept == ["B33-1.0-NSWGR-01", "CB-300A", "VAV105"]
