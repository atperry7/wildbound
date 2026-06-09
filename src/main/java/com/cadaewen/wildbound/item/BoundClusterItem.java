package com.cadaewen.wildbound.item;

import java.util.function.Consumer;

import com.cadaewen.wildbound.companion.CompanionCapture;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A single-use amethyst cluster holding a captured companion. Right-clicking a surface releases the
 * companion and consumes (breaks) the cluster. The item is created only by capturing a companion (see
 * {@code CompanionTaming}); an empty one never exists, so it has no creative-tab entry.
 */
public class BoundClusterItem extends Item {

    public BoundClusterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        Entity released = CompanionCapture.release(
                stack, (ServerLevel) level, context.getClickedPos(), context.getClickedFace());
        if (released == null) {
            // No companion stored, or reconstruction failed — leave the cluster intact, do nothing.
            return InteractionResult.FAIL;
        }
        // The cluster shatters on release (single-use, by design).
        stack.shrink(1);
        level.playSound(null, released.getX(), released.getY(), released.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("item.wildbound.bound_cluster.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
