# ZP-4834 — companies.data_epoch returned by /auth/v2/me (task #172)

**Verdict:** PASS for everything the web/API can show; the demo-reset rows are NOT WEB-TESTABLE.
QA (https://acme.qa.egalvanic.ai), build `index-C1ZRIunF.js` (checked 16:02 and again 16:26 IST, 12 Nov 2026, no change).

**Recommended status:** READY TO RELEASE. The field is live and correct for every seat. The reset bump, "real tenant never bumped" and the
idempotent migration are server-side and need a developer check on dev (the iOS drop is ZP-4829).

## Checked
| Check | How | Result |
|---|---|---|
| `/auth/v2/me` returns `company_data_epoch` for the acting company | SA seat, `GET /api/auth/v2/me` in the browser (video step 2, shot 1) | PASS: `"company_data_epoch": 0`, a number, next to `company_id` d59d449b… (EG-ACME) |
| All roles | Signed in as SA, ADMIN, PM, AM, EE, FM, CP, PS, TECH; same call | PASS: 9/9 return HTTP 200 with `company_data_epoch = 0` (number) and company EG-ACME. That includes PS and TECH, which have no web access (`has_web_access:false`) but still get their profile (shots 2–3, video card) |
| Stable value | Called twice in a row (SA) | PASS: 0 both times. Real tenants are never bumped, which fits |
| Unknown company | SA + `X-Subdomain: zz-not-a-company-qa` | PASS: 401 `No authorization provided`, no data |
| v1 profile | `GET /api/auth/me` | Has no `company_data_epoch` (22 keys). Expected: the ticket names v2/me only |
| Performance | `/auth/v2/me` timing and size | 356–784 ms warm (1.45 s on the first call after sign-in); 3–14 KB depending on role |
| Web client | Bundle `index-C1ZRIunF.js` | The web app does not read the field. That is expected: only iOS drops local data (ZP-4829) |
| Demo reset bumps the epoch / no-op without the column | Needs `admin-scripts demo_reset.py` | NOT WEB-TESTABLE. Also, the brief excludes the demo tenant |
| Real tenant never bumped | Only "stays 0 today" is visible on EG-ACME | Partly seen (0 on every seat) |
| Migration `epoch_a1` idempotent / single head | DB migration | NOT WEB-TESTABLE |
| Flags | No feature flag gates this field | n/a |

## Bugs found
None.

## Enhancement suggestions
- Also return `company_data_epoch` from the v1 `/api/auth/me` (or mark v1 deprecated). Any older client that still calls v1 cannot see a reset.

## Mobile impact
iOS tester (ZP-4829): on QA every EG-ACME seat reads epoch 0. A drop-local-data test needs a demo reset that bumps the epoch, which only
a developer can trigger. Check that the app does **not** wipe local data while the value stays 0.

## Not covered
- Demo reset bump, gating when the column is absent, migration idempotency: server/admin-script only.
- A second company: the only other QA login is the demo tenant, which the brief excludes, and the staff seat cannot act as another
  company through the profile call (an unknown subdomain gives 401).

## Evidence
- `QA-12Nov-ZP-4834-video-steps.mp4` (45 s): sign-in → `/api/auth/v2/me` with the field boxed → 9-seat sweep card.
- `QA-12Nov-ZP-4834-1-auth-v2-me-company-data-epoch.png` (SA), `-2-project-manager-epoch.png` (PM),
  `-3-technician-no-web-access-epoch.png` (TECH). Personal fields are hidden in the view; the orange box marks the field (PASS).
- Raw sweep: `.playwright-mcp/v26/eric7/r03-out.json` (`me` block).
