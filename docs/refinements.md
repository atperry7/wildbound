# Wildbound — Refinement Backlog

Open polish items only — the working log. *Known, accepted* rough edges; the core loop works, these make
it feel better. When something ships, move it to [`completed-refinements.md`](completed-refinements.md)
(rewritten to describe what was built) so this list stays short.

**Per item:** `subsystem` tag · rough effort **S**/**M**/**L** · a `→` source pointer. Checkboxes track
open work. _Needs a decision_ is blocked on a design call (not code); _Accepted / by design_ are choices,
not tasks.

## Active — ready to work

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

- [ ] **Turtle follow feel (play-test)** · `turtle` · M
  Turtles are very slow on land and have vanilla "home beach" pathing, so a following turtle trails far
  behind and may tug toward its home. Compiles + loads (11 companions); never tried in-client. Same family
  as the axolotl land-follow item — likely wants water-preferred teleport and/or a speed nudge while following.
  *Idea:* shared "aquatic follow" helper for axolotl + turtle.
  → `companion/turtle/TurtleCompanion.java`

## Needs a decision

_(none open)_

## Accepted / by design

Choices we've made, not tasks. Revisit only if they annoy in practice.

- **Effect lingers ~16s after the last companion sits** — no active teardown, by design (see design-doc-v1
  "Passive Effect System"). The refresh just stops; the effect fades on its own.
- **Effect lingers if the only companion dies** (vs. sits) — a dead entity can't refresh or clear, so it
  fades at natural expiry. *Optional:* a death hook for a prompt clear.
- **Frog tongue snatches nearby slimes/small mobs** — characterful vanilla behaviour, left in; note it can
  eat baby mobs. Suppress only if it proves annoying.
- **Night Vision granted by both the bat and the axolotl** — intentional, not a duplicate to dedupe. The
  bat is a land/cave light source (it won't dive); the axolotl carries that vision *underwater*, where it
  pairs with the turtle's Water Breathing. Same effect, two non-overlapping use contexts.

## How to use this file

When something is "good enough for now," jot it under **Active** (or the right section) with a source
pointer and a one-line fix sketch, then move on. Pull items into a refinement pass later. When one ships,
move its entry to [`completed-refinements.md`](completed-refinements.md), rewritten to say what was built —
this file holds **open items only**.

A feature isn't "shipped" until it's verified complete, and for gameplay/visual behaviour verification
**includes the in-client pass** — there's no separate "verify in-game" parking lot. If a change compiles and
loads but still needs a play-test, it's not done: keep it under **Active** (note the pending play-test in its
sketch) until the client pass confirms it, then move it to the archive.
