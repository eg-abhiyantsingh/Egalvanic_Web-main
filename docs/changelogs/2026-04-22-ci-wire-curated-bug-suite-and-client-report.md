# 2026-04-22 — CI: Wire curated 8-bug suite into parallel-suite-2.yml + upload PDF as client artifact

## Prompt
> this test case add in https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/workflows/parallel-suite-2.yml at top, i want to send this report to client too so you can consolidate this too.

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production website repo untouched.

## What changed

### 1. GitHub Actions workflow — [.github/workflows/parallel-suite-2.yml](.github/workflows/parallel-suite-2.yml)

| Before | After |
|---|---|
| 6 parallel groups, 169 TCs | **7 parallel groups, 177 TCs** |
| `max-parallel: 6` | `max-parallel: 7` |
| Summary referenced `/ 6` | Summary references `/ 7` |
| — | New first matrix entry: `curated-bug-verification` (8 TCs, stagger=0) |
| — | New artifact step uploads `bug pdf/*.pdf + *.html` (retention 90 days) |

Staggers shifted: smoke 0→10, load-api 10→20, ai-form 20→30, monkey 30→40, visual 40→50, ai-analyzer 50→60. The curated suite runs at stagger=0 so it finishes earliest — client artifact available as soon as possible.

### 2. Dashboard runner script — [.github/scripts/full-suite-dashboard.sh](.github/scripts/full-suite-dashboard.sh)

Added `curated-bug-verification` as the 16th group (index 15):
- `ALL_GROUPS[15] = "curated-bug-verification"`
- `ALL_GROUP_NAMES[15] = "Curated Bug Verification"`
- `ALL_GROUP_TESTS[15] = 8`
- `ALL_GROUP_XMLS[15] = "deep-bug-verification-testng.xml"`
- `get_group_index()` case clause: `curated-bug-verification) echo 15 ;;`
- Valid-group list in error message updated

Chose append-at-end (index 15) rather than insert-at-top to avoid renumbering every other group's index — which would have required edits to parallel-suite.yml (the companion workflow) too. The workflow YAML matrix order is what the user sees in the GitHub Actions UI; the dashboard script's internal index is implementation detail.

### 3. Consolidated report generator — [.github/scripts/consolidated-report.py](.github/scripts/consolidated-report.py)

- Added `CLASS_TO_MODULE` entries:
  - `'DeepBugVerificationTestNG': 'Curated Bug Verification'`
  - `'SecurityAuditTestNG': 'Curated Bug Verification'`
- Added `'Curated Bug Verification'` as the **first entry** in `MODULE_ORDER` — the client sees it first when reading the consolidated HTML report.

This means the consolidated client report now has a top section called "Curated Bug Verification" showing pass/fail for all 8 TCs.

### 4. New client artifact: `curated-bug-report-client`

A new upload step in the summary job packages:
- `bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.pdf` (2.32 MB, 8 validated bugs)
- `bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.html` (1.35 MB, same content)

Retention: 90 days (vs 30 for other artifacts) — so the client has a longer window to download.

The `GITHUB_STEP_SUMMARY` block at the end of the run now lists this artifact prominently as **(CLIENT)**.

## How the end-to-end flow looks after this change

```
Trigger workflow (workflow_dispatch)
      ↓
7 parallel matrix jobs kick off simultaneously:
  • Curated Bug Verification (stagger 0s)  ← runs DeepBugVerificationTestNG + SecurityAuditTestNG
  • Smoke Suites (stagger 10s)
  • Load + API + Critical Path (stagger 20s)
  • AI Form Creation (stagger 30s)
  • Monkey Exploratory (stagger 40s)
  • Visual Regression (stagger 50s)
  • AI Page Analyzer (stagger 60s)
      ↓
Each job writes results/<group>.json + uploads reports-s2-<group> artifact
      ↓
Summary job aggregates, generates:
  • Consolidated_Client_Report_Suite2.html (all modules, client-readable)
  • curated-bug-report-client artifact (the bug PDF — for client delivery)
      ↓
GitHub step summary table surfaces the 2 client-facing artifacts prominently
```

## How the client gets the report

Two options once the workflow run finishes:

**A. Manual download** — on the workflow run page, scroll to Artifacts. `curated-bug-report-client` contains the 8-bug PDF + HTML. Download and email to the client.

**B. Email automation** — if `SEND_EMAIL_ENABLED=true` + `EMAIL_PASSWORD` secret is set, the existing consolidated-report.py emails the consolidated HTML automatically. (The standalone bug PDF is not auto-emailed — that's a manual download step.)

## Preflight validation (ran locally before pushing)

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/parallel-suite-2.yml'))"
# → YAML OK
bash -n .github/scripts/full-suite-dashboard.sh
# → shell OK
python3 -c "import ast; ast.parse(open('.github/scripts/consolidated-report.py').read())"
# → python OK
```

All three files parse cleanly.

## What was NOT changed (and why)

- `parallel-suite.yml` — the companion workflow runs a different 9-group set (Auth + Asset + Issue + etc.). This change only affects Suite 2 per your request.
- `smoke-tests.yml`, `full-suite.yml`, `email-test.yml` — unrelated workflows, left alone.
- Dashboard script's main index order — kept existing indices intact to avoid cascade edits across workflows.

## Rollback
`git revert <this-commit>` restores the 6-group matrix + removes the curated-bug artifact upload step. The `DeepBugVerificationTestNG` + `SecurityAuditTestNG` classes still exist — they'd still be runnable via `mvn clean test -DsuiteXmlFile=deep-bug-verification-testng.xml` outside of CI.

## In-depth explanation (for learning + manager reporting)

### Why max-parallel=7 matters
GitHub-hosted runners are 2-core / 7 GB RAM. Running 7 parallel matrix jobs each spawning Chrome is intensive but works because each job is a separate runner machine (not threads on one runner). The bottleneck isn't CPU — it's the shared eGalvanic QA backend handling 7 simultaneous logins. That's why each job has a `stagger` (sleep N seconds before starting tests) — spreads login load across ~60 seconds instead of a thundering herd at t=0.

### Why the curated suite gets stagger=0
It's the most important group (client-facing gate). Running it first means its result is available earliest if other groups are slow or fail. If a client emails 30 minutes after trigger asking for the bug report, the PDF is usually already uploaded by then.

### Why 90-day artifact retention on the client report
Default is 30 days. Client approval cycles often take weeks — having the artifact expire mid-review creates friction. 90 days covers most workflows without indefinite storage cost.

### Why we add at index 15 in the dashboard script but position 1 in the YAML
The YAML matrix is what the user sees in the GitHub Actions UI — order matters visually. The dashboard script's internal index is used only to look up the XML path for a given group key. Appending at index 15 means zero cascade edits across other workflows that might hardcode index positions. Decoupling these two ordering systems keeps the diff small and focused.

### Why the consolidated report puts "Curated Bug Verification" first
Clients don't read reports sequentially — they scan the top. The `MODULE_ORDER` list determines section sequence in the HTML. Moving this module first surfaces the 8-bug status immediately, before the client has to scroll past 15+ other module summaries.
