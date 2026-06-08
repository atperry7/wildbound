package com.cadaewen.wildbound.companion;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

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

    public static CompanionMode getMode(Mob mob) {
        return mob.getAttachedOrElse(WildboundAttachments.MODE, CompanionMode.FOLLOW);
    }

    public static void setMode(Mob mob, CompanionMode mode) {
        mob.setAttached(WildboundAttachments.MODE, mode);
    }

    /**
     * Following: a companion trailing the owner and granting its passive while in range. Requires companion
     * status explicitly — {@link #getMode} defaults to {@code FOLLOW} for any mob, so an untamed mob would
     * otherwise read as "following".
     */
    public static boolean isFollowing(Mob mob) {
        return isCompanion(mob) && getMode(mob) == CompanionMode.FOLLOW;
    }

    /** Sitting: held in a natural pose, no passive. */
    public static boolean isSitting(Mob mob) {
        return getMode(mob) == CompanionMode.SIT;
    }

    /** Wandering: roaming on vanilla AI, owned but not following and granting no passive. */
    public static boolean isWandering(Mob mob) {
        return getMode(mob) == CompanionMode.WANDER;
    }

    public static UUID getOwnerUuid(Mob mob) {
        return mob.getAttached(WildboundAttachments.OWNER);
    }

    /** The spot a wandering companion is leashed to, or null if none is set. */
    public static BlockPos getWanderAnchor(Mob mob) {
        return mob.getAttached(WildboundAttachments.WANDER_ANCHOR);
    }

    public static void setWanderAnchor(Mob mob, BlockPos anchor) {
        mob.setAttached(WildboundAttachments.WANDER_ANCHOR, anchor);
    }

    public static Player getOwner(Mob mob) {
        UUID uuid = getOwnerUuid(mob);
        return uuid == null ? null : mob.level().getPlayerByUUID(uuid);
    }

    /** True if {@code owner} has a following (non-sitting) companion of {@code type} within follow range. */
    public static boolean hasActiveCompanion(Player owner, EntityType<?> type) {
        return findActiveCompanion(owner, type) != null;
    }

    /** The nearest following (non-sitting) companion of {@code type} owned by {@code owner} in range, or null. */
    public static Mob findActiveCompanion(Player owner, EntityType<?> type) {
        AABB box = owner.getBoundingBox().inflate(
                CompanionType.FOLLOW_RANGE, CompanionType.FOLLOW_RANGE, CompanionType.FOLLOW_RANGE);
        for (Mob mob : owner.level().getEntitiesOfClass(Mob.class, box, m -> m.getType() == type)) {
            if (isCompanion(mob) && isFollowing(mob)
                    && owner.getUUID().equals(getOwnerUuid(mob))
                    && mob.distanceToSqr(owner) <= CompanionType.FOLLOW_RANGE_SQR) {
                return mob;
            }
        }
        return null;
    }

    public static void tame(Mob mob, Player owner) {
        mob.setAttached(WildboundAttachments.OWNER, owner.getUUID());
        mob.setAttached(WildboundAttachments.MODE, CompanionMode.FOLLOW);
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
        CompanionMode mode = getMode(mob);

        refreshPassive(mob, type, owner, mode);
        syncWanderLeash(mob, type, mode);
        return type.serverTickBehavior(mob, level, owner, mode);
    }

    /**
     * Keeps a goal-driven companion's vanilla home-point restriction in sync with its mode: a wandering
     * companion is leashed to its anchor (re-applied every tick so it survives reload, where vanilla does
     * not persist the restriction), and any other mode clears it. The {@code MoveTowardsRestrictionGoal}
     * attached at load then walks the mob back whenever it drifts past {@link CompanionType#wanderLeashRadius}.
     *
     * <p>Only {@link PathfinderMob}s have this restriction system — the bat leashes itself in
     * {@link CompanionType#serverTickBehavior} instead.
     */
    private static void syncWanderLeash(Mob mob, CompanionType type, CompanionMode mode) {
        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }
        BlockPos anchor = getWanderAnchor(mob);
        if (mode == CompanionMode.WANDER && anchor != null) {
            if (!pathfinder.hasHome() || !pathfinder.getHomePosition().equals(anchor)) {
                pathfinder.setHomeTo(anchor, type.wanderLeashRadius());
            }
        } else if (pathfinder.hasHome()) {
            pathfinder.clearHome();
        }
    }

    /**
     * An active (following, in-range) companion refreshes its passive effect; an inactive one simply
     * stops, letting the effect fade on its own. There is no teardown scan, so a player can keep many
     * hundreds of companions cheaply, and a sitting companion never cancels an effect that another
     * following companion is still sustaining.
     */
    private static void refreshPassive(Mob mob, CompanionType type, Player owner, CompanionMode mode) {
        Holder<MobEffect> effect = type.passiveEffect();
        if (effect == null || !(owner instanceof ServerPlayer serverOwner)) {
            return;
        }
        boolean active = mode == CompanionMode.FOLLOW && mob.distanceToSqr(owner) <= CompanionType.FOLLOW_RANGE_SQR;
        if (active && mob.tickCount % CompanionType.REAPPLY_INTERVAL_TICKS == 0) {
            type.applyPassiveBonus(serverOwner);
        }
    }
}
