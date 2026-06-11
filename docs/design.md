# Wildbound — Design (living document)

**Platform:** Java Edition · Fabric · MC **26.2-rc-1** (port branch; re-pin to the 26.2 floor at release) · Mod ID `wildbound`

A vanilla+ Fabric mod that tames passive mobs that have never been tameable before. Each companion
grants a low-tier passive bonus to its owner while in range — a gentle reward for the bond between
player and animal.

> This document describes the mod **as built** and is kept current — when behaviour changes, this is
> the file that changes with it. History and the why-of-each-change live in git (commit bodies carry
> rationale and play-test status); the open backlog lives in [`refinements.md`](refinements.md).
> All names are **Mojang names**, matching the code.

---

## Design philosophy

- Feel like a natural extension of vanilla taming — vanilla mobs, sounds, particles, and mechanics.
- **No new entity types** — tamed state attaches to the existing vanilla mob (see Architecture). New
  *registered items* are allowed sparingly where vanilla has no carrier for a mechanic (the bound
  cluster is the only one); new *taming* items are not — taming uses an item already in vanilla.
- Balanced: passives are useful but low-tier, never replacing potions or gear (Level I by default).
- Respect each animal's vanilla identity (diet, habitat, behaviour).
- Extensible: adding a companion should require minimal new code and no mixins.

## Companion roster (11)

Taming is **universal** — every animal is tamed with an amethyst shard (see Taming). Each animal's
signature food survives only as its advancement icon (`CompanionType.tamingItem()`).

| Animal | Perk | Why it fits | Icon food |
|---|---|---|---|
| Bat | Night Vision I | Bats navigate in the dark; your bat extends that sense to you | Spider Eye |
| Rabbit | Jump Boost I | Rabbits leap; being around one makes you nimble | Dandelion |
| Frog | Slow Falling I | Frogs absorb impact; their instinct transfers to you near ledges | Slime Ball |
| Bee | Haste I | Bees are tireless workers; their energy is contagious | Poppy |
| Panda | Regeneration I | Pandas are resilient and unhurried; their calm restores you | Bamboo |
| Armadillo | Resistance I | Armadillos curl into armor; their presence fortifies you | Spider Eye |
| Axolotl | Night Vision I | Underwater vision the land-bound bat can't provide (it won't dive) | Tropical Fish |
| Turtle | Water Breathing I | The shell's gift, mirroring the turtle helmet | Seagrass |
| Ocelot | **XP ×2** (non-effect) | Their cunning sharpens what you learn from every encounter | Cod |
| Fox | **Item fetch** (non-effect) | Foxes waste nothing — yours collects for you | Sweet Berries |
| Sheep | **Rideable mount** (non-effect) | The flock's bravest lets you climb on | Apple |

---

## Architecture — attach to vanilla

Tamed state is **attached to the existing vanilla mob** via the Fabric Attachment API; we do not
register `Tamed*Entity` types or swap the mob on taming. A wild bat and a tamed bat are the *same*
entity instance — taming just attaches an owner. This keeps entity identity (age, name tag, UUID)
stable and needs no renderer/attribute wiring; per-animal code is one small class.

### State (`companion/WildboundAttachments.java`, all persistent)

| Attachment | Meaning |
|---|---|
| `OWNER` (UUID) | Its **presence** is what marks a mob as a companion — "tamed" is not a separate flag |
| `MODE` (`CompanionMode`) | `FOLLOW` / `SIT` / `WANDER`; absent = `FOLLOW` |
| `WANDER_ANCHOR` (BlockPos) | Where a wandering companion is leashed (set on entering WANDER) |
| `BUFF_DISABLED` (boolean) | Milk-bucket quiet; absent = false (passive active) |

Read mode via `CompanionBehavior.isFollowing` / `isSitting` / `isWandering` — **`isFollowing` and
`grantsPassive` also check `isCompanion`**, because `getMode` defaults *any* mob (even untamed) to
`FOLLOW`.

**Mode semantics — each control has one job:**

