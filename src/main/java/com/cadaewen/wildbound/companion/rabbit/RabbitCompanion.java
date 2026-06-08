package com.cadaewen.wildbound.companion.rabbit;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Rabbit companion: tamed with a Dandelion, grants Jump Boost I while following.
 *
 * <p>Unlike the bat, the rabbit drives movement through the vanilla goal system, so it adds no movement
 * override here — its follow/sit goals are attached in {@code RabbitMixin}, and the shared tick (passive
 * effect) runs via {@code CompanionTickGoal}.
 */
public class RabbitCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.DANDELION;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.JUMP_BOOST;
    }
}
