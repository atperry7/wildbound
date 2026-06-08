package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.cadaewen.wildbound.companion.fox.FoxXpBonus;

import net.minecraft.server.level.ServerPlayer;

/**
 * The Fox companion's x2 XP bonus. Every server-side XP award funnels through
 * {@code ServerPlayer.giveExperiencePoints}, so doubling the amount here covers all sources (mob kills,
 * mining, smelting, breeding, ...) when a following tamed fox is nearby.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExperienceMixin {

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int wildbound$foxXpBonus(int amount) {
        return FoxXpBonus.boosted((ServerPlayer) (Object) this, amount);
    }
}
