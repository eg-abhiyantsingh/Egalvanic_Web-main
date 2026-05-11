"""
Asset and connection naming utilities — K STAR / eGalvanic conventions.

Centralizes the rules from the K STAR project knowledge:
  - Main breakers: CB-MAIN-{amps}
  - Feeder breakers: CB-{equipment-id}-{amps}, building prefix dropped
  - Drop M1B{nn} prefix from load names for cleaner breaker names
  - VAVs, RTUs, motors → Motor class
"""
from __future__ import annotations

import re

from src.models.schema import EGalvanicAssetClass, Entity

# Common-language load name canonicalizations
LOAD_NAME_CANONICAL = {
    'AIR COMPRESSOR': 'AIR-COMPRESSOR',
    'ROOF TOP UNIT': 'ROOF-TOP-UNIT',
    'WATER HEATER': 'WATER-HEATER',
    'WELL WATER PUMP': 'WELL-WATER-PUMP',
    'FIRE PUMP': 'FIRE-PUMP',
    'JOCKEY PUMP': 'JOCKEY-PUMP',
    'EXHAUST FAN': 'EXHAUST-FAN',
    'CHILLER': 'CHILLER',
    'PILOT PANEL': 'PILOT-PANEL',
}


_BUILDING_PREFIX_RE = re.compile(r'^(B\d+|DC\d+)[-_]', re.IGNORECASE)


def _normalize_building(code: str) -> str:
    """K STAR building codes are zero-padded to 2 digits (B01, B04). Some
    load-tag formats omit the padding (M1B4RTU302 → B4). Normalize.

    DC codes are NOT padded (DC13 stays DC13).
    """
    code = code.upper()
    if code.startswith('B') and code[1:].isdigit() and len(code) == 2:
        return f"B0{code[1]}"
    return code


def extract_building_from_id(name: str) -> str | None:
    """If an asset name starts with a building prefix (B01-, DC13-, M1B30-),
    return the building code. Else return None.

    Used to detect cross-PDF references — when processing B33's drawing,
    an entity named B01-1.0-NPNLB-01 belongs to B01, not B33.
    """
    if not isinstance(name, str):
        return None
    name_u = name.upper().strip()
    m = _BUILDING_PREFIX_RE.match(name_u)
    if m:
        return _normalize_building(m.group(1))
    # M1B01..., M1B30..., M1B4... (some load tags drop the zero)
    m2 = re.match(r'^M\d+(B\d+)', name_u)
    if m2:
        return _normalize_building(m2.group(1))
    return None


def extract_equipment_id(text: str) -> str:
    """Pull a clean, canonical equipment ID from a label."""
    text_upper = text.upper().strip()

    # Branch tag (M1B01VAV105, M1B30CH101)
    m = re.search(r'\bM\d+B\d+\w+\b', text)
    if m:
        return m.group()

    # Full building-qualified equipment ID with version number
    # e.g. B01-1.0-NPNLB-01, B33-1.0-NSWGR-01
    m = re.search(
        r'\bB\d+[-_]\d+\.?\d*[-_][NGP]?[A-Z]+[-_]\d+\b',
        text,
    )
    if m:
        return m.group()

    # Building-qualified without version (EX equipment, MCC, etc.)
    # e.g. B30-EX-NTMV-01, B09-MCC-07, B09-PANEL-PP1,
    # B30-NSWGR01-CPNL, DC13-GSWBD-01
    m = re.search(
        r'\b(?:B\d+|DC\d+)(?:[-_][A-Z0-9]+){2,}\b',
        text.upper(),
    )
    if m:
        return m.group()

    # Building-qualified, two-component (just one suffix), uppercase
    # e.g. B30-EXISTING-GENERATOR, B07-FAB-SHOP, B15-PUMP-HOUSE-PANEL-A
    m = re.search(
        r'\b(?:B\d+|DC\d+)[-_][A-Z][A-Z0-9]+(?:[-_][A-Z0-9]+)*\b',
        text.upper(),
    )
    if m and len(m.group()) > 5:
        return m.group()

    # Common-language load names
    for k, v in LOAD_NAME_CANONICAL.items():
        if k in text_upper:
            return v

    # Generic: first non-stopword identifier-like token
    STOPWORDS = {'OUTDOOR', 'EXISTING', 'PANEL', 'NEW', 'WORK', 'ROOM',
                 'NAME', 'NOTE', 'ELECTRICAL', 'MECHANICAL', 'NORMAL',
                 'EMERGENCY', 'LOCATED', 'FROM', 'TO'}
    for m in re.finditer(r'\b[A-Z][A-Z0-9_]{2,}\b', text_upper):
        if m.group() not in STOPWORDS:
            return m.group()

    return text[:30].strip().upper().replace(' ', '-')


