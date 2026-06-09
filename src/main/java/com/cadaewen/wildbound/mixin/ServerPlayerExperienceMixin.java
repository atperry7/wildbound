package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.cadaewen.wildbound.companion.ocelot.OcelotXpBonus;

import net.minecraft.server.level.ServerPlayer;

/**
 * The Ocelot companion's x2 XP bonus. Every server-side XP award funnels through
 * {@code ServerPlayer.giveExperiencePoints}, so doubling the amount here covers all sources (mob kills,
 * mining, smelting, breeding, ...) when a following tamed ocelot is nearby.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExperienceMixin {

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int wildbound$ocelotXpBonus(int amount) {
        return OcelotXpBonus.boosted((ServerPlayer) (Object) this, amount);
    }
}
