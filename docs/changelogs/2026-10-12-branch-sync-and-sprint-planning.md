# 12 Oct, 11:52 IST check: branch sync on stage and QA, sprint planning

- **Ready for QA:** only ZP-3305, as before.

## Branch sync (ZP-4783): stage and QA rebuilt, code unchanged
Avani is running ZP-4783: re-baseline stage, QA and dev from prod, then promote dev to QA. The ci/cd space shows:

| Step | Environment | Deployed (IST) | New frontend build |
|---|---|---|---|
| 1. prod → stage | stage (backend + frontend) | 11:15–11:20 | `index-Bqlp6isx.js` |
| 2. stage → QA | QA (backend + frontend) | 11:37–11:40 | `index-CtHfwbE5.js` |

What changed in the code (checked without signing in):
- **Main bundle:** QA and stage differ from their previous builds only in the build timestamp.
- **QA lazy-loaded files:** all 302 are identical to the previous QA build once the file-name hashes are masked.
- **API:** `swagger.json` (1,438 operations) is byte-identical on QA, stage and prod, and identical to the 10 Oct snapshots.

So nothing new is on QA to test yet. Step 4 (promote dev → QA) needs Eric's approval and hasn't happened. When it lands, QA gets the v2.4 sprint code. ZP-4783's own QA review then asks for a smoke test (login, assets, sessions, forms), a single database migration head, and confirmation that no unfinished work reached QA.

## Sprint planning (about 11:20–11:48 IST)
- About 13 tickets moved into **Web v2.4** (release 23 Oct): ZP-4764, 4707, 4634, 4635, 4636, 4637, 4769, 4777, 4430, 4428, 4372, 4324 and 4189.
- **Web v2.3.1** (release date today) gained **ZP-4673**: every page load calls `/api/auth/v2/me` and `/api/devrev/session-token` twice. It now holds ZP-4767, 4768, 4776 and 4673, all To Do with Krunal.
- Avani moved ZP-4531 ("Report generation page number") to On Hold.

## Chat
eg-internal-dev has nothing new since Dharmesh's "please join" at about 11:02 IST.
