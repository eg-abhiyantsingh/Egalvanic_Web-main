"""Procore Markup PDF handler.

Procore's PDF markup feature exports files with a `.pdf` extension that are
actually ZIP archives containing:
    1.jpeg          — rasterized SLD drawing
    1.txt           — pre-extracted text (drawing labels, panel names, ratings)
    manifest.json   — page metadata

This is a *massive* accuracy multiplier compared to running OCR on a raster
PDF, because Procore has already extracted the text spatially and we can
just consume it.

Detection: real PDFs start with `%PDF-` (hex `25504446`). Procore Markup
files start with `PK\\x03\\x04` (hex `504b0304`) — the standard ZIP signature.

Usage:
    >>> from src.pdf.procore_zip import is_procore_zip, extract_procore_zip
    >>> is_procore_zip("/path/to/M1E600B01300_markup.pdf")
    True
    >>> bundle = extract_procore_zip("/path/to/M1E600B01300_markup.pdf")
    >>> bundle.pages[0].text[:80]
    'PANEL NAME: B01-1.0-NPNLB-01, 300A, 480/277V 3PH, 4W, KAIC, ...'
"""
from __future__ import annotations

import json
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

# --- Magic-byte signatures ---
ZIP_MAGIC = b"PK\x03\x04"
PDF_MAGIC = b"%PDF-"


@dataclass
class ProcorePage:
    """One page from a Procore Markup ZIP."""
    page_number: int
    image_bytes: bytes
    text: str
    width: int
    height: int
    media_type: str = "image/jpeg"


@dataclass
class ProcoreBundle:
    """Parsed Procore Markup ZIP — multi-page capable."""
    source_path: Path
    pages: list[ProcorePage] = field(default_factory=list)
    manifest: dict = field(default_factory=dict)

    @property
    def num_pages(self) -> int:
        return len(self.pages)

    @property
    def all_text(self) -> str:
        """Concatenated text across all pages."""
        return "\n".join(p.text for p in self.pages)


def is_procore_zip(path: str | Path) -> bool:
    """Return True if `path` is a Procore Markup ZIP (not a real PDF).

    Reads only the first 4 bytes. Cheap to call on many files.

    Raises FileNotFoundError if path doesn't exist.
    Returns False for empty files, real PDFs, or anything else.
    """
    path = Path(path)
    if not path.exists():
        raise FileNotFoundError(f"No such file: {path}")
    with open(path, "rb") as f:
        head = f.read(4)
    return head == ZIP_MAGIC


def is_real_pdf(path: str | Path) -> bool:
    """Return True if `path` is a real PDF (starts with %PDF-)."""
    path = Path(path)
    if not path.exists():
        raise FileNotFoundError(f"No such file: {path}")
    with open(path, "rb") as f:
        head = f.read(5)
    return head == PDF_MAGIC


def extract_procore_zip(path: str | Path) -> ProcoreBundle:
    """Extract a Procore Markup ZIP into a structured bundle.

    The bundle contains all pages with their text, image bytes, and dimensions.
    Multi-page files (rare in K STAR but supported elsewhere) are handled
    by reading the manifest's `num_pages` and iterating `{N}.jpeg`/`{N}.txt`.

    Raises:
        FileNotFoundError: if path doesn't exist
        ValueError: if path is not a Procore ZIP or is malformed
        zipfile.BadZipFile: if the ZIP itself is corrupt
    """
    path = Path(path)
    if not is_procore_zip(path):
        raise ValueError(
            f"Not a Procore Markup ZIP (no PK\\x03\\x04 signature): {path}"
        )

    bundle = ProcoreBundle(source_path=path)

    with zipfile.ZipFile(path, "r") as zf:
        names = set(zf.namelist())

        # Try to load manifest first — it tells us the page structure
        if "manifest.json" in names:
            with zf.open("manifest.json") as mf:
                bundle.manifest = json.loads(mf.read().decode("utf-8"))
            num_pages = bundle.manifest.get("num_pages", 1)
            page_meta = bundle.manifest.get("pages", [])
        else:
            # Fallback: infer from filenames
            num_pages = sum(1 for n in names if n.endswith(".jpeg"))
            page_meta = []

        for i in range(1, num_pages + 1):
            jpeg_name = f"{i}.jpeg"
            txt_name = f"{i}.txt"

            # Image is the only strictly required asset
            if jpeg_name not in names:
                raise ValueError(
                    f"Page {i} missing image file '{jpeg_name}' in {path}"
                )

            with zf.open(jpeg_name) as jf:
                image_bytes = jf.read()

            text = ""
            if txt_name in names:
                with zf.open(txt_name) as tf:
                    # Procore writes UTF-8 with CRLF line endings
                    text = tf.read().decode("utf-8", errors="replace")

            # Pull dimensions from manifest if available
            width = height = 0
            if i - 1 < len(page_meta):
                dims = page_meta[i - 1].get("image", {}).get("dimensions", {})
                width = dims.get("width", 0)
                height = dims.get("height", 0)

            bundle.pages.append(
                ProcorePage(
                    page_number=i,
                    image_bytes=image_bytes,
                    text=text,
                    width=width,
                    height=height,
                )
            )

    return bundle


def save_page_jpeg(bundle: ProcoreBundle, page_number: int, dest: str | Path) -> Path:
    """Save one page's JPEG to disk. Useful for visual inspection or OCR."""
    dest = Path(dest)
    page = next((p for p in bundle.pages if p.page_number == page_number), None)
    if page is None:
        raise ValueError(f"No page {page_number} in bundle (has {bundle.num_pages})")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(page.image_bytes)
    return dest
