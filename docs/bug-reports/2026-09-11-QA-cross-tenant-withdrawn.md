# The cross-tenant IDOR finding is withdrawn — the proof used a staff account

**Date:** 2026-09-11 · **Asked:** *"are you sure have you check cross teneent issue too?"*
**Artifact:** https://claude.ai/code/artifact/9b83e732-0072-40c8-8771-035f7d76617e (Version 16)

## The claim

The register's #1 P1, carried since 2026-08: *"Paste another company's id into ten addresses and the
product hands you their data."* Explicitly qualified as **"reproduced by a non-staff customer admin
(`is_eg_admin:false`)"**.

## What I found

That qualifier was false. The seat used for the cross-tenant work — `abhiyant.singh+admin@egalvanic.com`
— returns **`is_eg_admin: true`**. It is an Egalvanic staff account, which is entitled to read across
companies by design.

Same request, `GET /api/sld/24eb08b1-…` (a diagram owned by Demo Company), from **all eight EG-ACME
logins** — including the 7th seat the owner supplied on 2026-09-11:

| Seat | Role | `is_eg_admin` | Perms | Result |
|---|---|---|---|---|
| `+admin@` (used for the original proof) | Super Admin (+4) | **true** | 132 | **200 json** — reads it |
| `+adminqa@` **(new, 2026-09-11)** | Admin | **true** | 108 | **200 json** — reads it |
| `+project@` | Project Manager | false | 95 | **422** permission_denied |
| `+fm@` | Facility Manager | false | 75 | **422** |
| `+clientportal@` | Client Portal | false | 35 | **422** |
| `+accountm@` | Account Manager | false | 77 | **422** |
| `+electric@` | Electrical Engineer | false | 81 | **422** |
| `+tec@` | Technician | false | 95 | **422** |

**Two staff seats read it. All six customer roles are refused.**

**Product fact worth recording: the `Admin` role is itself an Egalvanic staff role on this platform**
(`+adminqa@` holds only `Admin` and still reports `is_eg_admin: true`). That is why the original write-up
could call its account a "customer admin" and be wrong without anyone noticing — on this tenant there is no
such thing as a customer admin. The six roles a customer actually gets are the six refused above.

**Positive control:** the same PM reading its *own* diagram → 200, 2,279,413 bytes, 291 nodes. So the 422 is
a real refusal, not a broken call. **Negative control:** a random UUID → 422, same as the foreign id.

## The other routes in the family, from the PM seat

| Route | Own id | Foreign (demo) id | Random id |
|---|---|---|---|
| `GET /api/sld/{id}` | 200, 2.2 MB, 291 nodes | **422** | 422 |
| `GET /api/contact/by-sld/{id}` | 200, real contact PII | 200 `{"contacts":[]}` | 200 text/html |
| `GET /api/lookup/v2/nodes/{id}` | 200, 50 nodes, 157 KB | 200 `data:[] total:0` | 200 `data:[] total:0` |
| `GET /api/sld/{id}/library-designations` | 200, 48 KB | 200, empty | 200, empty |
| `GET /api/issues/open-by-site?company_id=` | — | **422** permission_denied | — |
| `GET /api/company/{id}/*` | — | **422** (already known fixed) | — |
| `GET /api/ir_session?limit=500` | 1,569 rows, all own tenant | no demo rows present | — |

`POST /ir_session/scope-preview` was **inconclusive** — my payload lacked a work type, so all three arms
returned `applicable:false`. Not evidence either way.

## What this does NOT settle

1. **The demo tenant is nearly empty** (its own SLD reads back with 0 nodes). On every route except
   `/api/sld/{id}`, an empty answer is ambiguous: scoped-out, or genuinely nothing there.
2. **Session-keyed routes untested** — `eg-form-instance/by-session`, `mapping/node-session/by-session`,
   `ir_session/{id}/full` — because demo has **no work orders of its own** to aim at. (Reading *ACME's*
   session ids from the demo seat proves nothing: that seat is staff.)
3. **The only demo login available is also staff** (`shubham.goswami@` → `is_eg_admin: true`), so the
   reverse direction cannot be run at all. The 7th seat supplied on 2026-09-11 turned out to be staff too,
   so it does not unblock this.

**Status: withdrawn, not disproved.** I can show it does not happen to a customer on the routes I could
reach. I cannot yet show it never happens.

## What would close it

**A non-staff login on a second tenant that has real data.** That is the single blocker. With one, the
whole family is settled in an afternoon.

## The lesson

`is_eg_admin` was in every `/auth/me` response the whole time and was never checked. A staff seat makes
every tenancy test pass trivially and silently. **Any cross-tenant test must print the attacker's
`is_eg_admin` alongside the result, or it proves nothing.** This is the same class of error as the
masked-HTML trap: a 200 that means something other than what it looks like.
