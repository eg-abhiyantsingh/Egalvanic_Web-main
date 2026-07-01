#!/usr/bin/env python3
"""
Live probe for the eGalvanic API contract — zero third-party deps (stdlib only).

Checks one endpoint for: records + shape, pagination (page/per_page honored), max page-size cap,
default page size, filter/search safety, response time, and baseline auth (no-token => 401/403).

Usage:
  python3 probe_endpoint.py --base https://acme.dev.egalvanic.ai/api --path /node_classes --token "$DEV_TOKEN"
  EG_TOKEN=... python3 probe_endpoint.py --base .../api --path "/company/{id}/opportunities"

Read-only: issues GETs only. Never point --base at production. Exit code 1 if any FAIL, else 0.
"""
import argparse, json, os, ssl, sys, time, urllib.request, urllib.error
from urllib.parse import urlparse

MAX_ALLOWED = 100      # max page size the contract allows (50–100)
DEFAULT_IDEAL = 20     # preferred default page size (10–20)
SLOW_MS = 2000         # "few seconds" target
HARD_MS = 8000         # genuinely broken

# TLS is VERIFIED by default. dev/QA hosts that use a self-signed / internal-CA cert can opt out with
# --insecure (prefer adding the internal CA to your trust store instead). Never use --insecure vs prod.
CTX = ssl.create_default_context()

def base_rejection(base):
    """Return a reason string if this base URL must NOT be probed, else None (fail closed on parse errors).
    Guards against production egalvanic hosts using hostname-suffix matching (not substring), rejects
    userinfo, and requires https for egalvanic hosts. Non-egalvanic hosts (localhost, other dev) are allowed."""
    try:
        u = urlparse(base)
    except Exception as e:
        return f"unparseable base URL ({e})"
    if not u.hostname:
        return "no hostname in base URL"
    if u.username or u.password or "@" in (u.netloc or ""):
        return "base URL must not contain userinfo (user:pass@host)"
    host = u.hostname.lower()
    if host == "egalvanic.ai" or host.endswith(".egalvanic.ai"):
        allowed = (".dev.egalvanic.ai", ".qa.egalvanic.ai", ".stage.egalvanic.ai")
        if not any(host.endswith(s) for s in allowed):
            return f"'{host}' looks like PRODUCTION (or an unknown egalvanic host) — use a dev/qa/stage host"
        if u.scheme != "https":
            return f"'{host}' must be probed over https"
    return None

def _join(path, params):
    if not params: return path
    sep = "&" if "?" in path else "?"
    return path + sep + "&".join(f"{k}={v}" for k, v in params.items())

def get(base, path, token, params=None, timeout=25):
    url = base.rstrip("/") + _join(path, params or {})
    req = urllib.request.Request(url, method="GET")
    req.add_header("Accept", "application/json")
    if token: req.add_header("Authorization", "Bearer " + token)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=CTX) as r:
            body = r.read().decode("utf-8", "replace")
            return dict(status=r.status, ms=int((time.time()-t0)*1000), body=body)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace") if e.fp else ""
        return dict(status=e.code, ms=int((time.time()-t0)*1000), body=body)
    except Exception as e:
        return dict(status=0, ms=int((time.time()-t0)*1000), body="", error=str(e)[:80])

def shape_count(body):
    b = (body or "").strip()
    if b.startswith("["):
        try: return "array", len(json.loads(b)), None
        except Exception: return "array?", None, None
    if b.startswith("{"):
        try:
            d = json.loads(b)
        except Exception:
            return "object?", None, None
        total = next((d[k] for k in ("total","count","total_count") if isinstance(d.get(k), int)), None)
        for k, v in d.items():
            if isinstance(v, list): return "wrapped", len(v), total
        return "object", None, total
    if b.startswith("<"): return "html", None, None
    return "other", None, None

