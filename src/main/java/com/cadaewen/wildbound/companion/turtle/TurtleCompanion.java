package com.cadaewen.wildbound.companion.turtle;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Turtle companion: grants Water Breathing I while following — the shell's gift, mirroring vanilla's turtle
 * helmet. A {@link net.minecraft.world.entity.PathfinderMob}, so it rides the shared follow/sit/wander goals
 * with no mixin. (Turtles are slow on land, so a following turtle trails the player — by design.)
 */
public class TurtleCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        // Advancement icon only — seagrass for the turtle's flavour. Taming is the universal item.
        return Items.SEAGRASS;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.WATER_BREATHING;
    }
}
