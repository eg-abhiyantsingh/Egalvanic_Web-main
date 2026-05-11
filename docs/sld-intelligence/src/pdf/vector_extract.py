"""
PDF vector geometry extraction.

Uses PyMuPDF (fitz) to extract:
- Every line segment with exact coordinates
- Every text span with bbox, font, size
- Every rectangle (panel boundaries, breaker boxes)

No OCR. No raster processing. Pure vector extraction from the PDF content stream.

This is the most important module in the pipeline — its output is ground truth
that downstream stages depend on.
"""
from __future__ import annotations

import logging
from pathlib import Path

import fitz  # PyMuPDF

from src.models.schema import (
    BBox,
    LineSegment,
    PageGeometry,
    Point,
    TextSpan,
)

logger = logging.getLogger(__name__)


def _point_from_obj(obj) -> Point:
    """Normalize different PyMuPDF point representations."""
    if hasattr(obj, 'x'):
        return Point(x=round(obj.x, 2), y=round(obj.y, 2))
    return Point(x=round(obj[0], 2), y=round(obj[1], 2))


def extract_page(
    pdf_path: str | Path,
    page_number: int = 0,
    min_line_length: float = 0.5,
) -> PageGeometry:
    """
    Extract vector geometry from one PDF page.

    Args:
        pdf_path: Path to the PDF
        page_number: 0-indexed page number
        min_line_length: Lines shorter than this are filtered out (dots, marks)

    Returns:
        PageGeometry with all extracted primitives

    Raises:
        FileNotFoundError: PDF doesn't exist
        ValueError: Page out of range
        RuntimeError: PDF has no extractable vector content (raster-only)
    """
    pdf_path = Path(pdf_path)
    if not pdf_path.exists():
        raise FileNotFoundError(f"PDF not found: {pdf_path}")

    doc = fitz.open(str(pdf_path))
    if page_number >= len(doc):
        raise ValueError(f"Page {page_number} out of range (PDF has {len(doc)} pages)")

    page = doc[page_number]
    page_w, page_h = page.rect.width, page.rect.height

    # -------- TEXT --------
    text_spans = _extract_text_spans(page)

    # -------- VECTOR DRAWINGS --------
    lines, rectangles = _extract_drawings(page, min_line_length)

    if len(lines) < 50 and not text_spans:
        logger.warning(
            "PDF appears to be raster-only (no vector content). "
            "Pipeline accuracy will be severely degraded."
        )

    geometry = PageGeometry(
        source_pdf=str(pdf_path),
        page_number=page_number,
        page_width=page_w,
        page_height=page_h,
        lines=lines,
        text_spans=text_spans,
        rectangles=rectangles,
    )
    logger.info(f"Extracted geometry: {geometry.stats()}")
    return geometry


def _extract_text_spans(page) -> list[TextSpan]:
    """Extract every text span with its bounding box."""
    spans: list[TextSpan] = []
    text_dict = page.get_text("dict")
    for block in text_dict.get('blocks', []):
        if block.get('type') != 0:  # 0 = text, 1 = image
            continue
        for line in block.get('lines', []):
            for span in line.get('spans', []):
                txt = span['text'].strip()
                if not txt:
                    continue
                bb = span['bbox']
                spans.append(TextSpan(
                    text=txt,
                    bbox=BBox(x1=bb[0], y1=bb[1], x2=bb[2], y2=bb[3]),
                    font_size=round(span.get('size', 0), 1),
                    font=span.get('font', ''),
                    color=span.get('color', 0),
                ))
    return spans


def _extract_drawings(page, min_line_length: float) -> tuple[list[LineSegment], list[BBox]]:
    """Extract all line segments and rectangles."""
    lines: list[LineSegment] = []
    rectangles: list[BBox] = []

    for drawing in page.get_drawings():
        width = drawing.get('width', 0) or 0
        color = drawing.get('color')
        for item in drawing.get('items', []):
            op = item[0]
            if op == 'l':
                p1 = _point_from_obj(item[1])
                p2 = _point_from_obj(item[2])
                if p1.x == p2.x and p1.y == p2.y:
                    continue
                seg = LineSegment(p1=p1, p2=p2, width=round(width, 2),
                                   color=color if isinstance(color, int) else None)
                if seg.length >= min_line_length:
                    lines.append(seg)
            elif op == 're':
                r = item[1]
                rectangles.append(BBox(
                    x1=round(r.x0, 2), y1=round(r.y0, 2),
                    x2=round(r.x1, 2), y2=round(r.y1, 2),
                ))
            # Note: curves ('c', 'qu') are intentionally skipped for now
            # SLD breakers are drawn as Bezier arcs but we identify them by
            # their adjacent text label, not by curve shape.

    return lines, rectangles


def is_vector_pdf(pdf_path: str | Path, threshold: int = 100) -> bool:
    """Quick check: does this PDF have enough vector content to process?"""
    doc = fitz.open(str(pdf_path))
    page = doc[0]
    line_count = 0
    for d in page.get_drawings():
        for item in d.get('items', []):
            if item[0] == 'l':
                line_count += 1
                if line_count >= threshold:
                    return True
    return False
