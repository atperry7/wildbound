# Wildbound

A vanilla+ Fabric mod that lets you tame passive mobs that have never been tameable before. Each
companion follows you and grants a gentle passive bonus — a quiet reward for the bond between player and
animal. Taming uses only items already found in vanilla Minecraft; no new items, blocks, or recipes.

- **Platform:** Minecraft Java Edition · Fabric
- **Minecraft:** 26.1.2 · **Fabric Loader:** 0.19.3+ · **Fabric API** required · **Java:** 25+

## Companions

Right-click a wild animal while holding its taming item. Taming has a chance to succeed (heart
particles); on failure it puffs smoke — try again. Tamed companions never despawn and don't respawn if
they die (like classic tamed pets).

| Animal | Taming item | Passive (while following) | Sit pose |
|---|---|---|---|
| Bat | Spider Eye | Night Vision I | Hangs from a ceiling, or perches on the ground |
| Rabbit | Dandelion | Jump Boost I | Stays put |
| Fox | Sweet Berries | **Fetches dropped items** (not a status effect) | Lies down |
| Ocelot | Raw Cod or Salmon | **XP ×2** (not a status effect) | Stays put |
| Frog | Slimeball | Slow Falling I | Stays put |
| Axolotl | Tropical Fish | Water Breathing I | Stays put |
| Bee | Any flower | Haste I | Flies down and lands |
| Panda | Bamboo | Regeneration I | Sits (vanilla pose) |
| Armadillo | Spider Eye | Resistance I | Rolls into a ball |
| Sheep | Apple | **Rideable mount** (not a status effect) | — (right-click to ride) |

Most effects are intentionally the weakest tier (Level I), apply silently (no particles — just the
effect icon in your HUD), and only while the companion is **following** and within 24 blocks.

Three companions have non-effect passives instead: the **Ocelot** doubles all XP you gain while it
follows (it sparkles green when the bonus lands), the **Fox** fetches nearby dropped items into your
inventory, and the **Sheep** is a saddle-free mount — right-click it with an empty hand to ride.

## Controls

- **Tame:** right-click a wild companion animal while holding its taming item.
- **Sit / follow:** right-click your companion with an empty hand to toggle.
- **Wander / follow:** sneak + right-click with an empty hand to toggle. A wandering companion roams
  freely near where you set it — still yours and won't run off, but grants no passive.
- **Quiet / restore a passive:** right-click with a milk bucket to toggle a companion's passive off or
  on, in case an effect is in your way.
- Each toggle plays a quick colour + chime cue so you know the new state at a glance — **white = sit,
  purple = wander, gold = follow** — handy since not every companion has an obvious resting pose.
- While **sitting**, a companion stays put, takes a natural resting pose (where it has one), and its
  passive turns off.

A companion only obeys its owner, and companions won't attack each other (your fox won't hunt your
rabbit).

## Advancements

Tame your first companion to earn **Wildbound**, with a child advancement for each animal, plus the
challenge **"The Wild Knows Your Name"** for taming one of every kind.

## Building

```bash
./gradlew build
```

The built jar lands in `build/libs/`. To run a dev client or server:

```bash
./gradlew runClient
./gradlew runServer
```

> Note: this Minecraft version ships de-obfuscated, so the build uses native Mojang names and declares
> **no** `mappings` in `build.gradle` (Loom rejects `officialMojangMappings()` here).

## Project docs

- `docs/design-doc-v1` — design document (kept in sync with what's built).
- `docs/refinements.md` — polish backlog and known rough edges.
- `CLAUDE.md` — architecture and conventions for contributors/agents.

## License

Released under [CC0 1.0](LICENSE) (public domain). Learn from it, fork it, ship it.
