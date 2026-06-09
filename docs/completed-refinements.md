# Wildbound — Completed Refinements

Shipped refinements, moved out of [`refinements.md`](refinements.md) to keep that file open-items-only.
One line each — what shipped, plus the key class. Full rationale lives in `design-doc-v1` and `CLAUDE.md`.

## Sit keeps the buff (v1.8, play-tested)

- **SIT now grants the passive** — a sitting companion refreshes its effect under the same range and
  milk-quiet gates as a following one, making sit a *parked buff* (plant the bee at the mine entrance,
  work the area without it underfoot). Each control has one job: FOLLOW = trailing buff, SIT = parked
  buff, WANDER = off duty, milk = silenced. Mode-gating is centralized in `CompanionMode.grantsPassive`
  (used by both the effect refresh and `findActiveCompanion`, so the sitting ocelot doubles XP too). The
  fox is the deliberate exception — fetch stays FOLLOW-only (its sit pose is sleeping, and fetch would
  fight the sit goal for the MOVE flag). (`CompanionMode`, `CompanionBehavior`) ✅

## Multiplayer hardening (code-review batch, play-tested)

- **Passenger-capture guard** — capturing a companion riding a boat/minecart now does nothing (pet and
  cluster both kept). `Entity.save` returns false for passengers; previously the empty stack shipped and
  the discard deleted the pet. `capture()` now also honours `save`'s return value, so the
  "serialize-before-discard" invariant is enforced, not just sequenced.
  (`CompanionTaming`, `CompanionCapture`) ✅
- **Spectator owners ignored** — the ground follow goal stops mid-chase, and the bat perches instead of
  teleport-chasing a spectator through walls. (`CompanionFollowOwnerGoal`, `BatCompanion`) ✅
- **Per-player ocelot XP cache** — the per-tick scan cache is keyed by player UUID and cleared each tick,
  so simultaneous grinders don't evict each other and no entity reference outlives its tick.
  (`OcelotXpBonus`) ✅

## Fox fetch retune (work-area vacuum, play-tested)

- **Fetch outranks follow while loot is around** — type goals register ahead of the shared goals (priority
  ties break by registration order and never preempt a running goal), and the goal chains item to item
  inside `tick()` rather than stopping per pickup, so follow can't claim the MOVE flag between items. The
  fox clears a work area (tree felling) around a roaming owner and heels only when the area is clean or
  the owner leaves `FOLLOW_RANGE` (follow + its teleport recover). Replaces the old priority-1 setup,
  which yo-yoed the fox on loot just past follow's 7-block tether.
  (`FoxFetchItemGoal`, `FoxCompanion`, `CompanionGoals`) ✅
- **Search radius 8 → 10 blocks** — notices drops sooner; pairs with the chaining above so the wider
  radius is actually reachable. (`FoxFetchItemGoal`) ✅
