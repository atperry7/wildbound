package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Stops a tamed companion from attacking another tamed companion — e.g. a companion fox hunting the
 * owner's rabbit as prey. Combat targeting funnels through {@code canAttack} (TargetingConditions calls
 * it), so refusing here means the predator never acquires a companion as a target.
 */
@Mixin(Mob.class)
public abstract class MobCanAttackMixin {

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void wildbound$protectCompanions(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Mob targetMob
                && CompanionBehavior.isCompanion((Mob) (Object) this)
                && CompanionBehavior.isCompanion(targetMob)) {
            cir.setReturnValue(false);
        }
    }
}