- **FOLLOW** — trailing buff: follows the owner, grants the passive in range.
- **SIT** — *parked* buff: holds position (natural pose where the animal has one) and still grants
  the passive — plant the bee at the mine entrance and work the area without it underfoot.
- **WANDER** — off duty: roams on vanilla AI near its anchor, grants nothing, still owned
  (persists, won't flee, won't be hunted by other companions).
- **Milk quiet** — the deliberate buff off-switch, orthogonal to mode.

### Per-animal definition: `CompanionType`

Subclass `CompanionType` and register it in `CompanionRegistry.init()` keyed by `EntityType`.

| Member | Purpose |
|---|---|
| `tamingItem()` | Advancement icon only (taming is universal) |
| `passiveEffect()` | `Holder<MobEffect>`, or `null` for non-effect perks (ocelot/fox/sheep) |
| `passiveAmplifier()` / `tamingChanceOneInN()` / `wanderLeashRadius()` | Mutable, config-backed (defaults 0 / 3 / 12) |
| `followSpeed()` | Follow-goal nav-speed multiplier (default 1.2; fast swimmers override down — axolotl 1.0) |
| `applyPassiveBonus(ServerPlayer)` | Default grants the status effect |
| `serverTickBehavior(mob, level, owner, mode)` | Per-tick override; return `true` to cancel vanilla AI (bat) |
| `attachGoals(mob, goals)` | Type-specific goals (fox fetch) |
| `onOwnerEmptyHandUse(mob, owner, sneaking)` | Claim the empty-hand gesture before the mode toggle (sheep mount) |
| `onStartSitting` / `onSitTick` / `onStopSitting`, `controlsSitMovement()` | Sit-pose hooks (panda sit, armadillo roll, fox sleep, bee fly-down-and-land) |
| `modeToggleSound(mob)` | Mode-toggle voice; defaults to the mob's own ambient sound |

### Shared runtime

- `CompanionBehavior` — ownership/mode access, the per-tick driver `serverTick` (passive refresh +
  wander-leash sync + `serverTickBehavior` delegation), `findActiveCompanion`/`hasActiveCompanion`.
- `CompanionTaming` — taming, mode toggles, milk quiet, and capture, all via one Fabric
  `UseEntityCallback` (no per-animal interact mixin).
- **Goals** — a single `ServerEntityEvents.ENTITY_LOAD` hook attaches the shared goals
  (`CompanionSitGoal`, `CompanionFollowOwnerGoal` — start 7, stop 2.5, teleport 16 blocks —
  `CompanionTickGoal`, all priority 0) plus the type's `attachGoals` to any registered companion.
  Type goals are attached *first*: priority ties break by registration order and a tie never
  preempts a running goal, which is how fox fetch outranks follow while loot is around. Goals are
  dormant until the mob is actually tamed. The follow goal ignores spectator owners.

### Movement: two paths

- **Goal-driven mobs** (every companion except the bat — ground/flying/swimming `PathfinderMob`s)
  ride the shared goals above. No per-animal mixin.
- **The bat** bypasses the goal system entirely (vanilla `Bat` extends `AmbientCreature`, registers
  no movement goals; flight is hand-rolled in `customServerAiStep`, resting pins position in
  `tick()`). `BatMixin` hooks `customServerAiStep` and `BatCompanion.serverTickBehavior` steers
  `deltaMovement` directly, mirroring vanilla's movement math: follow targets a point just above the
  owner's head (each bat orbits its own golden-angle slot with a height stagger, so a flock spreads
  out instead of stacking), sit reuses the native resting flag (ceiling hang preferred, ground perch
  fallback), and in WANDER it returns `false`, handing flight back to vanilla while
  `BatCompanion.leashWander` steers it back inside the leash radius.

### Wander leash

