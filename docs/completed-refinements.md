# Wildbound — Completed Refinements

Refinements that have shipped, moved out of [`refinements.md`](refinements.md) (the working backlog) so
that file stays a list of *open* rough edges. Kept here for history: what the rough edge was and how it
was resolved. Newest at the bottom of each group.

## Sit poses

Generic sit delegates a natural per-animal pose via `CompanionType.onStartSitting/onSitTick/onStopSitting`,
driven by `CompanionSitGoal` (which also zeroes residual horizontal velocity each tick).

- **Panda** — uses vanilla `sit(true)` pose. ✅
- **Armadillo** — rolls into a ball on sit (vanilla scared state, which peeks periodically on its own). ✅
- **Axolotl** — the "wandered while sitting" bug is fixed (the per-tick velocity zeroing above, needed for
  swimmers). ✅
- **Fox** — lies down on sit (vanilla sleeping pose, via a `setSleeping` invoker). ✅

> The **Bee** sit pose (fly down and land) is still best-effort pending an in-game tuning pass — it stays
> in the working backlog.

## Modes & toggles

- **"Wander" mode (third mode beyond follow/sit).** ✅ `SITTING` boolean promoted to a `CompanionMode`
  enum attachment (FOLLOW / SIT / WANDER). Empty-hand RC toggles SIT↔FOLLOW; sneak + empty-hand RC toggles
  WANDER↔FOLLOW (`CompanionTaming.toggleMode`). In WANDER the companion roams on vanilla AI, grants no
  passive, but stays owned (persists, no-flee, not hunted by other companions). The bat returns `false`
  from `serverTickBehavior` in WANDER (handing flight to vanilla). *In-game pass for per-animal wander feel
  was still wanted at archive time — re-open a backlog item if it reads off.*
  - **Wander leash (12 blocks).** ✅ Entering WANDER anchors the companion to that spot (`WANDER_ANCHOR`
    attachment); goal mobs use vanilla's home-point (`setHomeTo` + `MoveTowardsRestrictionGoal`), the bat
    steers itself back (`BatCompanion.leashWander`). Radius is per-mob configurable (`wanderRadius`,
    default 12). The bat's leash is a 3D sphere (pulls back on height too) — verify it doesn't feel jittery
    at the boundary.
  - **Teaches itself in-game.** ✅ "Home on the Range" advancement (`wildbound:companion_wandered`, fired
    from `CompanionTaming` on first wander) names the mechanic; its greyed-out description hints the control
    (sneak + use).

- **"Buff off, still following" toggle.** ✅ Shipped as the **milk-bucket buff toggle**: right-click your
  own companion with a milk bucket to quiet/restore its passive (`BUFF_DISABLED` attachment;
  `CompanionBehavior.refreshPassive` skips when disabled, and `OcelotXpBonus`/`hasActiveCompanion` treat a
  quieted companion as inactive — follow/sit behaviour unchanged). This took the **held-item-on-tamed**
  lane the original sketch identified as correct, since sneak + empty-hand RC went to wander instead. A
  smoke puff + low chime mark "quieted"; a sparkle + bright chime mark "restored."

- **Consistent "is it sitting?" cue across all companions.** ✅ Not every companion has a distinct sit
  pose, so mode state wasn't legible. Each empty-hand mode toggle now emits a coloured dust puff + an
  `AMETHYST_BLOCK_CHIME` whose pitch rises with engagement — **white / low = SIT, purple / mid = WANDER,
  gold / high = FOLLOW** (`CompanionTaming.announceModeToggle`). Deliberately a one-shot event, not a
  persistent particle, so it doesn't clutter a build a companion is parked into. Mirrors the milk-bucket
  toggle's feedback style.

## Indicators

- **Effect-pet HUD icon.** ✅ Now ON (`showIcon=true`, particles off). The status-effect companions show
  their effect icon while their buff is active.

> The **ocelot/fox** non-status companions still lack a *persistent* "active" indicator — that gap stays
> in the working backlog (the ocelot's per-gain sparkle was made more visible, but a persistent cue is
> still open).

## Advancements

- **Taming-item hint — covered by the icon.** ✅ Each per-animal advancement's icon *is* its taming item
  (bat = spider eye, panda = bamboo, …), so the badge already hints what to use. Only nuance: the **Bee**
  icon is a single flower but taming accepts *any* flower — clarify in its description only if it proves
  confusing.
