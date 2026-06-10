# Wildbound — Refinement Backlog

Open items only. Settled design choices live in [`design.md`](design.md) ("Design decisions & accepted
trade-offs"); shipped work lives in git history (the closing commit notes verification status).

**Per item:** `subsystem` tag · rough effort **S**/**M**/**L** · a `→` source pointer. _Needs a decision_
is blocked on a design call, not code.

An item leaves this file only when **verified** — for gameplay/visual behaviour that means the in-client
pass, not compiles-and-loads (note a pending play-test in the item's sketch until then). On completion,
delete the item; the commit is the record.

## Active — ready to work

- [ ] **capture · S** — Bound-cluster release snaps the mob to the face-adjacent block centre with no
  hitbox adjustment, so a wide mob (panda, 1.3 blocks) released against a wall overlaps it. Sketch: offset
  the spawn point by entity dimensions the way vanilla spawn eggs do. First check in-client whether
  vanilla push-out already makes this a non-issue. → `companion/CompanionCapture.java` (`release`)

## Needs a decision

_(none open)_
