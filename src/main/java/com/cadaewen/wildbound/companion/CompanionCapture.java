package com.cadaewen.wildbound.companion;

import java.util.UUID;

import com.cadaewen.wildbound.Wildbound;
import com.cadaewen.wildbound.registry.ModComponents;
import com.cadaewen.wildbound.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Serialize/deserialize a companion to and from a {@code bound_cluster} item. Capture stores the mob's full
 * NBT — including the entity-type id and the persistent Wildbound attachments (owner/mode/wander anchor/buff)
 * — in the item's {@link ModComponents#BOUND_ENTITY} component, so release reproduces the exact same companion.
 */
public final class CompanionCapture {

    private CompanionCapture() {
    }

    /**
     * Build a loaded {@code bound_cluster} holding {@code mob}'s serialized state, or {@code null} if the
     * mob cannot be serialized — {@code Entity.save} refuses a passenger (returns false having written
     * nothing), so capturing a companion riding a boat/minecart would otherwise build an empty cluster.
     * The caller is responsible for removing the live mob — build the stack first and only then
     * {@code discard()}, never the reverse, and on {@code null} keep both the mob and the cluster.
     */
    public static ItemStack capture(Mob mob) {
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(mob.problemPath(), Wildbound.LOGGER)) {
            TagValueOutput out = TagValueOutput.createWithContext(reporter, mob.registryAccess());
            if (!mob.save(out)) {
                return null;
            }
            CompoundTag tag = out.buildResult();
            ItemStack stack = new ItemStack(ModItems.BOUND_CLUSTER);
            stack.set(ModComponents.BOUND_ENTITY, CustomData.of(tag));
            // Title the cluster after the companion, favouring a name-tagged name ("Bound Batty") over the
            // species ("Bound Bat"). Computed here, where the live mob's custom name is in hand, and baked into
            // the vanilla ITEM_NAME component (a translatable so the species still localizes client-side) —
            // simpler and more correct than decoding the saved name on the client. A faint enchant glint marks
            // the cluster as "charged" with a companion while keeping the vanilla amethyst texture/colour.
            Component animal = mob.hasCustomName() ? mob.getCustomName() : mob.getType().getDescription();
            stack.set(DataComponents.ITEM_NAME,
                    Component.translatable("item.wildbound.bound_cluster.named", animal));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
            return stack;
        }
    }

    /**
     * Spawn the companion stored in {@code stack} against the clicked block face. Returns the live entity on
     * success, or {@code null} if the stack carries no companion or the entity could not be reconstructed —
     * in which case the caller must NOT consume the cluster (never lose a pet to a failed release).
     */
    public static Entity release(ItemStack stack, ServerLevel level, BlockPos clickedPos, Direction face) {
        CustomData data = stack.get(ModComponents.BOUND_ENTITY);
        if (data == null || data.isEmpty()) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        Entity entity;
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(Wildbound.LOGGER)) {
            ValueInput in = TagValueInput.create(reporter, level.registryAccess(), tag);
            entity = EntityType.create(in, level, new EntitySpawnRequest(EntitySpawnReason.SPAWN_ITEM_USE, false)).orElse(null);
        }
        if (entity == null) {
            return null;
        }
        BlockPos spawnPos = clickedPos.relative(face);
        entity.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                entity.getYRot(), entity.getXRot());
        // Fresh identity: the captured mob was discarded, but a new UUID rules out any duplicate-UUID clash
        // (e.g. if the same cluster were somehow released twice across a desync).
        entity.setUUID(UUID.randomUUID());
        if (!level.addFreshEntity(entity)) {
            return null;
        }
        return entity;
    }
}
