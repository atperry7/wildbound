package com.cadaewen.wildbound.companion.axolotl;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Axolotl companion: grants Night Vision I while following — a cave-dweller's gift that lights up the dark
 * underwater depths the bat's land-bound Night Vision can't reach (the bat won't dive). Pairs naturally with
 * the turtle's Water Breathing for an underwater-explorer duo.
 */
public class AxolotlCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        // Advancement icon only — a tropical fish for the axolotl's flavour. Taming is the universal item.
        return Items.TROPICAL_FISH;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.NIGHT_VISION;
    }
}
