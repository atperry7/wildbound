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

- **Hard to aim where a bat perches.** Sit now prefers a ceiling overhead, falls back to perching on
  the ground below, and only hovers when neither is in reach (over a drop). But the player still can't
  *aim* it — it picks the nearest surface straight up/down, not the block they're looking at.
  - *Ideas:* search a small cone/radius rather than a straight column; prefer the block the player is
    looking at; snap to the owner's targeted block face.
  - Source: `BatCompanion.hangOrHover` / `findSurface` in `companion/bat/BatCompanion.java`.
  - *Note:* ground-perch floats ~0.1 block above the surface (vanilla resting pins to `floor(y)+0.1`).
    Negligible, but if it bugs us, drive a non-resting grounded pose instead.

## Effect lifecycle (accepted trade-offs, revisit only if they annoy in practice)

- Sitting your *last* companion leaves the passive effect lingering up to ~16s (no active teardown,
  by design — see design-doc-v1 "Passive Effect System"). Revisit only if players find the linger
  confusing.
- If your *only* companion dies (vs. sits), the effect also lingers to natural expiry, since a dead
  entity can't refresh or clear. Optional: a death hook for prompt clear.

## How to use this file

When something is "good enough for now," jot it here with the source location and a sketch of the
fix, then move on. Pull items into a refinement pass later.
