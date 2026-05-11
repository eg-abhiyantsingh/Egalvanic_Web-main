"""
Main pipeline: PDF → EGalvanicWorkbook.

Orchestrates all stages:
  1. Extract vector geometry from PDF
  2. Cluster text into compound labels
  3. Classify clusters into typed entities
  4. Detect buses (panel bus bars)
  5. Spatially match breakers to parent buses and downstream loads
  6. Build connection chains (insert transformers, disconnects)
  7. Generate eGalvanic asset + connection rows
"""
from __future__ import annotations

import logging
from collections import defaultdict
from pathlib import Path

from src.export.xlsx_export import write_workbook
from src.graph.spatial_match import (
    build_connection_chain,
    match_breaker_to_bus,
    match_breaker_to_downstream,
)
from src.models.schema import (
    BreakerLoadMatch,
    Bus,
    ConnectionChain,
    EGalvanicAsset,
    EGalvanicConnection,
    EGalvanicWorkbook,
    Entity,
    PageGeometry,
)
from src.pdf.vector_extract import extract_page
from src.utils.naming import (
    extract_building_from_id,
    extract_equipment_id,
    make_breaker_name,
    to_egalvanic_class,
)
from src.vision.bus_detect import find_horizontal_buses, merge_collinear_buses
from src.vision.classifier import cluster_to_entity
from src.vision.text_cluster import cluster_text_spans

logger = logging.getLogger(__name__)


class PipelineResult:
    """Container for all intermediate and final outputs of one PDF run."""

    def __init__(
        self,
        geometry: PageGeometry,
        entities: list[Entity],
        buses: list[Bus],
        matches: list[BreakerLoadMatch],
        chains: list[ConnectionChain],
        workbook: EGalvanicWorkbook,
    ):
        self.geometry = geometry
        self.entities = entities
        self.buses = buses
        self.matches = matches
        self.chains = chains
        self.workbook = workbook

    @property
    def entities_by_type(self) -> dict[str, list[Entity]]:
        out: dict[str, list[Entity]] = defaultdict(list)
        for e in self.entities:
            out[e.type].append(e)
        return dict(out)


def run_pipeline(
    pdf_path: str | Path,
    building: str,
    out_xlsx: str | Path | None = None,
    page_number: int = 0,
) -> PipelineResult:
    """
    Convert a single SLD PDF to an eGalvanic XLSX workbook.

    Args:
        pdf_path: Path to the source PDF (must be vector-based)
        building: Building code (e.g. 'B01') to populate the building column
        out_xlsx: Optional output path; if None, no file is written
        page_number: 0-indexed page to process

    Returns:
        PipelineResult with all intermediate artifacts
    """
    pdf_path = Path(pdf_path)
    logger.info(f"=== Pipeline start: {pdf_path.name} (building={building}) ===")

    # 1. Vector extraction
    geometry = extract_page(pdf_path, page_number=page_number)

    # 2. Text clustering
    clusters = cluster_text_spans(geometry.text_spans)

    # 3. Entity classification
    entities = [cluster_to_entity(c) for c in clusters]
    by_type: dict[str, list[Entity]] = defaultdict(list)
    for e in entities:
        by_type[e.type].append(e)
    logger.info(
        f"Entity types: {dict((k, len(v)) for k, v in sorted(by_type.items()))}"
    )

    # 4. Bus detection
    buses = find_horizontal_buses(geometry)
    buses = merge_collinear_buses(buses)

    # 5. Spatial matching: breaker → downstream entity
    downstream_candidates = (
        by_type['LOAD'] + by_type['TRANSFORMER'] + by_type['PANEL']
        + by_type['MCC'] + by_type['ATS'] + by_type['SWITCHBOARD']
        + by_type['DISCONNECT'] + by_type['GENERATOR'] + by_type['MOTOR']
    )
    matches: list[BreakerLoadMatch] = []
    # Process BOTH feeder breakers and main breakers. Mains are still
    # named CB-MAIN-* in the workbook stage; they just need to exist in
    # the chain list so they get emitted.
    for br in by_type['BREAKER'] + by_type['MAIN_BREAKER']:
        m = match_breaker_to_downstream(br, downstream_candidates)
        matches.append(m)

    # 6. Build connection chains (insert intermediates)
    chains: list[ConnectionChain] = []
    for m in matches:
        if m.load is None:
            chains.append(ConnectionChain(breaker=m.breaker, chain=[m.breaker], terminal=None))
            continue
        chains.append(build_connection_chain(m.breaker, m.load, downstream_candidates))

    # 7. Generate eGalvanic workbook
    workbook = _build_workbook(by_type, chains, buses, building)

    # 8. Write XLSX if requested
    if out_xlsx is not None:
        write_workbook(workbook, out_xlsx)

    logger.info(f"=== Pipeline complete: {workbook.stats()} ===")
    return PipelineResult(
        geometry=geometry,
        entities=entities,
        buses=buses,
        matches=matches,
        chains=chains,
        workbook=workbook,
    )


