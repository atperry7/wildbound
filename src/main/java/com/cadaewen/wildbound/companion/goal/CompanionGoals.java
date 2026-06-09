package com.cadaewen.wildbound.companion.goal;

import com.cadaewen.wildbound.companion.CompanionRegistry;
import com.cadaewen.wildbound.companion.CompanionType;
import com.cadaewen.wildbound.mixin.MobAccessor;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;

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
        CompanionType type = CompanionRegistry.get(mob);
        // Per-type follow speed (most use the default boost; fast swimmers override it down to avoid outrunning
        // the owner). type is normally non-null here — guard anyway since attach predates a future config gate.
        double followSpeed = type != null ? type.followSpeed() : CompanionType.DEFAULT_FOLLOW_SPEED;
        // Priority 0 so follow/sit preempt vanilla wandering while active.
        goals.addGoal(0, new CompanionSitGoal(mob));
        goals.addGoal(0, new CompanionFollowOwnerGoal(mob, followSpeed, 7.0F, 2.5F, 16.0F));
        goals.addGoal(0, new CompanionTickGoal(mob));
        // Wander leash: walks the mob back when it drifts outside its home-point restriction. Dormant
        // unless a restriction is set, which only happens in WANDER (see CompanionBehavior.syncWanderLeash).
        // Priority 5 so it sits below follow/sit but above vanilla idle wandering.
        goals.addGoal(5, new MoveTowardsRestrictionGoal(mob, 1.0));

        // Companion-type-specific goals (e.g. the fox's item fetch). No-op for most types.
        if (type != null) {
            type.attachGoals(mob, goals);
        }
    }
}
