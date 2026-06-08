package com.cadaewen.wildbound.companion.bee;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Bee companion: tamed with any flower, grants Haste I while following. Bees have no rest-pose flag, so
 * "sitting" means flying down to land on the ground and staying there.
 */
public class BeeCompanion extends CompanionType {

    private static final int GROUND_SEARCH = 12;

    @Override
    public Item tamingItem() {
        // Representative flower (used for the advancement icon); actual taming accepts the whole tag below.
        return Items.POPPY;
    }

    @Override
    public boolean isTamingItem(ItemStack stack) {
        return stack.is(ItemTags.FLOWERS);
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.HASTE;
    }

    @Override
    public boolean controlsSitMovement() {
        return true;
    }

    @Override
    public void onSitTick(Mob mob) {
        if (!(mob instanceof Bee bee)) {
            return;
        }
        if (bee.onGround()) {
            bee.getNavigation().stop();
            bee.setDeltaMovement(0.0, bee.getDeltaMovement().y, 0.0);
        } else if (bee.getNavigation().isDone()) {
            BlockPos ground = groundBelow(bee);
            if (ground != null) {
                bee.getNavigation().moveTo(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 1.0);
            } else {
                bee.setDeltaMovement(bee.getDeltaMovement().x * 0.6, -0.1, bee.getDeltaMovement().z * 0.6);
            }
        }
    }

    private static BlockPos groundBelow(Bee bee) {
        Level level = bee.level();
        BlockPos.MutableBlockPos pos = bee.blockPosition().mutable();
        for (int i = 0; i < GROUND_SEARCH; i++) {
            pos.move(Direction.DOWN);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
