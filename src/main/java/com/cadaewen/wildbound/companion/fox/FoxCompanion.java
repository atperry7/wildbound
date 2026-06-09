package com.cadaewen.wildbound.companion.fox;

import com.cadaewen.wildbound.companion.CompanionMode;
import com.cadaewen.wildbound.companion.CompanionType;
import com.cadaewen.wildbound.mixin.FoxAccessor;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Fox companion: tamed with Sweet Berries. The framework's one non-effect passive — instead of a status
 * effect, a following fox <b>fetches nearby dropped items</b> into its owner's inventory. The fetch is a
 * movement goal ({@link FoxFetchItemGoal}, attached via {@link #attachGoals}): the fox actually runs to the
 * item before delivering it. So {@link #passiveEffect()} is null.
 *
 * <p>On sit it takes the vanilla lying-down (sleeping) pose.
 */
public class FoxCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.SWEET_BERRIES;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return null;
    }

    @Override
    public void attachGoals(PathfinderMob mob, GoalSelector goals) {
        // Priority 0, tied with the follow goal but registered ahead of it (CompanionGoals attaches type
        // goals first; ties never preempt a running goal). While loot is around the fox keeps fetching —
        // even with the owner roaming past the follow-start distance — and heels only when the area is
        // clean or the owner leaves FOLLOW_RANGE, where the goal deactivates itself.
        goals.addGoal(0, new FoxFetchItemGoal(mob, 1.2));
    }

    /**
     * Suppress the vanilla "carry an item in its mouth" pickup for a companion fox, so the fetch goal is the
     * sole collector and everything is delivered to the owner instead of disappearing into the fox's mouth.
     * Never suppresses the fox's vanilla AI step.
     */
    @Override
    public boolean serverTickBehavior(Mob mob, ServerLevel level, Player owner, CompanionMode mode) {
        if (mob.canPickUpLoot()) {
            mob.setCanPickUpLoot(false);
        }
        return false;
    }

    @Override
    public void onStartSitting(Mob mob) {
        setLyingDown(mob, true);
    }

    @Override
    public void onSitTick(Mob mob) {
        // Re-assert in case a vanilla fox goal cleared the sleep flag (e.g. a nearby player would wake it).
        if (mob instanceof Fox fox && !fox.isSleeping()) {
            setLyingDown(mob, true);
        }
    }

    @Override
    public void onStopSitting(Mob mob) {
        setLyingDown(mob, false);
    }

    private static void setLyingDown(Mob mob, boolean lying) {
        if (mob instanceof Fox fox) {
            ((FoxAccessor) fox).wildbound$setSleeping(lying);
        }
    }
}
