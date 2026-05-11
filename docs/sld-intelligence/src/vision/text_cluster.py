"""
Cluster individual PDF text spans into logical compound labels.

PDFs split multi-line labels (e.g. "225A / W/150A / MCB / 208/120V / 3P 4W")
into separate spans. This module uses union-find to merge spans that belong
together based on:
  - Horizontal alignment (x-centers close)
  - Vertical proximity (small y-gap)
"""
from __future__ import annotations

import logging
from collections import defaultdict
from collections.abc import Sequence

from src.models.schema import BBox, TextCluster, TextSpan

logger = logging.getLogger(__name__)


def cluster_text_spans(
    spans: Sequence[TextSpan],
    max_gap_y: float = 14.0,
    x_center_tol: float = 25.0,
) -> list[TextCluster]:
    """
    Group text spans into compound labels using union-find.

    Args:
        spans: Text spans extracted from a page
        max_gap_y: Maximum vertical pixel gap to consider two spans part of
                   the same label
        x_center_tol: Maximum horizontal distance between span centers

    Returns:
        List of TextCluster, one per merged label group
    """
    n = len(spans)
    if n == 0:
        return []

    parent = list(range(n))

    def find(x: int) -> int:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a: int, b: int) -> None:
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb

    # Sort by (y, x) so we scan top-to-bottom, left-to-right
    sorted_idx = sorted(range(n), key=lambda i: (spans[i].bbox.y1, spans[i].bbox.x1))

    for ii, i in enumerate(sorted_idx):
        bb_i = spans[i].bbox
        cx_i = (bb_i.x1 + bb_i.x2) / 2
        for jj in range(ii + 1, len(sorted_idx)):
            j = sorted_idx[jj]
            bb_j = spans[j].bbox
            # Once the y-gap exceeds the threshold, stop scanning
            if bb_j.y1 - bb_i.y2 > max_gap_y:
                break
            cx_j = (bb_j.x1 + bb_j.x2) / 2
            if abs(cx_i - cx_j) < x_center_tol:
                union(i, j)

    # Build clusters
    groups: dict[int, list[TextSpan]] = defaultdict(list)
    for idx, span in enumerate(spans):
        groups[find(idx)].append(span)

    clusters: list[TextCluster] = []
    for members in groups.values():
        members.sort(key=lambda s: (s.bbox.y1, s.bbox.x1))
        x1 = min(m.bbox.x1 for m in members)
        y1 = min(m.bbox.y1 for m in members)
        x2 = max(m.bbox.x2 for m in members)
        y2 = max(m.bbox.y2 for m in members)
        clusters.append(TextCluster(
            text=' '.join(m.text for m in members),
            lines=[m.text for m in members],
            bbox=BBox(x1=x1, y1=y1, x2=x2, y2=y2),
            n_lines=len(members),
            font_sizes=sorted({round(m.font_size, 1) for m in members}),
        ))

    logger.info(f"Clustered {n} spans into {len(clusters)} labels")
    return clusters
