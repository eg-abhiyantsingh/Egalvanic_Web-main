#!/usr/bin/env python3
"""
test-customer-bug-report.py — regression self-test for customer-bug-report.py.

customer-bug-report.py is load-bearing: its PDF goes to customers, and it runs in
every test workflow. This test builds SYNTHETIC fixtures that mimic the real CI
artifact layout (testng-results.xml + ExtentSpark detail HTML with an inline
base64 screenshot + a *_FAIL_*.png on disk), runs the generator against them, and
asserts on the machine-readable Customer_Bug_Report.json.

It encodes the four defects found during the 2026-08-10 validation against real
run 31233370537, so they cannot silently come back:
  1. "tried for 20 second(s) with 500 milliseconds interval" must NOT read as a
     server 5xx and inflate severity to High.
  2. Java lambda / object refs (Foo$$Lambda$636/0x...@57b7) must never reach
     customer-visible text.
  3. Data-driven variants must not produce duplicate bug titles.
  4. Array refs ([Ljava.lang.String;@a22c4d8) must not leak into titles.

Run:
  python3 .github/scripts/test-customer-bug-report.py
Exit code 0 = all assertions passed. PDF/DOCX rendering is exercised only when
reportlab/python-docx are installed; the JSON assertions always run.
"""

import base64
import json
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
GENERATOR = os.path.join(HERE, "customer-bug-report.py")

# 1x1 red JPEG — smallest thing Pillow will happily decode and re-encode.
TINY_JPEG_B64 = (
    "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRof"
    "Hh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAAB"
    "AAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q=="
)


def testng_xml(methods):
    """methods: list of dicts(cls, name, status, desc, exc_class, msg, params)."""
    by_cls = {}
    for m in methods:
        by_cls.setdefault(m["cls"], []).append(m)
    parts = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<testng-results skipped="0" failed="1" total="1" passed="0">',
             '  <suite name="Synthetic">', '    <test name="Synthetic">']
    for cls, ms in by_cls.items():
        parts.append(f'      <class name="{cls}">')
        for m in ms:
            parts.append(
                f'        <test-method status="{m["status"]}" signature="{m["name"]}()" '
                f'name="{m["name"]}" description="{m.get("desc", "")}" '
                f'duration-ms="1234" started-at="2026-08-10T01:00:00 UTC" '
                f'finished-at="2026-08-10T01:00:12 UTC" is-config="{str(m.get("config", False)).lower()}">')
            if m.get("params"):
                parts.append("          <params>")
                for i, p in enumerate(m["params"]):
                    parts.append(f'            <param index="{i}"><value>'
                                 f'<![CDATA[{p}]]></value></param>')
                parts.append("          </params>")
            if m["status"] == "FAIL":
                parts.append(f'          <exception class="{m["exc_class"]}">')
                parts.append(f'            <message><![CDATA[{m["msg"]}]]></message>')
                parts.append(f'            <full-stacktrace><![CDATA[{m["exc_class"]}: '
                             f'{m["msg"]}\n\tat {cls}.{m["name"]}({cls.rsplit(".", 1)[-1]}.java:42)'
                             f']]></full-stacktrace>')
                parts.append("          </exception>")
            parts.append("        </test-method>")
        parts.append("      </class>")
    parts += ["    </test>", "  </suite>", "</testng-results>"]
    return "\n".join(parts)


def extent_item(display_name, steps, fail_text, stack, with_image=True):
    rows = []
    for t in steps:
        rows.append(f"""      <tr class="event-row">
        <td><span class="badge log info-bg">Info</span></td>
        <td>01:00:01 am</td>
        <td>{t}</td>
      </tr>""")
    img = (f"<img src='data:image/jpeg;base64,{TINY_JPEG_B64}' alt='failure screenshot'/>"
           if with_image else "")
    rows.append(f"""      <tr class="event-row">
        <td><span class="badge log fail-bg">Fail</span></td>
        <td>01:00:12 am</td>
        <td><div>{fail_text}</div>{img}</td>
      </tr>""")
    rows.append(f"""      <tr class="event-row">
        <td><span class="badge log fail-bg">Fail</span></td>
        <td>01:00:12 am</td>
        <td><textarea readonly class="code-block">{stack}</textarea></td>
      </tr>""")
    rows_html = "\n".join(rows)
    return f"""<li class="test-item"  status="fail" test-id="1" author="" tag="Synthetic" device="">
  <div class="test-detail"><p class="name">{display_name}</p></div>
  <div class="test-contents d-none"><div class="detail-body mt-4">
  <table class="table table-sm"><tbody>
{rows_html}
  </tbody></table>
  </div></div>
</li>"""


