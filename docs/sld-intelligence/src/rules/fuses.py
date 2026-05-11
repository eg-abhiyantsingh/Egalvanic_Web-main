"""Fuse handling and skip-list rules.

Fuse rule
=========
Don't use the bare `Fuse` class — use `Disconnect Switch` for stand-alone
fused disconnects and `Fused Disconnect Switch` for an explicit fuse+switch
combination. The `Fuse` class is reserved for primary fuses that appear
on their own (e.g. F-300E-A in B30's chain), where we still emit `Fuse`
class but with sane handling.

When the SLD shows fuse + switch together (very common — e.g. 100A fuse + 100A
disconnect in one enclosure), they merge into ONE `Fused Disconnect Switch`
asset with `ampere_rating = fuse rating`.

Skip list
=========
Some equipment appears on the SLD but isn't modeled in eGalvanic because
it's either too granular, redundant with what's already modeled, or out of
scope for the digitization. Skipping them keeps the model focused on what
actually matters for monitoring and analysis.
"""
from __future__ import annotations

import re
from collections.abc import Iterable

# --- Fuse handling ---

class FuseClassification:
    """Pre-computed class names the fuse rule can output."""
    BARE_FUSE = "Fuse"                          # standalone primary fuse
    DISCONNECT = "Disconnect Switch"            # disconnect without integrated fuse
    FUSED_DISCONNECT = "Fused Disconnect Switch"  # fuse+switch combo


def classify_fuse_or_switch(
    has_fuse: bool,
    has_switch: bool,
    is_primary: bool = False,
) -> str:
    """Decide the right eGalvanic class for a fuse-and/or-switch device.

    Args:
        has_fuse: drawing shows a fuse symbol
        has_switch: drawing shows a disconnect/switch symbol
        is_primary: True for medium-voltage primary fuses (e.g. F-300E)

    Returns the eGalvanic asset_class to emit.
    """
    if has_fuse and has_switch:
        return FuseClassification.FUSED_DISCONNECT
    if has_switch and not has_fuse:
        return FuseClassification.DISCONNECT
    if has_fuse and not has_switch:
        # Standalone fuse — only emit Fuse class for documented primary fuses,
        # otherwise prefer Disconnect Switch so it doesn't get lost in eGalvanic.
        return FuseClassification.BARE_FUSE if is_primary else FuseClassification.DISCONNECT
    raise ValueError("classify_fuse_or_switch called with neither fuse nor switch")


def fuse_rename(name: str) -> str:
    """Rewrite legacy fuse names per the project's renaming rule.

    The pattern from earlier work:
      FUSE-XXX-30A  →  DS-XXX-30A
      F-OVERHEAD    →  DS-OVERHEAD
    """
    name = re.sub(r"^FUSE-", "DS-", name)
    name = re.sub(r"^F-", "DS-", name)  # only if not already DS-
    return name


# --- Skip list ---

# Regex patterns for SLD labels/symbols we don't model.
# Each pattern is case-insensitive and matches against an asset's name OR
# the raw SLD label.
SKIP_PATTERNS: tuple[re.Pattern, ...] = tuple(re.compile(p, re.IGNORECASE) for p in (
    # Lightning arresters — protection devices that aren't tracked
    r"^LA-?",
    r"LIGHTNING\s*ARRESTER",
    # Neutral grounding resistor — single passive component, not interesting
    r"^NGR$",
    r"NEUTRAL\s+GROUND(ING)?\s+RESISTOR",
    # Ammeters and other simple instruments — too granular
    r"^A[\-\s]?METER$",
    r"^AMMETER",
    # Residual current monitors / core balance CTs — covered by metering
    r"^RCM[\-\s]?\d*$",
    r"^CBCT[\-\s]?\d*$",
    r"^CT[\-\s]?\d+/\d+",
    # Differential protection relay (ANSI 87) — protection logic, not flow
    r"^87[\-\s]",
    r"DIFFERENTIAL\s+RELAY",
    r"\b87\b\s*RELAY",
))


def should_skip(label: str) -> bool:
    """True if a label matches any skip pattern."""
    return any(p.search(label) for p in SKIP_PATTERNS)


def filter_skipped(labels: Iterable[str]) -> list[str]:
    """Drop any labels that match the skip list. Preserves input order."""
    return [lbl for lbl in labels if not should_skip(lbl)]
