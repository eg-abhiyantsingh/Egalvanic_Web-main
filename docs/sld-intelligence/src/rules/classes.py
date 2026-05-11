"""eGalvanic class taxonomy.

Encodes the class membership rules from Mukul's spec:

  OCP (overcurrent protection) classes — these MUST have a parent_asset_name
  pointing to a Box container:
      3-Pole Breaker, Circuit Breaker, Disconnect Switch, Fuse,
      Fused Disconnect Switch, Integrated Transformer, MCC Bucket,
      Other (OCP), PLCs, Relay

  Box container classes — these CAN be a parent_asset_name target:
      DC Bus, Disconnect Switch, Loadcenter, MCC, Motor Starter, Other,
      Panelboard, PDU, PLCs, Switchboard, VFD

  Everything else — connected via Connections only, no parent relationship:
      ATS, Battery, Busway, Capacitor, Generator, Inverter, Junction Box,
      Load, Meter, Motor, Oil Transformer, Reactor, Rectifier,
      Temperature Sensor, Transformer, Transformer (3-Winding), UPS, Utility

Note that Disconnect Switch and PLCs appear in BOTH OCP and Box — they can act
as either depending on context.
"""
from __future__ import annotations

OCP_CLASSES: frozenset[str] = frozenset({
    "3-Pole Breaker",
    "Circuit Breaker",
    "Disconnect Switch",
    "Fuse",
    "Fused Disconnect Switch",
    "Integrated Transformer",
    "MCC Bucket",
    "Other (OCP)",
    "PLCs",
    "Relay",
})


BOX_CONTAINER_CLASSES: frozenset[str] = frozenset({
    "DC Bus",
    "Disconnect Switch",
    "Loadcenter",
    "MCC",
    "Motor Starter",
    "Other",
    "Panelboard",
    "PDU",
    "PLCs",
    "Switchboard",
    "VFD",
})


# Classes that are neither OCP nor Box container — connected via Connections only
LEAF_AND_LINK_CLASSES: frozenset[str] = frozenset({
    "ATS",
    "Battery",
    "Busway",
    "Capacitor",
    "Generator",
    "Inverter",
    "Junction Box",
    "Load",
    "Meter",
    "Motor",
    "Oil Transformer",
    "Reactor",
    "Rectifier",
    "Temperature Sensor",
    "Transformer",
    "Transformer (3-Winding)",
    "UPS",
    "Utility",
})


# All valid asset_class values — sum of the three sets above
ALL_VALID_CLASSES: frozenset[str] = (
    OCP_CLASSES | BOX_CONTAINER_CLASSES | LEAF_AND_LINK_CLASSES
)


# Container classes we expect to have children OR connections (no empty containers)
NON_EMPTY_CONTAINER_CLASSES: frozenset[str] = frozenset({
    "Switchboard", "MCC", "Panelboard", "Loadcenter", "PDU", "VFD",
})


def is_ocp_class(cls: str) -> bool:
    """True if this class needs a parent_asset_name."""
    return cls in OCP_CLASSES


def is_box_container(cls: str) -> bool:
    """True if this class can be a parent_asset_name target."""
    return cls in BOX_CONTAINER_CLASSES


def is_valid_class(cls: str) -> bool:
    """True if this is a recognized eGalvanic class."""
    return cls in ALL_VALID_CLASSES


def validate_parent_child(child_class: str, parent_class: str) -> tuple[bool, str]:
    """Validate that a parent-child class pairing is allowed.

    Returns (is_valid, reason). For invalid pairings, `reason` explains why.
    """
    if not is_ocp_class(child_class):
        return False, (
            f"Child class '{child_class}' is not in OCP_CLASSES — "
            f"only OCP classes should have a parent_asset_name. "
            f"Did you mean to use a Connection instead?"
        )
    if not is_box_container(parent_class):
        return False, (
            f"Parent class '{parent_class}' is not in BOX_CONTAINER_CLASSES — "
            f"only Box containers can be parents."
        )
    return True, "ok"