# A real ExtentSpark report inlines its whole CSS/JS bundle, so it is never smaller
# than ~270 KB (measured range in this repo: 276 KB - 6.4 MB). The generator therefore
# treats anything under 2 KB as a corrupt/placeholder file and skips it. The fixture
# reproduces that shape with a representative inline bundle so the size guard sees a
# realistic document instead of a stub.
SPARK_INLINE_BUNDLE = """<style>
  .test-wrapper{display:flex}.test-list{width:395px}.test-item{cursor:pointer}
  .test-contents.d-none{display:none}.detail-body{overflow:auto}
  .badge.log.info-bg{background:#3498db}.badge.log.fail-bg{background:#e74c3c}
  .code-block{width:100%;font-family:monospace;font-size:11px}
</style>
<script>function toggleView(v){/* spark app shell */}</script>
<!-- """ + ("spark-bundle-padding " * 120) + " -->"


def extent_html(items):
    return ("<html><head><meta charset='utf-8'/>" + SPARK_INLINE_BUNDLE +
            "</head><body><div class='test-wrapper'><div class='test-list'>"
            "<ul class='test-list-item'>" + "\n".join(items) +
            "</ul></div></div></body></html>")


def build_fixture(root):
    """CI-like layout: <root>/reports-<group>/reports/{groups,detail-report} + screenshots."""
    grp = os.path.join(root, "reports-synthetic")
    results = os.path.join(grp, "reports", "groups", "group-1-synthetic")
    detail = os.path.join(grp, "reports", "detail-report")
    shots = os.path.join(grp, "test-output", "screenshots")
    for d in (results, detail, shots):
        os.makedirs(d, exist_ok=True)

    methods = [
        # 1. plain assertion WITH extent evidence (real steps + screenshot)
        dict(cls="com.egalvanic.qa.testcase.WorkOrderTestNG", name="testTC_WO_001_CreatePlan",
             status="FAIL", desc="TC-WO-001: plan survives cancelled delete",
             exc_class="java.lang.AssertionError",
             msg="Plan should remain after cancelling delete expected [true] but found [false]"),
        # 2. DEFECT-1 guard: Selenium timeout text contains "500 milliseconds" -> must stay Medium
        dict(cls="com.egalvanic.qa.testcase.OpportunitiesTestNG", name="testTC_OPP_39_OfflineCreate",
             status="FAIL", desc="TC_OPP_39: create failure surfaces an error",
             exc_class="org.openqa.selenium.TimeoutException",
             msg=("Expected condition failed: waiting for "
                  "com.egalvanic.qa.pageobjects.OpportunitiesPage$$Lambda$636/0x00007f8f102ae860@57b75756 "
                  "(tried for 20 second(s) with 500 milliseconds interval)\n"
                  "Build info: version: '4.29.0', revision: 'abc'\n"
                  "Driver info: org.openqa.selenium.chrome.ChromeDriver")),
        # 3. genuine server error -> MUST be High
        dict(cls="com.egalvanic.qa.testcase.TaskTestNG", name="testTC_TASK_09_Create",
             status="FAIL", desc="TC-TASK-09: create task",
             exc_class="java.lang.AssertionError",
             msg="Create failed: the server responded with a status of 500 () - psycopg2.errors.UndefinedColumn"),
        # 4+5. DEFECT-3/4 guard: data-driven variants, identical message, array-ref param noise
        dict(cls="com.egalvanic.qa.testcase.AssetEngineeringExhaustiveTestNG", name="testSubtypeOffered",
             status="FAIL", desc="ENGX-SUBTYPE: subtype dropdown offers the expected subtype",
             exc_class="java.lang.AssertionError",
             msg="Asset Subtype should offer the expected option. expected [true] but found [false]",
             params=["Circuit Breaker", "[Ljava.lang.String;@a22c4d8"]),
        dict(cls="com.egalvanic.qa.testcase.AssetEngineeringExhaustiveTestNG", name="testSubtypeOffered",
             status="FAIL", desc="ENGX-SUBTYPE: subtype dropdown offers the expected option",
             exc_class="java.lang.AssertionError",
             msg="Asset Subtype should offer the expected option. expected [true] but found [false]",
             params=["Disconnect Switch", "[Ljava.lang.String;@b31d5e9"]),
        # 6. a PASSing test -> must NOT become a bug
        dict(cls="com.egalvanic.qa.testcase.WorkOrderTestNG", name="testTC_WO_002_Passes",
             status="PASS", desc="TC-WO-002: passes"),
        # 7. config (classSetup) failure -> bug, severity High, no extent evidence
        dict(cls="com.egalvanic.qa.testcase.ArcFlashE2ETestNG", name="classSetup",
             status="FAIL", desc="", config=True,
             exc_class="java.lang.AssertionError",
             msg="Site 'qa site' should be selectable expected [true] but found [false]"),
    ]
    with open(os.path.join(results, "testng-results.xml"), "w", encoding="utf-8") as fh:
        fh.write(testng_xml(methods))

    # Extent evidence for #1 only -> proves the "real steps" tier
    item = extent_item(
        "TC_WO_001 — plan survives cancelled delete",
        ["Navigate to Work Order Planning (/planning)",
         "Open the row action menu for the first plan",
         "Click Delete, then click Cancel in the confirmation dialog"],
        "Test failed: testTC_WO_001_CreatePlan",
        "java.lang.AssertionError: Plan should remain after cancelling delete\n"
        "\tat com.egalvanic.qa.testcase.WorkOrderTestNG.testTC_WO_001_CreatePlan(WorkOrderTestNG.java:42)")
    with open(os.path.join(detail, "Detailed_Report_Work_Orders_20260810_010000.html"),
              "w", encoding="utf-8") as fh:
        fh.write(extent_html([item]))

    # Screenshot fallback tier for #2 (no extent item exists for it)
    png = base64.b64decode(TINY_JPEG_B64)
    with open(os.path.join(shots, "testTC_OPP_39_OfflineCreate_FAIL_20260810_010005.png"),
              "wb") as fh:
        fh.write(png)
    return root


