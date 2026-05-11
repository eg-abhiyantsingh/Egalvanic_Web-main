"""Tests for src.pdf.procore_zip — the Procore Markup ZIP handler.

We build small synthetic ZIPs in tmp_path so the tests don't depend on any
real Procore files.
"""
from __future__ import annotations

import json
import zipfile
from pathlib import Path

import pytest

from src.pdf.procore_zip import (
    PDF_MAGIC,
    ZIP_MAGIC,
    extract_procore_zip,
    is_procore_zip,
    is_real_pdf,
    save_page_jpeg,
)


def make_procore_zip(tmp_path: Path, name: str, pages: list[tuple[str, bytes]]) -> Path:
    """Build a Procore-style ZIP at tmp_path/name with the given pages.

    Each page is a (text, image_bytes) tuple.
    """
    path = tmp_path / name
    manifest = {
        "num_pages": len(pages),
        "pages": [
            {
                "page_number": i + 1,
                "image": {"path": f"{i+1}.jpeg",
                          "dimensions": {"width": 1316, "height": 924},
                          "media_type": "image/jpeg"},
                "text": {"path": f"{i+1}.txt"},
                "has_visual_content": True,
            }
            for i in range(len(pages))
        ],
    }
    with zipfile.ZipFile(path, "w") as zf:
        for i, (text, img) in enumerate(pages, start=1):
            zf.writestr(f"{i}.jpeg", img)
            zf.writestr(f"{i}.txt", text)
        zf.writestr("manifest.json", json.dumps(manifest))
    return path


def make_real_pdf_stub(tmp_path: Path, name: str = "real.pdf") -> Path:
    """Tiny stub that starts with %PDF- magic so `is_real_pdf` succeeds."""
    path = tmp_path / name
    path.write_bytes(b"%PDF-1.4\n%real pdf content goes here\n")
    return path


# --- Magic-byte detection ---

class TestMagicByteDetection:
    def test_procore_zip_detected(self, tmp_path):
        p = make_procore_zip(tmp_path, "ks.pdf", [("hello", b"fake jpeg")])
        assert is_procore_zip(p) is True

    def test_real_pdf_not_misidentified(self, tmp_path):
        p = make_real_pdf_stub(tmp_path)
        assert is_procore_zip(p) is False
        assert is_real_pdf(p) is True

    def test_procore_is_not_real_pdf(self, tmp_path):
        p = make_procore_zip(tmp_path, "ks.pdf", [("x", b"y")])
        assert is_real_pdf(p) is False

    def test_nonexistent_raises(self, tmp_path):
        with pytest.raises(FileNotFoundError):
            is_procore_zip(tmp_path / "nope.pdf")

    def test_magic_constants_correct(self):
        assert ZIP_MAGIC == b"PK\x03\x04"
        assert PDF_MAGIC == b"%PDF-"


# --- Extraction ---

class TestExtraction:
    def test_extracts_single_page(self, tmp_path):
        text = "PANEL NAME: B01-1.0-NPNLB-01, 300A, 480/277V"
        p = make_procore_zip(tmp_path, "B01.pdf", [(text, b"fake-jpeg-bytes")])
        bundle = extract_procore_zip(p)
        assert bundle.num_pages == 1
        assert bundle.pages[0].text.startswith("PANEL NAME: B01-1.0-NPNLB-01")
        assert bundle.pages[0].image_bytes == b"fake-jpeg-bytes"
        assert bundle.pages[0].width == 1316

    def test_extracts_multi_page(self, tmp_path):
        p = make_procore_zip(tmp_path, "multi.pdf", [
            ("page one text", b"img1"),
            ("page two text", b"img2"),
            ("page three text", b"img3"),
        ])
        bundle = extract_procore_zip(p)
        assert bundle.num_pages == 3
        assert bundle.pages[2].text == "page three text"

    def test_all_text_concatenates(self, tmp_path):
        p = make_procore_zip(tmp_path, "m.pdf", [
            ("alpha", b"a"), ("beta", b"b"),
        ])
        bundle = extract_procore_zip(p)
        assert "alpha" in bundle.all_text
        assert "beta" in bundle.all_text

    def test_real_pdf_rejected(self, tmp_path):
        p = make_real_pdf_stub(tmp_path)
        with pytest.raises(ValueError, match="Not a Procore Markup ZIP"):
            extract_procore_zip(p)


# --- Image saving ---

class TestSavePage:
    def test_save_writes_bytes(self, tmp_path):
        p = make_procore_zip(tmp_path, "x.pdf", [("text", b"JPEG-BYTES-HERE")])
        bundle = extract_procore_zip(p)
        out = save_page_jpeg(bundle, 1, tmp_path / "out" / "page1.jpeg")
        assert out.exists()
        assert out.read_bytes() == b"JPEG-BYTES-HERE"

    def test_save_invalid_page_raises(self, tmp_path):
        p = make_procore_zip(tmp_path, "x.pdf", [("text", b"data")])
        bundle = extract_procore_zip(p)
        with pytest.raises(ValueError, match="No page 99"):
            save_page_jpeg(bundle, 99, tmp_path / "won't-exist.jpeg")
