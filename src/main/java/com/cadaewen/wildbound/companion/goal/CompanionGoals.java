package com.cadaewen.wildbound.companion.goal;

import com.cadaewen.wildbound.mixin.MobAccessor;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/**
 * Attaches the shared companion goals to a goal-driven mob. Called once per entity load (see the
 * {@code ServerEntityEvents.ENTITY_LOAD} hook), so any registered ground/flying/swimming companion gets
 * follow/sit/tick behaviour with no per-animal mixin. The goals stay dormant until the mob is tamed.
 */
public final class CompanionGoals {

    private CompanionGoals() {
    }

    public static void attachTo(PathfinderMob mob) {
        GoalSelector goals = ((MobAccessor) mob).wildbound$goalSelector();
        // Priority 0 so follow/sit preempt vanilla wandering while active.
        goals.addGoal(0, new CompanionSitGoal(mob));
        goals.addGoal(0, new CompanionFollowOwnerGoal(mob, 1.2, 7.0F, 2.5F, 16.0F));
        goals.addGoal(0, new CompanionTickGoal(mob));
    }
}
