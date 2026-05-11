"""K STAR project rules — encoded conventions from project memory.

This package centralizes everything the manual workflow has converged on, so
the codebase can apply the same rules automatically instead of relying on
chat conversations.

Submodules:
    classes  — eGalvanic class taxonomy (OCP / Box / Leaf)
    naming   — asset and breaker naming conventions
    chains   — ATS dual-input, upstream placeholder, B30+ chain pattern
    fuses    — Fuse handling and skip list
"""
from src.rules import chains, classes, fuses, naming
from src.rules.chains import (
    B30_PLUS_CHAIN,
    EMERGENCY_HANDLE,
    NORMAL_HANDLE,
    ats_input_handle,
    building_uses_b30_pattern,
    upstream_placeholder_for_building,
    validate_ats_inputs,
)

# Re-export commonly used names for convenience
from src.rules.classes import (
    ALL_VALID_CLASSES,
    BOX_CONTAINER_CLASSES,
    LEAF_AND_LINK_CLASSES,
    OCP_CLASSES,
    is_box_container,
    is_ocp_class,
    is_valid_class,
    validate_parent_child,
)
from src.rules.fuses import (
    FuseClassification,
    classify_fuse_or_switch,
    filter_skipped,
    fuse_rename,
    should_skip,
)
from src.rules.naming import (
    PREFERRED_MAIN_BREAKER_FORMAT,
    container_name,
    detect_main_breaker_format,
    feeder_breaker_name,
    is_valid_breaker_name,
    main_breaker_name,
)

__all__ = [
    # classes
    "OCP_CLASSES", "BOX_CONTAINER_CLASSES", "LEAF_AND_LINK_CLASSES",
    "ALL_VALID_CLASSES", "is_ocp_class", "is_box_container", "is_valid_class",
    "validate_parent_child",
    # naming
    "container_name", "main_breaker_name", "feeder_breaker_name",
    "detect_main_breaker_format", "is_valid_breaker_name",
    "PREFERRED_MAIN_BREAKER_FORMAT",
    # chains
    "NORMAL_HANDLE", "EMERGENCY_HANDLE", "ats_input_handle",
    "validate_ats_inputs", "upstream_placeholder_for_building",
    "building_uses_b30_pattern", "B30_PLUS_CHAIN",
    # fuses
    "classify_fuse_or_switch", "fuse_rename", "should_skip", "filter_skipped",
    "FuseClassification",
]
