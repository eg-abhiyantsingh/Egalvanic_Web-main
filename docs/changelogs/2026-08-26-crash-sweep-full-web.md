# Full-web crash sweep — V1.36

**Date:** 2026-08-26 · **Prompt:** "check and find any other major issue incase if we miss in full web
related to crash but find only major bugs"

**Result: no new major crash bugs found.** Recorded so nobody repeats this sweep.

## Instrumentation
Captured on every step: `pageerror` (uncaught exceptions), console errors matching
`uncaught|unhandled|cannot read|undefined is not|is not a function|maximum update depth|removeChild|rejection`,
React error-boundary text (`Application Error` / `Something went wrong` / `Unexpected token` /
`is not valid JSON`), blank `main` (<15 chars), and any API `5xx`.
Third-party noise (DevRev `plug.js`, Beamer, extension content scripts) filtered out.

## Coverage
| Surface | What was driven |
|---|---|
| Core routes | 12 loaded, **17 action dialogs** opened and closed (Create Asset/Issue/EMP/WO/Connection, Upload Anything, Bulk Edit, SKM, Bulk Upload, Add to WO, Add to Quote, Add Tasks, Upload Attachment, Add Test Equipment, Bulk Ops, AI Setup) |
| Detail records | **9 real records** opened from their lists + **24 tabs** clicked (Asset 8, WO 6, Issue 4, Customer 6) |
| Heavy routes | 19 loaded incl. `/slds`, `/arc-flash`, `/reporting/builder`, `/eg-forms`, `/services`, `/pm-plans`, `/classes`, `/admin/audit-log`, `/legacy-*` + **18 tabs** |
| SLD editor | GoJS canvas entered — renders (1 canvas, diagram present, 11 data issues listed) |
| Create WO wizard | New 4-step wizard walked **forward through Scope → Team → Review**, then **Back ×3**, then cancelled |
| Race conditions | 8 routes navigated with no wait (`waitUntil: commit`, 320 ms apart); back ×5 / forward ×5 spam |
| Hostile input | 6 payloads into Assets search: `<script>alert(1)</script>`, `' OR 1=1--`, 600-char string, emoji, `%00%0d%0a`, `../../etc/passwd` |
| Large pages | 100/page on `/assets`, `/sessions`, `/connections` |

## Findings
```
uncaught exceptions   0
error boundaries      0
blank shells          0
API 5xx               0
```

The build is **crash-stable** under every interaction driven above, including the newest code paths
(the 4-step Create WO wizard and the Maintenance section).

## Deliberately NOT exercised
- **Destructive actions** — no Delete / Remove / Archive clicked anywhere.
- **Writes** — no record created; the wizard was cancelled at Review, dialogs closed via Cancel.
- File uploads, session-expiry behaviour, multi-user concurrency.

These are the remaining places a crash could still hide; they need either write authorisation or a
second session.

## Standing issues (unchanged by this sweep)
The three filed bugs remain open — Technician web access, Facility Manager cannot reach Customers,
and Maintenance routes ungated + missing site picker. None of them is a crash.