def _build_workbook(
    by_type: dict[str, list[Entity]],
    chains: list[ConnectionChain],
    buses: list[Bus],
    building: str,
) -> EGalvanicWorkbook:
    """Construct the final eGalvanic Assets+Connections workbook."""
    assets: dict[str, EGalvanicAsset] = {}
    connections: list[EGalvanicConnection] = []

    # Map every bus to its nearest panel/switchboard/MCC label.
    # Each bus is one panel's bar; breakers on that bus belong to that panel.
    panels = (by_type.get('PANEL', []) + by_type.get('SWITCHBOARD', [])
              + by_type.get('MCC', []))
    bus_to_panel: dict[int, str] = {}
    for i, bus in enumerate(buses):
        best, best_d = None, float('inf')
        for p in panels:
            if not bus.contains_x(p.center.x, tol=50):
                continue
            d = abs(p.center.y - bus.y)
            if d < best_d:
                best, best_d = p, d
        if best is not None:
            bus_to_panel[i] = extract_equipment_id(best.text)

    # Fallback: the panel sitting at the top of the drawing (longest bus)
    fallback_panel: str | None = None
    if panels and buses:
        top = buses[0]
        nearest = min(panels, key=lambda p: abs(p.center.y - top.y))
        fallback_panel = extract_equipment_id(nearest.text)

    def parent_panel_for(br: Entity) -> str | None:
        bus = match_breaker_to_bus(br, buses)
        if bus is None:
            return fallback_panel
        try:
            return bus_to_panel.get(buses.index(bus), fallback_panel)
        except ValueError:
            return fallback_panel

    # ===== SWITCHGEAR MODE =====
    # When the dominant equipment is a switchgear (NSWGR/SWGR) or MCC with
    # >= 4 breakers + >= 1 bus, we name breakers by their physical position
    # on the bus: CB-A1, CB-A2, CB-B3, ... (matches K STAR convention).
    switchgear_names = (by_type.get('SWITCHBOARD', [])
                        + by_type.get('MCC', []))
    is_switchgear = (
        len(switchgear_names) >= 1
        and len(buses) >= 1
        and len(by_type.get('BREAKER', [])) >= 4
        and any(
            kw in (e.text.upper() if hasattr(e, 'text') else '')
            for e in switchgear_names
            for kw in ('NSWGR', 'SWGR', 'NMCC', 'GMCC', 'MCC')
        )
    )

    # Position map: breaker entity → "A1" / "B3" / etc.
    breaker_positions: dict[int, str] = {}
    # Main-breaker bus-letter map: breaker entity → "A" / "B"
    main_bus_letters: dict[int, str] = {}
    if is_switchgear:
        # In switchgear schematics, the bus is often drawn as tick marks or
        # thin rectangles that don't pass the line-geometry detector. So
        # instead we INFER buses from the breaker layout itself:
        #
        #   1. Cluster breakers by y-position (within 30pt tolerance)
        #   2. For each y-cluster with >= 4 breakers, split by x-gap
        #      (gap > 200pt → two separate buses)
        #   3. Each segment becomes a logical bus A, B, C ...
        feeders = by_type.get('BREAKER', [])
        if feeders:
            # Group by quantized y (round to nearest 30pt)
            from collections import defaultdict as _dd
            by_y: dict[int, list[Entity]] = _dd(list)
            for br in feeders:
                by_y[round(br.center.y / 30) * 30].append(br)

            # Pick the y-cluster(s) that look like aligned breaker rows.
            # Sort y-clusters by size (descending) — the densest rows are buses.
            candidate_rows = [
                (y, brks) for y, brks in by_y.items() if len(brks) >= 4
            ]
            candidate_rows.sort(key=lambda t: -len(t[1]))

            logical_buses: list[list[Entity]] = []
            for _y, brks in candidate_rows[:3]:  # at most 3 buses considered
                # Sort by x and split on gaps > 200pt
                brks_sorted = sorted(brks, key=lambda b: b.center.x)
                segments: list[list[Entity]] = [[brks_sorted[0]]]
                for b in brks_sorted[1:]:
                    if b.center.x - segments[-1][-1].center.x > 200:
                        segments.append([b])
                    else:
                        segments[-1].append(b)
                # Each segment >= 3 breakers is its own bus
                for seg in segments:
                    if len(seg) >= 3:
                        logical_buses.append(seg)

            # Keep top 4 logical buses (A, B, C, D) by size
            logical_buses.sort(key=lambda seg: -len(seg))
            logical_buses = logical_buses[:4]

            # Re-sort buses left-to-right within each y-row so A is leftmost
            logical_buses.sort(key=lambda seg: seg[0].center.x)

            for bus_letter_idx, seg in enumerate(logical_buses):
                bus_letter = chr(ord('A') + bus_letter_idx)
                # seg already sorted by x
                for pos, br in enumerate(seg, start=1):
                    breaker_positions[id(br)] = f"{bus_letter}{pos}"
                # Assign the closest main breaker (by x to the segment center)
                # to this bus letter
                if by_type.get('MAIN_BREAKER'):
                    seg_center_x = sum(b.center.x for b in seg) / len(seg)
                    nearest_main = min(
                        by_type['MAIN_BREAKER'],
                        key=lambda m: abs(m.center.x - seg_center_x),
                    )
                    main_bus_letters.setdefault(id(nearest_main), bus_letter)

            logger.info(
                f"Switchgear mode: inferred {len(logical_buses)} buses, "
                f"assigned {len(breaker_positions)} position names + "
                f"{len(main_bus_letters)} main-bus letters"
            )

    # Add ALL detected entities to the asset list (de-dupe by canonical name).
    # Cross-PDF references (e.g. B33's drawing showing B01-1.0-NPNLB-01)
    # get tagged with their actual owning building, not the current one.
    for ent in (e for elist in by_type.values() for e in elist):
        if ent.type in ('BREAKER', 'MAIN_BREAKER'):
            continue  # added below
        if ent.type in ('PARAGRAPH', 'OTHER', 'EXISTING_TAG', 'EXISTING_FEEDER',
                        'EXISTING_PANEL', 'SPARE', 'SURGE'):
            continue
        name = extract_equipment_id(ent.text)
        if name in assets:
            continue
        ref_building = extract_building_from_id(name)
        assets[name] = EGalvanicAsset(
            asset_name=name,
            asset_class=to_egalvanic_class(ent),
            building=ref_building or building,
            voltage=ent.voltage,
            kva_rating=ent.kva,
        )

    def name_breaker(br: Entity, terminal_id: str | None,
                     terminal_type: str | None, amps: str | None,
                     is_main: bool) -> str:
        """Pick the right naming style for this breaker."""
        # Switchgear MAIN: CB-MAIN-{busletter}  (e.g. CB-MAIN-A)
        if is_main and id(br) in main_bus_letters:
            return f"CB-MAIN-{main_bus_letters[id(br)]}"
        # Switchgear FEEDER: CB-{busletter}{position}  (e.g. CB-A1, CB-B3)
        # GT convention OMITS amperage from these — they're identified by
        # physical bus position, not load size.
        if not is_main and id(br) in breaker_positions:
            return f"CB-{breaker_positions[id(br)]}"
        if terminal_id is None:
            return f"CB-UNK-{amps or 'UNK'}"
        return make_breaker_name(
            terminal_id, terminal_type or 'PANEL', amps,
            is_main=is_main, building=building,
        )

    # Process chains → breaker assets + connections
    for chain in chains:
        br = chain.breaker
        amps = br.amperage
        is_main = br.type == 'MAIN_BREAKER'
        br_parent = parent_panel_for(br)

        if chain.terminal is None:
            # Orphan breaker — still emit it. In switchgear mode it gets a
            # position name; otherwise named after its parent panel.
            breaker_name = name_breaker(
                br,
                br_parent if br_parent else None,
                'PANEL' if br_parent else None,
                amps, is_main,
            )
            if breaker_name not in assets:
                assets[breaker_name] = EGalvanicAsset(
                    asset_name=breaker_name,
                    asset_class='Circuit Breaker',
                    building=building,
                    parent_asset_name=br_parent,
                    ampere_rating=amps,
                )
            continue

        terminal = chain.terminal
        terminal_id = extract_equipment_id(terminal.text)
        breaker_name = name_breaker(br, terminal_id, terminal.type, amps, is_main)

        if breaker_name not in assets:
            assets[breaker_name] = EGalvanicAsset(
                asset_name=breaker_name,
                asset_class='Circuit Breaker',
                building=building,
                parent_asset_name=br_parent,
                ampere_rating=amps,
            )

        # Connection: breaker → terminal
        connections.append(EGalvanicConnection(
            source_asset_name=breaker_name,
            target_asset_name=terminal_id,
        ))

    return EGalvanicWorkbook(
        assets=list(assets.values()),
        connections=connections,
    )
