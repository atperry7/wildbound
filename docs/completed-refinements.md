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
