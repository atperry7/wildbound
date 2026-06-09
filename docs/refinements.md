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

- [ ] **Hard to aim where a bat perches** · `bat` · M
  Sit prefers a ceiling overhead, falls back to a ground perch below, hovers only when neither is in reach.
  But the player can't *aim* it — it takes the nearest surface straight up/down, not the block they're
  looking at.
  *Idea:* search a small cone/radius instead of a straight column; prefer the looked-at block; snap to the
  owner's targeted block face.
  → `companion/bat/BatCompanion.java` · `hangOrHover` / `findSurface`
  *Note:* ground-perch floats ~0.1 block above the surface (vanilla resting pins to `floor(y)+0.1`).
  Negligible; if it bugs us, drive a non-resting grounded pose instead.

- [ ] **Wild predators still hunt tamed prey** · `combat` · M
  `MobCanAttackMixin` only stops a *companion* attacking another *companion* (so the companion fox no longer
  kills the owner's rabbit). A *wild* fox/wolf can still hunt a tamed rabbit/companion.
  *Idea:* protect companions from all non-owned attackers, or make tamed prey not match predator targeting
  selectors.
  → `mixin/MobCanAttackMixin.java`

- **Fox fetch edges** · `fox`
  The fox pathfinds to the nearest item (`FoxFetchItemGoal`) and acts as a ~1.5-block *mobile magnet*
  centred on itself — items in the bubble go to the owner via `playerTouch`; its `canPickUpLoot` is off so
  it can't pocket them. A `STUCK_TICKS` + `BLACKLIST_TICKS` backstop (counting down only after navigation
  is *done*) covers genuinely walled-off items. Remaining rough spots:
  - [ ] **(a) Full inventory** makes `playerTouch` a no-op, so the fox keeps re-targeting an item it can't
    deposit. *Idea:* skip items the owner's inventory can't accept. · S
  - [ ] **(b) Freshly-thrown items** — it chases one and waits out the ~2s pickup delay rather than
    ignoring it until pickable. *Idea:* defer targeting until the pickup delay clears. · S
  - [ ] **(c) No persistent "fetching" indicator** — same no-status-icon gap as the ocelot (see _Needs a
    decision → Persistent indicator_). · —
  - *Knob (not a task):* `PICKUP_RADIUS` (1.5) is an internal feel value, deliberately not in config —
    adjust in code if grabbing reads as too magnet-y.
  → `companion/fox/FoxFetchItemGoal.java`

- [ ] **Axolotl follow on land** · `axolotl` · M
  Amphibious, so land-follow is a slow flop, and the follow-teleport (`canStandAt` wants air over solid)
  can strand it out of water.
  *Idea:* prefer water teleport targets.
  → `companion/axolotl/AxolotlCompanion.java`

- [ ] **Armadillo curls up mid-follow** · `armadillo` · S
  May still roll up when it senses a threat (e.g. a sprinting player) while following. Cosmetic.
  *Idea:* suppress the threat-roll while following, like the flee-suppress.
  → `companion/armadillo/ArmadilloCompanion.java`

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

- **Persistent "passive active" indicator for non-effect companions** · `ocelot` `fox`
  The status-effect companions show a HUD effect icon while active; the **ocelot** (XP ×2) and **fox** (item
  fetch) have no status effect, so they can't. The ocelot only sparkles green per XP-gain; the fox has no
  cue at all.
  *Tension:* the cleanest icon would be a **custom MobEffect** used purely as an indicator — but a new
  effect crosses the design doc's "vanilla items/effects only" rule, and no *vanilla* effect is a clean
  no-op indicator (Luck affects loot, etc.).
  *Decision:* accept the custom-effect exception for an indicator, or settle for a non-effect cue (a faint
  periodic particle while the companion follows, or a future custom HUD widget).

## Verify in-game

Compiles and loads headlessly; needs an in-client play-test to confirm feel.

- [ ] **Bee sit pose** · `bee` — no vanilla rest-pose flag, so sit makes it fly down and land
  (`controlsSitMovement` + `onSitTick`). Watch: flying move-control vs. our descent, and whether it lands
  cleanly vs. bobbing. → `companion/bee/BeeCompanion.java`
- [ ] **Per-animal wander feel** · `all` — wander roams on vanilla AI; confirm it reads well per species.
- [ ] **Bat wander-leash boundary** · `bat` — the bat's leash is a 3D sphere (pulls back on height too);
  confirm it doesn't feel jittery at the edge. → `companion/bat/BatCompanion.java` · `leashWander`

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
