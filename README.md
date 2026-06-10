# Wildbound

Some creatures the wild never meant to be tamed — until it heard the hum of amethyst. Wildbound is a
vanilla+ Fabric mod that lets you bond with passive mobs that have never been tameable before. Each
companion follows you (or settles where you ask) and grants a gentle passive bonus — a quiet reward
for the bond between player and animal. No new blocks, no crafting recipes; everything works through
items the game already gives you.

- **Platform:** Minecraft Java Edition · Fabric
- **Minecraft:** 26.1.2 · **Fabric Loader:** 0.19.3+ · **Fabric API** required · **Java:** 25+
- Install on the **server and the client** — riding the sheep needs the client side; everything else
  is server-driven and works even for vanilla clients.

## Taming

Every wild heart answers the same call: an **amethyst shard**. Right-click any companion animal while
holding one — the shard is spent, and the animal either trusts you (hearts) or doesn't yet (smoke —
try again, it's a 1-in-3 chance by default). A tamed companion keeps its name, its age, and its
quirks; it never despawns, and it doesn't respawn if it dies, so look after it.

## Companions

| Animal | Gift | Sit pose |
|---|---|---|
| Bat | Night Vision I | Hangs from a ceiling, or perches on the ground |
| Rabbit | Jump Boost I | Stays put |
| Frog | Slow Falling I | Stays put |
| Bee | Haste I | Flies down and lands |
| Panda | Regeneration I | Sits (vanilla pose) |
| Armadillo | Resistance I | Rolls into a ball |
| Axolotl | Night Vision I | Stays put |
| Turtle | Water Breathing I | Stays put |
| Ocelot | **XP ×2** | Stays put |
| Fox | **Fetches dropped items** | Curls up and sleeps |
| Sheep | **Rideable mount** | — (right-click to ride) |

Effects are intentionally the weakest tier, shown only as a HUD icon (no particle swirl), and granted
while the companion is **following or sitting** within 24 blocks. Sitting is a *parked* buff — plant
the bee at the mine entrance and work the area without it underfoot.

Three companions give something better than a status effect:

- The **Ocelot** doubles all XP you earn while it's near (a green sparkle marks the bonus landing).
- The **Fox** gathers for the den — yours, not its. While you chop, mine, or harvest, it darts from
  drop to drop and sends everything to your inventory, heeling only when the area is clean.
- The **Sheep** carries you, no saddle asked. Steer with movement keys, hold jump to charge a leap
  that clears a two-block ledge, and it paddles gamely across water while ridden.

## Controls

- **Tame:** right-click a wild animal with an **amethyst shard**.
- **Sit / follow:** right-click your companion with an empty hand. (The sheep mounts instead — park
  it with sneak + right-click.)
- **Wander / follow:** sneak + right-click with an empty hand. A wandering companion is off duty —
  it roams near where you set it, still yours and won't run off, but grants no passive.
- **Quiet / restore:** right-click with a **milk bucket** to toggle the passive off or on, for when
  an effect is in your way.
- **Capture:** right-click your companion with an **amethyst cluster** to fold it into the crystal —
  a **bound cluster** you can carry, chest, or ender-chest. Right-click a surface to release it; the
  crystal spends itself, and your companion steps out exactly as it went in. Made for long elytra
  trips and pets that would otherwise be stranded in unloaded chunks. (If you die holding it, it's
  gone with you — an ender chest is the safe place for a bound friend.)

Every toggle answers with a puff of dust in the new mode's colour — **white = sit, purple = wander,
gold = follow** — and the companion's own voice: the sheep baas, the frog croaks, the turtle keeps
its silence.

Companions only obey their owner, and tamed animals step out of the food chain entirely. **Nothing
wild hunts your companions** — a fox, wolf, or hostile mob won't target your tamed rabbit. And
**your companions hunt nothing** — a tamed fox, ocelot, or axolotl stays at your side instead of
chasing wild chickens or fish, so taming a predator calms it. (The frog's tongue is the one playful
exception — it'll still snap up a nearby slime.)

## Advancements

The first amethyst shard you pick up earns **Wildbound** — the wild has noticed you, and the
advancement tells you what to do with it. Your first bond opens **A Growing Menagerie**, with a child
advancement for each of the eleven animals, capped by the challenge **The Wild Knows Your Name** for
taming one of every kind. Three small teaching advancements mark your first wander, first
milk-quiet, and first capture.

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

- `docs/design.md` — living design document: architecture, mechanics, and design decisions, kept in sync with what's built.
- `docs/refinements.md` — open polish backlog.
- `CLAUDE.md` — working conventions for contributors/agents.

## License

Released under the [MIT License](LICENSE). Learn from it, fork it, ship it.
