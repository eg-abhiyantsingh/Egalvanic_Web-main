"""
Data models for SLD intelligence pipeline.

All extracted entities, connections, and intermediate data flow through these
Pydantic models. This ensures type safety, validation, and JSON serialization
across pipeline stages.
"""
from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

# ============================================================
# GEOMETRIC PRIMITIVES (Stage 1 output)
# ============================================================

class Point(BaseModel):
    """2D point in PDF coordinates (origin top-left, y increases downward)."""
    x: float
    y: float

    def __iter__(self):
        yield self.x
        yield self.y

    def tuple(self) -> tuple[float, float]:
        return (self.x, self.y)


class BBox(BaseModel):
    """Axis-aligned bounding box."""
    x1: float
    y1: float
    x2: float
    y2: float

    @property
    def width(self) -> float:
        return self.x2 - self.x1

    @property
    def height(self) -> float:
        return self.y2 - self.y1

    @property
    def center(self) -> Point:
        return Point(x=(self.x1 + self.x2) / 2, y=(self.y1 + self.y2) / 2)

    def contains_point(self, p: Point, expand: float = 0) -> bool:
        return (self.x1 - expand <= p.x <= self.x2 + expand
                and self.y1 - expand <= p.y <= self.y2 + expand)


class LineSegment(BaseModel):
    """A vector line segment extracted from the PDF stream."""
    p1: Point
    p2: Point
    width: float = 0
    color: int | None = None

    @property
    def length(self) -> float:
        from math import hypot
        return hypot(self.p2.x - self.p1.x, self.p2.y - self.p1.y)

    @property
    def is_horizontal(self) -> bool:
        return abs(self.p1.y - self.p2.y) < 1.0

    @property
    def is_vertical(self) -> bool:
        return abs(self.p1.x - self.p2.x) < 1.0


class TextSpan(BaseModel):
    """A single text span (one run from the PDF text dict)."""
    text: str
    bbox: BBox
    font_size: float = 0
    font: str = ''
    color: int = 0

    @property
    def center(self) -> Point:
        return self.bbox.center


class PageGeometry(BaseModel):
    """Complete vector + text geometry extracted from a single PDF page."""
    source_pdf: str
    page_number: int = 0
    page_width: float
    page_height: float
    lines: list[LineSegment] = Field(default_factory=list)
    text_spans: list[TextSpan] = Field(default_factory=list)
    rectangles: list[BBox] = Field(default_factory=list)

    def stats(self) -> dict[str, int]:
        return {
            'lines': len(self.lines),
            'text_spans': len(self.text_spans),
            'rectangles': len(self.rectangles),
        }


# ============================================================
# CLASSIFIED ENTITIES (Stage 2 output)
# ============================================================

EntityType = Literal[
    'BREAKER', 'MAIN_BREAKER', 'FUSE',
    'PANEL', 'SWITCHBOARD', 'MCC', 'LOADCENTER',
    'TRANSFORMER', 'ATS', 'GENERATOR', 'UPS', 'VFD',
    'DISCONNECT', 'PRIMARY_SWITCH',
    'LOAD', 'MOTOR',
    'BUS', 'BUSWAY',
    'METER', 'RELAY', 'CAPACITOR', 'JUNCTION_BOX',
    'SURGE',
    'EXISTING_TAG', 'EXISTING_FEEDER', 'EXISTING_PANEL',
    'SPARE', 'PARAGRAPH', 'OTHER',
]


class TextCluster(BaseModel):
    """A merged group of text spans that forms a single logical label."""
    text: str
    lines: list[str]
    bbox: BBox
    n_lines: int = 1
    font_sizes: list[float] = Field(default_factory=list)

    @property
    def center(self) -> Point:
        return self.bbox.center


class Entity(BaseModel):
    """A classified entity (breaker, load, panel, etc.) detected in the SLD."""
    cluster: TextCluster
    type: EntityType
    amperage: str | None = None
    voltage: str | None = None
    kva: float | None = None
    raw_text: str = ''

    @property
    def center(self) -> Point:
        return self.cluster.center

    @property
    def bbox(self) -> BBox:
        return self.cluster.bbox

    @property
    def text(self) -> str:
        return self.cluster.text


class Bus(BaseModel):
    """A long horizontal line interpreted as a panel bus bar."""
    y: float
    x1: float
    x2: float
    length: float
    width: float = 0

    def contains_x(self, x: float, tol: float = 100) -> bool:
        return self.x1 - tol <= x <= self.x2 + tol


# ============================================================
# CONNECTIVITY (Stage 3+ output)
# ============================================================

class BreakerLoadMatch(BaseModel):
    """A matched breaker → downstream entity pair."""
    breaker: Entity
    load: Entity | None = None
    dx: float | None = None
    dy: float | None = None
    method: Literal['spatial', 'wire_trace', 'manual', 'none'] = 'none'
    confidence: float = 0.0  # 0..1
    alternatives: list[dict[str, Any]] = Field(default_factory=list)


class ConnectionChain(BaseModel):
    """A breaker → [intermediates] → terminal entity chain."""
    breaker: Entity
    chain: list[Entity]
    terminal: Entity | None = None
    confidence: float = 0.0


# ============================================================
# EGALVANIC OUTPUT SCHEMA
# ============================================================

EGalvanicAssetClass = Literal[
    'DC Bus', 'Disconnect Switch', 'Loadcenter', 'MCC', 'Motor Starter',
    'Other', 'Panelboard', 'PDU', 'Switchboard', 'VFD',
    'ATS', 'Battery', 'Capacitor', 'Generator', 'Junction Box',
    'Load', 'Meter', 'Motor', 'Reactor', 'Rectifier',
    'Transformer', 'Transformer (3-Winding)',
    'Circuit Breaker', 'Fuse', 'MCC Bucket', 'Other (OCP)', 'Relay',
    'Utility', 'Busway', 'UPS',
]


class EGalvanicAsset(BaseModel):
    """A single row in the eGalvanic Assets sheet."""
    model_config = ConfigDict(populate_by_name=True)

    asset_id: str | None = None  # leave None for upload
    asset_name: str
    asset_class: EGalvanicAssetClass
    asset_subtype: str | None = None  # leave None
    building: str | None = None
    floor: float = 1
    room: str = 'default'
    com: int = 1
    criticality: str | None = None
    operating_conditions: str | None = None
    maintenance_state: str | None = None
    suggested_shortcut: str | None = None
    qr_code: str | None = None
    parent_asset_name: str | None = None
    delete: bool | None = None
    voltage: str | None = None
    ampere_rating: str | None = None
    kva_rating: float | None = None


class EGalvanicConnection(BaseModel):
    """A single row in the eGalvanic Connections sheet."""
    connection_id: str | None = None  # leave None for upload
    source_asset_name: str
    source_handle: str = 'bottom-source-0'
    target_asset_name: str
    target_handle: str = 'top-target-0'
    connection_class: str = 'feeder'
    delete: bool | None = None


class EGalvanicWorkbook(BaseModel):
    """The full workbook to be exported to .xlsx."""
    assets: list[EGalvanicAsset] = Field(default_factory=list)
    connections: list[EGalvanicConnection] = Field(default_factory=list)

    def asset_names(self) -> set[str]:
        return {a.asset_name for a in self.assets}

    def stats(self) -> dict[str, int]:
        return {'assets': len(self.assets), 'connections': len(self.connections)}
