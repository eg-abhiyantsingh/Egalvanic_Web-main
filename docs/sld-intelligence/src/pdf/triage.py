"""PDF triage — classify any incoming file so the pipeline can route it.

Three categories matter for K STAR / Cytiva work:

1. VECTOR_PDF      — Real PDF with extractable text/geometry (`pdfplumber` works)
2. PROCORE_ZIP     — ZIP-disguised .pdf from Procore Markup tool (use procore_zip.py)
3. RASTER_PDF      — Real PDF but image-only (needs OCR fallback, not yet wired)

Detection is by magic bytes first, then content inspection. This is deliberately
strict — we want zero ambiguity about which extractor to call.
"""
from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from pathlib import Path


class PdfKind(str, Enum):
    VECTOR_PDF = "vector_pdf"
    PROCORE_ZIP = "procore_zip"
    RASTER_PDF = "raster_pdf"
    EMPTY = "empty"
    UNKNOWN = "unknown"


@dataclass
class TriageResult:
    path: Path
    kind: PdfKind
    confidence: float       # 0.0-1.0
    page_count: int
    line_count: int = 0
    text_span_count: int = 0
    image_count: int = 0
    page_size: tuple[float, float] | None = None
    notes: str = ""

    def to_dict(self) -> dict:
        return {
            "path": str(self.path),
            "kind": self.kind.value,
            "confidence": self.confidence,
            "page_count": self.page_count,
            "stats": {
                "lines": self.line_count,
                "text_spans": self.text_span_count,
                "images": self.image_count,
            },
            "page_size": list(self.page_size) if self.page_size else None,
            "notes": self.notes,
        }


# Detection thresholds — tuned to K STAR PDFs
MIN_LINES_FOR_VECTOR = 100      # below this, content is probably raster-only
MIN_TEXT_SPANS_FOR_VECTOR = 50   # below this, text is sparse


def triage(path: str | Path) -> TriageResult:
    """Classify a PDF-extension file.

    Steps:
        1. Check magic bytes — distinguishes Procore ZIPs from real PDFs cheaply
        2. For real PDFs, inspect geometry counts to tell vector from raster
        3. Return a TriageResult that downstream code can pattern-match on
    """
    path = Path(path)
    if not path.exists():
        raise FileNotFoundError(f"No such file: {path}")

    # Import locally to keep this module's import cheap if not all kinds are used
    from src.pdf.procore_zip import is_procore_zip

    # --- Step 1: magic-byte check ---
    if is_procore_zip(path):
        # Procore ZIPs are always single-page in K STAR but we read manifest to be sure
        try:
            from src.pdf.procore_zip import extract_procore_zip
            bundle = extract_procore_zip(path)
            return TriageResult(
                path=path,
                kind=PdfKind.PROCORE_ZIP,
                confidence=1.0,
                page_count=bundle.num_pages,
                text_span_count=sum(len(p.text.split()) for p in bundle.pages),
                image_count=bundle.num_pages,
                page_size=(
                    (bundle.pages[0].width, bundle.pages[0].height)
                    if bundle.pages else None
                ),
                notes="Procore Markup ZIP — text already extracted, use procore_zip module",
            )
        except Exception as e:
            return TriageResult(
                path=path, kind=PdfKind.UNKNOWN, confidence=0.5,
                page_count=0, notes=f"ZIP signature but extraction failed: {e}",
            )

    # --- Step 2: it's a real PDF — inspect geometry ---
    try:
        import fitz  # PyMuPDF
    except ImportError:
        return TriageResult(
            path=path, kind=PdfKind.UNKNOWN, confidence=0.0,
            page_count=0, notes="PyMuPDF not installed; cannot triage real PDFs",
        )

    doc = fitz.open(path)
    if len(doc) == 0:
        return TriageResult(
            path=path, kind=PdfKind.EMPTY, confidence=1.0,
            page_count=0, notes="Empty document"
        )

    total_lines = 0
    total_text = 0
    total_imgs = 0
    page_size = None

    for page in doc:
        if page_size is None:
            page_size = (round(page.rect.width, 1), round(page.rect.height, 1))

        for d in page.get_drawings():
            for item in d.get("items", []):
                if item[0] == "l":
                    total_lines += 1

        text_dict = page.get_text("dict")
        for block in text_dict.get("blocks", []):
            for line in block.get("lines", []):
                total_text += len(line.get("spans", []))

        total_imgs += len(page.get_images())

    page_count = len(doc)
    doc.close()

    # --- Step 3: classify ---
    if total_lines >= MIN_LINES_FOR_VECTOR and total_text >= MIN_TEXT_SPANS_FOR_VECTOR:
        kind = PdfKind.VECTOR_PDF
        conf = 0.95
        notes = "Real PDF with vector geometry — use vector_extract"
    elif total_lines < 50 and total_text < 20 and total_imgs > 0:
        kind = PdfKind.RASTER_PDF
        conf = 0.9
        notes = "Real PDF but image-only — OCR fallback needed (not wired)"
    elif total_text == 0 and total_lines == 0 and total_imgs == 0:
        kind = PdfKind.EMPTY
        conf = 1.0
        notes = "Empty real PDF"
    else:
        kind = PdfKind.VECTOR_PDF  # mixed-content → still try vector extraction
        conf = 0.6
        notes = (
            f"Mixed: {total_lines} lines, {total_text} text spans, {total_imgs} images "
            f"— try vector_extract but expect lower accuracy"
        )

    return TriageResult(
        path=path,
        kind=kind,
        confidence=conf,
        page_count=page_count,
        line_count=total_lines,
        text_span_count=total_text,
        image_count=total_imgs,
        page_size=page_size,
        notes=notes,
    )


def triage_directory(directory: str | Path) -> list[TriageResult]:
    """Triage every .pdf in a directory. Useful before a batch run."""
    directory = Path(directory)
    return [triage(p) for p in sorted(directory.glob("*.pdf"))]
