# Wildbound — Refinement Backlog

Open polish items only — the working log. *Known, accepted* rough edges; the core loop works, these make
it feel better. When something ships, move it to [`completed-refinements.md`](completed-refinements.md)
(rewritten to describe what was built) so this list stays short.

**Per item:** `subsystem` tag · rough effort **S**/**M**/**L** · a `→` source pointer. Checkboxes track
open work. _Needs a decision_ is blocked on a design call (not code); _Accepted / by design_ are choices,
not tasks.

## Active — ready to work

_(none open)_

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
- **Aquatic companions (axolotl, turtle) are clumsy following on land** — amphibious/slow-on-land mobs flop
  along behind the owner, and the follow-teleport (`canStandAt` wants air over solid) can drop them out of
  water. Previously an Active item (water-preferred teleport / aquatic-follow helper); **de-scoped** now that
  **companion capture** exists — carry an aquatic companion in a bound cluster across dry stretches rather
  than reworking land pathing. Revisit only if the land-follow flop annoys in practice.
  → `companion/axolotl/AxolotlCompanion.java`, `companion/turtle/TurtleCompanion.java`
- **Night Vision granted by both the bat and the axolotl** — intentional, not a duplicate to dedupe. The
  bat is a land/cave light source (it won't dive); the axolotl carries that vision *underwater*, where it
  pairs with the turtle's Water Breathing. Same effect, two non-overlapping use contexts.
- **Rideable sheep makes the mod client+server, and `SheepMixin` stays a *common* mixin** — every other
  feature is server-authoritative and works for a vanilla client (attach-to-vanilla design: no custom
  registries to sync). The sheep's ridden steering + charged jump are the exception: the controlling client
  computes them (`LocalPlayer.jumpableVehicle()` does `instanceof PlayerRideableJumping`; the jump impulse in
  `tickRidden` is gated by `isLocalInstanceAuthoritative()`), so the mod is required client-side for that one
  feature. Do **not** move `SheepMixin` to `src/client` — the *server* also needs the
  `getControllingPassenger`/`PlayerRideableJumping` overrides to route ridden movement, show correct motion to
  other players, and receive the jump packet (vanilla `AbstractHorse` is likewise a common class). The
  client-coupling is a runtime branch, not a code-location concern. → `mixin/SheepMixin.java`

## How to use this file

When something is "good enough for now," jot it under **Active** (or the right section) with a source
pointer and a one-line fix sketch, then move on. Pull items into a refinement pass later. When one ships,
move its entry to [`completed-refinements.md`](completed-refinements.md), rewritten to say what was built —
this file holds **open items only**.

A feature isn't "shipped" until it's verified complete, and for gameplay/visual behaviour verification
**includes the in-client pass** — there's no separate "verify in-game" parking lot. If a change compiles and
loads but still needs a play-test, it's not done: keep it under **Active** (note the pending play-test in its
sketch) until the client pass confirms it, then move it to the archive.
