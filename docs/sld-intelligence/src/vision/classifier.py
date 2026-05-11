"""
Classify text label clusters into electrical entity types.

Uses pattern matching to detect:
  - Breakers (e.g. "20A/3P", "#26/#28/#30 7A/3P")
  - Main breakers (amperage + "MAIN CB")
  - Panels, switchboards, MCCs, transformers (via equipment ID patterns)
  - Loads (M1B01VAV105 tags + plain-English equipment names)

Stateless and deterministic — same input always produces same output.
"""
from __future__ import annotations

import re

from src.models.schema import Entity, EntityType, TextCluster

# ============================================================
# REGEX PATTERNS
# ============================================================

# Equipment identifier (NPNLB-01, GATS-01, NTLV-01, etc.)
EQUIPMENT_ID_RE = re.compile(
    r'(?:[NG]?PNLB|NSWGR|NSWBD|GSWBD|NTLV|GTLV|NTMV|GMCC|NMCC|MCC|'
    r'NVLT|GATS|NLIS|NPMS|SWBD|SWGR|GSWGR|EX-NTMV|EX-NLIS|EX-GTLV|'
    r'EX-GDIS|EXISTING-DP|EXISTING-DP\#?\d+)[-_]?\d+',
    re.IGNORECASE,
)

# Branch tag (M1B01VAV105, M1B30CH101)
LOAD_TAG_RE = re.compile(r'\bM\d+B\d+\w+\b')

# Breaker amperage label — strict match (anchored, full line)
BREAKER_AMP_RE = re.compile(
    r'^\(?(\d+\.?\d*)\s*A(?:F|T)?\s*/\s*[13]\s*P\)?$|'
    r'^\(?(\d+\.?\d*)\s*A(?:F|T)?\)?$',
    re.IGNORECASE,
)

# Same pattern but unanchored — recovers breakers with circuit-number prefix
# like "#1/#3/#5 * 45A/3P" where the strict regex misses
BREAKER_AMP_RE_LOOSE = re.compile(
    r'\b(\d+\.?\d*)\s*A(?:F|T)?\s*/\s*[13]\s*P\b',
    re.IGNORECASE,
)

# Switchgear / MV breaker amperage format: "5000AF 5000AT" with no /P.
# Common on big switchgear (B30, B34, NSWGR drawings). AT = Amp Trip,
# AF = Amp Frame — both convey the breaker's rated current.
BREAKER_AMP_RE_AFAT = re.compile(
    r'\b(\d+\.?\d*)\s*A[FT]\b',
    re.IGNORECASE,
)

# Generic load names
LOAD_NAME_PATTERNS = [
    'AIR COMPRESSOR', 'ROOF TOP UNIT', 'CHILLER', 'BOILER', 'PUMP',
    'FAN', 'EXHAUST FAN', 'CONDENSING UNIT', 'AHU', 'WATER HEATER',
    'HEATER', 'COMPRESSOR', 'BLOWER', 'DRYER',
    'WELL WATER PUMP', 'FIRE PUMP', 'JOCKEY PUMP', 'SUMP PUMP',
    'FUEL PUMP', 'OIL PUMP', 'WATER PUMP', 'MOTOR',
    'PILOT PANEL',  # B05 pilot panel
    'FAB SHOP',     # B07
]


def classify(cluster: TextCluster) -> EntityType:
    """
    Determine what kind of electrical entity a text cluster represents.

    Returns one of the EntityType literals.
    """
    text = cluster.text
    txt_upper = text.upper().strip()
    lines = cluster.lines
    total_chars = sum(len(line) for line in lines)

    # Reject huge paragraphs (notes, keynotes)
    if total_chars > 200 or cluster.n_lines > 12:
        return 'PARAGRAPH'

    # Equipment identifier check goes FIRST. A cluster like
    # "B03-1.0-GATS-01 200A" contains both an equipment ID and an amperage
    # pattern, but the equipment ID is the more specific signal.
    if EQUIPMENT_ID_RE.search(text):
        return _classify_equipment(txt_upper)

    # Breaker detection — try strict, then loose, then AF/AT.
    # Strict/loose run per line (anchored on label like "20A/3P");
    # AF/AT runs against the whole cluster text (switchgear breakers
    # often split labels across many spans: "1200AF" "800AT" "3" "LSIG")
    if cluster.n_lines <= 2:
        for line in lines:
            stripped = line.strip()
            if (BREAKER_AMP_RE.match(stripped)
                    or BREAKER_AMP_RE_LOOSE.search(stripped)):
                others = [ln for ln in lines if ln != line]
                if (any('MAIN' in o.upper() for o in others)
                        or 'MAIN' in stripped.upper()):
                    return 'MAIN_BREAKER'
                return 'BREAKER'

    # AF/AT switchgear breaker — works for any line count up to ~8.
    # Constrained to short clusters so we don't classify huge paragraphs.
    if cluster.n_lines <= 8 and total_chars < 80:
        if BREAKER_AMP_RE_AFAT.search(cluster.text):
            if any('MAIN' in ln.upper() for ln in lines):
                return 'MAIN_BREAKER'
            return 'BREAKER'

    # Multi-line breaker with explicit MAIN (legacy path)
    has_amp = any(
        BREAKER_AMP_RE.match(ln.strip())
        or BREAKER_AMP_RE_LOOSE.search(ln)
        for ln in lines
    )
    has_main = any('MAIN' in ln.upper() for ln in lines)
    if has_amp and has_main and cluster.n_lines <= 5:
        return 'MAIN_BREAKER'

    # Branch load tag
    if LOAD_TAG_RE.search(text):
        return 'LOAD'

    # Plain-English load names
    if total_chars < 100:
        for pattern in LOAD_NAME_PATTERNS:
            if pattern in txt_upper:
                return 'LOAD'

    # EXISTING markers
    if 'EXISTING' in txt_upper and total_chars < 50:
        if 'FEEDER' in txt_upper:
            return 'EXISTING_FEEDER'
        if 'PANEL' in txt_upper:
            return 'EXISTING_PANEL'
        return 'EXISTING_TAG'

    # SPARE / SPACE
    if re.fullmatch(r'SPARES?|SPACES?', txt_upper):
        return 'SPARE'

    # SPD / TVSS
    if 'SPD' in txt_upper or 'TVSS' in txt_upper:
        return 'SURGE'

    return 'OTHER'


