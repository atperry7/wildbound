package com.cadaewen.wildbound.companion.armadillo;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Armadillo companion: tamed with a Spider Eye, grants Resistance I while following. When it sits it rolls
 * into a ball — vanilla's scared state then periodically peeks on its own, so the curled pose feels alive.
 */
public class ArmadilloCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.SPIDER_EYE;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.RESISTANCE;
    }

    @Override
    public void onStartSitting(Mob mob) {
        rollUp(mob);
    }

    @Override
    public void onSitTick(Mob mob) {
        // Re-curl if it has fully unrolled back to idle (gives an occasional peek-and-reroll).
        rollUp(mob);
    }

    @Override
    public void onStopSitting(Mob mob) {
        if (mob instanceof Armadillo armadillo) {
            armadillo.switchToState(Armadillo.ArmadilloState.IDLE);
        }
    }

    private static void rollUp(Mob mob) {
        if (mob instanceof Armadillo armadillo && armadillo.getState() == Armadillo.ArmadilloState.IDLE) {
            armadillo.switchToState(Armadillo.ArmadilloState.ROLLING);
        }
    }
}
