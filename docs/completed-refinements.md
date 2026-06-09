# Wildbound — Completed Refinements

Shipped refinements, moved out of [`refinements.md`](refinements.md) to keep that file open-items-only.
One line each — what shipped, plus the key class. Full rationale lives in `design-doc-v1` and `CLAUDE.md`.

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

## Mounts

- **Sheep mount: charged jump + water float** — play-tested in-client. A full jump charge lands the rider on
  a two-block ledge (`WILDBOUND_JUMP_STRENGTH = 0.6`), a tap gives a low hop, and the charge bar shows while
  mounted; riding into water bobs to the surface and paddles across, while a free-roaming companion sheep
  keeps vanilla water behaviour. `SheepMixin` + the `can_float_while_ridden` tag. ✅

## Combat

- **Companions stay out of mob combat (both directions)** — `MobCanAttackMixin` now refuses `canAttack`
  whenever *either* side is a companion. Tamed prey is untargetable by any mob (a wild fox/wolf or a
  hostile won't hunt the owner's rabbit); tamed predators are fully pacified in every mode (a companion
  fox/ocelot/axolotl acquires no mob target, so it stays with the owner instead of chasing wild prey —
  no retaliation/self-defence either, by design). Subsumes the old companion-vs-companion guard. The
  frog's tongue is a separate goal path and is left characterful. ✅