- **Fetch idle-scan throttle** — the loot scan polls every 10 ticks instead of every tick when idle
  (it's an AABB entity query + per-item inventory check); fetch still feels instant in play.
  (`FoxFetchItemGoal`) ✅

## Sit poses

Natural per-animal pose on sit, via `CompanionType` hooks driven by `CompanionSitGoal`:

- **Panda** — vanilla `sit(true)`. ✅
- **Armadillo** — rolls into a ball. ✅
- **Axolotl** — fixed the "wandered while sitting" drift (per-tick velocity zeroing). ✅
- **Fox** — lies down (`setSleeping`). ✅
- **Bee** — flies down and lands; playtested, lands cleanly. ✅

## Modes & toggles

- **Wander mode** — third mode (FOLLOW / SIT / WANDER) as a `CompanionMode` attachment; sneak-RC toggles
  it. Playtested per species. ✅
- **Wander leash** — anchors where set, kept within `wanderRadius` (default 12); no boundary jitter. ✅
- **"Home on the Range" advancement** — fires on first wander to teach the control. ✅
- **Milk-bucket buff toggle** — RC a companion with milk to quiet/restore its passive (`BUFF_DISABLED`). ✅
- **Colored mode-toggle cue** — dust puff per mode (white = sit, purple = wander, gold = follow) via
  `CompanionTaming.announceModeToggle`; the colour is the unambiguous confirmation. ✅
- **Mode toggle voiced by the mob itself** — replaced the shared amethyst chime with each companion's own
  vanilla ambient sound (a sheep baas, a frog croaks), via a `CompanionType.modeToggleSound` hook defaulting
  to the mob's `getAmbientSound` (new `MobAccessor` invoker). Silent mobs (e.g. turtle → null) show just the
  particles. Played at the mob's sound source with the vanilla baby-pitch bump; mode is carried by the
  particle colour, so the sound is pure flavour. ✅

## Indicators

- **Effect-pet HUD icon** — `showIcon=true` so status companions show their effect icon while active. ✅

## Advancements

- **Per-animal advancement icons** — each per-animal advancement's icon is that animal's signature food
  (`CompanionType.tamingItem()`), kept for flavour even after taming went universal. ✅
- **Menagerie sub-node** — inserted `menagerie.json` (*A Growing Menagerie*) as a quiet hub under `root`;
  re-parented all ten per-animal advancements and the capstone under it, so the capstone reads as the
  visible culmination of the animal cluster instead of a bare sibling of `root`. Teaching nodes (`wander`,
  `quiet`) stay directly under `root`. Single-parent limit means animals still can't each draw a line into
  the capstone; this is the closest legible layout. ✅
- **Capstone covers every companion** — `wild_knows_your_name` was missing `sheep` (required 9 of 10);
  added it so "tame one of every kind" really means all ten. ✅
- **Root as a discovery hint** — `root` ("Wildbound") now fires from vanilla `minecraft:inventory_changed`
  the moment the player first picks up the universal taming item (an amethyst shard), with a description that
  teaches the mechanic, so the tree surfaces the mod before the first tame. The first-tame beat moved to
  `menagerie` (toast/announce re-enabled). Onboarding escalation: get the shard → first bond → collect →
  capstone. ✅

## Roster & taming

- **Turtle companion** — 11th companion; grants Water Breathing I (the shell's gift, like the turtle
  helmet). A `PathfinderMob`, so it rides the shared follow/sit/wander goals with no mixin. Added its
  `menagerie`-parented advancement and the matching capstone entry. ✅
- **Axolotl → Night Vision** — moved Water Breathing to the turtle and gave the axolotl Night Vision, an
  underwater-vision companion the land-bound bat can't be (the bat won't dive). Deliberately shares the
  bat's effect; the two cover non-overlapping contexts (caves vs. depths). ✅
- **Universal taming item (amethyst shard)** — replaced every per-animal taming food with one item,
  `CompanionTaming.TAMING_ITEM`. One-stack taming kit, and because amethyst is no mob's breeding food and
  has no vanilla mob right-click, taming no longer hijacks breeding (or any vanilla gesture) — wild animals
  breed normally again. `CompanionType.tamingItem()` survives as advancement-icon flavour only; the bee's
  flower-tag / golden-dandelion-exclusion and the ocelot's cod-or-salmon overrides are gone. `root.json`
  now keys off the single amethyst-shard pickup. ✅

## Movement & follow

- **Following bats spread out instead of stacking** — each bat now orbits its own point around the owner
  rather than all converging just above the head. A stable per-bat angle (golden-angle spread by entity id,
  `ORBIT_RADIUS = 1.2`) plus a small height stagger (`(id % 3) * 0.4`) gives a flock distinct slots; the
  follow teleport drops each bat at its own offset too. `BatCompanion.followOwner`. ✅
