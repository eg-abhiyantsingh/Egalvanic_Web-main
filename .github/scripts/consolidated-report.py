#!/usr/bin/env python3
"""
Consolidated Client Report Generator for Parallel CI

Merges testng-results.xml files from all parallel groups into a single
HTML client report. Sends one email with the report attached.

Usage:
  python3 consolidated-report.py <results-dir> <output-path>

  results-dir: Directory containing downloaded artifacts.
               Recursively searches for testng-results.xml files.
  output-path: Path for the generated HTML report.

Environment variables for email:
  SEND_EMAIL_ENABLED  "true" to send email (default: "false")
  EMAIL_FROM          Sender address
  EMAIL_PASSWORD      App password for SMTP auth
  EMAIL_TO            Comma-separated recipient addresses
  SMTP_HOST           SMTP server (default: smtp.gmail.com)
  SMTP_PORT           SMTP port (default: 587)

CI variables (auto-set by GitHub Actions):
  GITHUB_SERVER_URL   For "View on GitHub Actions" link
  GITHUB_REPOSITORY   Repo name (owner/repo)
  GITHUB_RUN_ID       Workflow run ID
"""

import xml.etree.ElementTree as ET
import os
import sys
import glob
import smtplib
import ssl
import platform
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email import encoders
from datetime import datetime
from collections import OrderedDict

# ============================================================
# Class Name → Module Name Mapping
# ============================================================
# Maps TestNG class names (without package) to the module names
# used in ExtentReportManager.createTest(MODULE, FEATURE, TEST).
# If a class isn't listed, it falls into "Other" module.

CLASS_TO_MODULE = {
    # Authentication
    'AuthenticationTestNG': 'Authentication',
    'AuthSmokeTestNG': 'Authentication',
    # Site Selection
    'SiteSelectionTestNG': 'Site Selection',
    'SiteSelectionSmokeTestNG': 'Site Selection',
    # Connections
    'ConnectionTestNG': 'Connections',
    'ConnectionPart2TestNG': 'Connections',
    'ConnectionSmokeTestNG': 'Connections',
    # Locations
    'LocationTestNG': 'Locations',
    'LocationPart2TestNG': 'Locations',
    'LocationSmokeTestNG': 'Locations',
    # Tasks
    'TaskTestNG': 'Tasks',
    # Issues
    'IssueTestNG': 'Issues',
    'IssuePart2TestNG': 'Issues',
    'IssuesSmokeTestNG': 'Issues',
    # Work Orders
    'WorkOrderTestNG': 'Work Orders',
    'WorkOrderPart2TestNG': 'Work Orders',
    'WorkOrderSmokeTestNG': 'Work Orders',
    # Asset Management
    'AssetPart1TestNG': 'Asset Management',
    'AssetPart2TestNG': 'Asset Management',
    'AssetPart3TestNG': 'Asset Management',
    'AssetPart4TestNG': 'Asset Management',
    'AssetPart5TestNG': 'Asset Management',
    'AssetSmokeTestNG': 'Asset Management',
    # SLD
    'SLDTestNG': 'SLD Module',
    # Dashboard
    'DashboardBugTestNG': 'Dashboard & Bug Verification',
    # Bug Hunt & Security
    'BugHuntTestNG': 'Bug Hunt & Security',
    'BugHuntConnectionsTestNG': 'Bug Hunt & Security',
    'BugHuntDashboardTestNG': 'Bug Hunt & Security',
    'BugHuntGlobalTestNG': 'Bug Hunt & Security',
    'BugHuntLocationsTestNG': 'Bug Hunt & Security',
    'BugHuntPagesTestNG': 'Bug Hunt & Security',
    'BugHuntTasksTestNG': 'Bug Hunt & Security',
    'BugHuntWorkOrdersTestNG': 'Bug Hunt & Security',
    # Load & Performance
    'LoadTestNG': 'Load & Performance',
    # Critical Path
    'CriticalPathTestNG': 'Critical Path',
    # Admin Forms
    'EgFormAITestNG': 'Admin Forms',
    # API Tests
    'APIAuthTest': 'API Tests',
    'APISecurityTest': 'API Tests',
    'APIUsersTest': 'API Tests',
    'APIPerformanceTest': 'API Tests',
    # AI Intelligence
    'MonkeyTestNG': 'AI Exploratory',
    'VisualRegressionTestNG': 'AI Visual Regression',
    'AIPageAnalyzerTestNG': 'AI Page Analysis',
}

