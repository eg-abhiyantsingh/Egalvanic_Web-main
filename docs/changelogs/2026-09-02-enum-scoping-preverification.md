# Nameplate enum scoping (#1059/#69) — QA pre-verification + artifact

Fix is dev-only and has no web fingerprint, so instead of guessing I measured the ORACLE on QA: all four
ticket numbers independently confirmed (Disconnect Switch mains 6→3 MCB/Fuse/Non; Panelboard 6→4
default_visible; busway insulation 3; cable 29 of 32), plus the four other is_busway enums. Identified the
for_entity override trap in advance (global DS class falls back to 4, company DS has explicit 3 — 3-vs-4 is
the tell). Found an adjacent robustness gap: ?is_busway=1/0 silently return the full 32 (only true/false
parse). Real screenshot of the Extract dialog on a Disconnect Switch. Framed as pre-verification, not a pass.
