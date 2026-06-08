package com.cadaewen.wildbound.companion.goal;

import java.util.EnumSet;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A flagless, always-running goal that drives the shared companion tick (passive effect refresh) for
 * goal-based companions. It holds no control flags, so it never conflicts with movement goals — it just
 * provides a reliable per-tick hook without an entity-class mixin.
 */
public class CompanionTickGoal extends Goal {

    private final Mob mob;

    public CompanionTickGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return CompanionBehavior.isCompanion(mob);
    }

    @Override
    public boolean canContinueToUse() {
        return CompanionBehavior.isCompanion(mob);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // Movement is handled by the follow/sit goals; we only need the passive-effect side effects here.
        CompanionBehavior.serverTick(mob);
    }
}
