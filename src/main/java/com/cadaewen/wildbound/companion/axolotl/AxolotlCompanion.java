package com.cadaewen.wildbound.companion.axolotl;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Axolotl companion: tamed with a Tropical Fish (item), grants Water Breathing I while following. */
public class AxolotlCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.TROPICAL_FISH;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.WATER_BREATHING;
    }
}