CHECKS = []


def check(name, condition, detail=""):
    CHECKS.append((name, bool(condition), detail))
    print(("  PASS  " if condition else "  FAIL  ") + name + (f"  [{detail}]" if detail else ""))


def main():
    tmp = tempfile.mkdtemp(prefix="cbr-selftest-")
    try:
        src = build_fixture(os.path.join(tmp, "all-reports"))
        out = os.path.join(tmp, "out")
        formats = "pdf,docx"
        try:
            import reportlab  # noqa: F401
        except ImportError:
            print("NOTE: reportlab missing -> JSON-only run (install reportlab pillow "
                  "python-docx to exercise rendering)")
            formats = ""

        cmd = [sys.executable, GENERATOR, src, out, "--title", "Self Test",
               "--run-number", "1", "--formats", formats]
        proc = subprocess.run(cmd, capture_output=True, text=True)
        print(proc.stdout.strip())
        if proc.returncode != 0:
            print(proc.stderr)
            print("\nGENERATOR CRASHED — self-test failed")
            return 1

        with open(os.path.join(out, "Customer_Bug_Report.json"), encoding="utf-8") as fh:
            report = json.load(fh)
        bugs = report["bugs"]
        by_ref = {b["test_ref"].split("#", 1)[1].split(" ")[0]: b for b in bugs}

        print("\n--- assertions ---")
        # bug set
        check("6 bugs (5 failed tests + 1 classSetup); PASSing test excluded",
              len(bugs) == 6, f"got {len(bugs)}")
        check("passing test did not become a bug",
              not any("testTC_WO_002_Passes" in b["test_ref"] for b in bugs))
        check("classSetup config failure became a bug",
              any("classSetup" in b["test_ref"] for b in bugs))

        # evidence tiers
        wo = by_ref.get("testTC_WO_001_CreatePlan", {})
        check("extent tier used for the test with a detail report",
              wo.get("evidence") == "extent", wo.get("evidence"))
        check("real executed steps extracted from the Extent report",
              any("row action menu" in s for s in wo.get("steps", [])),
              f"{len(wo.get('steps', []))} steps")
        check("inline base64 screenshot attached from the Extent report",
              wo.get("attachment_count", 0) >= 1)

        opp = by_ref.get("testTC_OPP_39_OfflineCreate", {})
        check("screenshot fallback tier found the *_FAIL_*.png on disk",
              opp.get("attachment_count", 0) >= 1, str(opp.get("attachment_count")))

        # DEFECT-1: "500 milliseconds" must not read as a server 5xx
        check("DEFECT-1 guard: Selenium timeout stays Medium (not High)",
              opp.get("severity") == "Medium", opp.get("severity"))
        # genuine 500 must still be High
        task = by_ref.get("testTC_TASK_09_Create", {})
        check("real HTTP 500 + psycopg2 is escalated to High",
              task.get("severity") == "High", task.get("severity"))
        check("classSetup failure is High severity",
              by_ref.get("classSetup", {}).get("severity") == "High")

        # DEFECT-2: java noise must never reach customer text
        blob = json.dumps([{k: v for k, v in b.items()
                            if k in ("title", "actual", "expected", "steps")} for b in bugs])
        check("DEFECT-2 guard: no Java lambda refs in customer-visible text",
              "$$Lambda$" not in blob)
        check("DEFECT-2 guard: no Selenium 'Build info:' block in customer text",
              "Build info:" not in blob)

        # DEFECT-3: duplicate titles
        titles = [b["title"] for b in bugs]
        check("DEFECT-3 guard: no duplicate bug titles",
              len(titles) == len(set(titles)),
              f"{len(titles) - len(set(titles))} dup(s)")

        # DEFECT-4: array refs in params
        check("DEFECT-4 guard: no [Ljava.lang.String;@ array refs in titles",
              not any("[L" in t and "@" in t for t in titles))

        # template completeness
        required = ("title", "severity", "priority", "preconditions", "steps",
                    "actual", "expected", "test_ref")
        missing = [f for b in bugs for f in required if not b.get(f)]
        check("every bug carries the full customer template",
              not missing, f"missing: {sorted(set(missing))}" if missing else "")
        check("every title is in '[Module] issue' form",
              all(t.startswith("[") and "]" in t for t in titles))

        # rendering
        if formats:
            pdf = os.path.join(out, "Customer_Bug_Report.pdf")
            check("PDF rendered and non-trivial",
                  os.path.exists(pdf) and os.path.getsize(pdf) > 3000,
                  f"{os.path.getsize(pdf)} bytes" if os.path.exists(pdf) else "missing")

        # re-run mode: flip one failure to PASS -> it must leave the bug list
        rerun = os.path.join(tmp, "rerun")
        os.makedirs(rerun, exist_ok=True)
        with open(os.path.join(src, "reports-synthetic", "reports", "groups",
                               "group-1-synthetic", "testng-results.xml"),
                  encoding="utf-8") as fh:
            xml = fh.read()
        xml = xml.replace('status="FAIL" signature="testTC_WO_001_CreatePlan()"',
                          'status="PASS" signature="testTC_WO_001_CreatePlan()"')
        with open(os.path.join(rerun, "testng-results.xml"), "w", encoding="utf-8") as fh:
            fh.write(xml)
        out2 = os.path.join(tmp, "out-rerun")
        proc2 = subprocess.run([sys.executable, GENERATOR, src, out2, "--title", "Self Test rerun",
                                "--rerun-results", rerun, "--formats", ""],
                               capture_output=True, text=True)
        if proc2.returncode == 0:
            with open(os.path.join(out2, "Customer_Bug_Report.json"), encoding="utf-8") as fh:
                r2 = json.load(fh)
            recovered_refs = " ".join(r2["recovered_on_rerun"])
            check("re-run mode: recovered test moved out of the bug list",
                  not any("testTC_WO_001_CreatePlan" in b["test_ref"] for b in r2["bugs"]))
            check("re-run mode: recovered test listed in the appendix",
                  "testTC_WO_001_CreatePlan" in recovered_refs, recovered_refs[:60])
            # DEFECT-5 guard: the "reproducible, not flaky" claim must be earned per test.
            # A test the re-run actually re-executed and that failed again -> True.
            # A config (setup) failure is never re-run at all -> must stay False, or the
            # emailed customer PDF asserts something that never happened.
            reran = [b for b in r2["bugs"] if "classSetup" not in b["test_ref"]]
            cfg = [b for b in r2["bugs"] if "classSetup" in b["test_ref"]]
            check("re-run mode: re-confirmed failures flagged reproducible",
                  reran and all(b["reproduced_on_rerun"] for b in reran))
            check("DEFECT-5 guard: config failures never claim 're-run confirmed'",
                  cfg and not any(b["reproduced_on_rerun"] for b in cfg))

            # DEFECT-5 guard, sharper: an EMPTY re-run dir must not mark anything
            # reproducible (the CI job can produce one when the re-run is skipped).
            empty_rerun = os.path.join(tmp, "rerun-empty")
            os.makedirs(empty_rerun, exist_ok=True)
            out3 = os.path.join(tmp, "out-rerun-empty")
            p3 = subprocess.run([sys.executable, GENERATOR, src, out3, "--title", "T",
                                 "--rerun-results", empty_rerun, "--formats", ""],
                                capture_output=True, text=True)
            if p3.returncode == 0:
                with open(os.path.join(out3, "Customer_Bug_Report.json"), encoding="utf-8") as fh:
                    r3 = json.load(fh)
                check("DEFECT-5 guard: empty re-run marks NOTHING reproducible",
                      not any(b["reproduced_on_rerun"] for b in r3["bugs"]),
                      f"{sum(b['reproduced_on_rerun'] for b in r3['bugs'])} falsely flagged")
            else:
                check("empty re-run dir handled without crashing", False, p3.stderr[-200:])
        else:
            check("re-run mode ran without crashing", False, proc2.stderr[-200:])

        # all-green input must still yield a valid empty report
        empty_in = os.path.join(tmp, "empty")
        os.makedirs(empty_in, exist_ok=True)
        proc3 = subprocess.run([sys.executable, GENERATOR, empty_in,
                                os.path.join(tmp, "out-empty"), "--formats", ""],
                               capture_output=True, text=True)
        check("all-green run produces a valid 0-bug report (no crash)",
              proc3.returncode == 0, proc3.stderr[-200:] if proc3.returncode else "")

        # DEFECT-6 guard: zero bugs because we found NO RESULTS must never be reported
        # as "all tests passed" — that would be a false all-clear to a customer.
        with open(os.path.join(tmp, "out-empty", "Customer_Bug_Report.json"),
                  encoding="utf-8") as fh:
            empty_report = json.load(fh)
        check("DEFECT-6 guard: no-evidence run is not claimed as a clean pass",
              empty_report["bugs"] == [] and "NO TEST RESULTS" in
              subprocess.run([sys.executable, GENERATOR, empty_in,
                              os.path.join(tmp, "out-empty2"), "--formats", ""],
                             capture_output=True, text=True).stdout.upper()
              or "WARNING" in proc3.stdout.upper(),
              "generator must warn when no testng-results.xml is found")

        failed = [n for n, ok, _ in CHECKS if not ok]
        print(f"\n{len(CHECKS) - len(failed)}/{len(CHECKS)} checks passed")
        if failed:
            print("FAILED CHECKS:")
            for n in failed:
                print("  -", n)
            return 1
        print("SELF-TEST PASSED")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
