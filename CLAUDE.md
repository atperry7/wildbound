# CLAUDE.md

How to work in this repo. Wildbound is a vanilla+ Fabric mod (MC **26.1.2**, Java **25**) that tames
passive mobs; each tamed companion follows the player and grants a low-tier passive while in range.

**Read `docs/design.md` before changing behaviour** — it's the living, as-built design doc (architecture,
mechanics, decisions & trade-offs) and changes in the same commit when behaviour does. This file is only
*how to work here*; it deliberately points into design.md rather than restating it.

## Commands

```bash
./gradlew build          # compile + mixin AP + remap + jar
./gradlew compileJava    # quick compile check
./gradlew runClient      # dev client
./gradlew runServer      # dev dedicated server (needs run/eula.txt -> eula=true)
./gradlew runGameTest    # headless behaviour tests; look for "All N required tests passed"
./gradlew genSources     # decompile MC sources (inspect real signatures)
```

- **`runServer`** is the cheapest catch for apply-time mixin errors and bad advancement JSON that still
  *compile* — it boots, applies mixins, loads the datapack. Look for `Wildbound initialised with N
  companion type(s).` and any `Mixin apply ... failed`.
- **`runGameTest`** drives the shared framework (taming, mode toggles, milk quiet, passive delivery,
  capture round-trip) with mock players. Dev-only mod under `src/gametest/` — all-lowercase; a
  `src/gameTest/` spelling works on a case-insensitive Mac but breaks Linux CI. Never ships. Run after any
  framework change and first when porting to a new MC version.
- Neither covers visual/feel (sheep riding, bat perch) — those still need an in-client pass.

## Build/version facts (don't "fix" these)

- **No `mappings` in `build.gradle`.** This MC version ships de-obfuscated; Loom rejects
  `loom.officialMojangMappings()` ("non-obfuscated environment"). The missing line is correct.
- Code uses **Mojang names**: `Identifier` (not `ResourceLocation`), `Mob`, `TamableAnimal`,
  `mobInteract`, `removeEffect`, etc.
- The dev client uses a **pinned username** (`--username WildboundDev` in the loom `runs { client {} }`
  block) so the offline player UUID is stable — without it owner-gated features (sit/stand) break after a
  reload.

## Architecture & adding a companion

Full map and rationale live in `docs/design.md` (Architecture, the `CompanionType` member table,
Advancements). In short: tamed state is Fabric attachments on the **existing vanilla mob** — no `Tamed*`
entity types, no swap. Per-animal code is one `CompanionType` subclass in `companion/<animal>/`; goals
attach via the `ENTITY_LOAD` hook, so most companions need no mixin.

Adding a ground/flying/swimming `PathfinderMob`:
1. `companion/<animal>/<Animal>Companion.java` extends `CompanionType`; register in `CompanionRegistry.init()`.
2. Add `data/wildbound/advancement/<animal>.json` (parent `wildbound:menagerie`, **not** `root`).
3. **Add a `companion_tamed` criterion + `requirements` entry to `wild_knows_your_name.json`** — the
   capstone is a hand-maintained per-animal list; skipping this silently drops the animal from "tame one
   of every kind".
4. Optional: override sit-pose hooks or `attachGoals` for type-specific goals (fox fetch is the example).

A mob that bypasses goals (bat) or needs behaviour vanilla doesn't expose to goals (rideable sheep) gets
its own mixin instead.

## Mixins

The authoritative list is `wildbound.mixins.json` + the `mixin/` package (keep them sorted and in sync);
design.md explains each behavioural mixin where its feature is described. The non-obvious rule:

- **`@Shadow`/`@Accessor`/`@Invoker` only resolve members *declared on* the `@Mixin` target class**, not
  inherited ones — it compiles fine and fails at mixin-apply (`InvalidAccessorException`). Target the
  declaring class or call public API (e.g. the mode cue uses `Level.playSound` because `makeSound` is on
  `LivingEntity`, not `Mob`; shared goals reach `goalSelector` via the `MobAccessor` `@Accessor`). Catch
  it with a headless `runServer`.

## Other gotchas learned

- Apply passive effects with the **6-arg** `MobEffectInstance(..., ambient, visible, showIcon)` — the
  5-arg form sets `showIcon = visible`, hiding the HUD icon (the buff's only indicator). Rationale in
  design.md.
- Vanilla bat **resting pins to `floor(y)+0.1`** — a ground perch's rest height is an integer, so a dip
  below it yanks the bat a block down. Snap to a stable resting Y before setting `resting`
  (`BatCompanion.settleTo`).
- Recurring **port 25565 in use** across back-to-back `runServer` runs = a leftover dev server (it also
  holds `run/world/session.lock`, so the next boot dies with `LockException`). Kill
  `net.minecraft`/`GradleWrapperMain`; not a mod error.

## Docs & conventions

One home per fact — link, don't restate:

- `docs/design.md` — **what & why** (living, as-built); behaviour changes and settled design questions land
  here in the same commit.
- `docs/refinements.md` — **open backlog only**. Jot rough edges with a source pointer and a one-line fix
  sketch instead of gold-plating mid-task; an item leaves only when verified (in-client for gameplay/visual)
  and is then deleted — the closing commit is the record.
- **The changelog is git.** Imperative subject + a short *why* body; for gameplay changes note verification
  status ("play-tested in-client" / "pending in-client pass"). One logical change per commit; feature
  branch, fast-forward into `main`.
