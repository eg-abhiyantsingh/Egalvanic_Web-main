"""Connection-chain rules for K STAR topologies.

Encodes three patterns we've codified through manual review:

1. **ATS dual-input rule**: every ATS has TWO target handles, one for Normal
   utility and one for Emergency generator. They must use distinct handles —
   `top-target-0` (Normal) and `top-target-1` (Emergency). If both sources
   end up on the same handle the eGalvanic UI flattens them and the topology
   is wrong.

2. **Upstream placeholder rule**: instead of fully modeling the utility chain
   (substation → transformer → meter → service entrance → main), use a single
   Utility-class placeholder that connects directly to the building's first
   real breaker. Two variants:
       - `UTILITY-{N}` for buildings fed straight from the utility (e.g. B33)
       - `EXISTING-FEEDER` for buildings fed from another building's DP

3. **B30+ chain pattern**: medium-voltage buildings (B30 and later) get the
   full in-series modeling — 300E fuse → NLIS disconnect → K-interlock relay →
   NTMV transformer → CBB busway → neutral junction → F-TVSS fuse → TVSS
   surge suppressor → EMM meter → LDU meter → main breaker.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Literal

# --- ATS dual-input ---

ATSHandle = Literal["top-target-0", "top-target-1"]
NORMAL_HANDLE: ATSHandle = "top-target-0"
EMERGENCY_HANDLE: ATSHandle = "top-target-1"


def ats_input_handle(source_kind: Literal["normal", "emergency"]) -> ATSHandle:
    """Map a source role to the correct ATS target handle.

    Normal (utility/transformer) feed → top-target-0
    Emergency (generator/backup) feed → top-target-1
    """
    return NORMAL_HANDLE if source_kind == "normal" else EMERGENCY_HANDLE


def validate_ats_inputs(
    ats_name: str,
    incoming_connections: list[dict],
) -> tuple[bool, list[str]]:
    """Check that an ATS's incoming connections obey the dual-input rule.

    `incoming_connections` should be a list of dicts with at least:
        {"source_asset_name": str, "target_handle": str}

    Returns (ok, errors). An ATS may temporarily have <2 sources during
    construction but must reach 2 before upload.
    """
    errors: list[str] = []
    if len(incoming_connections) < 2:
        errors.append(
            f"ATS {ats_name!r} has only {len(incoming_connections)} input(s); "
            f"every ATS needs exactly 2 (Normal + Emergency)."
        )
        return False, errors

    handles = [c.get("target_handle") for c in incoming_connections]
    if NORMAL_HANDLE not in handles:
        errors.append(
            f"ATS {ats_name!r} is missing the Normal input handle ({NORMAL_HANDLE})."
        )
    if EMERGENCY_HANDLE not in handles:
        errors.append(
            f"ATS {ats_name!r} is missing the Emergency input handle ({EMERGENCY_HANDLE})."
        )
    if len(set(handles)) < 2:
        errors.append(
            f"ATS {ats_name!r} has multiple inputs on the same handle "
            f"({handles}); each input needs a distinct handle."
        )
    return len(errors) == 0, errors


# --- Upstream placeholder ---

@dataclass
class UpstreamPlaceholder:
    name: str
    asset_class: str = "Utility"
    note: str = ""


def upstream_placeholder_for_building(
    building: str,
    source_dp: str | None = None,
) -> UpstreamPlaceholder:
    """Decide which upstream placeholder a building needs.

    If the building has its own utility tap → UTILITY-{N}.
    If it's fed from another building's distribution panel → EXISTING-FEEDER
    (with the source DP name embedded literally as it appears on the SLD).
    """
    if source_dp:
        return UpstreamPlaceholder(
            name=source_dp.upper().replace(" ", "-"),
            asset_class="Utility",
            note=f"Cross-building feed from {source_dp}",
        )
    return UpstreamPlaceholder(
        name=f"UTILITY-{building}",
        asset_class="Utility",
        note=f"Direct utility feed for {building}",
    )


# --- B30+ chain pattern ---

# Each tuple is (asset_class_suggestion, role_label, "skip" flag)
# Read top-to-bottom as the order of devices in series from utility to MAIN.
B30_PLUS_CHAIN = [
    ("Fuse",                 "300E primary fuse",              False),
    ("Disconnect Switch",    "NLIS load interrupter switch",   False),
    ("Relay",                "K-interlock",                    False),
    ("Transformer",          "NTMV step-down transformer",     False),
    ("Busway",               "CBB bus duct",                   False),
    ("Junction Box",         "Neutral connection box",         False),
    ("Fuse",                 "F-TVSS protection fuse",         False),
    ("Other",                "TVSS surge suppressor",          False),
    ("Meter",                "EMM energy meter",               False),
    ("Meter",                "LDU load distribution unit",     False),
    ("Circuit Breaker",      "MAIN breaker (in NSWGR)",        False),
]


def building_uses_b30_pattern(building: str) -> bool:
    """B30, B31, B33, B34 use the full chain. B12 has it but is partially scoped
    by the Data Center package. Earlier buildings (B01-B09) don't have MV.
    """
    if not building.startswith("B"):
        return False
    try:
        num = int(building[1:])
    except ValueError:
        return False
    return num >= 30
