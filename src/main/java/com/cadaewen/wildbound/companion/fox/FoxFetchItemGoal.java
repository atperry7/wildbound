package com.cadaewen.wildbound.companion.fox;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cadaewen.wildbound.companion.CompanionBehavior;
import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The Fox passive, as a movement goal: the fox spots a nearby dropped item, <b>pathfinds to it</b>, and once
 * it gets close acts as a small magnet — every item inside its pickup bubble is sent to the owner via vanilla
 * {@link ItemEntity#playerTouch} (giving the fly-to-player animation, sound, and pickup-delay / target /
 * full-inventory handling for free). The bubble is centred on the <i>fox</i> (so it still has to run over to
 * the loot), not the player, and is a touch wider than where pathfinding tends to park, so an item the fox
 * stops just short of — e.g. perched on a block edge — is still grabbed rather than stared at.
 *
 * <p>Holds MOVE+LOOK at priority 0 — <b>tied</b> with {@link CompanionFollowOwnerGoal} but registered ahead
 * of it (the goal selector breaks ties by registration order, and a tie never preempts a running goal). So
 * while there is loot to collect, fetch outranks follow: the fox chains item to item ({@link #tick}
 * retargets in-goal rather than stopping per pickup, so the MOVE flag is never yielded between items)
 * around an owner roaming the work area — the chop-a-forest case — and heels only once the area is clean.
 * How far it strays is bounded by {@code active()} in {@link #canContinueToUse}: fetch cuts out past
 * {@link CompanionType#FOLLOW_RANGE}, handing the fox back to follow (and its teleport). At the original
 * priority 1, follow instead preempted at its 7-block start distance, yo-yoing the fox on any loot just
 * past that tether.
 */
public class FoxFetchItemGoal extends Goal {

    /** How far from the fox a dropped item is noticed and chased (blocks). */
    private static final double SEARCH_RADIUS = 10.0;
    private static final double SEARCH_RADIUS_SQR = SEARCH_RADIUS * SEARCH_RADIUS;

    /** Radius of the fox's pickup bubble (blocks): items within it are magnetised to the owner. */
    private static final double PICKUP_RADIUS = 1.5;

    /** Once the fox has arrived but the target's still outside the bubble for this many ticks, it gives up. */
    private static final int STUCK_TICKS = 30;

    /** How long a failed-to-reach item is ignored before the fox will try it again. */
    private static final int BLACKLIST_TICKS = 100;

    /**
     * How often an idle fox rescans for loot. The goal selector polls {@code canUse} every tick the fox
     * isn't otherwise holding the MOVE flag, and the scan is an AABB entity query plus an owner-inventory
     * check per item found — too heavy for a per-tick idle poll, and a short delay before noticing a drop
     * is imperceptible (a player-dropped item isn't pickable for 40 ticks anyway).
     */
    private static final int SCAN_INTERVAL_TICKS = 10;

    private final PathfinderMob mob;
    private final double speed;
    private ItemEntity target;
    private Player owner;
    private int recalcCooldown;
    private int stuckTimer;
    private int scanCooldown;
    /** Items the fox couldn't reach, by entity id → game-time at which they become eligible again. */
    private final Map<Integer, Long> blacklist = new HashMap<>();

    public FoxFetchItemGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--scanCooldown > 0) {
            return false;
        }
        scanCooldown = adjustedTickDelay(SCAN_INTERVAL_TICKS);
        if (!active()) {
            return false;
        }
        ItemEntity item = nearestItem();
        if (item == null) {
            return false;
        }
        this.target = item;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // active() runs first in the chain so owner is set before ownerCanAccept reads it; if the owner's
        // inventory fills mid-chase, drop the target rather than keep running at undeliverable loot.
        return target != null && target.isAlive() && !target.getItem().isEmpty()
                && active() && ownerCanAccept(target.getItem())
                && mob.distanceToSqr(target) <= SEARCH_RADIUS_SQR;
    }

    @Override
    public void start() {
        recalcCooldown = 0;
        stuckTimer = STUCK_TICKS;
    }

    @Override
    public void stop() {
        target = null;
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        // Mobile magnet: vacuum up everything inside the fox's pickup bubble and hand it to the owner. Runs
        // every tick as the fox approaches, so an item it parks a little short of is still collected.
        collectBubble();
        if (!target.isAlive() || target.getItem().isEmpty()) {
            // Target got scooped up (or vanished) — chain straight to the next nearest item rather than
            // stopping: a stopped goal yields the MOVE flag, and follow (same priority, so it can't be
            // preempted back) would heel the fox before every next pickup whenever the owner is past the
            // follow-start distance. Only when no loot is left does the goal end and follow take over.
            target = nearestItem();
            if (target == null) {
                clearTarget();
                return;
            }
            recalcCooldown = 0;
            stuckTimer = STUCK_TICKS;
            return;
        }

        mob.getLookControl().setLookAt(target, 10.0F, mob.getMaxHeadXRot());
        boolean arrived = mob.getNavigation().isDone();
        if (!arrived) {
            // Still walking to it — not stuck, so reset the countdown and keep the path fresh on cadence.
            stuckTimer = STUCK_TICKS;
            if (--recalcCooldown <= 0) {
                recalcCooldown = adjustedTickDelay(10);
                mob.getNavigation().moveTo(target, speed);
            }
            return;
        }

        // Arrived (or the path ended) but the target's still outside the bubble — e.g. walled off where the
        // fox can't get within range. Keep replanning a few times; if it stays out of reach, blacklist it
        // briefly so the fox moves on to other items instead of freezing on this one.
        if (--stuckTimer <= 0 || !mob.getNavigation().moveTo(target, speed)) {
            blacklistTarget();
        }
    }

    /** Sends every item within {@link #PICKUP_RADIUS} of the fox to the owner via vanilla pickup-to-player. */
    private void collectBubble() {
        if (owner == null) {
            return;
        }
        AABB bubble = mob.getBoundingBox().inflate(PICKUP_RADIUS);
        for (ItemEntity item : mob.level().getEntitiesOfClass(ItemEntity.class, bubble,
                i -> i.isAlive() && !i.getItem().isEmpty())) {
            // playerTouch no-ops while an item's pickup delay is counting down, so a freshly thrown item is
            // collected once it's pickable, and a full inventory simply leaves it on the ground.
            item.playerTouch(owner);
        }
    }

    private void clearTarget() {
        target = null;
        mob.getNavigation().stop();
    }

    /** Drop the current target and ignore it for a short while, so the fox doesn't re-lock onto it at once. */
    private void blacklistTarget() {
        if (target != null) {
            blacklist.put(target.getId(), mob.level().getGameTime() + BLACKLIST_TICKS);
        }
        clearTarget();
    }

    /**
     * Fetch requires FOLLOW specifically — stricter than status passives, which also run while sitting. A
     * sitting fox is asleep, and fetch is a movement goal that would fight {@code CompanionSitGoal} for the
     * MOVE flag, so SIT parks the fox with no perk. In range and not milk-quieted, as for any passive.
     */
    private boolean active() {
        if (!CompanionBehavior.isFollowing(mob) || CompanionBehavior.isBuffDisabled(mob)) {
            return false;
        }
        Player candidate = CompanionBehavior.getOwner(mob);
        if (candidate == null || candidate.isSpectator()
                || mob.distanceToSqr(candidate) > CompanionType.FOLLOW_RANGE_SQR) {
            return false;
        }
        this.owner = candidate;
        return true;
    }

    /**
     * Whether the owner's inventory can take this stack — a free slot, or a partial stack of the same item
     * with room to top up ({@code getSlotWithRemainingSpace} handles the stack-matching). Guards the fox
     * against chasing loot that {@code playerTouch} would only bounce off a full inventory.
     */
    private boolean ownerCanAccept(ItemStack stack) {
        if (owner == null) {
            return false;
        }
        Inventory inventory = owner.getInventory();
        return inventory.getFreeSlot() >= 0 || inventory.getSlotWithRemainingSpace(stack) >= 0;
    }

    private ItemEntity nearestItem() {
        long now = mob.level().getGameTime();
        blacklist.values().removeIf(eligibleAt -> eligibleAt <= now);

        // Only chase loot worth chasing: skip items still in their pickup-delay window (a freshly thrown
        // item the fox would otherwise run to and wait out) and items the owner's inventory can't hold (a
        // full inventory makes playerTouch a no-op, so the fox would lock onto loot it can never deposit).
        AABB box = mob.getBoundingBox().inflate(SEARCH_RADIUS);
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, box,
                item -> item.isAlive() && !item.getItem().isEmpty()
                        && !item.hasPickUpDelay() && ownerCanAccept(item.getItem()));
        ItemEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            if (blacklist.containsKey(item.getId())) {
                continue;
            }
            double d = mob.distanceToSqr(item);
            if (d < best) {
                best = d;
                nearest = item;
            }
        }
        return nearest;
    }
}
