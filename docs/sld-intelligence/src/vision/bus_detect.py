"""
Detect bus bars in the SLD.

A bus bar is a long horizontal line, usually with a thicker stroke width than
regular wires. Breakers hang off the bus from above (typically the panel main
bus). Detecting buses lets us:
  - Identify which breakers belong to which panel
  - Set parent_asset_name for each breaker
"""
from __future__ import annotations

import logging
from collections.abc import Sequence

from src.models.schema import Bus, PageGeometry

logger = logging.getLogger(__name__)


def find_horizontal_buses(
    geometry: PageGeometry,
    min_length: float = 300.0,
    min_width: float = 1.0,
    long_bus_length: float = 800.0,
    horiz_tol: float = 1.0,
) -> list[Bus]:
    """
    Find horizontal line segments long enough to be bus bars.

    A line qualifies as a bus if EITHER:
      (a) length >= min_length AND width >= min_width  (thick painted bus,
                                                         common on building SLDs)
      (b) length >= long_bus_length                    (thin but very long —
                                                         switchgear schematics
                                                         like B33 use 0.2pt lines)

    Args:
        geometry: Extracted page geometry
        min_length: Minimum length for the "thick bus" rule
        min_width: Minimum stroke width for the "thick bus" rule
        long_bus_length: Length above which width doesn't matter
        horiz_tol: Y-coordinate tolerance for "horizontal"

    Returns:
        List of Bus objects sorted by length (longest first)
    """
    page_w = geometry.page_width
    page_h = geometry.page_height
    edge_margin = max(100.0, 0.05 * page_h)  # 5% of page height or 100pt

    buses: list[Bus] = []
    for line in geometry.lines:
        if abs(line.p1.y - line.p2.y) >= horiz_tol:
            continue
        length = abs(line.p2.x - line.p1.x)
        width = line.width or 0
        thick_bus = length >= min_length and width >= min_width
        long_thin_bus = length >= long_bus_length
        if not (thick_bus or long_thin_bus):
            continue
        # Reject page-border horizontals: lines spanning most of the page
        # width AND sitting near the top/bottom edge are title-block borders,
        # not buses.
        spans_page = length > 0.9 * page_w
        near_edge = line.p1.y < edge_margin or line.p1.y > (page_h - edge_margin)
        if spans_page and near_edge:
            continue
        buses.append(Bus(
            y=line.p1.y,
            x1=min(line.p1.x, line.p2.x),
            x2=max(line.p1.x, line.p2.x),
            length=length,
            width=width,
        ))

    buses.sort(key=lambda b: -b.length)
    logger.info(f"Detected {len(buses)} candidate buses")
    return buses


def merge_collinear_buses(buses: Sequence[Bus], y_tol: float = 2.0, x_gap_tol: float = 30.0) -> list[Bus]:
    """
    Merge bus segments that are roughly co-linear and overlap or are close together.

    Vector PDFs often store a single visual bus as 2-3 overlapping line segments
    (one for stroke, one for fill, one for shadow). This collapses them.
    """
    if not buses:
        return []

    # Group by y (within tolerance)
    sorted_buses = sorted(buses, key=lambda b: (b.y, b.x1))
    merged: list[Bus] = []
    current = sorted_buses[0]

    for nxt in sorted_buses[1:]:
        if abs(nxt.y - current.y) <= y_tol and nxt.x1 <= current.x2 + x_gap_tol:
            # Merge
            current = Bus(
                y=(current.y + nxt.y) / 2,
                x1=min(current.x1, nxt.x1),
                x2=max(current.x2, nxt.x2),
                length=max(current.x2, nxt.x2) - min(current.x1, nxt.x1),
                width=max(current.width, nxt.width),
            )
        else:
            merged.append(current)
            current = nxt
    merged.append(current)
    merged.sort(key=lambda b: -b.length)
    return merged
