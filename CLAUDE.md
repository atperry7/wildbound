# CLAUDE.md

Guidance for working in this repo. Wildbound is a vanilla+ Fabric mod (MC **26.1.2**, Java **25**) that
tames passive mobs; each tamed companion follows the player and grants a low-tier passive while in range.

**`docs/design.md` is the living, as-built design document** — architecture, mechanics, and the design
decisions/trade-offs, kept current with the code. This file covers only how to work here; read the design
doc before changing behaviour, and update it in the same commit when behaviour changes.

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
  `removeEffect`, etc.
- The dev client uses a **pinned username** (`programArgs("--username", "WildboundDev")` in the loom
  `runs { client {} }` block) so the offline player UUID is stable across launches. Without it the client
  gets a random `Player###`/UUID each run, which breaks owner-gated features (sit/stand) after a reload.

## Architecture map

Attach-to-vanilla: tamed state is Fabric attachments on the **existing vanilla mob** — no `Tamed*Entity`
types, no entity swap. Full design and rationale in `docs/design.md`; the code lives here:

- **State** — `companion/WildboundAttachments.java` (`OWNER`, `MODE`, `WANDER_ANCHOR`, `BUFF_DISABLED`;
  all persistent). Read via `CompanionBehavior` accessors — they also check `isCompanion`, because
  `getMode` defaults any mob to `FOLLOW`.
- **Per-animal definition** — subclass `CompanionType` (`companion/<animal>/`), register in
  `CompanionRegistry.init()`.
- **Shared runtime** — `companion/CompanionBehavior.java` (per-tick driver, passive refresh,
  `findActiveCompanion`).
- **Interactions** — `companion/CompanionTaming.java` (one `UseEntityCallback`: universal amethyst-shard
  taming, mode toggles, milk quiet, capture).
- **Goals** — attached by the `ENTITY_LOAD` hook in `Wildbound.java`; shared goals in `companion/goal/`.
  The bat bypasses goals (`BatMixin` + `BatCompanion`); the sheep's ridden control is `SheepMixin`.
- **Capture** — `companion/CompanionCapture.java` + `item/BoundClusterItem.java`
  (`registry/ModItems`/`ModComponents`).
- **Config** — `config/WildboundConfig.java` reads `config/wildbound.json` once at init.
- **Advancements** — triggers in `advancement/` (registered in `registry/ModCriteria`), JSON in
  `src/main/resources/data/wildbound/advancement/`.

## Adding a new companion

Ground/flying/swimming `PathfinderMob`:
1. `companion/<animal>/<Animal>Companion.java` extends `CompanionType` (`tamingItem()` = advancement icon
   only — taming is universal — plus `passiveEffect`).
2. Register it in `CompanionRegistry.init()`.
3. Add `src/main/resources/data/wildbound/advancement/<animal>.json` (parent `wildbound:menagerie`, **not**
   `root`), **and** add a `companion_tamed` criterion + `requirements` entry to
   `wild_knows_your_name.json` — the capstone is a hand-maintained per-animal list; skipping this quietly
   drops the animal from "tame one of every kind". `root.json` needs no change.
4. Optional: override sit-pose hooks, or `attachGoals` for type-specific goals (fox fetch is the example).
   **No mixin needed** — `ENTITY_LOAD` attaches the shared + per-type goals.

A mob that bypasses goals (like the bat) needs its own mixin into its AI step instead; a behavior vanilla
doesn't expose to goals (like the rideable sheep) likewise gets its own mixin.

## Mixins (`wildbound.mixins.json`)

`AvoidEntityGoalMixin` (companions don't flee players), `BatMixin` (bat AI step), `FoxAccessor`
(`setSleeping`), `MobAccessor` (goalSelector accessor + `getAmbientSound` invoker), `MobCanAttackMixin`
(companions stay out of mob combat, both directions), `ServerPlayerExperienceMixin` (ocelot XP),
`SheepMixin` (rideable-sheep ridden control + `PlayerRideableJumping`). Keep this list sorted and in sync
with the `mixin/` package.

## Gotchas learned

- **`@Shadow`/`@Accessor`/`@Invoker` only resolve members *declared on* the `@Mixin` target class, not
  inherited ones** — it compiles fine and fails at mixin-apply (`InvalidAccessorException`). Either target
  the declaring class or call public API instead (the mode-toggle cue uses `Level.playSound` because
  `makeSound` lives on `LivingEntity`, not `Mob`). The shared goals reach `goalSelector` via the
  `MobAccessor` `@Accessor` for the same reason. **Catch these with a headless `runServer`.**
- Apply passive effects with the **6-arg** `MobEffectInstance(..., ambient, visible, showIcon)` — the
  5-arg form sets `showIcon = visible`, hiding the HUD icon (the buff's only indicator).
- Vanilla bat **resting pins to `floor(y)+0.1`** — fine under a ceiling, but a ground perch's rest height
  is an integer, so a dip below it yanks the bat a block down. Snap to a stable resting Y before setting
  `resting` (`BatCompanion.settleTo`).
- Recurring **port 25565 in use** during back-to-back `runServer` tests = a leftover dev server. Kill
  `net.minecraft`/`GradleWrapperMain` (and free the port) between runs; it's not a mod error. (A leftover
  server also holds `run/world/session.lock`, so the next boot dies with `LockException` — same cleanup.)

## Docs & conventions

One home per fact — don't restate content across files, link to it:

- `docs/design.md` — **what & why** (living, as-built): architecture, mechanics, and the "Design decisions
  & accepted trade-offs" section. Behaviour changes update it in the same commit; settled design questions
  get recorded there, not in the backlog.
- `docs/refinements.md` — **open backlog only** (_Active_ / _Needs a decision_). Jot rough edges there with
  a source pointer and a one-line fix sketch instead of gold-plating mid-task. An item leaves the backlog
  only when verified — for gameplay/visual items that means the in-client pass, not compiles-and-loads —
  and simply gets deleted; the closing commit is the record.
- **The changelog is git.** Commit style: imperative subject + a short body explaining the *why*; for
  gameplay changes, note verification status in the body ("play-tested in-client" / "pending in-client
  pass"). One logical change per commit; work on a feature branch and fast-forward into `main`.
