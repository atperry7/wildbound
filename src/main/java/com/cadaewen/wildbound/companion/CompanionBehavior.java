package com.cadaewen.wildbound.companion;

import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Shared runtime logic for every companion: ownership/sit state access, and the per-tick driver that
 * refreshes the passive effect and delegates movement to the {@link CompanionType}.
 */
public final class CompanionBehavior {

    private CompanionBehavior() {
    }

    public static boolean isCompanion(Mob mob) {
        return mob.hasAttached(WildboundAttachments.OWNER);
    }

    public static boolean isSitting(Mob mob) {
        return mob.getAttachedOrElse(WildboundAttachments.SITTING, Boolean.FALSE);
    }

    public static void setSitting(Mob mob, boolean sitting) {
        mob.setAttached(WildboundAttachments.SITTING, sitting);
    }

    public static UUID getOwnerUuid(Mob mob) {
        return mob.getAttached(WildboundAttachments.OWNER);
    }

    public static Player getOwner(Mob mob) {
        UUID uuid = getOwnerUuid(mob);
        return uuid == null ? null : mob.level().getPlayerByUUID(uuid);
    }

    public static void tame(Mob mob, Player owner) {
        mob.setAttached(WildboundAttachments.OWNER, owner.getUUID());
        mob.setAttached(WildboundAttachments.SITTING, Boolean.FALSE);
        // Companions never despawn (matters most for the bat, an AmbientCreature that otherwise would).
        mob.setPersistenceRequired();
    }

    /**
     * Drives a tamed companion each server tick. Call from the entity's AI-step hook.
     *
     * @return {@code true} if the mob's vanilla AI step should be cancelled this tick.
     */
    public static boolean serverTick(Mob mob) {
        CompanionType type = CompanionRegistry.get(mob);
        if (type == null || mob.level().isClientSide() || !isCompanion(mob)) {
            return false;
        }

        ServerLevel level = (ServerLevel) mob.level();
        Player owner = getOwner(mob);
        boolean sitting = isSitting(mob);

        refreshPassive(mob, type, owner, sitting);
        return type.serverTickBehavior(mob, level, owner, sitting);
    }

    /**
     * An active (following, in-range) companion refreshes its passive effect; an inactive one simply
     * stops, letting the effect fade on its own. There is no teardown scan, so a player can keep many
     * hundreds of companions cheaply, and a sitting companion never cancels an effect that another
     * following companion is still sustaining.
     */
    private static void refreshPassive(Mob mob, CompanionType type, Player owner, boolean sitting) {
        Holder<MobEffect> effect = type.passiveEffect();
        if (effect == null || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }
        boolean active = !sitting && mob.distanceToSqr(owner) <= CompanionType.FOLLOW_RANGE_SQR;
        if (active && mob.tickCount % CompanionType.REAPPLY_INTERVAL_TICKS == 0) {
            type.applyPassiveBonus(serverOwner);
        }
    }
}
