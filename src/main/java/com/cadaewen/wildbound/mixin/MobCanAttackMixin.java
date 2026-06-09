package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Keeps companions out of mob combat in both directions. Targeting funnels through {@code canAttack}
 * (TargetingConditions calls it), so refusing here means the would-be attacker never acquires the target:
 *
 * <ul>
 *   <li><b>Tamed prey is protected</b> — when the <i>target</i> is a companion, no mob can attack it, so a
 *       wild fox/wolf won't hunt the owner's tamed rabbit (and neither will a hostile mob).</li>
 *   <li><b>Tamed predators are pacified</b> — when the <i>attacker</i> is a companion, it acquires no mob
 *       target at all, so a companion fox/ocelot/axolotl stays with the owner instead of wandering off
 *       after wild prey. Fully pacified in every mode (no retaliation/self-defence either, by design).</li>
 * </ul>
 *
 * <p>This subsumes the original companion-vs-companion guard (both ends are companions → still false). The
 * frog's tongue is a separate goal path and is intentionally left characterful (see refinements).
 */
@Mixin(Mob.class)
public abstract class MobCanAttackMixin {

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void wildbound$keepCompanionsOutOfCombat(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (CompanionBehavior.isCompanion((Mob) (Object) this)
                || (target instanceof Mob targetMob && CompanionBehavior.isCompanion(targetMob))) {
            cir.setReturnValue(false);
        }
    }
}