Entering WANDER stores the `WANDER_ANCHOR`. Goal mobs are kept near it by vanilla's home-point
system: `CompanionBehavior.syncWanderLeash` re-applies `Mob.setHomeTo` each tick (vanilla doesn't
persist the home; our anchor does) and a `MoveTowardsRestrictionGoal` (priority 5) walks them back
past the per-mob `wanderLeashRadius()`. The home-point API this version is
`setHomeTo`/`hasHome`/`getHomePosition`/`clearHome` on `Mob`. The bat self-leashes (above).

### Combat & targeting

Companions stay out of mob combat in both directions: `MobCanAttackMixin` refuses `canAttack`
whenever either side is a companion (tamed prey is untargetable; tamed predators acquire no targets
— no retaliation either, by design), and `AvoidEntityGoalMixin` stops companions fleeing players.
The frog's tongue is a separate (non-target) path and is left characterful.

---

## Taming & controls

**One universal taming item: the amethyst shard** (`CompanionTaming.TAMING_ITEM`). One stack to
carry afield — and because amethyst is no mob's breeding food and has no vanilla right-click-on-mob
interaction, taming never swallows breeding or any other vanilla gesture, so a wild animal still
breeds normally even though its food once tamed it. Taming is gated by `CompanionRegistry.isEnabled`
(config).

**Flow:** right-click a registered animal with the shard → consume one (unless creative) → roll
`tamingChanceOneInN()` (default 1-in-3) → smoke on failure, hearts on success. On success the
`OWNER` attachment lands on the same entity (mode `FOLLOW`, `setPersistenceRequired` so it never
despawns — matters most for the bat, an `AmbientCreature`).

**Controls (all owner-gated, server-side):**

| Gesture | Action |
|---|---|
| Empty-hand right-click | SIT ↔ FOLLOW (the sheep claims this slot to mount instead) |
| Sneak + empty-hand right-click | WANDER ↔ FOLLOW |
| Milk bucket | Quiet/restore the passive (`BUFF_DISABLED`) |
| Amethyst cluster | Capture into a bound cluster (see Capture) |

Other held items deliberately PASS through, staying free for future actions.

**Mode feedback:** every toggle emits a coloured dust puff — **white = SIT, purple = WANDER,
gold = FOLLOW** — plus the mob's own ambient voice (a sheep baas, a frog croaks; silent mobs like
the turtle show just the particles). The colour is the unambiguous confirmation; the sound is pure
flavour. Deliberately a one-shot cue, not a persistent particle that would clutter a build a
companion is parked in.

---

## Passive effect system

Runs in `CompanionBehavior.refreshPassive`, driven from each companion's tick path.

**While active (mode FOLLOW or SIT, not milk-quieted, owner within 24 blocks):** every 100 ticks
(5s) the companion (re)applies `passiveEffect()` at `passiveAmplifier()` with a **320-tick (16s)**
duration. Refreshing 320 every 100 keeps the remaining time oscillating 220–320 ticks — always above
Night Vision's ~200-tick end-of-effect flicker window, so an active effect never strobes.

**When it stops being active** (WANDER, quieted, out of range, unloaded): it simply **stops
refreshing** — there is **no active removal call**. Rationale: per-companion teardown either makes
an inactive companion cancel an effect an *active* one is still sustaining, or requires a per-tick
scan for other providers. Letting the effect lapse avoids both, scales flat to hundreds of
companions, and is self-correcting. The trade-offs are accepted (see Design decisions).

