package com.cadaewen.wildbound.companion.strider;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Strider companion: tamed with an amethyst shard (icon: Warped Fungus), grants Fire Resistance I while following. */
public class StriderCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.WARPED_FUNGUS;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.FIRE_RESISTANCE;
    }
}
