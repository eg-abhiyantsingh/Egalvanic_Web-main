# #1094 ghost form instances — from INCONCLUSIVE to PASS

**Date:** 2026-09-02 · Yesterday's attempt failed on fixture validity (bare ir_session rows are
invisible to every JSON view → deletion unobservable). Today's fix: create WOs through the real
4-step wizard (job-backed, listed, deletable via the row's Delete dialog), let the service
machinery mint 261 instances, add chain+direct+shared fixtures, delete through the UI, verify.
All checklist items pass incl. the shared-task negative and the 1.35s large-delete perf check.
Wizard recipe and the create-for-asset resolution recorded in memory.
