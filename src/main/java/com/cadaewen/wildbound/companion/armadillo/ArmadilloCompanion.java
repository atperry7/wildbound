package com.cadaewen.wildbound.companion.armadillo;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Armadillo companion: tamed with a Spider Eye, grants Resistance I while following. */
public class ArmadilloCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.SPIDER_EYE;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.RESISTANCE;
    }
}
