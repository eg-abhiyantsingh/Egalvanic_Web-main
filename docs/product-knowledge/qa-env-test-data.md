# QA environment fixtures

**Verified:** 2026-08-10, `https://acme.qa.egalvanic.ai`, build **V1.36**.

| Fact | Value |
|---|---|
| Company id | `d59d449b-09d8-45d6-8f0a-ef70024b1293` |
| Admin user id | `77e99d86-7f0a-4345-b056-6f470bb668ec` |
| Sites on this company | **187** |
| Default role after login | Super Admin |
| Quotes / EMPs / Planned Work volume | 150 / 147 / 9 |

## Empty sites (no assets) — for onboarding & empty-state tests

The "Let's get your assets in" invite only renders on a site with **zero assets**, so these are
the fixtures that make onboarding tests possible. Verified empty via
`GET /api/lookup/v2/nodes/{id}?page=1&page_size=1` → `total: 0`, and each had **no** active
onboarding job (clean control state).

| Site name | `sld_id` |
|---|---|
| Test without location | `f3651a74-c1d1-4bda-afcf-4727e01c6903` |
| Test android | `a3ed9103-a5fa-438b-9dd4-1b5dcaf33489` |
| Test franchb | `b6ce3bd3-a109-45ce-87ed-52265564798e` |
| Yuzi | `5bb45ac6-d89b-44bc-80be-c317f38a7c95` |
| Yuzi *(duplicate name, different id)* | `edb2cf89-dc41-47fa-bdf4-867a80adc108` |

Found by scanning the first 60 of 187 sites — 5 empties, so empty sites are not rare.

> **Duplicate site names exist** (two sites called "Yuzi"). Never key a test on site *name*.
> The picker's typeahead will match both and the test picks whichever renders first. Use ids.

## Sites with data (referenced during this work)

| Site name | `sld_id` | Note |
|---|---|---|
| Android Site 2 | `aadcee4c-7dd0-45b3-81b9-309c5c166084` | has a finished onboarding job (`status: imported`, 2 Transformer assets) |
| Z1 | `f5be0573-dd42-44de-906f-534e72c08eb0` | Work Type / Services-V2 fixture site (see memory) |

## Volatility warning

This is a shared QA tenant that humans and automated suites both mutate. Asset counts, quote
totals and "which sites are empty" **will drift**. Re-verify emptiness at test start rather
than trusting this table — treat it as a starting shortlist, not a guarantee.
