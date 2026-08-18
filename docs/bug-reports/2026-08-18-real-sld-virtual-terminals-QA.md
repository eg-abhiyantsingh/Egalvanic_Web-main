# Real SLD — markup stager, virtual terminals, edge repointing, PDF export parity — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-backend **#984** · eg-pz-frontend **#1161** · eg-pz-frontend **#1162** (lockfile)

---

## Verdict — deployed on QA; the shared model is confirmed serialized; the canvas/PDF behaviors need a hands-on GoJS pass

Both halves are live on QA (tested directly, not read off the deploy note). The backend virtual-terminal model reaches the client and the frontend carries the new code. But this is a GoJS-canvas + PDF-render epic — the substance (virtual-terminal dots, edge repointing, the markup stager, PDF parity) is interactive canvas work that automation can't verify reliably, and no existing SLD has been through the pipeline yet, so the new fields are present-but-empty.

## ✅ Confirmed deployed

**Backend #984 — the shared virtual-terminal model is serialized to the client:**
- Every **edge** now carries a first-class **`edge_manips`** field (`GET /api/sld/{id}` → 113 edges, all have the key), plus `source_node_terminal_id` / `target_node_terminal_id` / `source_handle` / `target_handle`.
- Every **node** carries **`node_manips`** and **`node_terminals`** fields (154 nodes).
- **`max_connections` is now serialized** — 342 occurrences in one SLD document. This is #984's named fix: *"NodeTerminal.to_dict now serializes max_connections, which previously never reached any client so no terminal connection limit could be enforced."* The field now reaches the client (the precondition for enforcement).

**Frontend #1161 — the new code is in the shipped bundle:** `virtual terminal`, `edge_manips`, `node_manips`, `busOff`, `Edit Label`, `markup` (31×), `Insert cable`, `max_connections`. (`pixi` — the #1162 dependency — is in a lazy chunk not scanned; its presence is implied by the bundle building at all.)

So the "none is in QA yet" note is **stale** — consistent with most tickets this session.

## ⚠️ Present but empty — the model is live, no SLD has used it yet

Across **6 SLDs** scanned (Android Site 2, Z-Hospital, ZTest, demo, …): `edge_manips` = `null`, `node_manips` = `null`, **zero** non-null `max_connections`, **zero** `busOff`, **zero** populated `elbows`. That's expected, not a defect: these fields are populated when an edge/terminal is manipulated on the GoJS canvas or when an SLD is brought in through the **markup-import** pipeline — and no existing SLD has been through either. The scaffolding is deployed and ready; there's just no live virtual-terminal data to inspect.

**Consequence:** I could not show a *populated* `edge_manips` (`{from|to:{slot,busOff}, elbows}`), a *non-null* `max_connections`, or the terminal-slot placements — because none exists on QA yet. Producing that state requires driving the canvas (drag a terminal, repoint an edge) or running a markup import.

## Not exercised — needs a hands-on GoJS / PDF pass

These are the substance of the epic and are genuinely hard to automate (GoJS canvas interaction + PDF pixel output):

- **Virtual terminals on the canvas** — one dot per occupant; terminals with nothing wired no longer render (the PR removed 221 empty dots); tooltips on each dot; slots persist on drag.
- **Edge repointing** — latch to the node body, then pick a terminal; minting a new virtual terminal of the picked blueprint with an explicit slot pin that survives reload; picker enforces **direction** (inbound → LINE/BOTH, outbound → LOAD/BOTH) and **max_connections**.
- **Markup review stager (WYSIWYG)** — per-node-class default datablocks from PDF-extracted fields, EG-class icons scaled to node dims, custom SVG symbols, label justify/clamp, live Edit Label dialog.
- **PDF export parity** — reads `node_manips`/`node_design`/`icon_svg`/`edge_manips` with the three-policy symbol layer (art/eg/legacy). *(I could not even locate the PDF export endpoint via API — it's built dynamically; it's an SLD-editor export action.)*
- **SLD Issues** — split edges inherit predecessor pins; `resolve_overcrowded_terminal` strips a stale busOff. (The `resolve-overcrowded-terminals` endpoint exists — confirmed on a prior ticket — but needs a real overcrowded terminal to exercise.)
- **Fixes** — labels follow nodes through straighten/align/distribute/magicLayout; tie breakers with `side=BOTH` connectable at both ends; Insert cable on bus→CB→bus; insert actions withheld while locked.

## ❌ Out of reach

- **Migration `vsq_a1`** — retires two of four vertical-square terminals (Cable/Busway/Node Bus) and remaps edges by role, editing the **global** `node_orientation_terminals` table. This is a DB migration with a cross-tenant blast radius; a backend engineer must review the docstring safety argument (99% of cables already flow top-in/bottom-out) and confirm single-head at `vsq_a1`. Not verifiable by me.

## Recommendation

The model layer is confirmed on QA. To verify the epic's substance, this needs a **manual SLD-editor session**: open a real SLD, drag a terminal (confirm a dot renders + slot persists on reload), repoint an edge (confirm the picker enforces direction + max_connections and the attachment survives reload), run a markup import through the stager, and export a PDF to eyeball parity. I can drive parts of that in the browser, but GoJS drag-and-connect and PDF pixel comparison are where automation gets unreliable — a human pass is the right call here, which the epic's own scope implies.

## Method notes
- Backend confirmed by direct use: `GET /api/sld/{id}` returns the new fields; scanned 6 SLDs for populated data.
- Frontend confirmed via shipped bundle strings.
- Read-only; nothing created or modified.