**Application flags:** the **6-arg** `MobEffectInstance(effect, 320, amp, ambient=true,
visible=false, showIcon=true)` — particles off, HUD icon on (the buff's only indicator). The 5-arg
form sets `showIcon = visible`, which would hide the icon.

**Constraints:** amplifier defaults to 0 (Level I) — config may raise it, the design intent is
"background" effects at the weakest tier. Multiple companions granting the same effect don't stack;
they refresh the same instance.

### Non-effect passives (`passiveEffect()` returns `null`)

- **Ocelot — XP ×2.** `ServerPlayerExperienceMixin` (`@ModifyVariable` on `giveExperiencePoints` —
  every server-side XP award funnels through it) doubles the amount when
  `CompanionBehavior.hasActiveCompanion(player, EntityType.OCELOT)`; `OcelotXpBonus` does the
  doubling plus a green sparkle (particle-only, 1-in-4 throttled — the vanilla orb ding covers the
  audio; its per-tick scan cache is keyed by player UUID and cleared each tick, so simultaneous
  grinders don't evict each other).
- **Fox — item fetch.** `FoxFetchItemGoal` (via `attachGoals`, priority 0 registered ahead of
  follow) pathfinds to the nearest deliverable `ItemEntity` within 10 blocks, then acts as a
  **mobile magnet**: every item within ~1.5 blocks *of the fox* is sent to the owner via vanilla
  `ItemEntity.playerTouch(owner)` (fly-to-player animation, sound, pickup-delay and full-inventory
  handling for free). The bubble being wider than where pathfinding parks lets it grab an item it
  stops just short of; a stuck-ticks blacklist skips genuinely walled-off items. It only commits to
  *deliverable* loot — items the owner's inventory can hold, past their pickup delay. While loot is
  around it **chains item to item** in-goal (never yielding the MOVE flag for follow to claim) around
  an owner roaming the work area — built for the chop-a-forest case — and heels when the area is
  clean or the owner passes the 24-block range (follow's teleport recovers). Fetch is deliberately
  **FOLLOW-only** (a sitting fox is asleep; fetch would fight the sit goal for the MOVE flag).
  `FoxCompanion.serverTickBehavior` disables the fox's vanilla `canPickUpLoot` so the goal is the
  sole collector.
- **Sheep — rideable mount, no saddle.** The owner's plain empty-hand right-click mounts it (via
  `onOwnerEmptyHandUse` — a rideable companion has no use for SIT, so it repurposes that slot;
  sneak-RC still parks it via WANDER). `SheepMixin` adds the pig/horse-style ridden-control
  overrides (`getControllingPassenger`/`getRiddenInput`/`tickRidden`/`getRiddenSpeed`): WASD + look
  steering at `MOVEMENT_SPEED` × 0.85 ≈ 0.195 — faster than a sprinting player. Step height is free
  (vanilla `maxUpStep()` returns ≥1.0 whenever a Player controls a mob). Control is gated only on
  the first passenger being a `Player` — that works client-side, where the server-only owner
  attachment is absent; the owner gate lives in the mount interaction.
  - **Charged jump:** `SheepMixin implements PlayerRideableJumping` — the lone hook the client needs
    (`LocalPlayer.jumpableVehicle()` is an `instanceof` check) to show the jump-charge bar and send
    the charge. The impulse is applied client-authoritatively in `tickRidden` mirroring
    `AbstractHorse`; full charge (`WILDBOUND_JUMP_STRENGTH = 0.6`, the sheep has no `JUMP_STRENGTH`
    attribute) clears ~2.2 blocks, with a forward boost when moving. Interfaces on a mixin class
    merge into the target — a documented Mixin feature.
  - **Floats on water (ridden only):** no code — vanilla `floatInWaterWhileRidden` is gated on the
    `minecraft:can_float_while_ridden` entity-type tag; we add `minecraft:sheep` to it via a data
    tag (tags merge). A free-roaming companion sheep keeps vanilla water behaviour.

---

## Capture & transport — the bound cluster

Long-distance travel (elytra) and entity lag both strand pets in unloaded chunks; capture solves
both. An owner right-clicks **their own** companion with an **amethyst cluster** (the silk-touch
block, distinct from the shard tamer) to pocket it into a single-use `wildbound:bound_cluster` item;
right-clicking a surface releases it and the cluster shatters.

As built (`CompanionCapture` + `item/BoundClusterItem`, registered in `ModItems`/`ModComponents` —
the mod's first registered item and data component, fine under the philosophy: the no-new-types rule
is about *entities*, and the companion stays a plain vanilla mob both in and out of the crystal):

- `Entity.save` via a `TagValueOutput` serializes the whole mob — entity-type `id` **and** the
  persistent attachments — into the `BOUND_ENTITY` `CustomData` component. The component is
  **persistent only** (read server-side at release, never shipped to clients — the display title is
  baked separately into a vanilla `ITEM_NAME` at capture, favouring a name-tagged name: "Bound
  Batty" over "Bound Bat"). An `ENCHANTMENT_GLINT_OVERRIDE` glint marks a charged cluster on the
  vanilla amethyst texture; the item model is a flat `item/generated` sprite (parenting the 3-D
  `block/cross` model made the glint bleed across the cross planes).
- The live mob is `discard()`ed **only after** the stack is built, and `capture()` honours `save`'s
  return value — never lose a pet to a failed serialize. (`save` returns false for a mob riding a
  boat/minecart, so capturing a passenger does nothing; capture is likewise refused on a mount
  carrying a rider, `mob.isVehicle()`.) One cluster is consumed.
- Release (`useOn`) rebuilds via `EntityType.create(ValueInput, …)` against the clicked face,
  assigns a **fresh UUID**, and shrinks the cluster. Goals re-attach through the normal
  `ENTITY_LOAD` hook — it comes back a fully-wired companion in its saved mode.
- Stack of 1; stores in chests/ender chests; survives relog. In **no creative tab** (an empty bound
  cluster is meaningless). Non-owners and wild mobs → PASS, nothing happens.
- New companions are auto-capturable — no per-animal work.

---

## Config

`config/WildboundConfig.java` reads `config/wildbound.json` once at init (generated from defaults if
missing). Per mob: `enabled` (gates **new taming only** — existing companions persist),
`tamingChanceOneInN`, `effectAmplifier`, `wanderRadius` (all clamped, applied to the matching
`CompanionType` setters / `CompanionRegistry.setEnabled`). The flicker-safe effect constants and the
24-block follow range are deliberately **not** configurable — they encode correctness, not taste.

---

## Advancements

Custom triggers (`advancement/`, registered in `registry/ModCriteria`, fired from `CompanionTaming`):
`wildbound:companion_tamed` (successful tame, parameterized by animal), `wildbound:companion_wandered`
(first wander), `wildbound:companion_captured` (first capture). JSON lives in
`src/main/resources/data/wildbound/advancement/` (**singular** `advancement` this version).

**Tree shape — progressive reveal.** The root fires from vanilla `minecraft:inventory_changed` on
first picking up an **amethyst shard**, so the tree hints at the mod before the first tame and its
description teaches the mechanic. *A Growing Menagerie* fires on the first tame and parents every
per-animal advancement **and** the capstone (advancements are single-parent, so the animals can't
each line into the capstone — under the hub it reads as that cluster's culmination). The three
teaching advancements hang off the root.

| Advancement | Trigger | Parent |
|---|---|---|
| *Wildbound* (root) | Pick up an amethyst shard | — |
| *Home on the Range* | First wander | root |
| *Peace and Quiet* | First milk-quiet | root |
| *In Safe Hands* | First capture | root |
| *A Growing Menagerie* | First tame | root |
| 11 per-animal advancements | Tame that animal | menagerie |
| *The Wild Knows Your Name* (capstone) | Tame one of every kind | menagerie |

**The capstone is a hand-maintained per-animal list** — `wild_knows_your_name.json` enumerates one
`companion_tamed` criterion + `requirements` entry per animal (no "tame all" trigger exists). Adding
a companion means adding it here too, or the capstone silently stops meaning "every kind" (exactly
how the sheep was once missed — it required 9 of 10).

---

## Design decisions & accepted trade-offs

Choices, not tasks. Revisit one only if it annoys in practice.

- **Attach-to-vanilla, no parallel entity types** — less code per animal, stable identity, no
  renderer wiring. The whole architecture follows from this.
- **No active effect teardown** (see Passive effect system). Accepted consequences:
  - The effect **lingers up to ~16s** after the last companion goes inactive (wander, quiet, out of
    range — or **dies**: a dead entity can't refresh or clear, so it fades at natural expiry).
  - **Re-entering a parked companion's range can take ~5s** (one refresh interval) to re-buff. A
    following companion never exposes this; a sitting one does. Refresh-model texture.
- **SIT grants the passive** (changed by play-test: sit and milk previously overlapped as two
  off-switches). FOLLOW = trailing buff, SIT = parked buff, WANDER = off duty, milk = silenced. The
  fox is the deliberate exception — fetch stays FOLLOW-only.
- **Companion buffs vs. beacons/potions of the same effect — no special handling.** Vanilla
  `MobEffectInstance.update` merges same-effect applications: a higher amplifier takes over and
  stashes the weaker instance as a `hiddenEffect` that resurfaces when the stronger expires; an
  equal-or-weaker application can only extend duration or top up the hidden instance, never
  downgrade. Beacon Haste II over a bee's Haste I wins in beam range with the companion's buff
  riding (and refreshed) underneath, restored seamlessly on walking away — no fight, no flicker.
  Only cosmetic seam: `update` stamps incoming display flags unconditionally, so our `visible=false`
  refresh suppresses an overlapping potion's swirl particles (and ping-pongs against a beacon's
  `visible=true`); icon and strength unaffected. Verified against decompiled 26.1.2 sources.
- **Universal tamer over per-animal foods** — one-stack kit, and it un-hijacks breeding (an item
  with any vanilla mob interaction would swallow it in `UseEntityCallback`).
- **Capture loss is by design** — die holding a bound cluster and the pet is gone; the safeguard is
  an ender chest. The cluster is single-use (shatters on release) to keep it a consumable, not a
  pokéball economy.
- **Night Vision from both bat and axolotl** — intentional, not a dedupe target: caves vs. depths,
  non-overlapping contexts (the axolotl's pairs with the turtle's Water Breathing underwater).
- **Frog tongue still snatches small mobs** — characterful vanilla behaviour, left in (it can eat
  baby mobs). Suppress only if it proves annoying.
- **Aquatic companions (axolotl, turtle) are clumsy following on land** — flop along behind the
  owner; the follow-teleport (`canStandAt` wants air over solid) can drop them out of water.
  De-scoped rather than fixed: carry them across dry stretches in a bound cluster.
- **Client arm-swing ghost when shard-clicking your own companion** — attachments don't sync, so the
  client predicts a taming interaction the server PASSes. Cosmetic only (the shard is consumed
  server-side only); fixing it means syncing the owner attachment to clients, which one ghost swing
  doesn't justify. → `CompanionTaming`
- **`syncWanderLeash` clears any externally-set home restriction** on a non-wandering companion each
  tick. No Wildbound species uses the `Mob` home system in vanilla 26.1.2 (turtle/bee track homes
  via their own fields), so this only matters under another mod. Revisit on a reported conflict.
- **The rideable sheep makes the mod client+server, and `SheepMixin` stays a *common* mixin** —
  every other feature is server-authoritative and works for a vanilla client. The sheep's steering +
  charged jump are computed by the controlling client, so the mod is required client-side for that
  one feature. Do **not** move `SheepMixin` to `src/client`: the server also needs the overrides to
  route ridden movement and receive the jump packet (vanilla `AbstractHorse` is likewise common).
- **Flicker constants and follow range are not configurable** — they encode correctness (the
  flicker window) and balance, not preference.

---

## Scope

**This release is passive mobs only.** Deliberately excluded to keep the vanilla+ feel:

- New taming items (vanilla items only; the bound cluster is transport, not taming)
- Companion breeding and equipment/inventory
- GUI for companion stats
- Shoulder riding
- Anything already tameable in vanilla (wolves, cats, horses, parrots, …)

**Possible future directions** — not in this release, but the Wildbound narrative can stretch to them
if we expand:

- Hostile or neutral mobs as companions
- Companion combat capabilities (this would mean revisiting the full-pacification decision in
  `MobCanAttackMixin` — companions currently acquire no targets and never retaliate, by design)
