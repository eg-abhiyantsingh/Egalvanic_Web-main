# Consolidated detailed report — fix silent test-case loss from module-name collision

- **Title:** Consolidated DETAILED report was dropping whole groups of test cases (and their
  screenshots) — fixed the per-module dedup that wrongly collapsed parallel groups.
- **Date:** 2026-06-24
- **Time:** 17:50
- **Prompt:** "screenshot is not showing properly and all test case are not showing in
  consilideade report. consolidated-detailed-report-suite2 all test case are not showing why"

---

## What changed (plain summary)

The consolidated **detailed** report (`consolidated-detailed-report-suite2` artifact) was
showing only a fraction of the tests that actually ran. Root cause: the report builder assumed
**one module = one HTML report**, but in the parallel CI suites a single module — "Asset
Management" — is produced by up to **10 separate jobs** (5 `AssetPartN` + the 5 new Engineering
suites), each emitting its own `Detailed_Report_Asset_Management_<ts>.html`. The builder kept
only the newest one and silently discarded the rest, so most engineering test cases (and the
screenshots attached to them) disappeared from the bundle.

Fixed `consolidated-detailed-report.py` to keep **one report per (group, module)** — keyed off
the per-job artifact subdir — and to disambiguate the display name (`Asset Management ·
asset-engineering`, `… · asset-exhaustive`, …) when a module comes from more than one group.
Also mapped the 6 newer test classes into `consolidated-report.py`'s module table so the
client/summary report files them under Asset Management / Work Orders instead of "Other".

## Depth explanation (for learning + manager review)

**The bug class:** a *deduplication key that's too coarse*. The de-dup was meant to drop
genuine re-run duplicates ("same module reported twice → keep newest"). That's correct for a
single sequential run. But the parallel-suite architecture violates the hidden assumption: many
independent jobs legitimately produce the *same module name* with *different test sets*. Keying
the de-dup on `module` alone therefore conflated "duplicate of the same run" with "a different
shard of the same module," and threw away real data.

**Why the artifact layout made the fix clean:** GitHub's `download-artifact` *without*
`merge-multiple` preserves each job's artifact in its own subdirectory
(`all-reports/reports-s2-<group>/…`). That subdirectory name is a reliable, already-present
identity for the shard. So the fix is just: refine the de-dup key from `module` →
`(group, module)`, where `group` = first path component (prefix-stripped). No Java change, no
CI-YAML change, and it self-heals Parallel Suite 1 too (same script).

**Preserving backward compatibility:** the local workflow runs the script against a *flat*
directory (`reports/detail-report`), where there is no group subdir. `group_from_relpath()`
returns `""` there, the `(group, module)` key degenerates to `("", module)`, and behavior is
byte-for-byte the old "newest per module." This is the important discipline — a fix for the
parallel case must not regress the simple case; verified by re-running against the flat local
dir (10 clean module names, no group suffixes).

**Separating two symptoms that share one cause:** the user reported *both* "tests missing" and
"screenshots not showing." It would have been easy to chase a CSS/rendering bug for the
screenshots. Instead I rendered the bundle in a real browser and measured the DOM: every base64
screenshot loaded (`naturalWidth > 0`) and every iframe sized correctly. That proved the
rendering path was healthy and both symptoms were the *same* collision (the missing screenshots
belonged to the dropped shards). I still added a defensive `window.frameElement` self-sizing
path to the embedded-module shim so the one-page report can never collapse to its 200px
min-height floor (which *would* clip tests + screenshots below the fold) when opened directly.

**Validation philosophy:** proven, not compiled. Built a faithful CI-shaped input (5
`reports-s2-asset-*` subdirs, same module, different reports), ran the old script (→ 3 modules,
4 groups lost) and the new script (→ 7 modules, all groups kept) side by side, then loaded the
fixed single-file report in Chromium and counted tests/images per module iframe.
