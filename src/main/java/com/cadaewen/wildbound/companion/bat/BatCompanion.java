package com.cadaewen.wildbound.companion.bat;

import com.cadaewen.wildbound.companion.CompanionBehavior;
import com.cadaewen.wildbound.companion.CompanionMode;
import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Bat companion: tamed with a Spider Eye, grants Night Vision I while following.
 *
 * <p>The vanilla {@link Bat} ignores the goal system — its flight is hand-rolled in
 * {@code customServerAiStep}. So follow/sit can't be a {@code FollowOwnerGoal}; instead we steer the
 * bat's velocity directly (mirroring vanilla's own movement math) and reuse its native resting state
 * to hang from a ceiling when sitting.
 */
public class BatCompanion extends CompanionType {

    private static final double PREFERRED_DISTANCE = 4.0;
    private static final double PREFERRED_DISTANCE_SQR = PREFERRED_DISTANCE * PREFERRED_DISTANCE;
    private static final double TELEPORT_DISTANCE_SQR = 20.0 * 20.0;
    private static final double LEASH_RADIUS_SQR =
            (double) CompanionType.WANDER_LEASH_RADIUS * CompanionType.WANDER_LEASH_RADIUS;
    private static final int CEILING_SEARCH = 5;
    private static final int GROUND_SEARCH = 8;

    @Override
    public Item tamingItem() {
        return Items.SPIDER_EYE;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.NIGHT_VISION;
    }

    @Override
    public boolean serverTickBehavior(Mob mob, ServerLevel level, Player owner, CompanionMode mode) {
        if (!(mob instanceof Bat bat)) {
            return false;
        }
        if (mode == CompanionMode.WANDER) {
            return leashWander(bat);
        }
        if (mode == CompanionMode.SIT || owner == null) {
            hangOrHover(bat, level);
        } else {
            followOwner(bat, owner);
        }
        return true;
    }

    /**
     * Sit mode, in order of preference: hang from a ceiling overhead, else perch on the ground below,
     * else (over a drop with neither in reach) hover in place.
     */
    private void hangOrHover(Bat bat, ServerLevel level) {
        BlockPos ceiling = findSurface(bat, level, Direction.UP, CEILING_SEARCH);
        if (ceiling != null) {
            settleTo(bat, ceiling.getY() - bat.getBbHeight());
            return;
        }
        BlockPos ground = findSurface(bat, level, Direction.DOWN, GROUND_SEARCH);
        if (ground != null) {
            settleTo(bat, ground.getY() + 1.0);
            return;
        }
        bat.setResting(false);
        bat.setDeltaMovement(bat.getDeltaMovement().multiply(0.6, 0.6, 0.6));
    }

    /** Drift toward {@code restY}, then fold up and rest there. Works for both ceiling-hang and ground-perch. */
    private void settleTo(Bat bat, double restY) {
        if (Math.abs(restY - bat.getY()) > 0.15) {
            bat.setResting(false);
            // Approach speed eases to zero near the target so it never overshoots into a block.
            double step = Math.max(-0.2, Math.min(0.2, restY - bat.getY()));
            bat.setDeltaMovement(bat.getDeltaMovement().x * 0.6, step, bat.getDeltaMovement().z * 0.6);
        } else {
            // Snap to vanilla's stable resting height (floor(y)+0.1). For a ground perch restY is an
            // integer, and without this snap a slight dip below it would make the resting pin yank the
            // bat a full block down each tick — an oscillation that never settles.
            bat.snapTo(bat.getX(), Math.floor(restY) + 0.1, bat.getZ(), bat.getYRot(), bat.getXRot());
            bat.setResting(true);
            bat.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Follow mode: trail just above the owner's head, teleporting if left too far behind. */
    private void followOwner(Bat bat, Player owner) {
        bat.setResting(false);

        if (bat.distanceToSqr(owner) > TELEPORT_DISTANCE_SQR) {
            bat.teleportTo(owner.getX(), owner.getY() + 1.5, owner.getZ());
            return;
        }

        double targetX = owner.getX();
        double targetY = owner.getEyeY() + 1.0;
        double targetZ = owner.getZ();

        double dxOwner = targetX - bat.getX();
        double dzOwner = targetZ - bat.getZ();

        if (dxOwner * dxOwner + dzOwner * dzOwner <= PREFERRED_DISTANCE_SQR) {
            // Close enough: ease horizontal drift, just track the owner's height.
            Vec3 m = bat.getDeltaMovement();
            double dy = targetY - bat.getY();
            bat.setDeltaMovement(m.x * 0.6, m.y + (Math.signum(dy) * 0.3 - m.y) * 0.1, m.z * 0.6);
            return;
        }

        approachPoint(bat, targetX, targetY, targetZ);
    }

    /**
     * Wander mode: roam on vanilla flight, but stay within {@link CompanionType#WANDER_LEASH_RADIUS} of the
     * anchor. Inside the bubble we cede to vanilla (return {@code false}); once the bat drifts outside we
     * steer it back and cancel vanilla for the tick (return {@code true}).
     */
    private boolean leashWander(Bat bat) {
        BlockPos anchor = CompanionBehavior.getWanderAnchor(bat);
        if (anchor == null) {
            if (bat.isResting()) {
                bat.setResting(false);
            }
            return false;
        }
        double cx = anchor.getX() + 0.5;
        double cy = anchor.getY() + 1.0;
        double cz = anchor.getZ() + 0.5;
        if (bat.distanceToSqr(cx, cy, cz) <= LEASH_RADIUS_SQR) {
            if (bat.isResting()) {
                bat.setResting(false);
            }
            return false;
        }
        bat.setResting(false);
        approachPoint(bat, cx, cy, cz);
        return true;
    }

    /** Nudge the bat's velocity toward a target point and face it (vanilla-style flight steering). */
    private void approachPoint(Bat bat, double targetX, double targetY, double targetZ) {
        Vec3 m = bat.getDeltaMovement();
        double dx = targetX - bat.getX();
        double dy = targetY - bat.getY();
        double dz = targetZ - bat.getZ();
        Vec3 next = m.add(
                (Math.signum(dx) * 0.5 - m.x) * 0.1,
                (Math.signum(dy) * 0.7 - m.y) * 0.1,
                (Math.signum(dz) * 0.5 - m.z) * 0.1);
        bat.setDeltaMovement(next);

        float yaw = (float) (Mth.atan2(next.z, next.x) * 180.0F / (float) Math.PI) - 90.0F;
        bat.setYRot(bat.getYRot() + Mth.wrapDegrees(yaw - bat.getYRot()));
        bat.zza = 0.5F;
    }

    /**
     * Scans up to {@code range} blocks in {@code dir} for a block the bat can attach to, returning the
     * first whose facing surface is sturdy — the underside for a ceiling (UP), the top for ground (DOWN).
     */
    private BlockPos findSurface(Bat bat, ServerLevel level, Direction dir, int range) {
        BlockPos.MutableBlockPos pos = bat.blockPosition().mutable();
        Direction attachFace = dir.getOpposite();
        for (int i = 0; i < range; i++) {
            pos.move(dir);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, attachFace)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
