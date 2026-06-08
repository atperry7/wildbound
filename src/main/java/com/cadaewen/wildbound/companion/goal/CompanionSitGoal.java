package com.cadaewen.wildbound.companion.goal;

import java.util.EnumSet;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Generic "stay put while sitting" goal for ground companions. Holds the movement and jump controls at
 * high priority so the mob stops wandering and remains where it was told to sit.
 */
public class CompanionSitGoal extends Goal {

    private final PathfinderMob mob;

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
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
    }
}
