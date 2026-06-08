package com.cadaewen.wildbound.companion.fox;

import com.cadaewen.wildbound.companion.CompanionType;
import com.cadaewen.wildbound.mixin.FoxAccessor;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Fox companion: tamed with Sweet Berries. The framework's one exception — its passive is not a status
 * effect but a <b>x2 XP bonus</b> while following, applied by {@link FoxXpBonus} via the experience mixin.
 * So {@link #passiveEffect()} is null and the per-tick effect path does nothing for the fox.
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
