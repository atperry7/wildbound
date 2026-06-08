package com.cadaewen.wildbound.companion.bee;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Bee companion: tamed with any flower, grants Haste I while following. */
public class BeeCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        // Representative flower (used for the advancement icon); actual taming accepts the whole tag below.
        return Items.POPPY;
    }

    @Override
    public boolean isTamingItem(ItemStack stack) {
        return stack.is(ItemTags.FLOWERS);
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.HASTE;
    }
}
