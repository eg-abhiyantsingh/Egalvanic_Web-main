# 12 Oct: production redeploy (label only) and the Web v2.3.1 hotfix scope

- **Date:** Mon 12 Oct 2026, 10:39–10:55 IST (the /loop Ready-for-QA check)

## Ready for QA
Only ZP-3305 (Track ID T16, Avani). It has been waiting since 14 Sep. Nothing new.

## Production frontend redeploy at 10:08–10:14 IST (Avani, commit ae35ad77)
- Prod moved from `index-BvjbCmrB.js` (10 Oct) to `index-CY_YgNqA.js`.
- Diff of the two builds, with chunk hashes masked:
  - the main bundle changes only in the version: `VITE_WEB_APP_VERSION`, the sidebar label and the Sentry release go from `V2.2` to `V2.3`. The build timestamp also changes;
  - all 317 lazy chunks are byte-identical.
- So no code shipped. This only fixes the "V2.2" label that prod showed after the v2.3 release.

## Environments right now
| Host | Bundle | Deployed (IST) | Label |
|---|---|---|---|
| acme.egalvanic.ai (prod) | index-CY_YgNqA.js | 12 Oct 10:11 | V2.3 |
| acme.stage.egalvanic.ai (stage) | index-BH4gulmz.js | 9 Oct 15:03 | V2.2 |
| acme.qa.egalvanic.ai (QA) | index-BJGNnWD7.js | 9 Oct 14:21 | V1.36 |

Note: `acme.stag.egalvanic.ai` (no "e") serves the **production** build and API. It is not stage.

## Web v2.3.1 hotfix (Jira version 14279, release date 12 Oct, not released)
| Ticket | What | Priority | Owner | Status at 10:40 IST |
|---|---|---|---|---|
| ZP-4767 | DevRev Z icon missing again (regression of ZP-1686) | Highest | Krunal | To Do |
| ZP-4768 | A new Admin user gets two welcome emails, one with no password, then a blank page after sign-in | Highest | Krunal | To Do |
| ZP-4776 | DevRev session-token creation fails (Sentry 7784435757 / 7778359754) | Medium | Krunal | To Do |

None of these fixes is in today's prod build. Krunal moved ZP-4776 to In Progress at 10:23 and back to To Do at 10:26.

## Overlap found
**ZP-4798** (filed by QA on 10 Oct, 14:55 IST) is the same failure as **ZP-4768 part 2**: the URL stays on `/login`, the signed-in menus render, and the main area is empty. ZP-4768 was filed 4 hours earlier. ZP-4798 adds a repro for any user (sign out, then sign in again). The tickets are not linked yet; that's up to the owner.

## Chat
- eg-internal-dev: since Saturday, only Kush's iOS 1.69 release note (to Nency).
- Dharmesh DM: no new message.
- Dharmesh already had the DevRev 409 on 10 Oct (his attachment on ZP-4767), so the drafted Z-icon follow-up is no longer needed.
