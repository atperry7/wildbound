package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Stops a tamed companion from fleeing players (its own owner included) — a pet shouldn't run from you.
 * Avoid goals targeting non-players (wolves, monsters) are left intact, so a companion rabbit still flees
 * genuine threats. Generic: applies to any companion mob with a player-avoidance goal.
 */
@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin {

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Final
    protected Class<?> avoidClass;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void wildbound$dontFleePlayers(CallbackInfoReturnable<Boolean> cir) {
        if (avoidClass != null && avoidClass.isAssignableFrom(Player.class) && CompanionBehavior.isCompanion(mob)) {
            cir.setReturnValue(false);
        }
    }
}
