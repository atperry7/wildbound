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
- **Colored mode-toggle cue** — dust puff + rising chime per mode (white = sit, purple = wander, gold =
  follow), via `CompanionTaming.announceModeToggle`. ✅

## Indicators

- **Effect-pet HUD icon** — `showIcon=true` so status companions show their effect icon while active. ✅

## Advancements

- **Taming-item hint** — each per-animal advancement's icon is its taming item, so the badge hints what
  to use. ✅
- **Menagerie sub-node** — inserted `menagerie.json` (*A Growing Menagerie*) as a quiet hub under `root`;
  re-parented all ten per-animal advancements and the capstone under it, so the capstone reads as the
  visible culmination of the animal cluster instead of a bare sibling of `root`. Teaching nodes (`wander`,
  `quiet`) stay directly under `root`. Single-parent limit means animals still can't each draw a line into
  the capstone; this is the closest legible layout. ✅
- **Capstone covers every companion** — `wild_knows_your_name` was missing `sheep` (required 9 of 10);
  added it so "tame one of every kind" really means all ten. ✅
- **Root as a discovery hint** — `root` ("Wildbound") now fires from vanilla `minecraft:inventory_changed`
  the moment the player first picks up *any* taming item, with a description that teaches the mechanic, so
  the tree surfaces the mod before the first tame. The first-tame beat moved to `menagerie` (toast/announce
  re-enabled). Onboarding escalation: get the food → first bond → collect → capstone. ✅

## Combat

- **Companions stay out of mob combat (both directions)** — `MobCanAttackMixin` now refuses `canAttack`
  whenever *either* side is a companion. Tamed prey is untargetable by any mob (a wild fox/wolf or a
  hostile won't hunt the owner's rabbit); tamed predators are fully pacified in every mode (a companion
  fox/ocelot/axolotl acquires no mob target, so it stays with the owner instead of chasing wild prey —
  no retaliation/self-defence either, by design). Subsumes the old companion-vs-companion guard. The
  frog's tongue is a separate goal path and is left characterful. ✅
