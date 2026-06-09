# CLAUDE.md

Guidance for working in this repo. Wildbound is a vanilla+ Fabric mod (MC **26.1.2**, Java **25**) that
tames passive mobs; each tamed companion follows the player and grants a low-tier passive while in range.

## Commands

```bash
./gradlew build          # compile + run mixin AP + remap + jar
./gradlew compileJava    # quick compile check
./gradlew runClient      # dev client
./gradlew runServer      # dev dedicated server (needs run/eula.txt -> eula=true)
./gradlew genSources     # decompile MC sources (inspect real signatures)
```

**Verify load headlessly:** `runServer` boots, applies mixins, and loads the datapack — the cheapest way
to catch apply-time mixin errors and bad advancement JSON that still *compile* fine. Look for
`Wildbound initialised with N companion type(s).`, the advancement count, and any `Mixin apply ... failed`.
Gameplay/visual behavior still needs an in-client pass.

## Build/version facts (don't "fix" these)

- **No `mappings` in `build.gradle`.** This MC version ships de-obfuscated; Loom rejects
  `loom.officialMojangMappings()` ("Cannot use Mojang mappings in a non-obfuscated environment"). The
  missing mappings line is correct.
- Code uses **Mojang names**: `Identifier` (not `ResourceLocation`), `Mob`, `TamableAnimal`, `mobInteract`,
  `removeEffect`, etc. The design doc is written in Yarn names — translate.
- The dev client uses a **pinned username** (`programArgs("--username", "WildboundDev")` in the loom
  `runs { client {} }` block) so the offline player UUID is stable across launches. Without it the client
  gets a random `Player###`/UUID each run, which breaks owner-gated features (sit/stand) after a reload.

## Architecture — attach-to-vanilla (NOT separate entity types)

Tamed state is attached to the **existing vanilla mob**; we do not register `Tamed*Entity` types or swap
the mob on taming. This keeps entity identity stable and avoids renderer/attribute wiring.

