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
    them back past `WANDER_LEASH_RADIUS` (12). The **bat** has no goals, so it leashes itself in
    `BatCompanion.leashWander` (steer back when outside the radius, else cede to vanilla flight). NB the
    home-point API is on `Mob` and named `setHomeTo`/`hasHome`/`getHomePosition`/`clearHome` this version.
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
  and `effectAmplifier`. Flicker constants and follow range are deliberately **not** configurable.
- **Advancements** — custom `CompanionTamedTrigger` (`advancement/`), registered as
  `wildbound:companion_tamed` in `registry/ModCriteria`, fired from `CompanionTaming` on success. JSON in
  `src/main/resources/data/wildbound/advancement/` (folder is singular `advancement` this version).

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
- **Fox is the one exception:** its passive is XP ×2, not a status effect. `passiveEffect()` is `null`;
  the bonus is `ServerPlayerExperienceMixin` (`@ModifyVariable` on `giveExperiencePoints`) gated by
  `CompanionBehavior.hasActiveCompanion(player, EntityType.FOX)`. `FoxXpBonus` does the doubling + sparkle.

### Sit poses

`CompanionType` has `onStartSitting` / `onSitTick` / `onStopSitting` and a `controlsSitMovement()` flag;
`CompanionSitGoal` calls them and zeroes residual horizontal velocity each tick. Panda `sit(true)`,
Armadillo `switchToState(ROLLING)`, Fox `setSleeping(true)` (via `FoxAccessor` `@Invoker`), Bee flies down
to land (`controlsSitMovement`).

## Adding a new companion

Ground/flying/swimming `PathfinderMob`:
1. `companion/<animal>/<Animal>Companion.java` extends `CompanionType` (taming item + `passiveEffect`).
2. Register it in `CompanionRegistry.init()`.
3. Add `src/main/resources/data/wildbound/advancement/<animal>.json` (child of `wildbound:root`).
4. Optional: override sit-pose hooks for a natural pose. **No mixin needed** — `ENTITY_LOAD` attaches goals.

A mob that bypasses goals (like the bat) needs its own mixin into its AI step instead.

## Mixins (`wildbound.mixins.json`)

`AvoidEntityGoalMixin` (companions don't flee players), `BatMixin` (bat AI step), `MobAccessor`
(goalSelector), `MobCanAttackMixin` (companions don't attack companions), `ServerPlayerExperienceMixin`
(fox XP), `FoxAccessor` (`setSleeping`). Keep this list sorted and in sync with the `mixin/` package.

## Gotchas learned

- Vanilla bat **resting pins to `floor(y)+0.1`** — fine hanging under a ceiling (fractional rest height),
  but a ground perch's rest height is an integer, so a dip below it yanks the bat a block down. Snap to a
  stable resting Y before setting `resting` (`BatCompanion.settleTo`).
- Recurring **port 25565 in use** during back-to-back `runServer` tests = a leftover dev server. Kill
  `net.minecraft`/`GradleWrapperMain` (and free the port) between runs; it's not a mod error.

## Docs & conventions

- `docs/design-doc-v1` — design doc, kept current (has a "revision note / as built" section).
- `docs/refinements.md` — polish backlog; jot rough edges here with source locations instead of
  gold-plating mid-task.
- Commit style: imperative subject + a short body explaining the *why*; one logical change per commit.
  Work on a feature branch and fast-forward into `main`.
