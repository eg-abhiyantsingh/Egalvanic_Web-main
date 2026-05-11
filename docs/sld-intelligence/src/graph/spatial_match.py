"""
Spatial matching: assign each breaker its parent bus and downstream entity.

Pure geometry. No AI. Works because SLDs have predictable spatial structure:
  - Breakers sit just below a horizontal bus (the panel)
  - Each breaker's outgoing wire drops vertically to a downstream entity
  - Downstream entities are usually directly aligned in x with their breaker

This module produces confidence-scored matches that the AI reasoner can later
override for edge cases.
"""
from __future__ import annotations

import logging
from collections.abc import Sequence

from src.models.schema import (
    BreakerLoadMatch,
    Bus,
    ConnectionChain,
    Entity,
)

logger = logging.getLogger(__name__)


def match_breaker_to_bus(
    breaker: Entity,
    buses: Sequence[Bus],
    max_y_distance: float = 100.0,
    x_tolerance: float = 20.0,
) -> Bus | None:
    """
    Find the bus immediately above a breaker (its parent panel bus).

    A breaker's parent bus must be ABOVE it (smaller y) and the breaker's
    x must fall within the bus's x range.
    """
    bx = breaker.center.x
    by = breaker.center.y
    best_bus = None
    best_dy = float('inf')

    for bus in buses:
        if not bus.contains_x(bx, tol=x_tolerance):
            continue
        dy = by - bus.y
        if 0 < dy < max_y_distance + 100 and dy < best_dy:
            best_dy = dy
            best_bus = bus
    return best_bus


def match_breaker_to_downstream(
    breaker: Entity,
    candidates: Sequence[Entity],
    max_dx: float = 80.0,
    max_dy: float = 500.0,
) -> BreakerLoadMatch:
    """
    Find the nearest downstream entity below a breaker.

    A "downstream" entity is below the breaker (greater y) and roughly
    aligned in x. Returns a match with confidence based on how close
    the alignment is.
    """
    bx = breaker.center.x
    by = breaker.center.y
    found: list[tuple[float, float, Entity]] = []

    for ent in candidates:
        if ent is breaker:
            continue
        ex = ent.center.x
        ey = ent.center.y
        if ey < by:
            continue  # must be below
        dx = abs(ex - bx)
        dy = ey - by
        if dx > max_dx or dy > max_dy:
            continue
        found.append((dx, dy, ent))

    if not found:
        return BreakerLoadMatch(breaker=breaker, method='none', confidence=0.0)

    # Best = minimum dx, then minimum dy
    found.sort(key=lambda f: (f[0], f[1]))
    best = found[0]
    confidence = _compute_confidence(best[0], best[1])

    return BreakerLoadMatch(
        breaker=breaker,
        load=best[2],
        dx=best[0],
        dy=best[1],
        method='spatial',
        confidence=confidence,
        alternatives=[
            {'load_text': c[2].text, 'dx': c[0], 'dy': c[1]}
            for c in found[1:3]
        ],
    )


def _compute_confidence(dx: float, dy: float) -> float:
    """
    Heuristic confidence score for a spatial match.
    Closer alignment → higher confidence.
    """
    # dx is the dominant factor — perfect alignment = 1.0
    dx_score = max(0.0, 1.0 - dx / 80.0)
    # dy contributes secondarily
    dy_score = max(0.0, 1.0 - dy / 500.0)
    return round(0.7 * dx_score + 0.3 * dy_score, 3)


def build_connection_chain(
    breaker: Entity,
    terminal: Entity,
    all_entities: Sequence[Entity],
    tol_x: float = 40.0,
) -> ConnectionChain:
    """
    Build a breaker → [intermediates] → terminal chain.

    Inserts any transformers, disconnects, or sub-panels that sit physically
    between the breaker and its final downstream load.
    """
    bx = breaker.center.x
    y_top = breaker.bbox.y2  # bottom edge of breaker bbox
    y_bot = terminal.bbox.y1  # top edge of terminal bbox

    intermediates: list[tuple[float, Entity]] = []
    for ent in all_entities:
        if ent is breaker or ent is terminal:
            continue
        if abs(ent.center.x - bx) > tol_x:
            continue
        if y_top < ent.center.y < y_bot:
            intermediates.append((ent.center.y, ent))

    intermediates.sort(key=lambda t: t[0])
    chain = [breaker] + [e for _, e in intermediates] + [terminal]

    return ConnectionChain(
        breaker=breaker,
        chain=chain,
        terminal=terminal,
        confidence=0.8 if not intermediates else 0.7,
    )
