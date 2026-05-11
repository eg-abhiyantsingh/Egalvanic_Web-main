"""K STAR validators.

Two complementary checks:

    structural — catches upload-blockers (uniqueness, ref integrity, ATS rule, …)
    audit      — catches format inconsistencies (mixed types, naming drift, …)

Run both before publishing a merged XLSX.
"""
from src.validators import audit, structural
from src.validators.audit import (
    AuditFinding,
    AuditReport,
)
from src.validators.audit import (
    audit as run_audit,
)
from src.validators.audit import (
    print_report as print_audit,
)

# Re-export
from src.validators.structural import (
    ValidationIssue,
    ValidationReport,
    validate,
)
from src.validators.structural import (
    print_report as print_structural,
)

__all__ = [
    "ValidationIssue", "ValidationReport", "validate", "print_structural",
    "AuditFinding", "AuditReport", "run_audit", "print_audit",
]
