package com.cadaewen.wildbound.companion.goal;

import java.util.EnumSet;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Generic "follow your owner" goal for ground companions (anything with navigation). Activates only
 * while the mob is a following companion; teleports to the owner if left too far behind. Registered at
 * high priority so it preempts vanilla wandering.
 */
public class CompanionFollowOwnerGoal extends Goal {

    private final PathfinderMob mob;
    private final double speed;
    private final float startDistSqr;
    private final float stopDistSqr;
    private final float teleportDistSqr;
    private Player owner;
    private int recalcCooldown;

    public CompanionFollowOwnerGoal(PathfinderMob mob, double speed, float startDist, float stopDist, float teleportDist) {
        this.mob = mob;
        this.speed = speed;
        this.startDistSqr = startDist * startDist;
        this.stopDistSqr = stopDist * stopDist;
        this.teleportDistSqr = teleportDist * teleportDist;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!CompanionBehavior.isFollowing(mob)) {
            return false;
        }
        Player candidate = CompanionBehavior.getOwner(mob);
        if (candidate == null || candidate.isSpectator() || mob.distanceToSqr(candidate) < startDistSqr) {
            return false;
        }
        this.owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!CompanionBehavior.isFollowing(mob)) {
            return false;
        }
        return owner != null && owner.isAlive() && mob.distanceToSqr(owner) > stopDistSqr;
    }

    @Override
    public void start() {
        recalcCooldown = 0;
    }

    @Override
    public void stop() {
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
        if (--recalcCooldown > 0) {
            return;
        }
        recalcCooldown = adjustedTickDelay(10);
        if (mob.distanceToSqr(owner) >= teleportDistSqr) {
            teleportToOwner();
        } else {
            mob.getNavigation().moveTo(owner, speed);
        }
    }

    private void teleportToOwner() {
        BlockPos around = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = mob.getRandom().nextInt(7) - 3;
            int dy = mob.getRandom().nextInt(3) - 1;
            int dz = mob.getRandom().nextInt(7) - 3;
            BlockPos dest = around.offset(dx, dy, dz);
            if (canStandAt(dest)) {
                mob.snapTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canStandAt(BlockPos pos) {
        Level level = mob.level();
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.isEmptyBlock(pos)
                && level.isEmptyBlock(pos.above());
    }
}
