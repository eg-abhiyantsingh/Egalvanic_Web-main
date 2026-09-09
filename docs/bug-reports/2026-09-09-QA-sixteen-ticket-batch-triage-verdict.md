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
| **ZP-3859** | Extract from Photos → Engineering header | code present | ⚠️ code present, UI unconfirmed |
| **ZP-3901** | panelboard SCCR + inline circuits | superseded | ✅ **superseded by ZP-3902**, correctly |
| **ZP-3902** | bus SCCR first-class | code yes, **seed not run** | ⛔ **blocked on seed** (documented caveat reproduced) |
| **ZP-3904** | designations schedules | **live** | ✅ **largely PASS** + 2 observations |
| **ZP-3905** | bulk-extraction confidence chip | code present | ✅ **PASS by code** (all 4 review points) |
| **ZP-3906** | many-image 2000px cap | AI pipeline | ⛔ not web-testable |
| **ZP-3908** | core/custom attr split + Open in SLD | partial | ⚠️ split present, lock tooltip **absent** |
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
  `features.slds.view` (4 refs). **However** the described lock tooltip is **not** in the bundle —
  no `platform-defined`, `LockOutlined`, `LockIcon`, `reservedProps` or `isReserved`. Either the tooltip
  wording differs from the ticket or the lock affordance did not ship; **needs UI confirmation.** Note
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

## Why the UI checks are unconfirmed

Mandatory 2FA enrollment now covers the app for every seat I hold. A fresh login returns 200 and is
recognised, then renders *"Set up two-factor authentication"* over the application, with no "set up
later". The recorded session-priming recipe (in-page `fetch` so the browser takes the HttpOnly cookies)
authenticates but does **not** clear the enrollment screen, and the app's auth storage key is computed
rather than a literal, so priming `localStorage` blind is not possible. `useSessionToken` in the bundle
belongs to the Form.io SDK, not app auth.

Enrolling a seat in Email OTP **is** possible — the repo carries a Gmail app-password and OTP would be
deliverable to the plus-addressed inbox — but enrolling changes account state and is what breaks the
headless RBAC suite, so it is the owner's call, not mine. **Decision needed:** enroll one seat (say
`+project@`) in Email OTP to unblock UI verification, accepting the effect on the automated suite, or
keep the suite intact and accept API/bundle-level verification for these tickets.

## Footprint

Read-only. Every QA call was a GET or a list POST; no work order, asset, class, user, site, report config
or procedure was created, modified or deleted. No bulk-extraction job was submitted (those are billable
and auto-submit). No LaunchDarkly flag was touched. The production staff MCP was read-only and is flagged
above.
