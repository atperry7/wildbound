package com.cadaewen.wildbound.companion.bat;

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
    private static final int CEILING_SEARCH = 5;

    @Override
    public Item tamingItem() {
        return Items.SPIDER_EYE;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.NIGHT_VISION;
    }

    @Override
    public boolean serverTickBehavior(Mob mob, ServerLevel level, Player owner, boolean sitting) {
        if (!(mob instanceof Bat bat)) {
            return false;
        }
        if (sitting || owner == null) {
            hangOrHover(bat, level);
        } else {
            followOwner(bat, owner);
        }
        return true;
    }

    /** Sit mode: rise to the nearest ceiling within range and hang from it, else hover in place. */
    private void hangOrHover(Bat bat, ServerLevel level) {
        BlockPos ceiling = findCeiling(bat, level);
        if (ceiling != null) {
            double restY = ceiling.getY() - bat.getBbHeight();
            if (bat.getY() < restY - 0.1) {
                bat.setResting(false);
                bat.setDeltaMovement(0.0, 0.2, 0.0);
            } else {
                bat.setResting(true);
                bat.setDeltaMovement(Vec3.ZERO);
            }
        } else {
            bat.setResting(false);
            bat.setDeltaMovement(bat.getDeltaMovement().multiply(0.6, 0.6, 0.6));
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
        Vec3 m = bat.getDeltaMovement();

        if (dxOwner * dxOwner + dzOwner * dzOwner <= PREFERRED_DISTANCE_SQR) {
            // Close enough: ease horizontal drift, just track the owner's height.
            double dy = targetY - bat.getY();
            bat.setDeltaMovement(m.x * 0.6, m.y + (Math.signum(dy) * 0.3 - m.y) * 0.1, m.z * 0.6);
            return;
        }

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

    private BlockPos findCeiling(Bat bat, ServerLevel level) {
        BlockPos.MutableBlockPos pos = bat.blockPosition().mutable();
        for (int i = 0; i < CEILING_SEARCH; i++) {
            pos.move(Direction.UP);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.DOWN)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