# Display order for modules in the report
MODULE_ORDER = [
    'Authentication', 'Site Selection', 'Connections', 'Locations', 'Tasks',
    'Issues', 'Work Orders', 'Asset Management', 'SLD Module',
    'Dashboard & Bug Verification', 'Bug Hunt & Security',
    'Load & Performance', 'Critical Path', 'Admin Forms', 'API Tests',
    'AI Exploratory', 'AI Visual Regression', 'AI Page Analysis',
]


# ============================================================
# Parse TestNG Results XML
# ============================================================

def parse_testng_xml(filepath):
    """
    Parse a testng-results.xml file and return a list of test results.
    Each result is a dict: {module, name, description, status, duration_ms, class_name}
    """
    tests = []
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
    except ET.ParseError as e:
        print(f"  WARNING: Could not parse {filepath}: {e}")
        return tests

    # Navigate: <testng-results> → <suite> → <test> → <class> → <test-method>
    for suite in root.findall('.//suite'):
        for test_elem in suite.findall('.//test'):
            for cls in test_elem.findall('.//class'):
                fqcn = cls.get('name', '')
                # Extract simple class name from fully qualified name
                simple_name = fqcn.rsplit('.', 1)[-1] if '.' in fqcn else fqcn
                module = CLASS_TO_MODULE.get(simple_name, 'Other')

                for method in cls.findall('test-method'):
                    # Skip config methods (@BeforeSuite, @AfterSuite, etc.)
                    if method.get('is-config') == 'true':
                        continue

                    name = method.get('name', 'Unknown')
                    description = method.get('description', '')
                    status = method.get('status', 'UNKNOWN')
                    duration_ms = int(method.get('duration-ms', '0'))

                    tests.append({
                        'module': module,
                        'name': name,
                        'description': description,
                        'status': status,
                        'duration_ms': duration_ms,
                        'class_name': simple_name,
                    })

    return tests


def find_and_parse_all(results_dir):
    """Find all testng-results.xml recursively, parse them, and deduplicate."""
    all_tests = []
    xml_files = glob.glob(os.path.join(results_dir, '**', 'testng-results.xml'), recursive=True)

    if not xml_files:
        print(f"WARNING: No testng-results.xml files found in {results_dir}")
        return all_tests

    print(f"Found {len(xml_files)} testng-results.xml files:")
    for f in sorted(xml_files):
        tests = parse_testng_xml(f)
        print(f"  {f}: {len(tests)} test methods")
        all_tests.extend(tests)

    # Deduplicate: if the same test (class+method) appears multiple times
    # (e.g. from duplicate XML files in artifacts), keep only one copy.
    # Prefer FAIL > SKIP > PASS so we never hide a failure.
    before = len(all_tests)
    seen = {}
    status_priority = {'FAIL': 0, 'SKIP': 1, 'PASS': 2}
    for t in all_tests:
        key = (t['class_name'], t['name'])
        if key not in seen:
            seen[key] = t
        else:
            # Keep the one with higher priority (lower number = keep)
            existing_pri = status_priority.get(seen[key]['status'], 3)
            new_pri = status_priority.get(t['status'], 3)
            if new_pri < existing_pri:
                seen[key] = t

    all_tests = list(seen.values())
    after = len(all_tests)
    if before != after:
        print(f"  Deduplicated: {before} → {after} tests ({before - after} duplicates removed)")

    return all_tests


# ============================================================
# Group Tests by Module
# ============================================================

