package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ambient.Bat;

/**
 * Hands a tamed bat over to the companion framework each server AI step. The bat's flight is
 * implemented in {@code customServerAiStep} (not the goal system), so this is where follow/sit and
 * the passive effect have to be driven; we cancel the vanilla step when the bat is a companion.
 */
@Mixin(Bat.class)
public abstract class BatMixin {

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void wildbound$companionAiStep(ServerLevel level, CallbackInfo ci) {
        if (CompanionBehavior.serverTick((Bat) (Object) this)) {
            ci.cancel();
        }
    }
}
