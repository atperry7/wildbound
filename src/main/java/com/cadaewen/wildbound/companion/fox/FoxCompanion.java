package com.cadaewen.wildbound.companion.fox;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Fox companion: tamed with Sweet Berries. The framework's one exception — its passive is not a status
 * effect but a <b>x2 XP bonus</b> while following, applied by {@link FoxXpBonus} via the experience mixin.
 * So {@link #passiveEffect()} is null and the per-tick effect path does nothing for the fox.
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
}
