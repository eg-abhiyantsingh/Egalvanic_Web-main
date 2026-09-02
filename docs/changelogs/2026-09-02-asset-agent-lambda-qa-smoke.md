# asset-agent Dockerfile import failure (#70) — QA smoke: not affected + artifact

Fix is dev-only and all five checklist items are dev/CloudWatch/CI. The one QA-relevant question — is QA
suffering the same module-scope import outage, given nameplate code is on QA? — answered by smoke-invoking
the configure flavor: 25-asset bulk run, 22 done, 0 failed, no ImportModuleError. QA healthy, dev outage not
replicated. Zero residue (staged proposals, closed without applying). Flagged that AI Extraction fires with no
confirm step. Pressed the carried-forward gap: wire the runner's __smoke__ mode into deploy — a static guard
can't catch "builds but won't start", which is how this shipped green.
