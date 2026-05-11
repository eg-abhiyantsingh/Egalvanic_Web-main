"""Naming conventions for K STAR assets and breakers.

Distilled from v3/v4 patterns:

  Container assets (panels, switchgear, MCCs):
      {BLDG}-{LEVEL}-{KIND}-{NUM}        e.g. B33-1.0-NSWGR-01, B03-1.0-NPNLB-04
      {BLDG}-EX-{KIND}-{NUM}             e.g. B33-EX-NLIS-01 (existing equipment)

  Main breakers — for site-wide collision-freedom, prefer the building-prefixed form:
      CB-MAIN-{BLDG}-{PANEL}             e.g. CB-MAIN-B04-NPNLB01 (preferred)
      CB-MAIN-{PANEL}                    e.g. CB-MAIN-NPNLB03 (legacy — kept for B33-side panels)
      CB-MAIN-{AMPS}                     e.g. CB-MAIN-300A (only when panel name is implicit, B01/B02)

  Feeder breakers (children of a panel):
      CB-{TARGET-SHORTNAME}-{AMPS}       e.g. CB-VAV105-20A, CB-NPNLB14-200A
      CB-CKT{N}-{AMPS}                   e.g. CB-CKT1-125A (when target isn't labeled)

  Motors and loads (matches actual SLD tags — never invent these):
      M1B{BLDG}{KIND}{NUM}               e.g. M1B01VAV105, M1B4ACT118

The naming is deliberately deterministic — given the same inputs we always
produce the same name, which makes the merge step idempotent.

Asset names must be unique site-wide. The functions below enforce that
implicitly by including disambiguators in their output.
"""
from __future__ import annotations

import re

# --- Container naming ---

def container_name(building: str, level: str, kind: str, num: int | str) -> str:
    """Generate a container asset name.

    Examples:
        >>> container_name("B33", "1.0", "NSWGR", 1)
        'B33-1.0-NSWGR-01'
        >>> container_name("B33", "EX", "NLIS", 1)
        'B33-EX-NLIS-01'
    """
    num_str = f"{num:02d}" if isinstance(num, int) else str(num)
    return f"{building}-{level}-{kind}-{num_str}"


# --- Main-breaker naming ---

# Three accepted formats — we standardize NEW assets on the "bldg+panel" form
# because it's collision-free site-wide.
MAIN_BREAKER_FORMATS = ("bldg_panel", "panel_only", "amps_only")
PREFERRED_MAIN_BREAKER_FORMAT = "bldg_panel"


def main_breaker_name(
    panel_name: str,
    building: str | None = None,
    amps: int | None = None,
    fmt: str = PREFERRED_MAIN_BREAKER_FORMAT,
) -> str:
    """Generate a main-breaker name in one of the three accepted formats.

    Examples:
        >>> main_breaker_name("NPNLB01", building="B04", fmt="bldg_panel")
        'CB-MAIN-B04-NPNLB01'
        >>> main_breaker_name("NPNLB03", fmt="panel_only")
        'CB-MAIN-NPNLB03'
        >>> main_breaker_name("", amps=300, fmt="amps_only")
        'CB-MAIN-300A'
    """
    if fmt == "bldg_panel":
        if not building:
            raise ValueError("bldg_panel format requires `building`")
        return f"CB-MAIN-{building}-{panel_name}"
    if fmt == "panel_only":
        if not panel_name:
            raise ValueError("panel_only format requires `panel_name`")
        return f"CB-MAIN-{panel_name}"
    if fmt == "amps_only":
        if amps is None:
            raise ValueError("amps_only format requires `amps`")
        return f"CB-MAIN-{amps}A"
    raise ValueError(f"Unknown format: {fmt!r}. Choose from {MAIN_BREAKER_FORMATS}")


def detect_main_breaker_format(name: str) -> str | None:
    """Reverse-engineer the format used for an existing main-breaker name."""
    if re.match(r"^CB-MAIN-\d+A?$", name):
        return "amps_only"
    if re.match(r"^CB-MAIN-B\d+-", name):
        return "bldg_panel"
    if name.startswith("CB-MAIN-"):
        return "panel_only"
    return None


# --- Feeder-breaker naming ---

def feeder_breaker_name(
    target_or_circuit: str,
    amps: int | str,
    spare: bool = False,
) -> str:
    """Generate a feeder-breaker name.

    Examples:
        >>> feeder_breaker_name("VAV105", 20)
        'CB-VAV105-20A'
        >>> feeder_breaker_name("CKT7", 20)
        'CB-CKT7-20A'
        >>> feeder_breaker_name("NPNLB14", 200, spare=False)
        'CB-NPNLB14-200A'
    """
    amps_str = f"{amps}A" if isinstance(amps, int) else str(amps)
    base = f"CB-{target_or_circuit}-{amps_str}"
    if spare and "SPARE" not in base.upper():
        base = f"{base}-SPARE"
    return base


# --- Validation ---

CB_NAME_PATTERN = re.compile(r"^CB-")

def is_valid_breaker_name(name: str) -> bool:
    """Cheap structural check — breaker names must start with CB-."""
    return bool(CB_NAME_PATTERN.match(name))
