# Nameplate "Extract from Photos" (#1058/#1246/#68) — PARTIAL verdict + artifact

Ticket claimed dev-only/absent-from-qa — WRONG: bundle + apply route prove it's on QA. Confirmed the UI
contract (button, 3 gating checkboxes with helper copy, apply route). Could NOT verify the core (Sonnet
photo extraction filling node.* fields) — external Lambda + no nameplate photo fixture; nor bulk counter/
500-cap (bulk surface not reached), 413, tenant-404 (demo tenant empty), ledger/polling/StepFn (infra).
Deliberately NOT stamped PASS. Artifact published (partial, honest). Hand infra half to pipeline repo.
