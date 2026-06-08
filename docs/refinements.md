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
- **Fox bonus indicator is per-gain only.** A green sparkle shows on the fox when it doubles XP, but there
  is no persistent "bonus active" cue like the status-effect companions' inventory icon. *Idea:* a subtle
  persistent indicator (e.g. faint periodic particle while a fox follows), or surface it in a future HUD.
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

## Indicators

- **Effect-pet HUD icon** — now ON (showIcon=true, particles off). The seven status-effect companions
  show their effect icon while their buff is active. ✅
- **Fox HUD indicator** — the fox has no status effect, so it can't show an effect icon; right now it
  only sparkles green when it doubles XP. Idea: give the fox a persistent "active" cue similar to an
  effect icon — e.g. a **custom green XP-bonus MobEffect** used purely as an indicator. ⚠️ *Tension:* a
  custom effect is a new effect, which crosses the design doc's "vanilla items/effects only" rule. Decide
  whether the indicator is worth that exception, or settle for a non-effect cue (periodic faint particle
  while a fox follows, or a future custom HUD widget). No vanilla effect is a clean no-op indicator (Luck
  affects loot, etc.).

## Companion modes / interactions

- **"Buff off, still following" toggle.** Let the player keep a pet following but turn its passive OFF —
  e.g. hold an item and **sneak + right-click**. Useful when they want the companion around but not the
  effect (avoid effect clutter, or save it for when needed).
  - *Implementation sketch:* a per-companion `buffEnabled` flag (attachment, default true). When false,
    `CompanionBehavior.refreshPassive` skips applying (and `FoxXpBonus`/`hasActiveCompanion` treat it as
    inactive). Follow/sit behaviour unchanged.
  - *Interaction collision:* this and the "wander" idea both want sneak+right-click. Need ONE coherent
    interaction map. Proposal to pin down: empty-hand RC = sit/follow toggle (current); sneak + empty-hand
    RC = cycle/secondary mode; held-item interactions reserved (taming already uses held item on untamed).
    Resolve before implementing either, so controls stay predictable.

- **"Wander" mode (third mode beyond follow/sit).** Toggle with **shift + right-click** (sneak + use)
  on a tamed companion. In wander mode the animal roams freely on its vanilla AI — it does *not*
  follow, does *not* stay sitting, and grants **no passive effect**. It stays owned (persists, doesn't
  despawn, doesn't flee its owner). Use case: letting tamed animals mill around a base/pen as living
  decoration without buffing or trailing the player.
  - *Implementation sketch:*
    - State is currently a `SITTING` boolean. Promote to a tri-state mode (FOLLOW / SIT / WANDER) —
      e.g. an enum attachment, or add a `WANDER` boolean alongside `SITTING`.
    - Interaction: empty-hand right-click keeps toggling sit/follow (`CompanionTaming`); add a
      sneak-right-click branch that toggles WANDER. Read sneak via `player.isShiftKeyDown()`.
    - Passive effect: `CompanionBehavior.refreshPassive` should only apply in FOLLOW (wander = inactive,
      same as sit — it already just stops refreshing).
    - Goals: `CompanionFollowOwnerGoal` / `CompanionSitGoal` `canUse` must be false in WANDER so vanilla
      goals take over; the bat's `serverTickBehavior` should return `false` (let vanilla fly) in WANDER.
    - Keep `setPersistenceRequired` and the no-flee behavior regardless of mode.
  - Touches: `WildboundAttachments`, `CompanionBehavior`, `CompanionTaming`, `BatCompanion`,
    `CompanionFollowOwnerGoal`, `CompanionSitGoal`.

## How to use this file

When something is "good enough for now," jot it here with the source location and a sketch of the
fix, then move on. Pull items into a refinement pass later.