RESULTS = []
def rec(check, verdict, detail):
    RESULTS.append((verdict, check, detail))
    icon = {"PASS":"✓","WARN":"⚠","FAIL":"✗","INFO":"·"}.get(verdict,"?")
    print(f"  [{icon} {verdict:4}] {check:22} {detail}")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True, help="API base, e.g. https://acme.dev.egalvanic.ai/api")
    ap.add_argument("--path", required=True, help="endpoint path, e.g. /node_classes")
    ap.add_argument("--token", default=os.environ.get("EG_TOKEN",""), help="bearer token (or EG_TOKEN env)")
    ap.add_argument("--insecure", action="store_true", help="skip TLS verification (self-signed dev/QA certs only; never prod)")
    a = ap.parse_args()
    if a.insecure:
        CTX.check_hostname = False; CTX.verify_mode = ssl.CERT_NONE
        print("WARNING: TLS verification disabled (--insecure). Use only against a trusted dev/QA host.\n", file=sys.stderr)
    reason = base_rejection(a.base)
    if reason:
        print(f"Refusing to probe: {reason}.", file=sys.stderr); sys.exit(2)
    if not a.token:
        print("WARNING: no --token/EG_TOKEN; authed checks will be limited.\n")

    print(f"API contract probe · {a.base}{a.path}\n")

    base_r = get(a.base, a.path, a.token)
    if base_r["status"] == 0:
        rec("reachable","FAIL", base_r.get("error","connection failed")); return finish()
    shape, count, total = shape_count(base_r["body"])
    is_json_list = shape in ("array","wrapped")
    coll = total if isinstance(total,int) else (count if isinstance(count,int) else None)

    # 1. records & shape
    if base_r["status"] == 200 and shape in ("array","wrapped","object"):
        rec("records", "PASS", f"HTTP 200, shape={shape}" + (f", {count} items" if count is not None else ""))
    elif base_r["status"] == 200 and shape == "html":
        rec("records", "WARN", "HTTP 200 but HTML (not a JSON API? or auth redirect)")
    else:
        rec("records", "WARN", f"HTTP {base_r['status']}, shape={shape}")
    if shape == "array":
        rec("shape", "INFO", "bare array — prefer {success, data} envelope")

    # 2. response time
    ms = base_r["ms"]
    rec("response_time", "FAIL" if ms > HARD_MS else ("WARN" if ms > SLOW_MS else "PASS"), f"{ms} ms")

    if is_json_list:
        # 3. pagination honored (per_page=1, then limit=1)
        if coll and coll >= 2:
            p1 = get(a.base, a.path, a.token, {"page":1,"per_page":1})
            _, c1, _ = shape_count(p1["body"])
            if c1 == 1:
                rec("pagination", "PASS", "per_page honored (per_page=1 → 1 item)")
            else:
                l1 = get(a.base, a.path, a.token, {"page":1,"limit":1})
                _, cl, _ = shape_count(l1["body"])
                if cl == 1: rec("pagination", "PASS", "limit honored (limit=1 → 1 item)")
                else: rec("pagination", "FAIL", f"per_page/limit ignored — still returns {c1 if c1 is not None else '?'}")
        else:
            rec("pagination", "INFO", f"collection too small to tell ({coll} items)")

        # 4. default page size
        if count is not None:
            if count <= DEFAULT_IDEAL: rec("default_size", "PASS", f"{count} items (≤ {DEFAULT_IDEAL})")
            elif coll and coll > MAX_ALLOWED and count >= coll: rec("default_size", "FAIL", f"unbounded — returns all {count}")
            else: rec("default_size", "WARN", f"{count} items (> ideal {DEFAULT_IDEAL})")

        # 5. max page-size cap
        if coll and coll > MAX_ALLOWED:
            big = get(a.base, a.path, a.token, {"page":1,"per_page":1000})
            _, cb, _ = shape_count(big["body"])
            rec("max_limit", "PASS" if (cb is not None and cb <= MAX_ALLOWED) else "FAIL",
                f"per_page=1000 → {cb} items (cap {MAX_ALLOWED})")
        else:
            rec("max_limit", "INFO", f"collection ≤ {MAX_ALLOWED}; cap not exercisable")

    # 6. filter/search safety
    f = get(a.base, a.path, a.token, {"search":"zzq_probe_zzq"})
    if f["status"] >= 500: rec("filter_safety", "FAIL", f"search param → HTTP {f['status']} (server error)")
    else: rec("filter_safety", "PASS", f"search param → HTTP {f['status']} (no 5xx)")

    # 7. auth required (no token → 401/403)
    if a.token:
        na = get(a.base, a.path, None)
        ns, nc, _ = shape_count(na["body"])
        if na["status"] in (401,403): rec("auth_required", "PASS", f"no token → HTTP {na['status']}")
        elif na["status"] == 200 and ns in ("array","wrapped","object"): rec("auth_required", "FAIL", "no token → 200 with JSON data (endpoint is OPEN)")
        elif na["status"] == 200: rec("auth_required", "WARN", "no token → 200 (HTML/redirect? confirm not leaking data)")
        else: rec("auth_required", "INFO", f"no token → HTTP {na['status']}")

    finish()

def finish():
    n_fail = sum(1 for v,_,_ in RESULTS if v=="FAIL")
    n_warn = sum(1 for v,_,_ in RESULTS if v=="WARN")
    print(f"\nResult: {n_fail} FAIL · {n_warn} WARN · {sum(1 for v,_,_ in RESULTS if v=='PASS')} PASS")
    sys.exit(1 if n_fail else 0)

if __name__ == "__main__":
    main()
