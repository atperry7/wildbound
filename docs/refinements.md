# Wildbound — Refinement Backlog

Open polish items only — the working log. *Known, accepted* rough edges; the core loop works, these make
it feel better. When something ships, move it to [`completed-refinements.md`](completed-refinements.md)
(rewritten to describe what was built) so this list stays short.

**Per item:** `subsystem` tag · rough effort **S**/**M**/**L** · a `→` source pointer. Checkboxes track
open work. _Needs a decision_ is blocked on a design call (not code); _Accepted / by design_ are choices,
not tasks.

## Active — ready to work

- [ ] **Bats stack on top of each other when following** · `bat` · M
  Multiple following bats converge on the same target (just above the owner's head) and overlap — one
  briefly vanished inside another. Every bat steers toward an identical point with no separation.
  *Idea:* per-bat orbit offset seeded by entity id; a light separation nudge under ~1 block; staggered
  follow heights.
  → `companion/bat/BatCompanion.java` · `followOwner`

- **Fox fetch edges** · `fox`
  The fox pathfinds to the nearest item (`FoxFetchItemGoal`) and acts as a ~1.5-block *mobile magnet*
  centred on itself — items in the bubble go to the owner via `playerTouch`; its `canPickUpLoot` is off so
  it can't pocket them. A `STUCK_TICKS` + `BLACKLIST_TICKS` backstop (counting down only after navigation
  is *done*) covers genuinely walled-off items. Remaining rough spots:
  - [ ] **(a) Full inventory** makes `playerTouch` a no-op, so the fox keeps re-targeting an item it can't
    deposit. *Idea:* skip items the owner's inventory can't accept. · S
  - [ ] **(b) Freshly-thrown items** — it chases one and waits out the ~2s pickup delay rather than
    ignoring it until pickable. *Idea:* defer targeting until the pickup delay clears. · S
  - *Knob (not a task):* `PICKUP_RADIUS` (1.5) is an internal feel value, deliberately not in config —
    adjust in code if grabbing reads as too magnet-y.
  → `companion/fox/FoxFetchItemGoal.java`

- [ ] **Axolotl follow on land** · `axolotl` · M
  Amphibious, so land-follow is a slow flop, and the follow-teleport (`canStandAt` wants air over solid)
  can strand it out of water.
  *Idea:* prefer water teleport targets.
  → `companion/axolotl/AxolotlCompanion.java`

## Needs a decision

- **Capstone tree wiring** · `advancements`
  Today the tree fans out: `root` → 8 animal advancements + the capstone, all siblings of `root`. The
  capstone already *requires* all types, so it's functionally "tame everything" — but visually it only
  connects to `root`.
  *Constraint:* MC advancements are **single-parent**, so "all animals draw a line into the capstone" isn't
  literally possible.
  *Options:* (a) leave as-is — requirements already gate it; (b) chain it after one specific animal so it
  reads as an endpoint (arbitrary); (c) move the per-animal advancements under a sub-node and place the
  capstone as the visible culmination off that node.
  → pick a layout, then it's `parent` edits in `data/wildbound/advancement/*.json`

## Accepted / by design

Choices we've made, not tasks. Revisit only if they annoy in practice.

- **Effect lingers ~16s after the last companion sits** — no active teardown, by design (see design-doc-v1
  "Passive Effect System"). The refresh just stops; the effect fades on its own.
- **Effect lingers if the only companion dies** (vs. sits) — a dead entity can't refresh or clear, so it
  fades at natural expiry. *Optional:* a death hook for a prompt clear.
- **Frog tongue snatches nearby slimes/small mobs** — characterful vanilla behaviour, left in; note it can
  eat baby mobs. Suppress only if it proves annoying.

## How to use this file

When something is "good enough for now," jot it under **Active** (or the right section) with a source
pointer and a one-line fix sketch, then move on. Pull items into a refinement pass later. When one ships,
move its entry to [`completed-refinements.md`](completed-refinements.md), rewritten to say what was built —
this file holds **open items only**.
