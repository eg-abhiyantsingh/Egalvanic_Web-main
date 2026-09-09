# The board becomes customer-facing release notes

**Date:** 2026-09-09
**Time:** 17:20 – 17:55 IST
**Prompt:** "we dont need to show defect just want to know what is going to production. Remove
everything else - just keep new in this release. bugs we don't care about in customer success team"

---

## What changed

The V1.36 artifact (same URL, `88483448-82ad-45f4-be80-822f638c43d1`) is now **release notes for the
customer success team**, retitled **“V1.36 Release Notes”** with the H1 *“What customers get in V1.36”*.

**Removed:** the eleven decisions-before-promotion, the verified-and-ready cards' defect language, the
“every page in the product” nav map, the not-testable list, the 66-row verdict ledger, the
how-this-was-tested notes, and the PASS / defect-led / decide counts. A text scan of the published page
confirms **zero occurrences** of defect, verdict, PASS, blocker, ledger, bug or promotion.

**Kept and expanded:** the new-features material, grown from 4 cards to **22 features across 7 areas**,
because "what is going to production" is more than the four that had no ticket — the ticket-driven work
is where most of the new capability lives. Each feature now has a plain description, a
**“Where to find it”** numbered path, and a screenshot: 20 images, none broken.

| Area | Features |
|---|---|
| Signing in | 1 — two-factor sign-in |
| Issues that turn into revenue | 5 — resolution→priced quote, Pull-Through Work, issue photos, Issue Suggestions, issues register |
| Work orders | 4 — multi-service work orders, per-service tick-offs, summary panel, 4-step create wizard |
| Maintenance | 4 — Maintenance Portal, Free/Premium plans, check-driven programmes, programme + compliance + reports |
| Materials & pricing | 4 — typed catalogue linkage, per-section pricing, reserved-name guard, quotes from accepted work |
| Site data & engineering | 3 — Connections graph, bulk nameplate extraction, SKM import/export |
| Accounts & sites | 1 — hand a site to a new contractor, history intact |

**A “DAY ONE” callout opens the page**, because 2FA is the one change every user meets on release day,
with the two questions CS will field: can it be skipped (yes, "Set up later", for now), and who holds
the code for a shared or admin login.

## Where the removed material lives

Nothing was lost — the release-decision content is still available for the audience that needs it:

- **Audit page** (licence lock, hidden-but-reachable pages, unguarded routes) — https://claude.ai/code/artifact/e13e2861-33d2-43ae-9c08-2ebde7d4fa5d
- **ZP-4123 page** (menu vs route access) — https://claude.ai/code/artifact/e90e971d-49f8-4987-97dd-521f75cd4d44
- **Per-ticket pages** — one per ticket, linked from each verdict file
- **Verdict files** — all 66 in `docs/bug-reports/`
- **The full board source** is preserved at `scratchpad/release.board-full.src.html`, so the
  promotion-board view can be rebuilt and published separately if the release meeting wants it back.

## Depth explanation

**1. Two audiences, two documents — not one document with a filter.** A promotion board exists to
support a go/no-go decision, so it leads with what could go wrong. Release notes exist to help someone
explain the product to a customer, so they lead with what it does and where to click. Trying to serve
both produces a page that buries the features under caveats and still reads as a risk register. The
right move was to rewrite rather than delete sections.

**2. "Just keep new in this release" meant more content, not less.** The literal reading — keep the
four untick eted features — would have shrunk the page to a quarter of the release. What is actually
going to production includes everything the tickets delivered: the resolution-to-quote funnel,
multi-service work orders, check-driven maintenance, typed materials. Those were re-described as
capabilities instead of as tested items.

**3. Keeping honesty without defect language.** Some caveats matter to CS on a customer call, so they
stay as neutral *“Worth knowing”* lines: the quote needs *Save & Regenerate* after a resolution is
edited; the Account row only shows for roles that can see accounts; per-section pricing needs section
counts filled in; a site has one owning account at a time; the engineering pages need their account
option enabled. Facts a rep needs, with no bug framing.

**4. Design shifted with the audience.** New palette and an editorial serif (Newsreader) for feature
headings against IBM Plex Sans body — release notes get read, so they are set to be read, where the
board was built to be scanned under time pressure.

## Files

- `docs/report-artifacts/2026-09-09-QA-V136-promotion-board.html` — rebuilt as release notes, 1.3 MB,
  20 screenshots (path unchanged deliberately, to preserve the artifact URL)
- `scratchpad/relnotes.src.html` — new source; `scratchpad/release.board-full.src.html` — the board source, preserved

---

## Addendum — renamed to V2.1, and two blocks removed

**Prompts:** "V2.1 update name" · "…DAY ONE…remove this" · "Release Egalvanic Web V1.36 · … Need the
detail? … remove this too from artifact"

Three edits, same URL (`88483448-82ad-45f4-be80-822f638c43d1`):

1. **Renamed to V2.1.** Title *V2.1 Release Notes*, H1 *“What customers get in V2.1”*, eyebrow and the
   Release line all read **Web V2.1**. The build QA tested still labels itself V1.36 in the app footer —
   that is the internal build number; the customer-facing release is V2.1. (Web v2.1 is also the fix
   version on the tickets in this batch, e.g. ZP-3978.)
2. **DAY ONE callout removed** — the two-factor "every customer will notice this" block at the top. The
   2FA **feature card** under “Signing in” stays, since it is genuinely new in the release; only the
   callout is gone.
3. **Footer removed** — the three lines about this not being a test report, where it was checked, and QA
   holding per-feature reports.

The page is now masthead → contents → 22 features across 7 areas, and nothing else. Verified on the
published page: no DAY ONE text, no footer element, zero occurrences of "V1.36", 20 screenshots, none
broken.

**Note on the file name.** The output path is still
`docs/report-artifacts/2026-09-09-QA-V136-promotion-board.html`, which no longer describes its
contents. It is deliberate: the artifact URL is tied to that path, and renaming the file would publish
a new page at a new link. The title and content are what a reader sees.