def _classify_equipment(txt_upper: str) -> EntityType:
    """Sub-classify equipment based on its identifier prefix."""
    if 'KVA' in txt_upper or 'NTLV' in txt_upper or 'GTLV' in txt_upper \
            or 'NTMV' in txt_upper or 'NVLT' in txt_upper or 'XFMR' in txt_upper:
        return 'TRANSFORMER'
    if 'GEN' in txt_upper and ('GEN-01' in txt_upper or 'GGEN' in txt_upper):
        return 'GENERATOR'
    if 'GATS' in txt_upper or re.search(r'\bATS[-_]\d', txt_upper):
        return 'ATS'
    if 'MCC' in txt_upper:
        return 'MCC'
    if 'NSWGR' in txt_upper or 'NSWBD' in txt_upper or 'GSWBD' in txt_upper \
            or 'SWBD' in txt_upper or 'SWGR' in txt_upper:
        return 'SWITCHBOARD'
    if 'NLIS' in txt_upper:
        return 'DISCONNECT'
    if 'NPMS' in txt_upper:
        return 'PRIMARY_SWITCH'
    return 'PANEL'


# ============================================================
# ATTRIBUTE EXTRACTION
# ============================================================

def extract_amperage(cluster: TextCluster) -> str | None:
    """Pull the amperage value from a breaker cluster."""
    for line in cluster.lines:
        m = BREAKER_AMP_RE.match(line.strip())
        if m:
            return f"{m.group(1) or m.group(2)}A"
    # Loose match: handles "#1/#3/#5 * 45A/3P"
    for line in cluster.lines:
        m = BREAKER_AMP_RE_LOOSE.search(line)
        if m:
            return f"{m.group(1)}A"
    # AF/AT switchgear breakers: prefer AT (Amp Trip) over AF (Amp Frame)
    at_matches: list[str] = []
    af_matches: list[str] = []
    for line in cluster.lines:
        for m in BREAKER_AMP_RE_AFAT.finditer(line):
            suffix = m.group(0).upper()
            if suffix.endswith('AT'):
                at_matches.append(m.group(1))
            else:
                af_matches.append(m.group(1))
    if at_matches:
        return f"{at_matches[0]}A"
    if af_matches:
        return f"{af_matches[0]}A"
    m = re.search(r'(\d+\.?\d*)\s*A\s*/?\s*[13]?\s*P?', cluster.text)
    if m:
        return f"{m.group(1)}A"
    return None


def extract_voltage(cluster: TextCluster) -> str | None:
    """Pull voltage spec (e.g. '480/277V', '12.47KV') from a cluster."""
    txt = cluster.text.upper()
    m = re.search(r'(\d+\.?\d*\s*K?V|\d+/?\d*V)', txt)
    if m:
        return m.group(1).replace(' ', '')
    return None


def extract_kva(cluster: TextCluster) -> float | None:
    """Pull kVA rating from a transformer cluster."""
    m = re.search(r'(\d+\.?\d*)\s*K[VW]A', cluster.text.upper())
    if m:
        return float(m.group(1))
    return None


def cluster_to_entity(cluster: TextCluster) -> Entity:
    """Classify and enrich a cluster into a typed Entity."""
    etype = classify(cluster)
    return Entity(
        cluster=cluster,
        type=etype,
        amperage=extract_amperage(cluster) if etype in ('BREAKER', 'MAIN_BREAKER') else None,
        voltage=extract_voltage(cluster),
        kva=extract_kva(cluster) if etype == 'TRANSFORMER' else None,
        raw_text=cluster.text,
    )
