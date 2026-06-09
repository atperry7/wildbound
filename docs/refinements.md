# Wildbound — Refinement Backlog

Polish items deferred from initial implementation. These are *known, accepted* rough edges — the core
loop works; these make it feel better. Grouped loosely; not strictly ordered.

## Bat movement & perching

- **Bats stack on top of each other when following.** Multiple following bats converge on the same
  target point (just above the owner's head) and overlap. One briefly disappeared inside another.
  - *Likely cause:* every bat steers toward an identical target with no separation/spread.
  - *Ideas:* per-bat target offset (ring/orbit around the owner, seeded by entity id); a light
    separation nudge when two companions are within ~1 block; stagger follow heights.
  - Source: `BatCompanion.followOwner` in `companion/bat/BatCompanion.java`.

- **Hard to aim where a bat perches.** Sit now prefers a ceiling overhead, falls back to perching on
  the ground below, and only hovers when neither is in reach (over a drop). But the player still can't
  *aim* it — it picks the nearest surface straight up/down, not the block they're looking at.
  - *Ideas:* search a small cone/radius rather than a straight column; prefer the block the player is
    looking at; snap to the owner's targeted block face.
  - Source: `BatCompanion.hangOrHover` / `findSurface` in `companion/bat/BatCompanion.java`.
  - *Note:* ground-perch floats ~0.1 block above the surface (vanilla resting pins to `floor(y)+0.1`).
    Negligible, but if it bugs us, drive a non-resting grounded pose instead.

## Roster movement quirks (new companions, accepted for now)

These ride the generic goal-based path (follow via navigation; sit holds position via `CompanionSitGoal`,
which now also delegates a natural pose through `CompanionType.onStartSitting/onSitTick/onStopSitting`).

Sit poses now wired:
- **Panda** — uses vanilla `sit(true)` pose. ✅
- **Armadillo** — rolls into a ball on sit (vanilla scared state, which peeks periodically on its own). ✅
- **Axolotl** — the "wandered while sitting" bug is fixed (sit goal now zeroes residual horizontal
  velocity each tick, needed for swimmers). ✅
- **Fox** — lies down on sit (vanilla sleeping pose, via a `setSleeping` invoker). ✅
- **Bee** — no vanilla rest-pose flag exists, so sit makes it fly down and land on the ground
  (`controlsSitMovement` + `onSitTick`). **Best-effort — verify in-game; may need tuning** (flying move
  control vs. our descent, and whether it lands cleanly vs. bobbing).

Still open:
- **Wild predators still hunt tamed prey.** `MobCanAttackMixin` only stops a *companion* from attacking
  another *companion* (fixed: companion fox no longer kills the owner's rabbit). A *wild* fox/wolf can
  still hunt a tamed rabbit/companion. *Idea:* optionally protect companions from all non-owned attackers,
  or make tamed prey not match predator targeting selectors.
- **Ocelot bonus indicator is per-gain only.** A green sparkle shows on the ocelot when it doubles XP, but
  there is no persistent "bonus active" cue like the status-effect companions' inventory icon. *Idea:* a
  subtle persistent indicator (e.g. faint periodic particle while an ocelot follows), or surface it in a HUD.
- **Fox fetch edges.** The fox pathfinds to the nearest item (`FoxFetchItemGoal`) and acts as a ~1.5-block
  *mobile magnet* centred on itself — items in the bubble go to the owner via `playerTouch`; its vanilla
  `canPickUpLoot` is off so it can't pocket items. The bubble being wider than where pathfinding parks grabs
  edge-of-block items that used to freeze it; a `STUCK_TICKS` + `BLACKLIST_TICKS` backstop (countdown only
  after the navigation is *done*, so far items aren't abandoned mid-walk) covers genuinely walled-off items.
  Remaining rough spots: (a) a full inventory makes `playerTouch` no-op, so the fox keeps re-targeting an
  item it can't deposit; (b) it chases a freshly thrown item and waits out the ~2s pickup delay rather than
  ignoring it until pickable; (c) no persistent "fetching" indicator (same no-status-icon gap as the ocelot
  above); (d) `PICKUP_RADIUS` (1.5) is an internal feel knob, deliberately not in config — adjust in code if
  grabbing reads as too magnet-y. *Ideas:* skip
  items the owner's inventory can't accept; defer targeting until the pickup delay clears.
- **Axolotl follow on land** — amphibious, so following on land is a slow flop, and the follow-teleport
  (`canStandAt` wants air over solid) can strand it out of water. *Idea:* prefer water teleport targets.
- **Armadillo follow** — may still curl up mid-follow if it senses a threat (sprinting player). Cosmetic;
  *idea:* suppress the threat-roll while following, like the flee-suppress.
- **Frog** — jump-follows fine; its tongue still snatches nearby slimes/small mobs (characterful, but
  note it eats baby mobs).

## Effect lifecycle (accepted trade-offs, revisit only if they annoy in practice)

- Sitting your *last* companion leaves the passive effect lingering up to ~16s (no active teardown,
  by design — see design-doc-v1 "Passive Effect System"). Revisit only if players find the linger
  confusing.
- If your *only* companion dies (vs. sits), the effect also lingers to natural expiry, since a dead
  entity can't refresh or clear. Optional: a death hook for prompt clear.

## Advancements

- **Taming-item hint — covered by the icon.** ✅ Each per-animal advancement's icon *is* its taming item
  (bat = spider eye, panda = bamboo, ...), so the badge already hints what to use. Only nuance: the **Bee**
  icon is a poppy but taming accepts *any* flower — clarify in its description only if it proves confusing.

- **Capstone tree wiring — wants all animals to lead into "The Wild Knows Your Name".** Today the tree
  fans out: `root` → 7 animal advancements + the capstone, all siblings of `root`. The capstone already
  *requires* all 8 types (its criteria/requirements), so it's functionally the "tame everything" goal —
  but visually it only connects to `root`.
  - **Constraint:** Minecraft advancements are **single-parent** — you cannot have all 7 draw lines into
    the capstone. So "all point to the big one" isn't literally possible in the tree.
  - **Options to consider:** (a) leave as-is — capstone is a `root` child, requirements already gate it;
    (b) chain it after a specific animal so it reads as an endpoint (arbitrary); (c) move the per-animal
    advancements under a sub-node and place the capstone as the visible culmination off that node. Pick
    the layout, then it's just `parent` edits in the JSON.

## Indicators

- **Effect-pet HUD icon** — now ON (showIcon=true, particles off). The seven status-effect companions
  show their effect icon while their buff is active. ✅
- **Ocelot HUD indicator** — the ocelot has no status effect, so it can't show an effect icon; right now it
  only sparkles green when it doubles XP. Idea: give the ocelot a persistent "active" cue similar to an
  effect icon — e.g. a **custom green XP-bonus MobEffect** used purely as an indicator. ⚠️ *Tension:* a
  custom effect is a new effect, which crosses the design doc's "vanilla items/effects only" rule. Decide
  whether the indicator is worth that exception, or settle for a non-effect cue (periodic faint particle
  while an ocelot follows, or a future custom HUD widget). No vanilla effect is a clean no-op indicator (Luck
  affects loot, etc.). The fox (item fetch) shares this no-status-icon gap.

## Companion modes / interactions

- **"Buff off, still following" toggle.** Let the player keep a pet following but turn its passive OFF —
  e.g. hold an item and **sneak + right-click**. Useful when they want the companion around but not the
  effect (avoid effect clutter, or save it for when needed).
  - *Implementation sketch:* a per-companion `buffEnabled` flag (attachment, default true). When false,
    `CompanionBehavior.refreshPassive` skips applying (and `OcelotXpBonus`/`hasActiveCompanion` treat it as
    inactive). Follow/sit behaviour unchanged.
  - *Interaction map (now settled by the wander work):* empty-hand RC = SIT↔FOLLOW; sneak + empty-hand
    RC = WANDER↔FOLLOW; held-item-on-untamed = taming. So buff-off should take the remaining lane —
    **held-item-on-tamed** (e.g. sneak + RC while holding a specific item, on your own companion) — rather
    than reusing sneak+empty-hand RC, which now toggles wander. Pick the trigger item, then `CompanionTaming`
    grows one more branch (held + tamed + owner).

- **"Wander" mode (third mode beyond follow/sit).** ✅ **Done.** `SITTING` boolean promoted to a
  `CompanionMode` enum attachment (FOLLOW / SIT / WANDER). Empty-hand RC toggles SIT↔FOLLOW; sneak +
  empty-hand RC toggles WANDER↔FOLLOW (`CompanionTaming.toggleMode`). In WANDER the companion roams on
  vanilla AI, grants no passive, but stays owned (persists, no-flee, not hunted by other companions). The
  bat returns `false` from `serverTickBehavior` in WANDER (handing flight to vanilla). **Verify in-client:**
  the held-item lane is still reserved for the future buff-off toggle (see next item). *Best-effort —
  in-game pass still wanted for wander movement feel per-animal.*
  - **Wander leash (12 blocks).** ✅ Entering WANDER anchors the companion to that spot (`WANDER_ANCHOR`
    attachment); goal mobs use vanilla's home-point (`setHomeTo` + `MoveTowardsRestrictionGoal`), the bat
    steers itself back (`BatCompanion.leashWander`). So a wandering pet stays in a ~12-block bubble and
    doesn't get lost. The radius is per-mob configurable (`wanderRadius`, default 12). *Possible follow-up:*
    the bat's leash is a 3D sphere (pulls back on height too) — fine, but verify it doesn't feel jittery at
    the boundary.
  - **Teaches itself in-game.** ✅ "Home on the Range" advancement (`wildbound:companion_wandered`, fired
    from `CompanionTaming` on first wander) names the mechanic; its greyed-out description hints the control
    (sneak + use).

## How to use this file

When something is "good enough for now," jot it here with the source location and a sketch of the
fix, then move on. Pull items into a refinement pass later.
