# Sixteen-ticket batch · QA triage verdict (2026-09-09)

**Artifact:** https://claude.ai/code/artifact/a4b8d0da-50fa-4ccd-8b54-acf4632239fe
**Env:** `acme.qa.egalvanic.ai` · V1.36 · bundle `index-jYhUcFb4.js` (15.6 MB) · tenant acme
`d59d449b-09d8-45d6-8f0a-ef70024b1293` · **read-only** (GET/POST-list only; nothing created, changed or
deleted)
**Method:** live API against QA with the PM seat, plus string/structure analysis of the shipped QA
bundle. **Every ticket in this batch says "cicd/dev only" — that is wrong for most of them: their code
is already deployed on QA.** That is exactly why the "ignore dev-only deploy notes" rule exists.

## Headline

| Ticket | Subject | On QA? | Verdict |
|---|---|---|---|
| **ZP-3874** | staff elevation, `/staff/*` | code yes, inert | ✅ default-deny **PASS**; positive path not testable |
| **ZP-3887** | MCP connector LD gate | route not reachable | ⛔ **not verifiable on QA** |
| **ZP-3859** | Extract from Photos → Engineering header | **live** | ✅ **PASS** (control + subtype confirmed in UI) |
| **ZP-3901** | panelboard SCCR + inline circuits | superseded | ✅ **superseded by ZP-3902**, correctly |
| **ZP-3902** | bus SCCR first-class | **UI live**, seed/migration not run | ⚠️ **UI PASS, data BLOCKED** |
| **ZP-3904** | designations schedules | **live** | ⚠️ **PASS on function, FAILS on access control** |
| **ZP-3905** | bulk-extraction confidence chip | code present | ✅ **PASS by code** (all 4 review points) |
| **ZP-3906** | many-image 2000px cap | AI pipeline | ⛔ not web-testable |
| **ZP-3908** | core/custom attr split + Open in SLD | **live** | ✅ **split + lock + tooltip PASS**; Open-in-SLD hidden from PM/AM by its gate |
| **ZP-3910** | resolution-agent lambda | AI pipeline | ⛔ not web-testable |
| **ZP-3911** | Issues list perf | **live** | ✅ **PASS** |
| **ZP-3912** | workbench crash on panel_type | fix present | ✅ **PASS by code** |
| **ZP-3913** | kA voltage rule | AI pipeline | ⛔ not web-testable |
| **ZP-3933** | resolution agent eligible fixes | AI pipeline | ⛔ not web-testable |
| **ZP-3938** | Issues list 500 | not reproducible | ✅ **N/A — QA never had the bug** |
| **ZP-3941** | per-section pricing | code present | ⚠️ control present, option label deviates |

---

## ZP-3911 — Issues list performance · PASS

The ticket describes ~6.3 s on dev. On QA, `POST /api/v2/issues/list` (page_size 50, 924 issues):

| run | time |
|---|---|
| 1 | 1.56 s |
| 2 | 1.56 s |
| 3 | 1.58 s |
| 4 | **0.51 s** (warm) |

**Funnel reconciliation — the check the ticket calls out.** `stat_counts` sums to 935 against a total of
924, which looks like a defect until you separate the stages:

```
unset 841 + ready 2 + quoted 27 + resolved 54 = 924  ==  list total 924   ✅ exact
awaiting_review 11                                    <- a FIFTH, overlapping counter
```

So the four canonical stages are disjoint and reconcile exactly. `awaiting_review` is an additional
counter the ticket does not name and is a subset of another stage — **not** a double-count defect. A
100-row page carried 100 unique ids, 0 duplicates, so the `DISTINCT` join is behaving.

Not verified: the index `ix_pwl_issue_id` itself (no DB access) and the "issue referenced by >1 planned
workorder line" fixture — no row in the first 100 was flagged `quoted`, so the duplicate case was not
exercised against a known-duplicate issue.

## ZP-3938 — Issues list 500 · not applicable to QA