- **State** — Fabric Attachment API (`companion/WildboundAttachments.java`): `OWNER` (UUID, persistent;
  its presence == "is a companion") and `MODE` (the `CompanionMode` enum — `FOLLOW`/`SIT`/`WANDER`,
  persistent; absent == `FOLLOW`). Persistence is automatic. Read mode via `CompanionBehavior`'s
  `isFollowing`/`isSitting`/`isWandering` — **`isFollowing` also checks `isCompanion`** because `getMode`
  defaults any mob (even untamed) to `FOLLOW`. Only `FOLLOW` is "active" (grants the passive); `WANDER`
  roams on vanilla AI, still owned (persists, won't flee, won't be hunted by other companions).
  - **Wander leash** — entering WANDER stores a `WANDER_ANCHOR` (BlockPos, persistent). Goal mobs are kept
    near it by vanilla's home-point system: `CompanionBehavior.syncWanderLeash` re-applies `Mob.setHomeTo`
    each tick (vanilla doesn't persist the home, our anchor does) and a `MoveTowardsRestrictionGoal` walks
    them back past the per-mob `CompanionType.wanderLeashRadius()` (default 12, config-overridable). The
    **bat** has no goals, so it leashes itself in `BatCompanion.leashWander` (steer back when outside the
    radius, else cede to vanilla flight). NB the home-point API is on `Mob` and named
    `setHomeTo`/`hasHome`/`getHomePosition`/`clearHome` this version.
- **Per-animal definition** — subclass `CompanionType` (taming item/predicate, passive effect + amplifier,
  taming chance, sit-pose hooks) and register it in `CompanionRegistry` keyed by `EntityType`. Amplifier and
  taming chance are mutable fields with config setters (see Config below); the rest is behaviour.
- **Shared runtime** — `CompanionBehavior` (ownership/mode access, the per-tick driver `serverTick`, the
  passive-effect refresh, and `findActiveCompanion`/`hasActiveCompanion`).
- **Taming + mode toggle** — `CompanionTaming` via Fabric `UseEntityCallback` (no per-animal interact mixin).
  Empty-hand RC toggles SIT↔FOLLOW; **sneak**+empty-hand RC toggles WANDER↔FOLLOW. Held-item interactions
  stay free for future per-companion actions. Taming is gated by `CompanionRegistry.isEnabled`.
- **Config** — `config/WildboundConfig.java` reads `config/wildbound.json` once at init (generated from
  defaults if missing) and applies per-mob `enabled` (→ `CompanionRegistry.setEnabled`), `tamingChanceOneInN`,
  `effectAmplifier`, and `wanderRadius` (→ the matching `CompanionType` setters, all clamped). Flicker
  constants and follow range are deliberately **not** configurable.
- **Advancements** — custom triggers in `advancement/`, registered in `registry/ModCriteria` and fired from
  `CompanionTaming`: `CompanionTamedTrigger` (`wildbound:companion_tamed`, on a successful tame) and
  `CompanionWanderedTrigger` (`wildbound:companion_wandered`, when a companion is first set to wander). JSON in
  `src/main/resources/data/wildbound/advancement/` (folder is singular `advancement` this version).
  - **Tree shape (progressive reveal).** `root` ("Wildbound") is **not** a tame trigger — it fires from
    vanilla `minecraft:inventory_changed` when the player first picks up *any* taming item (item list +
    `#minecraft:flowers` tag, OR'd in one `requirements` group), so the tree hints at the mod *before* the
    first tame and its description teaches the mechanic. `menagerie` ("A Growing Menagerie") fires on the
    first `companion_tamed` and is the **parent of every per-animal advancement and the capstone** (MC
    advancements are single-parent, so the animals can't each line into the capstone — this makes it read as
    the culmination of that cluster). `wander`/`quiet` (teaching advancements) hang off `root`.
  - **The capstone is a hand-maintained per-animal list.** `wild_knows_your_name.json` has **no "tame all"
    trigger** — it enumerates one `companion_tamed` criterion *per animal type* with a matching `requirements`
    entry. Adding a companion means adding it here too, or the capstone silently stops meaning "every kind"
    (this is exactly how the **sheep** entry was missed — it required 9 of 10).

### Movement: two paths

- **Goal-driven mobs (most companions — ground/flying/swimming `PathfinderMob`s).** A single
  `ServerEntityEvents.ENTITY_LOAD` hook (in `Wildbound`) attaches the generic goals
  (`CompanionFollowOwnerGoal`, `CompanionSitGoal`, `CompanionTickGoal` in `companion/goal/`) to any
  registered companion. **No per-animal mixin.** The goal selector is reached via the `MobAccessor`
  `@Accessor` (do NOT `@Shadow` the inherited `goalSelector` field — Mixin doesn't resolve inherited
  *fields* and it fails at apply time though it compiles).
- **The Bat** is the exception: it bypasses the goal system (flight is in `customServerAiStep`), so
  `BatMixin` drives follow/sit there and `BatCompanion` steers `deltaMovement` directly. In `WANDER` its
  `serverTickBehavior` returns `false` (clearing any leftover rest), handing flight back to vanilla.

### Passive effects

- Following + in-range companions refresh the effect every 100 ticks at a **flicker-safe 320-tick**
  duration (stays above Night Vision's ~200-tick flicker window). Inactive companions just stop — the
  effect fades naturally. **No active teardown** (so a sitting companion never cancels a following one's
  shared effect, and it scales to many companions).
- Apply with the **6-arg** `MobEffectInstance(..., ambient=true, visible=false, showIcon=true)`. The 5-arg
  form sets `showIcon = visible`, which would hide the HUD icon (the buff's only indicator).
- **Three companions have non-effect passives** (`passiveEffect()` is `null`; the work runs elsewhere):
  - **Ocelot** (tamed with raw cod/salmon): XP ×2. The bonus is `ServerPlayerExperienceMixin`
    (`@ModifyVariable` on `giveExperiencePoints`) gated by
    `CompanionBehavior.hasActiveCompanion(player, EntityType.OCELOT)`; `OcelotXpBonus` does the doubling +
    sparkle. (Was the fox's passive originally.)
  - **Fox** (tamed with sweet berries): fetches nearby dropped items into the owner's inventory.
    `FoxFetchItemGoal` (attached via the `CompanionType.attachGoals` hook, priority 1, just under follow's 0)
    pathfinds the fox to the nearest `ItemEntity` within 8 blocks, then acts as a **mobile magnet** — every
    item within a ~1.5-block bubble *around the fox* (not the player) is sent to the owner via vanilla
    `ItemEntity.playerTouch(owner)` (reusing the fly-to-player animation, sound, pickup-delay/target checks,
    and full-inventory handling). The bubble being a little wider than where pathfinding parks is what lets it
    grab an item it stops just short of (e.g. on a block edge) instead of freezing; a `STUCK_TICKS`/`blacklist`
    backstop only triggers for genuinely walled-off items so the fox moves on. Same activation rules as a
    status passive — following, in range, not milk-quieted; follow outranks it so a fox that falls behind
    catches up first, bounding how far it strays. `FoxCompanion.serverTickBehavior` turns off the companion
    fox's vanilla `canPickUpLoot` so the goal is the sole collector (items go to the owner, not its mouth).
  - **Sheep** (tamed with an apple): **rideable, no saddle**. An owner's plain empty-hand RC mounts it (via the
    new `CompanionType.onOwnerEmptyHandUse` hook — a rideable companion repurposes the SIT slot for mounting;
    sneak-RC still parks it via WANDER). `SheepMixin` adds the pig/horse-style ridden-control overrides
    (`getControllingPassenger`/`getRiddenInput`/`tickRidden`/`getRiddenSpeed`) so the player steers it with
    WASD + look, faster than a sprinting player (ridden speed = `MOVEMENT_SPEED` × 0.85 ≈ 0.195). **Step height
    is free**: vanilla `LivingEntity.maxUpStep()` already returns ≥1.0 (horse-tier) whenever a Player controls
    the mob, so single blocks need no jump. Control is gated only on the first passenger being a `Player`
    (works client-side, unlike the server-only owner attachment) — the owner gate lives in the mount interaction.

### Sit poses

`CompanionType` has `onStartSitting` / `onSitTick` / `onStopSitting` and a `controlsSitMovement()` flag;
`CompanionSitGoal` calls them and zeroes residual horizontal velocity each tick. Panda `sit(true)`,
Armadillo `switchToState(ROLLING)`, Fox `setSleeping(true)` (via `FoxAccessor` `@Invoker`), Bee flies down
to land (`controlsSitMovement`).

## Adding a new companion

Ground/flying/swimming `PathfinderMob`:
1. `companion/<animal>/<Animal>Companion.java` extends `CompanionType` (taming item + `passiveEffect`).
2. Register it in `CompanionRegistry.init()`.
3. Add `src/main/resources/data/wildbound/advancement/<animal>.json` (parent `wildbound:menagerie`, **not**
   `root`), **and** add a `companion_tamed` criterion + `requirements` entry for the new type to
   `wild_knows_your_name.json` — the capstone is a hand-maintained per-animal list, so skipping this quietly
   drops the animal from "tame one of every kind" (how sheep got missed). If the new taming item isn't
   already covered by `root.json`'s `inventory_changed` list, add it there too so the hint still fires.
4. Optional: override sit-pose hooks for a natural pose, or `attachGoals` for type-specific goals (the fox's
   item fetch is the example). **No mixin needed** — `ENTITY_LOAD` attaches the shared + per-type goals.

A mob that bypasses goals (like the bat) needs its own mixin into its AI step instead. A companion with a
behavior vanilla doesn't expose to goals (like the rideable sheep) likewise gets its own mixin — `SheepMixin`
merges the ridden-control overrides into `Sheep`.

## Mixins (`wildbound.mixins.json`)

`AvoidEntityGoalMixin` (companions don't flee players), `BatMixin` (bat AI step), `MobAccessor`
(goalSelector), `MobCanAttackMixin` (companions stay out of mob combat — untargetable as prey,
pacified as predators), `ServerPlayerExperienceMixin`
(ocelot XP), `FoxAccessor` (`setSleeping`), `SheepMixin` (rideable-sheep ridden control). Keep this list
sorted and in sync with the `mixin/` package.

## Gotchas learned

- Vanilla bat **resting pins to `floor(y)+0.1`** — fine hanging under a ceiling (fractional rest height),
  but a ground perch's rest height is an integer, so a dip below it yanks the bat a block down. Snap to a
  stable resting Y before setting `resting` (`BatCompanion.settleTo`).
- Recurring **port 25565 in use** during back-to-back `runServer` tests = a leftover dev server. Kill
  `net.minecraft`/`GradleWrapperMain` (and free the port) between runs; it's not a mod error.

## Docs & conventions

- `docs/design-doc-v1` — design doc, kept current (has a "revision note / as built" section).
- `docs/refinements.md` — **working log of open polish items only.** Jot rough edges here with a source
  pointer and a one-line fix sketch instead of gold-plating mid-task. Sections by readiness: _Active_
  (ready to work, checkboxes), _Needs a decision_ (blocked on a design call, not code), _Verify in-game_
  (compiles/loads but wants a play-test), _Accepted / by design_ (choices, not tasks). Items carry a
  `subsystem` tag and rough **S/M/L** effort.
- `docs/completed-refinements.md` — archive of shipped refinements. When an item in `refinements.md` lands,
  move it here rewritten to describe what was built, so the backlog stays short and current.
- Commit style: imperative subject + a short body explaining the *why*; one logical change per commit.
  Work on a feature branch and fast-forward into `main`.
