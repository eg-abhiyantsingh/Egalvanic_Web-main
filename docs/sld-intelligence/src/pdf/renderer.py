"""
PDF rendering utilities for debug visualization and downstream raster processing.
"""
from __future__ import annotations

from pathlib import Path

import fitz
import numpy as np


def render_page(
    pdf_path: str | Path,
    page_number: int = 0,
    dpi: int = 300,
) -> np.ndarray:
    """
    Render a PDF page as a numpy image (RGB, uint8).

    Args:
        pdf_path: Path to PDF
        page_number: 0-indexed page
        dpi: Output resolution. 300 DPI is recommended for SLDs;
             600 DPI for OCR of small labels.

    Returns:
        (H, W, 3) RGB image
    """
    doc = fitz.open(str(pdf_path))
    page = doc[page_number]
    zoom = dpi / 72  # PDF native resolution is 72 DPI
    matrix = fitz.Matrix(zoom, zoom)
    pix = page.get_pixmap(matrix=matrix, alpha=False)
    img = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, 3)
    return img.copy()


def pdf_to_image_file(
    pdf_path: str | Path,
    out_path: str | Path,
    page_number: int = 0,
    dpi: int = 300,
) -> Path:
    """Render a PDF page directly to a PNG/JPEG file."""
    img = render_page(pdf_path, page_number, dpi)
    out_path = Path(out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    try:
        import cv2
        cv2.imwrite(str(out_path), cv2.cvtColor(img, cv2.COLOR_RGB2BGR))
    except ImportError:
        from PIL import Image
        Image.fromarray(img).save(str(out_path))
    return out_path
