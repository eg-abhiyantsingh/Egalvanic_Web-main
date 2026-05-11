"""Unit tests for graph.spatial_match."""
from src.graph.spatial_match import (
    build_connection_chain,
    match_breaker_to_bus,
    match_breaker_to_downstream,
)
from src.models.schema import BBox, Bus, Entity, TextCluster


def make_entity(text: str, etype: str, x: float, y: float,
                w: float = 30, h: float = 10, amps: str = None) -> Entity:
    """Helper: build an Entity at a given coordinate."""
    bbox = BBox(x1=x - w/2, y1=y - h/2, x2=x + w/2, y2=y + h/2)
    cluster = TextCluster(text=text, lines=[text], bbox=bbox, n_lines=1)
    return Entity(cluster=cluster, type=etype, amperage=amps, raw_text=text)


class TestBreakerToBusMatching:
    def test_breaker_finds_bus_above(self):
        bus = Bus(y=500, x1=100, x2=1700, length=1600, width=2.5)
        breaker = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        result = match_breaker_to_bus(breaker, [bus])
        assert result is bus

    def test_breaker_ignores_bus_below(self):
        bus = Bus(y=700, x1=100, x2=1700, length=1600, width=2.5)
        breaker = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        result = match_breaker_to_bus(breaker, [bus])
        assert result is None

    def test_breaker_skips_bus_out_of_x_range(self):
        bus = Bus(y=500, x1=100, x2=400, length=300, width=2.5)
        breaker = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        result = match_breaker_to_bus(breaker, [bus])
        assert result is None

    def test_breaker_picks_closer_bus_when_multiple(self):
        far_bus = Bus(y=400, x1=100, x2=1700, length=1600, width=2.5)
        near_bus = Bus(y=515, x1=100, x2=1700, length=1600, width=2.5)
        breaker = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        result = match_breaker_to_bus(breaker, [far_bus, near_bus])
        assert result is near_bus


class TestBreakerToDownstreamMatching:
    def test_finds_load_directly_below(self):
        br = make_entity('20A/3P', 'BREAKER', x=600, y=530, amps='20A')
        load = make_entity('M1B01VAV105', 'LOAD', x=605, y=700)
        match = match_breaker_to_downstream(br, [load])
        assert match.load is load
        assert match.method == 'spatial'
        assert match.confidence > 0.8

    def test_picks_aligned_over_close_but_offset(self):
        """Vertical alignment dominates over y-distance proximity."""
        br = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        aligned = make_entity('VAV-aligned', 'LOAD', x=602, y=800)
        misaligned = make_entity('VAV-near', 'LOAD', x=660, y=620)  # closer dy, larger dx
        match = match_breaker_to_downstream(br, [misaligned, aligned])
        assert match.load is aligned

    def test_rejects_load_above_breaker(self):
        br = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        load = make_entity('M1B01VAV105', 'LOAD', x=600, y=400)
        match = match_breaker_to_downstream(br, [load])
        assert match.load is None
        assert match.method == 'none'

    def test_rejects_load_too_far_horizontally(self):
        br = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        load = make_entity('M1B01VAV105', 'LOAD', x=800, y=700)  # dx = 200
        match = match_breaker_to_downstream(br, [load], max_dx=80)
        assert match.load is None

    def test_confidence_is_higher_for_better_alignment(self):
        br = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        perfect = make_entity('A', 'LOAD', x=600, y=700)
        ok = make_entity('B', 'LOAD', x=640, y=700)

        m1 = match_breaker_to_downstream(br, [perfect])
        m2 = match_breaker_to_downstream(br, [ok])
        assert m1.confidence > m2.confidence


class TestConnectionChain:
    def test_inserts_transformer_between_breaker_and_panel(self):
        br = make_entity('70A/3P', 'BREAKER', x=1683, y=530)
        # Transformer sits between (y between bbox.y2 of breaker and bbox.y1 of panel)
        xfmr = make_entity('NTLV-01 45KVA', 'TRANSFORMER', x=1685, y=600)
        panel = make_entity('NPNLB-02', 'PANEL', x=1685, y=840)
        chain = build_connection_chain(br, panel, [xfmr], tol_x=40)
        assert len(chain.chain) == 3
        assert chain.chain[0] is br
        assert chain.chain[1] is xfmr
        assert chain.chain[2] is panel

    def test_direct_connection_no_intermediate(self):
        br = make_entity('20A/3P', 'BREAKER', x=600, y=530)
        load = make_entity('VAV105', 'LOAD', x=600, y=700)
        chain = build_connection_chain(br, load, [])
        assert len(chain.chain) == 2
        assert chain.terminal is load
