package com.cadaewen.wildbound.companion.panda;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Panda companion: tamed with Bamboo, grants Regeneration I while following. */
public class PandaCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.BAMBOO;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.REGENERATION;
    }
}
