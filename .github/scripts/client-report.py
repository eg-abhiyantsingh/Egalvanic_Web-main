#!/usr/bin/env python3
"""
Transform the technical CI consolidated report (HTML) into a client-ready
version optimized for external delivery.

The input is the HTML produced by consolidated-report.py. The output HTML:
  - Starts with an executive summary in plain language
  - Separates "functional failures" (needs attention) from
    "known issues status" (documented bugs - failure is expected)
  - Presents a feature-area coverage table
  - Uses clean, print-friendly visuals (PDF export-ready)

Usage:
  python3 client-report.py <input-html> <output-html>
"""
import re
import html as html_lib
import sys
from pathlib import Path


def transform(src: str, dst: str) -> None:
    raw = Path(src).read_text()

    module_re = re.compile(
        r'<div class="module[^"]*"[^>]*>\s*'
        r'<div class="module-header">\s*'
        r'<div class="module-name">\s*'
        r'<span class="toggle">[^<]*</span>([^<]+)</div>\s*'
        r'<div class="module-stats">(.*?)</div>\s*</div>\s*'
        r'<div class="test-list">(.*?)</div>\s*</div>',
        re.DOTALL,
    )
    test_re = re.compile(
        r'<div class="test-row">\s*'
        r'<span class="test-name">(.*?)</span>\s*'
        r'<span class="test-duration">([^<]*)</span>\s*'
        r'<span class="badge badge-(pass|fail|skip)">[^<]*</span>\s*</div>',
        re.DOTALL,
    )

    modules = []
    for m in module_re.finditer(raw):
        name = m.group(1).strip()
        stats_block = m.group(2)
        test_block = m.group(3)
        passed = int(re.search(r'(\d+) passed', stats_block).group(1)) if 'passed' in stats_block else 0
        failed = int(re.search(r'(\d+) failed', stats_block).group(1)) if 'failed' in stats_block else 0
        skipped = int(re.search(r'(\d+) skipped', stats_block).group(1)) if 'skipped' in stats_block else 0
        tests = []
        for t in test_re.finditer(test_block):
            tests.append({
                'name': html_lib.unescape(t.group(1).strip()),
                'duration': t.group(2).strip(),
                'status': t.group(3),
            })
        modules.append({
            'name': name, 'passed': passed, 'failed': failed, 'skipped': skipped,
            'total': passed + failed + skipped, 'tests': tests,
        })

    total_passed = sum(m['passed'] for m in modules)
    total_failed = sum(m['failed'] for m in modules)
    total_tests = total_passed + total_failed + sum(m['skipped'] for m in modules)
    pass_rate = (total_passed / total_tests * 100.0) if total_tests else 0.0

    KNOWN_ISSUE_MODULE = 'Curated Bug Verification'
    known_issues_mod = next((m for m in modules if m['name'] == KNOWN_ISSUE_MODULE), None)
    functional_modules = [m for m in modules if m['name'] != KNOWN_ISSUE_MODULE]

    BUG_DESCRIPTIONS = {
        'BUG-001': ('404 Page Handling', 'Invalid URLs should show a friendly "Page Not Found" message'),
        'BUG-002': ('CSP / Font Loading', 'Third-party notification fonts should load without being blocked'),
        'BUG-003': ('Required Field Validation', 'Issue form should reject submissions missing required fields'),
        'BUG-004': ('Error Message Quality', 'Invalid asset URLs should show a user-friendly error, not internal details'),
        'BUG-005': ('Input Length Limits', 'Issue title should enforce a reasonable character limit'),
        'BUG-006': ('Page Load Performance', 'Key pages should load in under 6 seconds on average'),
        'BUG-007': ('Cookie Security (CSRF)', 'Auth cookies should use SameSite=Lax to prevent form-POST CSRF'),
        'BUG-008': ('Login Rate Limiting', 'Failed login attempts should be throttled after N tries'),
    }

    def extract_bug_id(test_name):
        m = re.match(r'BUG-(\d{3})', test_name)
        return f'BUG-{m.group(1)}' if m else None

    functional_failures = []
    for m in functional_modules:
        for t in m['tests']:
            if t['status'] == 'fail':
                functional_failures.append({'area': m['name'], 'name': t['name'], 'duration': t['duration']})

    CLIENT_AREA_NAMES = {
        'Authentication': 'User Login & Access',
        'Site Selection': 'Site & Facility Selection',
        'Connections': 'Electrical Connections',
        'Locations': 'Locations (Buildings, Floors, Rooms)',
        'Issues': 'Issue Tracking',
        'Asset Management': 'Asset Management',
        'Admin Forms': 'Admin Configuration Forms',
        'AI Exploratory': 'Exploratory / Stability',
        'AI Visual Regression': 'Visual Regression',
        'AI Page Analysis': 'Page Structure Analysis',
        'Work Orders': 'Work Orders',
        'Tasks': 'Tasks',
    }

    # Feature Area table rows
    area_rows = []
    for m in functional_modules:
        display_name = CLIENT_AREA_NAMES.get(m['name'], m['name'])
        rate = (m['passed'] / m['total'] * 100.0) if m['total'] else 0
        state_cls = 'ok' if (m['failed'] == 0 and m['skipped'] == 0) else ('fail' if m['passed'] == 0 else 'partial')
        state_label = 'Passed' if state_cls == 'ok' else ('Failed' if state_cls == 'fail' else 'Partial')
        area_rows.append(f'''
      <tr>
        <td class="area-name">{html_lib.escape(display_name)}</td>
        <td class="num">{m['total']}</td>
        <td class="num passed">{m['passed']}</td>
        <td class="num failed">{m['failed']}</td>
        <td class="rate"><span class="rate-pill rate-{state_cls}">{rate:.0f}%</span></td>
        <td><span class="status-chip status-{state_cls}">{state_label}</span></td>
      </tr>''')

    # Functional failures grouped by area
    if functional_failures:
        grouped = {}
        for f in functional_failures:
            grouped.setdefault(f['area'], []).append(f)
        parts = []
        for area, items in grouped.items():
            display = CLIENT_AREA_NAMES.get(area, area)
            rows = []
            for f in items:
                rows.append(f'''
          <li>
            <div class="fail-name">{html_lib.escape(f['name'])}</div>
            <div class="fail-duration">Ran for {html_lib.escape(f['duration'])}</div>
          </li>''')
            parts.append(f'''
      <div class="fail-area">
        <h3>{html_lib.escape(display)} <span class="count-pill">{len(items)}</span></h3>
        <ul class="fail-list">{"".join(rows)}</ul>
      </div>''')
        func_fail_html = f'''
    <section class="section">
      <div class="section-header">
        <h2>Functional Issues &mdash; Needs Attention</h2>
        <p class="section-lead">{len(functional_failures)} verification{"" if len(functional_failures) == 1 else "s"} did not pass. These are genuine regressions or environment issues in the current build that require engineering review.</p>
      </div>
      {"".join(parts)}
    </section>'''
    else:
        func_fail_html = '''
    <section class="section">
      <div class="section-header">
        <h2>Functional Issues &mdash; Needs Attention</h2>
        <p class="section-lead"><strong>No functional regressions detected.</strong> All feature-level verifications passed.</p>
      </div>
    </section>'''

    # Known Issues table
    known_issues_html = ''
    if known_issues_mod:
        rows = []
        for t in known_issues_mod['tests']:
            bug_id = extract_bug_id(t['name'])
            label, desc = BUG_DESCRIPTIONS.get(bug_id, ('Unknown', t['name']))
            if t['status'] == 'pass':
                status_cls, status_label = 'resolved', 'Resolved'
                detail = 'Current build passes this check &mdash; the previously reported issue is not present.'
            elif t['status'] == 'fail':
                status_cls, status_label = 'active', 'Still Active'
                detail = 'Current build exhibits the previously reported behavior &mdash; engineering fix pending.'
            else:
                status_cls, status_label = 'partial', 'Inconclusive'
                detail = 'Test could not run to completion.'
            rows.append(f'''
      <tr>
        <td><span class="bug-id">{bug_id or '&mdash;'}</span></td>
        <td>
          <div class="bug-label">{html_lib.escape(label)}</div>
          <div class="bug-desc">{desc}</div>
        </td>
        <td><span class="status-chip status-{status_cls}">{status_label}</span></td>
        <td class="detail-cell">{detail}</td>
      </tr>''')
        resolved_count = sum(1 for t in known_issues_mod['tests'] if t['status'] == 'pass')
        active_count = sum(1 for t in known_issues_mod['tests'] if t['status'] == 'fail')
        known_issues_html = f'''
    <section class="section">
      <div class="section-header">
        <h2>Known Issues &mdash; Status Check</h2>
        <p class="section-lead">
          This section tracks the status of <strong>{known_issues_mod['total']} previously documented issues</strong>.
          <strong class="text-resolved">{resolved_count} resolved</strong> (no longer reproducible in current build) &bull;
          <strong class="text-active">{active_count} still active</strong> (engineering fix pending).
          "Still Active" here is <em>expected</em> until the underlying issue is fixed &mdash; it is NOT a new regression.
        </p>
      </div>
      <table class="issues-table">
        <thead>
          <tr><th>ID</th><th>Area</th><th>Status in Current Build</th><th>Detail</th></tr>
        </thead>
        <tbody>{"".join(rows)}</tbody>
      </table>
    </section>'''

    pass_count_func = sum(sum(1 for t in m['tests'] if t['status'] == 'pass') for m in functional_modules)
    passing_summary = f'''
    <section class="section">
      <div class="section-header">
        <h2>Verifications Passed</h2>
        <p class="section-lead"><strong>{pass_count_func} functional verifications executed successfully</strong> across {len(functional_modules)} product areas. See the Feature Area Coverage table above for the distribution.</p>
      </div>
    </section>'''

    exec_paragraph = (
        f'Across <strong>{total_tests} automated verifications</strong> run against the '
        f'eGalvanic Web platform, <strong class="text-passed">{total_passed} passed</strong> '
        f'and <strong class="text-failed">{total_failed} did not pass</strong>. '
    )
    if known_issues_mod:
        active_count = sum(1 for t in known_issues_mod['tests'] if t['status'] == 'fail')
        functional_fail_count = total_failed - active_count
        exec_paragraph += (
            f'Of the {total_failed} failures, <strong>{active_count}</strong> are '
            f'<em>known issues</em> currently being tracked by engineering '
            f'(see &ldquo;Known Issues&rdquo; section), and <strong>{functional_fail_count}</strong> are '
            f'functional verifications that need follow-up. '
        )
    exec_paragraph += f'Overall pass rate: <strong>{pass_rate:.1f}%</strong>.'

    html_out = f'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>eGalvanic Web Platform &mdash; QA Verification Report</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  :root {{
    --ink: #1b2330; --subtle: #5c6878; --border: #e3e7ec;
    --pass: #2d7a4a; --pass-soft: #e6f4ec;
    --fail: #b83a42; --fail-soft: #fbeceb;
    --partial: #c47608; --partial-soft: #fdf3e2;
    --accent: #1f3a5f; --accent-soft: #eff4fa;
  }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; background: #f7f8fa; color: var(--ink); line-height: 1.55; font-size: 14px; -webkit-font-smoothing: antialiased; }}
  .container {{ max-width: 1000px; margin: 0 auto; padding: 0 24px; }}
  .cover {{ background: linear-gradient(135deg, #1f3a5f 0%, #2a4a7a 100%); color: #fff; padding: 48px 0 40px; margin-bottom: 32px; }}
  .cover .wrap {{ max-width: 1000px; margin: 0 auto; padding: 0 24px; }}
  .cover .client-note {{ font-size: 13px; letter-spacing: 0.08em; text-transform: uppercase; opacity: 0.75; margin-bottom: 8px; }}
  .cover h1 {{ font-size: 28px; font-weight: 600; letter-spacing: -0.01em; margin-bottom: 8px; }}
  .cover .meta {{ font-size: 14px; opacity: 0.85; }}
  .section {{ margin-bottom: 36px; background: #fff; border: 1px solid var(--border); border-radius: 10px; padding: 28px; box-shadow: 0 1px 2px rgba(16,24,40,0.04); }}
  .section-header {{ margin-bottom: 20px; }}
  .section-header h2 {{ font-size: 18px; font-weight: 600; color: var(--ink); margin-bottom: 8px; }}
  .section-lead {{ font-size: 14px; color: var(--subtle); }}
  .exec-hero {{ display: grid; grid-template-columns: 180px 1fr; gap: 32px; align-items: center; }}
  .exec-hero .ring {{ position: relative; width: 160px; height: 160px; }}
  .exec-hero .ring svg {{ width: 100%; height: 100%; transform: rotate(-90deg); }}
  .exec-hero .ring-center {{ position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }}
  .exec-hero .ring-center .big {{ font-size: 32px; font-weight: 700; color: var(--ink); }}
  .exec-hero .ring-center .small {{ font-size: 11px; color: var(--subtle); letter-spacing: 0.08em; text-transform: uppercase; margin-top: 4px; }}
  .key-stats {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-top: 24px; }}
  .key-stat {{ padding: 16px 18px; border-radius: 8px; border: 1px solid var(--border); }}
  .key-stat .n {{ font-size: 24px; font-weight: 700; margin-bottom: 2px; }}
  .key-stat .l {{ font-size: 11px; color: var(--subtle); letter-spacing: 0.08em; text-transform: uppercase; }}
  .key-stat.passed .n {{ color: var(--pass); }}
  .key-stat.failed .n {{ color: var(--fail); }}
  .key-stat.total .n {{ color: var(--ink); }}
  .key-stat.rate .n {{ color: var(--accent); }}
  table.coverage-table, table.issues-table {{ width: 100%; border-collapse: collapse; }}
  .coverage-table th, .coverage-table td, .issues-table th, .issues-table td {{ padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--border); font-size: 13.5px; vertical-align: middle; }}
  .coverage-table th, .issues-table th {{ font-size: 11px; font-weight: 600; color: var(--subtle); letter-spacing: 0.08em; text-transform: uppercase; background: #fafbfc; }}
  .coverage-table td.num {{ text-align: right; font-variant-numeric: tabular-nums; }}
  .coverage-table td.passed {{ color: var(--pass); font-weight: 600; }}
  .coverage-table td.failed {{ color: var(--fail); font-weight: 600; }}
  .coverage-table td.rate {{ text-align: right; }}
  .coverage-table td.area-name {{ font-weight: 500; color: var(--ink); }}
  .rate-pill {{ display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }}
  .rate-ok {{ background: var(--pass-soft); color: var(--pass); }}
  .rate-partial {{ background: var(--partial-soft); color: var(--partial); }}
  .rate-fail {{ background: var(--fail-soft); color: var(--fail); }}
  .status-chip {{ display: inline-block; padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; letter-spacing: 0.03em; text-transform: uppercase; }}
  .status-ok, .status-resolved {{ background: var(--pass-soft); color: var(--pass); }}
  .status-fail, .status-active {{ background: var(--fail-soft); color: var(--fail); }}
  .status-partial {{ background: var(--partial-soft); color: var(--partial); }}
  .fail-area {{ margin-bottom: 20px; }}
  .fail-area h3 {{ font-size: 15px; margin-bottom: 10px; color: var(--ink); display: flex; align-items: center; gap: 10px; }}
  .count-pill {{ background: var(--fail-soft); color: var(--fail); font-size: 12px; font-weight: 600; padding: 1px 8px; border-radius: 999px; }}
  .fail-list {{ list-style: none; }}
  .fail-list li {{ padding: 12px 16px; border-left: 3px solid var(--fail); background: #fff; margin-bottom: 6px; border-radius: 0 6px 6px 0; }}
  .fail-name {{ font-size: 13.5px; color: var(--ink); line-height: 1.45; }}
  .fail-duration {{ font-size: 11.5px; color: var(--subtle); margin-top: 3px; }}
  .bug-id {{ font-family: 'SF Mono', Menlo, monospace; font-size: 12px; font-weight: 600; color: var(--accent); background: var(--accent-soft); padding: 3px 8px; border-radius: 4px; }}
  .bug-label {{ font-weight: 500; color: var(--ink); }}
  .bug-desc {{ font-size: 12.5px; color: var(--subtle); margin-top: 2px; }}
  .issues-table td.detail-cell {{ font-size: 12.5px; color: var(--subtle); }}
  .text-passed {{ color: var(--pass); }}
  .text-failed {{ color: var(--fail); }}
  .text-resolved {{ color: var(--pass); }}
  .text-active {{ color: var(--fail); }}
  footer {{ text-align: center; padding: 28px 0 40px; font-size: 12px; color: var(--subtle); }}
  @media print {{
    body {{ background: #fff; }}
    .cover {{ break-inside: avoid; page-break-inside: avoid; }}
    .section {{ break-inside: avoid; page-break-inside: avoid; box-shadow: none; }}
  }}
</style>
</head>
<body>

<div class="cover">
  <div class="wrap">
    <div class="client-note">QA Verification Report &bull; For Client Review</div>
    <h1>eGalvanic Web Platform &mdash; Automated Test Results</h1>
    <div class="meta">Environment: acme.qa.egalvanic.ai &bull; Pass rate: <strong style="color:#fff;">{pass_rate:.1f}%</strong></div>
  </div>
</div>

<div class="container">
  <section class="section">
    <div class="section-header"><h2>Executive Summary</h2></div>
    <div class="exec-hero">
      <div class="ring">
        <svg viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="44" fill="none" stroke="#e3e7ec" stroke-width="8"/>
          <circle cx="50" cy="50" r="44" fill="none" stroke="#2d7a4a" stroke-width="8"
                  stroke-dasharray="{pass_rate / 100 * 276.46:.2f} 276.46" stroke-linecap="round"/>
        </svg>
        <div class="ring-center">
          <div class="big">{pass_rate:.0f}%</div>
          <div class="small">Pass Rate</div>
        </div>
      </div>
      <div><p style="font-size: 15px; line-height: 1.65;">{exec_paragraph}</p></div>
    </div>
    <div class="key-stats">
      <div class="key-stat total"><div class="n">{total_tests}</div><div class="l">Total Verifications</div></div>
      <div class="key-stat passed"><div class="n">{total_passed}</div><div class="l">Passed</div></div>
      <div class="key-stat failed"><div class="n">{total_failed}</div><div class="l">Did Not Pass</div></div>
      <div class="key-stat rate"><div class="n">{len(functional_modules)}</div><div class="l">Feature Areas</div></div>
    </div>
  </section>

  <section class="section">
    <div class="section-header">
      <h2>Feature Area Coverage</h2>
      <p class="section-lead">Verification outcome grouped by product area. "Known Issues Status Check" covers documented bugs separately (see dedicated section below).</p>
    </div>
    <table class="coverage-table">
      <thead>
        <tr>
          <th>Product Area</th>
          <th class="num">Verifications</th>
          <th class="num">Passed</th>
          <th class="num">Did Not Pass</th>
          <th class="num">Pass Rate</th>
          <th>Overall</th>
        </tr>
      </thead>
      <tbody>{"".join(area_rows)}</tbody>
    </table>
  </section>

  {func_fail_html}
  {known_issues_html}
  {passing_summary}

  <footer>
    Generated automatically from CI run &bull;
    {total_tests} automated verifications across {len(modules)} modules &bull;
    For questions, contact QA
  </footer>
</div>
</body>
</html>
'''

    Path(dst).write_text(html_out)
    print(f'Wrote client-ready report: {dst}')
    print(f'Size: {Path(dst).stat().st_size / 1024:.1f} KB')
    print(f'Summary: {total_tests} total, {total_passed} passed, {total_failed} failed, {pass_rate:.1f}% pass rate')


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print('Usage: python3 client-report.py <input-html> <output-html>', file=sys.stderr)
        sys.exit(1)
    transform(sys.argv[1], sys.argv[2])
