package com.cadaewen.wildbound.companion.goal;

import java.util.EnumSet;

import com.cadaewen.wildbound.companion.CompanionBehavior;
import com.cadaewen.wildbound.companion.CompanionRegistry;
import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Generic "stay put while sitting" goal for goal-driven companions. Holds the movement and jump controls
 * at high priority so the mob stops wandering, kills residual velocity each tick (needed for swimmers like
 * the axolotl, which otherwise drifts), and delegates a natural sit pose to the {@link CompanionType}.
 */
public class CompanionSitGoal extends Goal {

    private final PathfinderMob mob;
    private CompanionType type;

    public CompanionSitGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return CompanionBehavior.isCompanion(mob) && CompanionBehavior.isSitting(mob);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        type = CompanionRegistry.get(mob);
        if (type == null || !type.controlsSitMovement()) {
            mob.getNavigation().stop();
        }
        if (type != null) {
            type.onStartSitting(mob);
        }
    }

    @Override
    public void stop() {
        if (type != null) {
            type.onStopSitting(mob);
        }
        type = null;
    }

    @Override
    public void tick() {
        if (type == null || !type.controlsSitMovement()) {
            mob.getNavigation().stop();
            // Cancel residual horizontal drift (swim/hover momentum); leave vertical to gravity/buoyancy.
            mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        }
        if (type != null) {
            type.onSitTick(mob);
        }
    }
}
