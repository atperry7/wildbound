package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cadaewen.wildbound.companion.goal.CompanionFollowOwnerGoal;
import com.cadaewen.wildbound.companion.goal.CompanionSitGoal;
import com.cadaewen.wildbound.companion.goal.CompanionTickGoal;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

/**
 * Attaches the shared companion goals to every rabbit. They lie dormant (their {@code canUse} checks for
 * companion state) until the rabbit is tamed, so wild rabbits are unaffected. Priority 0 lets the
 * follow/sit goals preempt vanilla wandering; fleeing the owner is handled by {@link AvoidEntityGoalMixin}.
 */
@Mixin(Rabbit.class)
public abstract class RabbitMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void wildbound$addCompanionGoals(CallbackInfo ci) {
        Rabbit self = (Rabbit) (Object) this;
        GoalSelector goals = ((MobAccessor) (Object) self).wildbound$goalSelector();
        goals.addGoal(0, new CompanionSitGoal(self));
        goals.addGoal(0, new CompanionFollowOwnerGoal(self, 1.2, 7.0F, 2.5F, 16.0F));
        goals.addGoal(0, new CompanionTickGoal(self));
    }
}
