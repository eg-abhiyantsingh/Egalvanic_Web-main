# ZP-4572 — stage sanity test (Maintenance Portal menu vs route)

- **Date:** 2026-10-07, 13:47–14:15 IST
- **Prompt:** "https://egalvanic.atlassian.net/browse/ZP-4572 — changes deployed on stag test sanity testing"
- **Ticket:** [ZP-4572](https://egalvanic.atlassian.net/browse/ZP-4572) "[Web] Maintenance Portal: nav shows the section to five roles the route then refuses" · Bug · High · Web v2.2.4 · In QA
- **Environment:** https://acme.stage.egalvanic.ai, build `index-aLJ0EMXb.js` (Last-Modified 07 Oct 2026 07:12 GMT, unchanged at 08:33 UTC)
- **Artifact:** https://claude.ai/artifact/SM2S3LvuCqLmxyPBVAWCmZ
- **Evidence:** `docs/bug-evidence/2026-10-07-zp4572-stage/` (52 screenshots incl. live demo `demo-1..4`, + `results.json`)
- **Jira:** QA comment [44931](https://egalvanic.atlassian.net/browse/ZP-4572?focusedCommentId=44931) with 8 inline screenshots; status left **In QA**

## Verdict
**Menu fix PASSES (option (a) of the ticket). The 2 pages the ticket named (Condition Assessment, SLD) still open by typed address: needs a decision.**

| Seat | Tier | `maintenance_portal.manage` | Menu section | 8 protected pages (typed) | `/condition` + `/sld` (typed) |
|---|---|---|---|---|---|
| Admin (staff) | T1 | yes | shown, 10 unlocked, Site Health opens | open | open |
| Super Admin (staff) | T1 | yes | shown, 10 unlocked, Site Health opens | open | open |
| Project Manager | T1 | no | hidden | Access Denied | **open** |
| Account Manager | T1 | no | hidden | Access Denied | **open** |
| Electrical Engineer | T1 | no | hidden | Access Denied | **open** |
| Facility Manager | T2 | no | shown, 10 unlocked, Site Health opens | open | open |
| Client Portal | T2 | no | shown, 10 unlocked, Site Health opens | open | open |
| Technician | T1 | no | Web Access Restricted | n/a | n/a |

## What changed in the build (why it passes)
- Menu: section `maintenance-portal` now has `requiresTier2: !hasPermission("maintenance_portal.manage")`,
  hidden when `requiresTier2 && !isT2`. The old five-role list (`["Project Manager","Account Manager","Admin",
  "Facility Manager","Super Admin"]`) is gone.
- Route: the `/maintenance-portal/*` group is wrapped in `Tier2Route({orPermission: "maintenance_portal.manage",
  fallback: <AccessDenied/>})`; T2 seats pass first. Same condition as the menu, so menu and route agree.
- The bundle no longer contains the string "Portal Sales".
- Inside the section, items are padlocked only when `!(isT2 || hasPermission)`, which can no longer happen while the
  section is visible, so every visible item opens.

## What is still open (why it is not a clean pass)
- `/maintenance-portal/condition` has **no `<Route>`**; `Layout` renders `ConditionAssessment` by pathname, so the
  permission check never runs.
- `/maintenance-portal/sld` is inside the guarded group, but `Layout` draws the persistent SLD canvas for that
  pathname and sets the outlet (where Access Denied renders) to `display:none`.
- The ticket listed both under Actual and asked to "check the two exempt items as part of the fix".
- Impact: none on data. The same PM seat sees identical Condition content at `/pm-readiness` (in its menu,
  `pm-deep-pm-readiness.png`) and the identical diagram + tools by typing `/sld` (NOT in its menu, `pm-control-sld.png`). The portal's "License" preview picker also appears in the PM's Site Data
  panel on those two pages.

## Method
1. API: `POST /api/auth/login` per seat → `/api/auth/v2/me` + `/api/features/access` (tier, roles, permissions).
2. Bundle read: `requiresTier2`, `orPermission:CV` (`CV="maintenance_portal.manage"`), Layout pathname renders.
3. Browser (Playwright MCP, headed): per seat, real login (email → "Use my password" → Sign In → "Set up later"),
   read the rail, open the portal section, click Site Health; then type each of the 10 portal addresses.
4. Deeper check of the two leaks on PM with a 14 s wait and API capture (all 200; same calls as `/pm-readiness`).
5. An independent checker reviewed the Jira comment against the screenshots and results before posting.

## Side facts
- Stage Account Manager seat: 78 permissions incl. `maintenance_portal.manage` on 28 Sep → 77 without it today.
- Acme on stage has the `maintenance-portal` company feature on; FM and CP seats are T2 (`license_type: interactive`).
- Trap: the app's boot screen "Checking agreements" is an `<h*>` heading; a "first heading wins" wait stops on it.
  The route sweep now treats it as loading (two Account Manager rows were re-run after the fix).

## Fact-check (independent checker, before posting)
Accepted: (1) "same diagram at /sld from its own menu" was wrong — PM's menu has no SLD link; re-tested, `/sld` opens
by typed address with the same diagram/tools → reworded + control screenshot 8; (2) "open from the menu" overclaimed —
only Site Health was clicked → reworded; (3) framing tightened to "menu fix passes / the 2 named pages not fixed";
nits: staff seats hold several roles, T1/T2 defined, developer wording vs T2 noted, AM 78→77 added, EE "open" = no
Access Denied (no site, empty pages). Rejected: placeholder link (already replaced before the check).

## Live demo (owner asked "show me this in real browser")
Playwright Chrome window, PM seat, 4 slow steps with a step banner: 1 menu without the section (green), 2 typed
`/overview` → Access Denied (green control), 3 typed `/condition` → page loads (red), 4 typed `/sld` → diagram loads
(red). Frames: `demo-1-pm-menu.png` … `demo-4-pm-sld-loads.png`.

## Jira
- Comment 44931 (8 inline screenshots, rendered and verified). No status change: the menu fix passes, but the ticket
  asked to confirm the two pages. Owner to decide READY TO RELEASE vs back to the developer.
- Artifact v2 (fact-checked): https://claude.ai/artifact/SM2S3LvuCqLmxyPBVAWCmZ
- Repo commit/push was blocked by the permission check (public repo); evidence + this changelog are local only.