def group_by_module(tests):
    """Group tests by module, maintaining MODULE_ORDER."""
    modules = OrderedDict()

    # Initialize in preferred order
    for mod in MODULE_ORDER:
        modules[mod] = []

    for t in tests:
        mod = t['module']
        if mod not in modules:
            modules[mod] = []
        modules[mod].append(t)

    # Remove empty modules
    return OrderedDict((k, v) for k, v in modules.items() if v)


# ============================================================
# Generate HTML Report
# ============================================================

def generate_html(modules, timestamp):
    """Generate a self-contained HTML client report."""
    # Calculate totals
    total = sum(len(v) for v in modules.values())
    passed = sum(1 for v in modules.values() for t in v if t['status'] == 'PASS')
    failed = sum(1 for v in modules.values() for t in v if t['status'] == 'FAIL')
    skipped = sum(1 for v in modules.values() for t in v if t['status'] == 'SKIP')
    pass_rate = (passed / total * 100) if total > 0 else 0

    # Status icon for overall
    overall_icon = "&#10004;" if failed == 0 else "&#10060;"
    overall_class = "pass" if failed == 0 else "fail"

    date_str = datetime.now().strftime("%B %d, %Y %H:%M")

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>eGalvanic Web - Consolidated Test Report</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    background: #f5f6fa;
    color: #333;
    line-height: 1.5;
  }}

  /* Header */
  .header {{
    background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
    color: #fff;
    padding: 28px 32px;
  }}
  .header h1 {{
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 0.3px;
    margin-bottom: 4px;
  }}
  .header .subtitle {{
    font-size: 13px;
    color: #bdc3c7;
  }}

  /* Summary Cards */
  .summary {{
    display: flex;
    gap: 16px;
    padding: 24px 32px;
    flex-wrap: wrap;
  }}
  .stat-card {{
    flex: 1;
    min-width: 120px;
    padding: 18px 16px;
    border-radius: 10px;
    text-align: center;
    box-shadow: 0 2px 6px rgba(0,0,0,0.06);
  }}
  .stat-card .number {{
    font-size: 32px;
    font-weight: 700;
    line-height: 1.1;
  }}
  .stat-card .label {{
    font-size: 12px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-top: 4px;
  }}
  .card-total  {{ background: #fff; border: 2px solid #e9ecef; }}
  .card-total .number {{ color: #2c3e50; }}
  .card-passed {{ background: #d4edda; border: 2px solid #c3e6cb; }}
  .card-passed .number {{ color: #155724; }}
  .card-failed {{ background: #f8d7da; border: 2px solid #f5c6cb; }}
  .card-failed .number {{ color: #721c24; }}
  .card-skipped {{ background: #fff3cd; border: 2px solid #ffeeba; }}
  .card-skipped .number {{ color: #856404; }}

  /* Progress Bar */
  .progress-wrap {{
    padding: 0 32px 20px;
  }}
  .progress-bar {{
    height: 8px;
    border-radius: 4px;
    background: #e9ecef;
    overflow: hidden;
    display: flex;
  }}
  .progress-pass {{ background: #28a745; }}
  .progress-fail {{ background: #dc3545; }}
  .progress-skip {{ background: #ffc107; }}
  .progress-label {{
    font-size: 13px;
    color: #666;
    margin-top: 6px;
    text-align: right;
  }}

  /* Module Sections */
  .modules {{
    padding: 0 32px 32px;
  }}
  .module {{
    background: #fff;
    border-radius: 10px;
    margin-bottom: 16px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    overflow: hidden;
  }}
  .module-header {{
    padding: 14px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    user-select: none;
  }}
  .module-header:hover {{ background: #fafbfc; }}
  .module-name {{
    font-size: 15px;
    font-weight: 600;
    color: #2c3e50;
  }}
  .module-stats {{
    display: flex;
    gap: 8px;
    align-items: center;
  }}
  .module-stats .mini-badge {{
    font-size: 11px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 3px;
  }}
  .mini-pass {{ background: #d4edda; color: #155724; }}
  .mini-fail {{ background: #f8d7da; color: #721c24; }}
  .mini-skip {{ background: #fff3cd; color: #856404; }}
  .module-all-pass {{ border-left: 4px solid #28a745; }}
  .module-has-fail {{ border-left: 4px solid #dc3545; }}
  .module-all-skip {{ border-left: 4px solid #ffc107; }}

  /* Test Rows */
  .test-list {{ border-top: 1px solid #f0f2f5; }}
  .test-row {{
    padding: 9px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #f8f9fa;
    font-size: 13px;
  }}
  .test-row:last-child {{ border-bottom: none; }}
  .test-row:hover {{ background: #fafbfc; }}
  .test-name {{ color: #444; flex: 1; padding-right: 12px; }}
  .test-duration {{
    color: #999;
    font-size: 11px;
    min-width: 50px;
    text-align: right;
    padding-right: 12px;
  }}
  .badge {{
    display: inline-block;
    padding: 3px 10px;
    border-radius: 3px;
    font-size: 11px;
    font-weight: 600;
    min-width: 44px;
    text-align: center;
  }}
  .badge-pass {{ background: #28a745; color: #fff; }}
  .badge-fail {{ background: #dc3545; color: #fff; }}
  .badge-skip {{ background: #ffc107; color: #000; }}

  /* Footer */
  .footer {{
    background: #fff;
    border-top: 1px solid #e9ecef;
    padding: 16px 32px;
    font-size: 11px;
    color: #999;
    text-align: center;
  }}
  .footer a {{ color: #007bff; text-decoration: none; }}

  /* Collapsible toggle */
  .toggle {{ transition: transform 0.2s; display: inline-block; margin-right: 8px; font-size: 12px; color: #999; }}
  .module.collapsed .test-list {{ display: none; }}
  .module.collapsed .toggle {{ transform: rotate(-90deg); }}
</style>
</head>
<body>

<!-- Header -->
<div class="header">
  <h1>eGalvanic Web Automation - Test Results</h1>
  <div class="subtitle">Consolidated Report from Parallel CI Execution &bull; {date_str}</div>
</div>

<!-- Summary Cards -->
<div class="summary">
  <div class="stat-card card-total">
    <div class="number">{total}</div>
    <div class="label">Total Tests</div>
  </div>
  <div class="stat-card card-passed">
    <div class="number">{passed}</div>
    <div class="label">Passed</div>
  </div>
  <div class="stat-card card-failed">
    <div class="number">{failed}</div>
    <div class="label">Failed</div>
  </div>
  <div class="stat-card card-skipped">
    <div class="number">{skipped}</div>
    <div class="label">Skipped</div>
  </div>
</div>

<!-- Progress Bar -->
<div class="progress-wrap">
  <div class="progress-bar">
    <div class="progress-pass" style="width:{passed/total*100 if total else 0:.1f}%"></div>
    <div class="progress-fail" style="width:{failed/total*100 if total else 0:.1f}%"></div>
    <div class="progress-skip" style="width:{skipped/total*100 if total else 0:.1f}%"></div>
  </div>
  <div class="progress-label">{pass_rate:.1f}% pass rate</div>
</div>

<!-- Modules -->
<div class="modules">
"""

    # Generate module sections
    for mod_name, tests in modules.items():
        m_passed = sum(1 for t in tests if t['status'] == 'PASS')
        m_failed = sum(1 for t in tests if t['status'] == 'FAIL')
        m_skipped = sum(1 for t in tests if t['status'] == 'SKIP')
        m_total = len(tests)

        # Module styling
        if m_failed > 0:
            mod_class = 'module-has-fail'
        elif m_skipped > 0 and m_passed == 0:
            mod_class = 'module-all-skip'
        else:
            mod_class = 'module-all-pass'

        # Auto-collapse passed modules, expand failed ones
        collapsed = 'collapsed' if m_failed == 0 else ''

        html += f"""  <div class="module {mod_class} {collapsed}" onclick="this.classList.toggle('collapsed')">
    <div class="module-header">
      <div class="module-name">
        <span class="toggle">&#9660;</span>{mod_name}
      </div>
      <div class="module-stats">
"""
        if m_passed > 0:
            html += f'        <span class="mini-badge mini-pass">{m_passed} passed</span>\n'
        if m_failed > 0:
            html += f'        <span class="mini-badge mini-fail">{m_failed} failed</span>\n'
        if m_skipped > 0:
            html += f'        <span class="mini-badge mini-skip">{m_skipped} skipped</span>\n'

        html += f"""      </div>
    </div>
    <div class="test-list">
"""
        # Sort: failed first, then skipped, then passed
        sort_order = {'FAIL': 0, 'SKIP': 1, 'PASS': 2}
        sorted_tests = sorted(tests, key=lambda t: (sort_order.get(t['status'], 3), t['name']))

        for t in sorted_tests:
            display_name = t['description'] if t['description'] else t['name']
            status_lower = t['status'].lower()
            badge_class = f'badge-{status_lower}' if status_lower in ('pass', 'fail', 'skip') else 'badge-pass'

            # Format duration
            dur_ms = t['duration_ms']
            if dur_ms >= 60000:
                dur_str = f"{dur_ms // 60000}m {(dur_ms % 60000) // 1000}s"
            elif dur_ms >= 1000:
                dur_str = f"{dur_ms // 1000}s"
            else:
                dur_str = f"{dur_ms}ms"

            html += f"""      <div class="test-row">
        <span class="test-name">{escape_html(display_name)}</span>
        <span class="test-duration">{dur_str}</span>
        <span class="badge {badge_class}">{t['status']}</span>
      </div>
"""

        html += """    </div>
  </div>
"""

    # GitHub Actions link
    gh_link = ''
    server_url = os.environ.get('GITHUB_SERVER_URL', '')
    repo = os.environ.get('GITHUB_REPOSITORY', '')
    run_id = os.environ.get('GITHUB_RUN_ID', '')
    if server_url and repo and run_id:
        actions_url = f"{server_url}/{repo}/actions/runs/{run_id}"
        gh_link = f' &bull; <a href="{actions_url}">View on GitHub Actions</a>'

    html += f"""</div>

<!-- Footer -->
<div class="footer">
  Generated by <strong>eGalvanic Web Automation Framework</strong> &bull; {date_str}{gh_link}
</div>

</body>
</html>"""

    return html, {'total': total, 'passed': passed, 'failed': failed, 'skipped': skipped, 'pass_rate': pass_rate}


def escape_html(text):
    """Escape HTML special characters."""
    return (text
            .replace('&', '&amp;')
            .replace('<', '&lt;')
            .replace('>', '&gt;')
            .replace('"', '&quot;')
            .replace("'", '&#x27;'))


# ============================================================
# Email Sending
# ============================================================

def send_email(report_path, stats):
    """Send a single email with the consolidated report attached."""
    send_enabled = os.environ.get('SEND_EMAIL_ENABLED', 'false').lower()
    if send_enabled != 'true':
        print("[Email] SEND_EMAIL_ENABLED is not 'true' — skipping email")
        return

    email_from = os.environ.get('EMAIL_FROM', 'abhiyant.singh@egalvanic.com')
    email_password = os.environ.get('EMAIL_PASSWORD', '')
    email_to = os.environ.get('EMAIL_TO', 'dharmesh.avaiya@egalvanic.com, mukul@egalvanic.com, abhiyant.singh@egalvanic.com')
    smtp_host = os.environ.get('SMTP_HOST', 'smtp.gmail.com')
    smtp_port = int(os.environ.get('SMTP_PORT', '587'))

    if not email_password:
        print("[Email] EMAIL_PASSWORD not set — skipping email")
        return

    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    subject = f"eGalvanic Web Automation - Test Report - {timestamp}"

    # Build email
    msg = MIMEMultipart()
    msg['From'] = email_from
    msg['To'] = email_to
    msg['Subject'] = subject

    # HTML body
    body_html = build_email_body(stats, timestamp)
    msg.attach(MIMEText(body_html, 'html', 'utf-8'))

    # Attach the consolidated report
    if os.path.exists(report_path):
        with open(report_path, 'rb') as f:
            part = MIMEBase('text', 'html')
            part.set_payload(f.read())
            encoders.encode_base64(part)
            report_filename = os.path.basename(report_path)
            part.add_header('Content-Disposition', f'attachment; filename="{report_filename}"')
            msg.attach(part)
            print(f"[Email] Attached: {report_filename}")

    # Send
    try:
        print(f"[Email] Connecting to {smtp_host}:{smtp_port}...")
        context = ssl.create_default_context()
        with smtplib.SMTP(smtp_host, smtp_port, timeout=15) as server:
            server.starttls(context=context)
            server.login(email_from, email_password)
            recipients = [r.strip() for r in email_to.split(',') if r.strip()]
            server.sendmail(email_from, recipients, msg.as_string())
        print(f"[Email] Report sent to: {email_to}")
    except Exception as e:
        print(f"[Email] Failed to send: {e}")


def build_email_body(stats, timestamp):
    """Build HTML email body matching the eGalvanic format."""
    date_str = datetime.now().strftime('%B %d, %Y %H:%M')
    plat = f"{platform.system()} {platform.machine()}"

    # Status banner color
    if stats['failed'] > 0:
        banner_color = '#dc3545'
        status_text = f"{stats['failed']} FAILED"
    else:
        banner_color = '#28a745'
        status_text = 'ALL PASSED'

    html = f"""<!DOCTYPE html><html><head><meta charset="utf-8"></head>
<body style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;background:#f5f6fa;padding:20px;margin:0;">
<div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;">

<!-- Header -->
<div style="background:#2c3e50;color:#fff;padding:24px 30px;">
  <h2 style="margin:0;font-size:20px;font-weight:600;">eGalvanic Web Automation - Test Report</h2>
  <p style="margin:4px 0 0;font-size:13px;color:#bdc3c7;">Consolidated Parallel CI Report</p>
</div>

<!-- Status Banner -->
<div style="background:{banner_color};color:#fff;padding:14px 30px;font-size:16px;font-weight:600;text-align:center;">
  {status_text} &mdash; {stats['passed']}/{stats['total']} tests passed ({stats['pass_rate']:.1f}%)
</div>

<!-- Info Table -->
<div style="padding:24px 30px;">
  <table style="width:100%;border-collapse:collapse;margin-bottom:20px;">
    <tr>
      <td style="padding:6px 0;color:#666;font-size:13px;width:130px;"><strong>Execution Date:</strong></td>
      <td style="padding:6px 0;color:#333;font-size:13px;">{date_str}</td>
    </tr>
    <tr>
      <td style="padding:6px 0;color:#666;font-size:13px;"><strong>Total Tests:</strong></td>
      <td style="padding:6px 0;color:#333;font-size:13px;">{stats['total']}</td>
    </tr>
    <tr>
      <td style="padding:6px 0;color:#666;font-size:13px;"><strong>Passed:</strong></td>
      <td style="padding:6px 0;color:#155724;font-size:13px;font-weight:600;">{stats['passed']}</td>
    </tr>
    <tr>
      <td style="padding:6px 0;color:#666;font-size:13px;"><strong>Failed:</strong></td>
      <td style="padding:6px 0;color:#721c24;font-size:13px;font-weight:600;">{stats['failed']}</td>
    </tr>
    <tr>
      <td style="padding:6px 0;color:#666;font-size:13px;"><strong>Skipped:</strong></td>
      <td style="padding:6px 0;color:#856404;font-size:13px;font-weight:600;">{stats['skipped']}</td>
    </tr>
    <tr>
      <td style="padding:6px 0;color:#666;font-size:13px;"><strong>Platform:</strong></td>
      <td style="padding:6px 0;color:#333;font-size:13px;">{plat}</td>
    </tr>
  </table>

  <p style="color:#333;font-size:14px;margin:20px 0 8px;font-weight:500;">
    Please find the attached consolidated test report:</p>
  <ul style="color:#555;font-size:13px;margin:0 0 10px 0;padding-left:20px;line-height:1.8;">
    <li><strong>Consolidated Client Report</strong> (single HTML file covering all modules)</li>
  </ul>
"""

    # GitHub Actions link
    server_url = os.environ.get('GITHUB_SERVER_URL', '')
    repo = os.environ.get('GITHUB_REPOSITORY', '')
    run_id = os.environ.get('GITHUB_RUN_ID', '')
    if server_url and repo and run_id:
        url = f"{server_url}/{repo}/actions/runs/{run_id}"
        html += f"""  <p style="margin:10px 0 0;">
    <a href="{url}" style="display:inline-block;background:#007bff;color:#fff;padding:10px 20px;border-radius:5px;text-decoration:none;font-size:13px;font-weight:500;">
      View Full Run on GitHub Actions</a></p>
"""

    html += """</div>

<!-- Footer -->
<div style="background:#f8f9fa;padding:16px 30px;border-top:1px solid #e9ecef;font-size:11px;color:#999;text-align:center;">
  Sent by <strong>eGalvanic Web Automation Framework</strong>
</div>

</div></body></html>"""

    return html


# ============================================================
# Main
# ============================================================

def main():
    if len(sys.argv) < 3:
        print("Usage: consolidated-report.py <results-dir> <output-path>")
        print("  results-dir: Directory to search for testng-results.xml files")
        print("  output-path: Path for the consolidated HTML report")
        sys.exit(1)

    results_dir = sys.argv[1]
    output_path = sys.argv[2]

    print("=" * 60)
    print("  Consolidated Client Report Generator")
    print("=" * 60)
    print(f"  Results dir: {results_dir}")
    print(f"  Output path: {output_path}")
    print()

    # 1. Find and parse all testng-results.xml
    all_tests = find_and_parse_all(results_dir)

    if not all_tests:
        print("\nERROR: No test results found. Cannot generate report.")
        sys.exit(1)

    # 2. Group by module
    modules = group_by_module(all_tests)

    print(f"\nParsed {len(all_tests)} tests across {len(modules)} modules:")
    for mod, tests in modules.items():
        p = sum(1 for t in tests if t['status'] == 'PASS')
        f = sum(1 for t in tests if t['status'] == 'FAIL')
        s = sum(1 for t in tests if t['status'] == 'SKIP')
        icon = "PASS" if f == 0 else "FAIL"
        print(f"  [{icon}] {mod}: {p} passed, {f} failed, {s} skipped")

    # 3. Generate HTML report
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    html, stats = generate_html(modules, timestamp)

    # Write output
    os.makedirs(os.path.dirname(output_path) or '.', exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html)
    print(f"\nConsolidated report written: {output_path}")
    print(f"  Total: {stats['total']} | Passed: {stats['passed']} | Failed: {stats['failed']} | Skipped: {stats['skipped']}")
    print(f"  Pass rate: {stats['pass_rate']:.1f}%")

    # 4. Send email
    send_email(output_path, stats)

    # 5. Write stats to GITHUB_OUTPUT for downstream steps
    github_output = os.environ.get('GITHUB_OUTPUT', '')
    if github_output:
        with open(github_output, 'a') as f:
            f.write(f"report_path={output_path}\n")
            f.write(f"total_tests={stats['total']}\n")
            f.write(f"total_passed={stats['passed']}\n")
            f.write(f"total_failed={stats['failed']}\n")
            f.write(f"total_skipped={stats['skipped']}\n")
            f.write(f"pass_rate={stats['pass_rate']:.1f}\n")

    print("\nDone.")


if __name__ == '__main__':
    main()
