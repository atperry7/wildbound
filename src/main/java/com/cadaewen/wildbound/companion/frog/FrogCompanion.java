package com.cadaewen.wildbound.companion.frog;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Frog companion: tamed with a Slimeball, grants Slow Falling I while following. */
public class FrogCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.SLIME_BALL;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.SLOW_FALLING;
    }
}
