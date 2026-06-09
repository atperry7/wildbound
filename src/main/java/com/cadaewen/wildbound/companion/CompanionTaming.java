package com.cadaewen.wildbound.companion;

import com.cadaewen.wildbound.registry.ModCriteria;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Shared taming + sit-toggle interaction, wired to Fabric's {@code UseEntityCallback} so no per-animal
 * {@code interactMob} mixin is needed.
 */
public final class CompanionTaming {

    private CompanionTaming() {
    }

    public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
            Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        CompanionType type = CompanionRegistry.get(mob);
        if (type == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean tamed = CompanionBehavior.isCompanion(mob);

        // Empty main hand on our own companion → mode toggle. Sneaking toggles WANDER; otherwise SIT.
        // Each lands the companion in a predictable place: plain RC ⇒ SIT or FOLLOW; sneak+RC ⇒ WANDER or
        // FOLLOW. (Held-item interactions stay free for future per-companion actions, e.g. a buff toggle.)
        if (tamed && held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (!player.getUUID().equals(CompanionBehavior.getOwnerUuid(mob))) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                // Give the type first crack at the gesture (e.g. a rideable sheep mounts on plain RC).
                InteractionResult handled = type.onOwnerEmptyHandUse(mob, serverPlayer, player.isShiftKeyDown());
                if (handled != InteractionResult.PASS) {
                    return handled;
                }
                CompanionMode newMode = toggleMode(mob, player.isShiftKeyDown());
                if (newMode == CompanionMode.WANDER) {
                    ModCriteria.COMPANION_WANDERED.trigger(serverPlayer);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Milk bucket on your own companion → toggle its passive ("quieting" the buff, mirroring how milk
        // clears player effects). This rides the held-item channel, so it never clashes with the empty-hand
        // mode toggles, and milk has no vanilla mob interaction for us to swallow. The stack is NOT consumed.
        if (tamed && held.is(Items.MILK_BUCKET) && hand == InteractionHand.MAIN_HAND) {
            if (!player.getUUID().equals(CompanionBehavior.getOwnerUuid(mob))) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                boolean nowDisabled = !CompanionBehavior.isBuffDisabled(mob);
                CompanionBehavior.setBuffDisabled(mob, nowDisabled);
                announceBuffToggle((ServerLevel) level, mob, nowDisabled);
                if (nowDisabled && player instanceof ServerPlayer serverPlayer) {
                    ModCriteria.COMPANION_QUIETED.trigger(serverPlayer);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Taming attempt: holding the right item on an untamed animal. A config-disabled type is skipped
        // here so it behaves like a plain wild animal (already-tamed companions are unaffected).
        if (!tamed && CompanionRegistry.isEnabled(mob.getType()) && type.isTamingItem(held)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            ServerLevel serverLevel = (ServerLevel) level;
            if (!player.hasInfiniteMaterials()) {
                held.shrink(1);
            }
            boolean success = mob.getRandom().nextInt(type.tamingChanceOneInN()) == 0;
            if (success) {
                CompanionBehavior.tame(mob, player);
                spawnParticles(serverLevel, mob, ParticleTypes.HEART);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModCriteria.COMPANION_TAMED.trigger(serverPlayer, mob.getType());
                }
            } else {
                spawnParticles(serverLevel, mob, ParticleTypes.SMOKE);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    /**
     * Plain right-click toggles between {@link CompanionMode#SIT} and {@link CompanionMode#FOLLOW};
     * sneak right-click toggles between {@link CompanionMode#WANDER} and {@link CompanionMode#FOLLOW}.
     */
    private static CompanionMode toggleMode(Mob mob, boolean sneaking) {
        CompanionMode mode = CompanionBehavior.getMode(mob);
        CompanionMode next;
        if (sneaking) {
            next = mode == CompanionMode.WANDER ? CompanionMode.FOLLOW : CompanionMode.WANDER;
        } else {
            next = mode == CompanionMode.SIT ? CompanionMode.FOLLOW : CompanionMode.SIT;
        }
        CompanionBehavior.setMode(mob, next);
        // Anchor the wander leash to wherever the companion is told to wander (the sit spot, in the usual
        // sit-then-wander flow). The leash itself is applied each tick from this anchor.
        if (next == CompanionMode.WANDER) {
            CompanionBehavior.setWanderAnchor(mob, mob.blockPosition());
        }
        return next;
    }

    private static void spawnParticles(ServerLevel level, Mob mob, SimpleParticleType particle) {
        level.sendParticles(particle, mob.getX(), mob.getY() + 0.5, mob.getZ(), 7, 0.3, 0.3, 0.3, 0.02);
    }

    /** Small aud/visual cue for a buff toggle: a smoke puff + low chime when quieted, sparkle + bright chime when restored. */
    private static void announceBuffToggle(ServerLevel level, Mob mob, boolean disabled) {
        spawnParticles(level, mob, disabled ? ParticleTypes.SMOKE : ParticleTypes.HAPPY_VILLAGER);
        SoundEvent sound = disabled ? SoundEvents.AMETHYST_BLOCK_HIT : SoundEvents.AMETHYST_BLOCK_CHIME;
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound, SoundSource.NEUTRAL,
                0.7f, disabled ? 0.8f : 1.2f);
    }
}
