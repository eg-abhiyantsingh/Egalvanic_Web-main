#!/usr/bin/env python3
"""
Regression Signal Board — rebuild from real CI data.

The dashboard shows the FULL automation suite ("Parallel Full Suite — Core Regression"),
partitioned by run date. Data is the *consolidated client report (after-rerun)* that each
full-suite CI run already produces — nothing is estimated; figures are the run's own results.

Refresh workflow:
  1. Download the consolidated client report from each full-suite run you want to show:
       gh run list --workflow=parallel-suite.yml --limit 40           # find run IDs + dates
       gh run download <RUN_ID> -n consolidated-client-report-after-rerun -D reports/<YYYY-MM-DD>
     (fall back to -n consolidated-client-report if a run had no rerun)
  2. python3 build_dashboard.py reports/            # parses every reports/<date>/*.html
  3. Outputs dashboard-data.json + index.html (self-contained; open or publish).

Artifacts expire (~90d on GitHub), so older dates drop off automatically as they age out.
"""
import re, json, glob, os, sys, html as H

def clean(s): return H.unescape(re.sub(r'<[^>]+>','',s)).strip()

def parse_report(path):
    raw=open(path,encoding="utf-8",errors="replace").read()
    idxs=[m.start() for m in re.finditer(r'<div class="module (?:module-|collapsed)', raw)]+[len(raw)]
    modules=[]
    for i in range(len(idxs)-1):
        blk=raw[idxs[i]:idxs[i+1]]
        nm=re.search(r'module-name">\s*(?:<span class="toggle">[^<]*</span>)?\s*([^<]+)', blk)
        name=clean(nm.group(1)) if nm else "Unknown"
        tests=[]
        for tb in re.finditer(r'<span class="test-name">(.*?)</span>\s*<span class="test-duration">(.*?)</span>\s*<span class="badge badge-(\w+)">', blk, re.S):
            tests.append({"name":clean(tb.group(1)),"duration":clean(tb.group(2)),"status":tb.group(3).upper()})
        p=sum(1 for t in tests if t["status"]=="PASS"); f=sum(1 for t in tests if t["status"]=="FAIL"); s=sum(1 for t in tests if t["status"]=="SKIP")
        tot=len(tests)
        if not tot: continue
        modules.append({"name":name,"passed":p,"failed":f,"skipped":s,"total":tot,
            "pass_rate":round(100*p/tot,1),
            "status":"issues" if f else ("skips" if s and not p else "clean"),
            "tests":tests,"failures":[{"name":t["name"],"duration":t["duration"]} for t in tests if t["status"]=="FAIL"]})
    return modules

def build(reports_dir):
    runs=[]
    for d in sorted(glob.glob(os.path.join(reports_dir,"2026-*"))+glob.glob(os.path.join(reports_dir,"20*-*-*"))):
        if not os.path.isdir(d): continue
        date=os.path.basename(d)
        html=[h for h in sorted(glob.glob(f"{d}/*.html")) if "Consolidated" in h] or sorted(glob.glob(f"{d}/*.html"))
        if not html: continue
        runid=open(f"{d}/.runid").read().strip() if os.path.exists(f"{d}/.runid") else ""
        src=open(f"{d}/.src").read().strip() if os.path.exists(f"{d}/.src") else "after-rerun"
        mods=parse_report(html[0])
        if not mods: continue
        T=sum(m["total"] for m in mods); P=sum(m["passed"] for m in mods)
        F=sum(m["failed"] for m in mods); S=sum(m["skipped"] for m in mods)
        runs.append({"date":date,"run_id":runid,"source":src,"modules":mods,
            "summary":{"total":T,"passed":P,"failed":F,"skipped":S,"pass_rate":round(100*P/T,1) if T else 0,
                "modules_total":len(mods),"modules_clean":sum(1 for m in mods if not m["failed"]),
                "modules_with_issues":sum(1 for m in mods if m["failed"])}})
    return {"product":"eGalvanic Web","suite":"Parallel Full Suite — Core Regression (848 TCs)",
            "source":"GitHub Actions consolidated client report (after-rerun)","runs":runs}

if __name__=="__main__":
    rd=sys.argv[1] if len(sys.argv)>1 else "reports"
    data=build(rd)
    json.dump(data,open("dashboard-data.json","w"),indent=1)
    blob=json.dumps(data,separators=(",",":")).replace("</","<\\/")
    tpl=open("_template.html",encoding="utf-8").read()
    open("index.html","w",encoding="utf-8").write(tpl.replace("__DATA__",blob))
    print(f"built {len(data['runs'])} runs -> index.html ({os.path.getsize('index.html')} bytes)")