- **Axolotl swims at the owner's pace, no zip** — play-tested in-client. The follow goal passed a fixed
  1.2 nav-speed boost to every type; for the axolotl (`SmoothSwimmingMoveControl`, in-water mod 0.1,
  `MOVEMENT_SPEED` 1.0) that meant 0.12 b/t — faster than a swimming player, and the move control's ~10°/tick
  yaw cap turned each overshoot into a wide arc. Added a per-type `CompanionType.followSpeed()` knob (default
  1.2) and set the axolotl to 1.0 (0.10 b/t, parity); the 16-block follow-teleport still backstops a sprinting
  owner. Frog (in-water mod 0.02) and turtle (slow by default) were already slow enough to leave alone.
  `CompanionGoals` + `AxolotlCompanion.followSpeed`. ✅

## Passives

- **Fox fetch only commits to deliverable loot** — play-tested in-client. The fox-fetch goal
  (`FoxFetchItemGoal`) now filters its target search (and drops a target mid-chase) by two conditions via a
  new `ownerCanAccept` helper and vanilla `ItemEntity.hasPickUpDelay`: it won't target loot the owner's
  inventory can't hold (a free slot or a partial stack with room) — a full-inventory owner's fox investigates,
  finds no room, and moves on, picking the item up the moment a slot frees — and it ignores a freshly-thrown
  item until its pickup delay clears instead of running over and waiting it out. Follow (priority 0) still
  outranks fetch, so a fox that can't deposit just trails the owner rather than getting stuck. No new
  mixins/registration — pure goal logic. `companion/fox/FoxFetchItemGoal.java`. ✅

## Mounts

- **Sheep mount: charged jump + water float** — play-tested in-client. A full jump charge lands the rider on
  a two-block ledge (`WILDBOUND_JUMP_STRENGTH = 0.6`), a tap gives a low hop, and the charge bar shows while
  mounted; riding into water bobs to the surface and paddles across, while a free-roaming companion sheep
  keeps vanilla water behaviour. `SheepMixin` + the `can_float_while_ridden` tag. ✅

## Transport & storage

- **Companion capture (bound cluster)** — play-tested in-client. An owner right-clicks their own companion
  with an **amethyst cluster** (the silk-touch block, distinct from the shard tamer) to pocket it into a
  single-use `wildbound:bound_cluster`; right-clicking a surface releases it and the cluster shatters. Solves
  pets stranded in unloaded chunks (elytra travel) and lets a player shelve a herd to cut entity lag — the
  bound cluster stacks to 1 and stores in chests/ender chests, surviving a relog. The mod's **first registered
  item + data component** (`ModItems` / `ModComponents`); the whole mob NBT — entity-type id + persistent
  attachments — serializes into a persistent-only `BOUND_ENTITY` `CustomData` (`CompanionCapture`), the mob is
  discarded only after the stack is built (never lose a pet to a failed serialize), and release rebuilds it
  with a fresh UUID and its goals re-attached via the normal `ENTITY_LOAD` hook. Owner-gated (others/wild do
  nothing) and refused on a mount carrying a rider. The cluster's title favours a **name-tagged** name ("Bound
  Batty") over the species via a baked `ITEM_NAME`, and an `ENCHANTMENT_GLINT_OVERRIDE` glint marks it as
  charged. Item model mirrors vanilla's cluster *item* (flat `item/generated` sprite) so the glint renders
  clean — parenting the 3-D `block/cross` model made the glint bleed across the cross planes. First capture
  lights the **"In Safe Hands"** advancement off `root`. New companions are auto-capturable, no per-animal
  work. `CompanionTaming` capture branch + `CompanionCapture` + `item/BoundClusterItem`. ✅

## Combat

- **Companions stay out of mob combat (both directions)** — `MobCanAttackMixin` now refuses `canAttack`
  whenever *either* side is a companion. Tamed prey is untargetable by any mob (a wild fox/wolf or a
  hostile won't hunt the owner's rabbit); tamed predators are fully pacified in every mode (a companion
  fox/ocelot/axolotl acquires no mob target, so it stays with the owner instead of chasing wild prey —
  no retaliation/self-defence either, by design). Subsumes the old companion-vs-companion guard. The
  frog's tongue is a separate goal path and is left characterful. ✅
