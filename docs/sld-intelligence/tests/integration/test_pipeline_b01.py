"""End-to-end pipeline test using the B01 reference PDF."""
from pathlib import Path

import pytest

from src.pipeline import run_pipeline

# This fixture PDF is the B01 K STAR SLD. Skip if not available.
FIXTURE_PDF = Path(__file__).parent.parent / 'fixtures' / 'B01_reference.pdf'


@pytest.mark.skipif(not FIXTURE_PDF.exists(), reason='B01 reference PDF not in fixtures')
def test_b01_extracts_known_entities(tmp_path):
    """Pipeline should detect B01's 17 breakers and the AIR COMPRESSOR / 7A swap."""
    out = tmp_path / 'B01_out.xlsx'
    result = run_pipeline(FIXTURE_PDF, building='B01', out_xlsx=out)

    assert out.exists(), "XLSX was not written"

    # The fixture B01 must contain at least these
    asset_names = result.workbook.asset_names()
    assert 'AIR-COMPRESSOR' in asset_names, \
        "AIR COMPRESSOR not detected as an asset"
    assert 'M1B01VAV110' in asset_names, "VAV110 motor missing"

    # Critical: the 7A breaker must connect to AIR-COMPRESSOR, NOT to VAV110
    conns = result.workbook.connections
    air_breakers = [c for c in conns if c.target_asset_name == 'AIR-COMPRESSOR']
    assert len(air_breakers) == 1
    assert '7A' in air_breakers[0].source_asset_name, \
        f"AIR-COMPRESSOR is fed by {air_breakers[0].source_asset_name}, expected ...-7A"

    # And VAV110 must be on 20A
    vav_breakers = [c for c in conns if c.target_asset_name == 'M1B01VAV110']
    assert len(vav_breakers) == 1
    assert '20A' in vav_breakers[0].source_asset_name


@pytest.mark.skipif(not FIXTURE_PDF.exists(), reason='B01 reference PDF not in fixtures')
def test_pipeline_produces_no_blank_asset_names(tmp_path):
    """Every asset row must have a non-empty name."""
    result = run_pipeline(FIXTURE_PDF, building='B01')
    for asset in result.workbook.assets:
        assert asset.asset_name and asset.asset_name.strip(), \
            f"Blank asset_name: {asset}"


@pytest.mark.skipif(not FIXTURE_PDF.exists(), reason='B01 reference PDF not in fixtures')
def test_pipeline_only_emits_blank_ids(tmp_path):
    """asset_id and connection_id must be None (eGalvanic generates them on upload)."""
    result = run_pipeline(FIXTURE_PDF, building='B01')
    for asset in result.workbook.assets:
        assert asset.asset_id is None
    for conn in result.workbook.connections:
        assert conn.connection_id is None