_CROSS_BUILDING_TYPES = {
    'PANEL', 'SWITCHBOARD', 'MCC', 'LOADCENTER',
    'TRANSFORMER', 'ATS', 'DISCONNECT', 'PRIMARY_SWITCH',
}


def make_breaker_name(
    terminal_id: str,
    terminal_type: str,
    amperage: str | None,
    is_main: bool = False,
    building: str | None = None,
) -> str:
    """
    Generate the canonical breaker name (K STAR / eGalvanic convention).

      - Main breakers:        CB-MAIN-{amps}                      e.g. CB-MAIN-300A
      - Load feeders:         CB-{LOAD}-{amps}                    e.g. CB-VAV105-20A
      - Panel / xfmr / ATS:   CB-{ID}-{building}-{amps}           e.g. CB-NPNLB03-B01-100A
      - Building prefix is stripped from the inner ID
      - For panel / xfmr targets, dashes inside the ID are also stripped
    """
    amps = amperage or 'UNK'

    if is_main:
        return f"CB-MAIN-{amps}"

    short_id = _strip_building_prefix(terminal_id)
    if terminal_id == 'AIR-COMPRESSOR':
        short_id = 'AIRCOMP'
    elif terminal_id.startswith('ROOF-TOP-UNIT'):
        short_id = 'RTU'
    elif terminal_type == 'TRANSFORMER':
        # e.g. B01-1.0-NTLV-01 → NTLV01
        parts = terminal_id.split('-')
        if len(parts) >= 2:
            short_id = parts[-2] + parts[-1]

    if terminal_type in _CROSS_BUILDING_TYPES:
        # Strip remaining dashes inside the ID (NPNLB-03 → NPNLB03)
        short_id = short_id.replace('-', '')
        if building:
            return f"CB-{short_id}-{building}-{amps}"

    return f"CB-{short_id}-{amps}"


def _strip_building_prefix(name: str) -> str:
    """Drop M1B01 and B01-1.0- prefixes for compact naming."""
    name = re.sub(r'^M\d+B\d+', '', name)
    name = re.sub(r'^B\d+[-_]\d+\.?\d*[-_]', '', name)
    return name.replace('--', '-').strip('-')


# Entity type → eGalvanic class
_CLASS_MAP: dict[str, EGalvanicAssetClass] = {
    'BREAKER': 'Circuit Breaker',
    'MAIN_BREAKER': 'Circuit Breaker',
    'FUSE': 'Disconnect Switch',  # Per K STAR Rule 9
    'PANEL': 'Panelboard',
    'SWITCHBOARD': 'Switchboard',
    'MCC': 'MCC',
    'LOADCENTER': 'Loadcenter',
    'TRANSFORMER': 'Transformer',
    'ATS': 'ATS',
    'GENERATOR': 'Generator',
    'UPS': 'UPS',
    'VFD': 'VFD',
    'DISCONNECT': 'Disconnect Switch',
    'PRIMARY_SWITCH': 'Disconnect Switch',
    'BUSWAY': 'Busway',
    'METER': 'Meter',
    'RELAY': 'Relay',
    'CAPACITOR': 'Capacitor',
    'JUNCTION_BOX': 'Junction Box',
    'SURGE': 'Other',
    'LOAD': 'Load',
    'MOTOR': 'Motor',
}


def to_egalvanic_class(entity: Entity) -> EGalvanicAssetClass:
    """Map an Entity to its eGalvanic asset_class."""
    if entity.type == 'LOAD':
        return _load_to_class(entity.text)
    return _CLASS_MAP.get(entity.type, 'Other')


def _load_to_class(text: str) -> EGalvanicAssetClass:
    """Refine LOAD classification: motor-driven loads → Motor, others → Load."""
    txt = text.upper()
    motor_keywords = ['COMPRESSOR', 'PUMP', 'FAN', 'VAV', 'RTU', 'AHU',
                      'MOTOR', 'CHILLER', 'BLOWER', 'DRYER']
    if any(kw in txt for kw in motor_keywords):
        return 'Motor'
    return 'Load'