`POST /api/v2/issues/list` on QA returns **200** with 924 issues, and a search matching nothing returns
an empty list rather than an error. But the field the fix adds is **absent**: rows carry
`resolution_processing`, not `resolution_processing_at`, and `resolution_processing_at` appears nowhere in
the QA bundle. So the change that introduced the 500 (backend #1118 / ZP-3934) is not on QA, the 500
never existed here, and the one-line fix cannot be verified on this environment. Verify on dev.

## ZP-3904 — designations schedules · largely PASS

**Shipped and live on QA.** The bundle carries the exact path→kind map:

```js
{"/equipment-designations":"all","/short-circuit-ratings":"sccr","/feeder-schedule":"feeder",
 "/ocpd-settings":"ocpd","/transformer-schedule":"transformer"}
```

and the grid hook is kind-aware (`{mode, scopeNodeIds, scopeSessionId, sldId, kind="all"}`). All four
routes sit in the route table on `features.equipment_designations.view` + company flag `eng-lib`, and the
role-exclusion map trims **all** of them for Facility Manager, which is the "FM sees the trimmed set"
requirement.

**Kind scoping genuinely changes the result set** — `GET /sld/{sldId}/library-designations?kind=…`:

| kind | items | |
|---|---|---|
| sccr | 5 | rows carry `bus_summary` |
| ocpd | 1 | `included_seg_count` present |
| feeder | 0 | |
| transformer | 0 | |
| all | 1 | |

Row fields match what each schedule reads by: `ampere_rating`, `length`, `kva_rating`, `bus_summary`,
`engineering_status`, `node_class`, `included_seg_count`.

**Primary Winding and Secondary Winding are present** on QA's node classes (47 classes), so revision
`xfmrw_a1`'s reserved attrs landed here.

**Two observations (leads, not filed defects):**

1. **An unknown `kind` is not rejected.** `kind=banana` returns the same payload as `kind=all` rather
   than a 400. This is the *same silent-fallback pattern* as ZP-3863's `query_name`, so a typo in a
   deep link is indistinguishable from the default scope.
2. **`total` is `null`** on every kind-scoped response while `stats` is populated. The ticket says
   "paging … inherit the scope"; a null total may leave the grid unable to page. Worth a look.

Not verified (needs UI, blocked — see below): exact column sets and headline strings
("SCCR Established" / "Specs Complete" are both in the bundle), the French locale, the editor being
scoped per schedule, SKM export/import of Pri/Sec Connection, and the transformer-completeness
regression.

## ZP-3902 — bus SCCR first-class · blocked on the per-environment seed

The **code is on QA** and implements the tri-state exactly as described:

```js
checked: e.aic_label_present === false,
onChange: checked ? (…, n("aic_rating", null), n("aic_label_present", false))
                  : n("aic_label_present", null)
// and the rating input is hidden while "no marking" is checked:
e.aic_label_present !== false && <SCCR (Label) field>
```

That is all three states — `null` not captured, `false` no marking (value cleared), a typed value marks
it — plus the label *"No SCCR marking on the label (rating is derived)"*. `panel_type_id` and
`manufacturer_id` are in the reserved-field list.

**But the designations library is not seeded on QA.** `GET /api/eqp-lib/panel-manufacturers` returns
**200 with an empty array — 0 manufacturers**. That is precisely the caveat the ticket documents ("an
unseeded environment shows an empty list rather than an error"), so the graceful-degradation behaviour
**passes**, and every functional check behind it — Manufacturer filtering the Panel Type list, the
migration of old free-text values, the bus-row summary wording — is **blocked until the seed and
`sccr_first_class_migrate` are run on QA**.

## ZP-3901 — panelboard SCCR · superseded, correctly

`sccrMethod` and `panelType` are **absent** from QA's node classes and from the bundle. That is the right
outcome, not a regression: ZP-3902 explicitly strips those attrs and replaces them with
`node.panel_type_id` + the `aic_label_present` tri-state. QA carries the **ZP-3902 generation**. The parts
of ZP-3901 that survived are present — `schedule_config` (33 refs), `aic_rating` (27), `bus_summary` (5),
`"Create as SLD asset"`, `"on schedule only"` — so its inline-circuit model is live. Its own review steps
should be run against ZP-3902's field set, not as written.

## ZP-3905 — bulk-extraction confidence chip · PASS by code

All four review points are satisfied in the shipped code:

```js
const Ze = String(he.config.confidence).toLowerCase(),
      ot = Ze === "high" ? "success" : Ze === "medium" ? "warning" : "error";
<Tooltip title={`Agent confidence: ${Ze}`}>
  <Chip size="small" variant="outlined" color={ot} label={Ze} sx={{height:20,fontSize:11}}/>
```

- colour map **high → success (green), medium → warning (amber), else error (red)** ✅
- the chip is guarded by `$e && he.config?.confidence &&`, so **no chip at all** when the agent returned
  no confidence — the ticket's negative case ✅
- the expanded Notes still render `Confidence: {value}` ✅ unchanged
- it renders on the collapsed row beside the other controls ✅

Only the narrow-window wrap check needs a real browser.

## ZP-3912 — workbench crash on panels with a panel type · PASS by code

The crash fix is present, at both render sites:

```js
["Panel type", (n.panel_type && typeof n.panel_type === "object" ? n.panel_type.code : n.panel_type) || "—"]
r.panel_type?.code && <Chip label={`Type ${r.panel_type.code}`}/>
```

That covers the object shape `{code, group}`, the plain-value fallback, **and** the `"—"` placeholder for
a panel with no type (the ticket's negative case). The sibling facts rows are all there —
`… kA effective` / `"SCCR not established"`, `${identified ?? 0} unknown`, `${Math.round(voltage)} V`.
`lookupService` is absent from the bundle, consistent with the Record card and its status-history call
being removed. Header badge count and the assumption/note coercion need the live UI.

## ZP-3874 — staff elevation · default-deny PASS, positive path not testable

The `/staff/*` routes **are registered on QA and refuse every caller**, with correctly distinct messages:

| call | result |
|---|---|
| `GET /staff/companies`, no token | **401** `eg_staff_denied` — "Staff authentication required. Send the Cognito ID token as a bearer token." |
| `GET /staff/companies`, customer product token | **401** `eg_staff_denied` — "Your token was not issued by the Egalvanic staff directory." |
| `/staff/reporting/configs`, `/staff/eg-forms`, `/staff/reporting/page-templates`, `/staff/reporting/sample-entities`, `POST /staff/dataprep` | all **401** `eg_staff_denied` |
| a bogus `/staff/…` path | **200 SPA HTML** — which is how you tell a registered API route from a fall-through |

So "unset config must refuse everyone, not allow everyone" **passes on QA**, and it fails at the
token-issuer check before the allowlist is even consulted. Customer-facing auth is unaffected (the PM
seat kept working throughout).

Everything requiring an internal-tools Cognito **ID token** is **not testable from here**: the act-as
header requirement, the 403 `wrong_tenant` ownership check, the audit-log record, the dataprep mode
allowlist, and — the important one — the `X-Act-As-Company: A` + body `company_id: B` override test.

**Environment warning.** The connected "Egalvanic - Internal" staff MCP server is pointed at
**PRODUCTION**, not QA: its `acme` is `0a61e613-7887-4c10-99bf-59cedc4460f2` with branding in
`eg-pz-prod-s3-branding-ohio`, and `list_companies` returns 36 real customer tenants. All calls made
through it were read-only. It must never be used as QA evidence — see the retraction in the ZP-3863
verdict.

## ZP-3887 — MCP connector gate · not verifiable on QA

`GET /auth/oauth-config/<company_code>` is **not reachable on the QA tenant host** at any path tried
(`/auth/…`, `/api/auth/…`, `/api/v2/auth/…`, `/api/auth/oauth_config/…`) — every one returns the SPA's
200 HTML, while `/api/auth/login` correctly returns 405, proving `/api` routing works and unknown `/api`
paths fall through to the SPA. So neither the default-deny 404 `MCP_NOT_ENABLED` nor the
`COMPANY_NOT_FOUND` distinction can be exercised here; the route presumably lives on the MCP server's own
host. The grant/revoke cycle additionally needs LaunchDarkly targeting on `feature-mcp` — the LaunchDarkly
MCP server in this session is unauthenticated, so that remains out of reach. Unchanged from the earlier
ZP-3887 verdict, now with the concrete reason.

## ZP-3859 / ZP-3908 / ZP-3941 · code present, UI unconfirmed

- **ZP-3859** — `"Extract from Photos"`, `"Attributes + Library"` and `extract_subtype` are all in the QA
  bundle, and `eng-lib` is present (14 refs), so the moved control, the split button and the subtype
  request all shipped. Placement in the Engineering header and the sequential run need the UI.
- **ZP-3908** — the **split is present**: a `Custom Attributes` sub-header with its own divider rendering
  `pl.custom`, in a ternary whose else-branch renders one flat `pl.all` grid — matching "with eng-lib off
  the grid stays exactly as it was". `Open in SLD` shipped with the required accessible name
  (`openInSldAria: "Open {{label}} in single line diagram"`), a `notPlacedOnDiagram` string, and
  `features.slds.view` (4 refs). The **lock icon is present** on the Core header (confirmed by screenshot); only the tooltip *wording* differs from the ticket, which is cosmetic. Note
  `"Core Attributes"` alone proves nothing here — it is a long-standing i18n key used in unrelated
  screens (Edit Core Attributes, copy-field, advanced search).
- **ZP-3941** — `unit_attributes_available` is consumed (`unitAttrs: a.unit_attributes_available || []`)
  and the Pricing control exists, with `<option value="">Per asset</option>` plus one option per unit
  attribute and helper text switching between *"Labor minutes are per asset."* and the per-unit wording.
  **Deviation:** the ticket says the options are "Per asset or Per section", but the second option's label
  is the **attribute's own name** (e.g. `sections`), not the string "Per section" — `"Per section"` and
  `"minutes per section"` are both absent from the bundle. Cosmetic, but it means the ticket's expected
  wording will not be what a tester sees.

## The four AI-pipeline tickets · not web-testable

**ZP-3906** (many-image 2000px cap), **ZP-3910** (resolution-agent lambda), **ZP-3913** (kA voltage rule),
**ZP-3933** (eligible-fix menus, materials contract). All live in `eg-pz-engineering-ai-pipeline`, whose
runner is an external Lambda invoked out of band; their review steps call for direct lambda invocation,
Step Function execution inspection, CloudWatch log lines and prompt-narrative review. None of that is
reachable from the web tier, and ZP-3913 and ZP-3933 are explicitly prompt-level, so verification means
reading agent narrative rather than a code path. **These need dev + AWS access, not a web QA pass.**
ZP-3910's web half (ZP-3909) is one of the ten tickets assigned to the other QA and stays skipped.

## UI verification — completed

**The 2FA blocker was my error, not an environment limit.** The enrollment screen carries a
**"Set up later"** button; one click goes straight to the dashboard with no enrollment and no
account-state change. I had asserted it was mandatory from a stale memory note instead of reading the
screen. No seat was enrolled and the headless RBAC suite is untouched.

### ZP-3904 — functionally correct, but the routes are not access-controlled

All six routes render live with real, scope-specific data:

| route | headline | count | columns |
|---|---|---|---|
| `/short-circuit-ratings` | **SCCR Established** 1 of 162 (1%) | 1–25 of 162 | Asset · Class · Voltage · Type · **Available Fault Current** · SCCR · Designated · Status · Actions |
| `/feeder-schedule` | Library Designated 0 of 40 | 1–25 of 40 | Asset · Class · Length · Designation · Designated · Status · Actions |
| `/ocpd-settings` | Library Designated 4 of 111 (4%) | 1–25 of 111 | Asset · Class · Device · Frame · Settings · Designated · Status · Actions |
| `/transformer-schedule` | **Specs Complete** 0 of 2 | 1–2 of 2 | Asset · Voltages · kVA · %Z · Winding Configuration · Designated · Status · Actions — **no Class column** ✅ |
| `/custom-devices` | *"Coming soon — define your own protective devices…"* | — | placeholder, as specified |
| `/equipment-designations` | Library Designated 5 of 153 | 1–25 of 153 | deep link still resolves ✅ |

Both required headlines are exact, the transformer page correctly omits Class, the OCPD row renders
Device over manufacturer/trip-unit with Frame on one line (`Emax 2, E1.2, Ekip DIP — LI, 250-1200A,
UL1066 ABB` / `250 A · 254 V · 65 kA`), and the five distinct asset counts prove stats inherit the scope.

**DEFECT — the four schedules are reachable by roles without the entitlement.** The route table gates
them on `features.equipment_designations.view`, but on acme QA that permission is held by **only the
Electrical Engineer seat**:

| seat | `features.equipment_designations.view` | `features.slds.view` | `slds.view` |
|---|---|---|---|
| PM | **no** | **no** | yes |
| Technician | no | yes | yes |
| Facility Manager | no | yes | yes |
| Client Portal | no | yes | yes |
| Electrical Engineer | **yes** | yes | yes |
| Account Manager | no | **no** | yes |

I loaded all four schedules, with full data, on the **PM** seat — which does not hold that permission.
These routes are among the 21 with **no page guard** recorded in the earlier nav/route audit, so the
permission in the table is never enforced. ZP-3904's first review step ("confirm the entries appear for
a user *with* the engineering-library entitlement") cannot be judged by presence alone, because they
also appear for users without it.

**SECURITY DEFECT found by API-testing this endpoint — filed separately.**
`GET /sld/{sld_id}/library-designations` never checks the caller's `accessible_sld_ids`: the Facility
Manager (11 sites) and Electrical Engineer (4 sites) seats get **byte-identical rows to the PM** for an
unmapped site, across all four kinds. Full write-up:
`docs/bug-reports/2026-09-10-QA-library-designations-site-scope-leak.md` ·
https://claude.ai/code/artifact/06ad5f78-f47f-4875-bb22-588cc34ce68f

**Column deviation:** the SCCR page shows **Available Fault Current** where the ticket specifies a
**Devices** column ("Line and Load side by side, greyed out when the rating is label-marked"). Either
the ticket's column set changed or that column did not ship.

**Needs a dev answer:** the one populated transformer reads `primary — → 208V`, kVA 1000, %Z 6.2%, yet
"Specs Complete" counts it as 0 of 2. That is consistent with the stated rule *if* its primary is
genuinely absent and not derivable from an upstream bus — but the ticket also says a derived primary
"should read like any other value, not as a placeholder", and here the primary renders as a dash.

### ZP-3902 — the UI shipped and is correct; the data migration has not run

On a Panelboard's editor drawer, the ENGINEERING section renders exactly what the ticket describes:
**Manufacturer**, **Panel Type**, the checkbox **"No SCCR marking on the label (rating is derived)"**,
and **SCCR (Label) kA**. The tri-state control is present and wired.

But the migration has **not** run on QA, and it shows: the old free-text **Manufacturer**, **Type** and
**Model** attributes are still present under CUSTOM ATTRIBUTES on that bus class — precisely the fields
ZP-3902's `sccr_first_class_migrate` is supposed to strip. Combined with
`GET /api/eqp-lib/panel-manufacturers` returning **0 manufacturers**, the Panel Type picker has nothing
to offer. So the migration checks, the manufacturer→panel-type filtering and the free-text-preservation
spot-check are all **blocked until the seed and migration run on QA**.

### ZP-3908 — the split shipped; two gaps

The editor drawer renders **two separate groups with their own sub-headers**, Core first:

- **CORE ATTRIBUTES** — Electrode Configuration, Enclosure Depth/Height/Width
- **CUSTOM ATTRIBUTES** — Size, Mains Type, Columns, Notes, Voltage, Ampere Rating, Manufacturer,
  Serial Number, Catalog Number, Model, Configuration, Type

That is the described split, and it matches the ticket. Two gaps:

1. **CORRECTED TWICE — the lock AND its tooltip both shipped.** I first reported the lock missing, then
   reported it present with different wording. Both wrong. The padlock is on the CORE ATTRIBUTES header
   and its `aria-label` is verbatim the ticket's requirement: *"Reserved attributes defined by the
   platform. They're the same on every company's classes and feed arc-flash readiness, the SKM export,
   pricing, and imports."* The first error came from enumerating only `button[aria-label]` (the lock is
   an `svg`); the second from grepping for "platform-defined" when the string reads "defined by the
   platform". **This half of ZP-3908 is a clean PASS.**

2. **Open in SLD is absent from the editor header — correctly, but for a reason worth raising.** The
   action is gated on `features.slds.view`, and **PM and AM do not hold it** while all six roles hold
   plain `slds.view`. So the shortcut is invisible to the two roles most likely to be doing this work.
   Not a code defect; a permission-mapping question for whoever owns the RBAC matrix.

### ZP-3859 — PASS

The editor header carries **Extract from Photos** (aria-label *"Extract nameplate data from photos using
AI"*), and the ENGINEERING section carries **Asset Subtype**. The section order is Basic Info →
**Engineering** → Schedule → OCP → Connections → Condition → Notes, so the control sits with the fields
it fills rather than under Custom Attributes.

**"Attributes + Library" split option did not appear** on this panelboard. That is expected rather than
wrong: the library half needs the panel-designations library, which is unseeded here, and the asset has
**0 photos** ("Trust the Photos — Add at least one photo of this asset to enable"), so extraction cannot
run at all. Re-check once the seed lands.

### ZP-3901 — the inline schedule is live

The same drawer carries a **SCHEDULE** section with *Edit Panel Schedule*, Status, Circuit Count and
Schedule Photos, so the inline-circuit model is reachable in the UI.

### Still not done

**ZP-3905** (confidence chip) and **ZP-3912** (workbench header/badges) remain code-verified only. Both
need a live agent job: ZP-3905 needs a bulk-extraction job, which **auto-submits and is billable**, so I
did not run one; ZP-3912 needs an issue with a resolution proposal on a panel carrying a panel type,
which the unseeded library makes hard to produce. Their code-level evidence is in the sections above.

**No screenshots for this batch.** Captures on the designations grids and the asset editor timed out
repeatedly on "waiting for element to be stable" — those pages animate continuously. The /sessions
captures earlier the same day worked, so it is page-specific, not a tooling failure. Recorded rather than
substituting an unrelated image.

## Footprint

Read-only. Every QA call was a GET or a list POST; no work order, asset, class, user, site, report config
or procedure was created, modified or deleted. No bulk-extraction job was submitted (those are billable
and auto-submit). No LaunchDarkly flag was touched. The production staff MCP was read-only and is flagged
above.

## Per-ticket artifact pages (published 2026-09-10, with screenshots)

- **ZP-3859** — https://claude.ai/code/artifact/37c1d11d-4f03-425b-b548-94e62076f77a
- **ZP-3901** — https://claude.ai/code/artifact/1666cfa3-ed2f-4fb8-8b2c-bc9503d5e99a
- **ZP-3902** — https://claude.ai/code/artifact/80383dfa-7121-4587-8c2f-c52eba366bf9
- **ZP-3904** — https://claude.ai/code/artifact/9088dc6f-514a-4a11-b245-93f38f45b1ce
- **ZP-3905** — https://claude.ai/code/artifact/8439b08b-045d-432e-94de-aa08a2831bab
- **ZP-3908** — https://claude.ai/code/artifact/8652fed7-aaa2-4287-9dc1-598615d27841
- **ZP-3911** — https://claude.ai/code/artifact/df6dc03f-e1aa-4d90-9db4-8e2c8ed9e500
- **ZP-3912** — https://claude.ai/code/artifact/2eb73207-85d2-4f3c-b85d-f0f37b0cefd8
- **ZP-3874** — https://claude.ai/code/artifact/6fcadb40-931a-438e-b5db-ae2936658639
- **ZP-3887** — https://claude.ai/code/artifact/9dcbfac9-b2ea-44f3-8ef9-fe710c20e5fe
- **ZP-3938** — https://claude.ai/code/artifact/07da2520-8c03-4991-97b8-8b38d341711f (2026-09-08)
- **ZP-3941** — https://claude.ai/code/artifact/4acd4d82-ea97-45be-8331-f5385fb33e25 (2026-09-08)
- **Site-scope leak** — https://claude.ai/code/artifact/06ad5f78-f47f-4875-bb22-588cc34ce68f

`ZP-3906`, `ZP-3910`, `ZP-3913` and `ZP-3933` have no page of their own: they are
`eg-pz-engineering-ai-pipeline` work with nothing web-testable to photograph. They stay in the batch page.

**Screenshots** — 15 captures in `docs/bug-evidence/2026-09-09-sixteen-ticket-triage/`. The earlier
"screenshots impossible" note is withdrawn: `page.screenshot` was timing out on "waiting for element to
be stable" because those pages animate continuously. Freezing animation first via CDP
(`Animation.enable` then `Animation.setPlaybackRate {playbackRate: 0}`) makes every capture succeed.
