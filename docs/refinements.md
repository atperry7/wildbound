# Wildbound — Refinement Backlog

Polish items deferred from initial implementation. These are *known, accepted* rough edges — the core
loop works; these make it feel better. Grouped loosely; not strictly ordered.

## Bat movement & perching

- **Bats stack on top of each other when following.** Multiple following bats converge on the same
  target point (just above the owner's head) and overlap. One briefly disappeared inside another.
  - *Likely cause:* every bat steers toward an identical target with no separation/spread.
  - *Ideas:* per-bat target offset (ring/orbit around the owner, seeded by entity id); a light
    separation nudge when two companions are within ~1 block; stagger follow heights.
  - Source: `BatCompanion.followOwner` in `companion/bat/BatCompanion.java`.

- **Hard to get bats to perch where you want.** Sit-to-ceiling rises to the *nearest* sturdy block
  within 5 blocks straight up; players can't aim it, and "hover in place" when no ceiling feels
  floaty.
  - *Ideas:* search a small cone/radius rather than straight up; prefer the block the player is
    looking at; snap to the owner's targeted block face; tune hover damping so a no-ceiling sit
    settles instead of drifting.
  - Source: `BatCompanion.hangOrHover` / `findCeiling` in `companion/bat/BatCompanion.java`.

## Effect lifecycle (accepted trade-offs, revisit only if they annoy in practice)

- Sitting your *last* companion leaves the passive effect lingering up to ~16s (no active teardown,
  by design — see design-doc-v1 "Passive Effect System"). Revisit only if players find the linger
  confusing.
- If your *only* companion dies (vs. sits), the effect also lingers to natural expiry, since a dead
  entity can't refresh or clear. Optional: a death hook for prompt clear.

## How to use this file

When something is "good enough for now," jot it here with the source location and a sketch of the
fix, then move on. Pull items into a refinement pass later.
