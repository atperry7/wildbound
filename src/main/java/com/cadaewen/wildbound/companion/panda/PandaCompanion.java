package com.cadaewen.wildbound.companion.panda;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Panda companion: tamed with Bamboo, grants Regeneration I while following. Uses the vanilla sit pose. */
public class PandaCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.BAMBOO;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.REGENERATION;
    }

    @Override
    public void onStartSitting(Mob mob) {
        if (mob instanceof Panda panda) {
            panda.sit(true);
        }
    }

    @Override
    public void onSitTick(Mob mob) {
        // Re-assert in case a vanilla panda goal toggled it back off.
        if (mob instanceof Panda panda && !panda.isSitting()) {
            panda.sit(true);
        }
    }

    @Override
    public void onStopSitting(Mob mob) {
        if (mob instanceof Panda panda) {
            panda.sit(false);
        }
    }
}
