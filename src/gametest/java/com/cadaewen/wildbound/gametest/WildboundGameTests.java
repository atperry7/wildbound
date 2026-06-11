package com.cadaewen.wildbound.gametest;

import com.cadaewen.wildbound.companion.CompanionBehavior;
import com.cadaewen.wildbound.companion.CompanionCapture;
import com.cadaewen.wildbound.companion.CompanionMode;
import com.cadaewen.wildbound.companion.CompanionRegistry;
import com.cadaewen.wildbound.companion.CompanionTaming;
import com.cadaewen.wildbound.registry.ModComponents;
import com.cadaewen.wildbound.registry.ModItems;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Behaviour tests for the shared companion framework, run on a real headless server (mixins applied,
 * datapack loaded) via {@code ./gradlew runGameTest}. The rabbit stands in for every goal-driven
 * companion — these tests exercise the shared taming/mode/passive/capture paths, not per-animal quirks.
 *
 * <p>Interactions are driven by calling {@link CompanionTaming#onUseEntity} directly rather than through
 * the Fabric event, so a failure here points at Wildbound, never at another handler on the event.
 */
// makeMockServerPlayerInLevel() is deprecated-for-removal in 26.1, but it is the only mock that yields a
// ServerPlayer — which the mode-toggle, passive-refresh, and criteria paths all gate on. Revisit at 26.2.
@SuppressWarnings("removal")
public final class WildboundGameTests {

    private static final BlockPos SPAWN = new BlockPos(2, 2, 2);

    @GameTest
    public void directTameSetsCompanionState(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Mob rabbit = helper.spawn(EntityTypes.RABBIT, SPAWN);

        CompanionBehavior.tame(rabbit, owner);

        helper.assertTrue(CompanionBehavior.isCompanion(rabbit), "tame() should mark the mob as a companion");
        helper.assertValueEqual(CompanionBehavior.getMode(rabbit), CompanionMode.FOLLOW, "mode after taming");
        helper.assertValueEqual(CompanionBehavior.getOwnerUuid(rabbit), owner.getUUID(), "owner UUID");
        helper.assertTrue(rabbit.isPersistenceRequired(), "a companion must never despawn");
        helper.succeed();
    }

    @GameTest
    public void shardTamingThroughInteraction(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Mob rabbit = helper.spawn(EntityTypes.RABBIT, SPAWN);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD, 64));

        // Taming is RNG-gated (1-in-3 by default); 64 attempts make a miss astronomically unlikely
        // ((2/3)^64 ≈ 5e-12) without touching the mob's random.
        for (int i = 0; i < 64 && !CompanionBehavior.isCompanion(rabbit); i++) {
            CompanionTaming.onUseEntity(player, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        }

        helper.assertTrue(CompanionBehavior.isCompanion(rabbit), "shard interaction should tame within 64 attempts");
        helper.assertValueEqual(CompanionBehavior.getOwnerUuid(rabbit), player.getUUID(), "owner UUID");
        helper.succeed();
    }

    @GameTest
    public void emptyHandTogglesModes(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Mob rabbit = helper.spawn(EntityTypes.RABBIT, SPAWN);
        CompanionBehavior.tame(rabbit, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        CompanionTaming.onUseEntity(owner, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        helper.assertValueEqual(CompanionBehavior.getMode(rabbit), CompanionMode.SIT, "plain RC should sit");

        CompanionTaming.onUseEntity(owner, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        helper.assertValueEqual(CompanionBehavior.getMode(rabbit), CompanionMode.FOLLOW, "second RC should resume following");

        owner.setShiftKeyDown(true);
        CompanionTaming.onUseEntity(owner, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        helper.assertValueEqual(CompanionBehavior.getMode(rabbit), CompanionMode.WANDER, "sneak RC should wander");
        helper.assertTrue(CompanionBehavior.getWanderAnchor(rabbit) != null, "entering WANDER should anchor the leash");
        helper.succeed();
    }

    @GameTest
    public void milkQuietsAndRestoresThePassive(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Mob rabbit = helper.spawn(EntityTypes.RABBIT, SPAWN);
        CompanionBehavior.tame(rabbit, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));

        CompanionTaming.onUseEntity(owner, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        helper.assertTrue(CompanionBehavior.isBuffDisabled(rabbit), "milk should quiet the buff");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).is(Items.MILK_BUCKET),
                "the bucket must not be consumed");

        CompanionTaming.onUseEntity(owner, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        helper.assertFalse(CompanionBehavior.isBuffDisabled(rabbit), "a second milk should restore the buff");
        helper.succeed();
    }

    // One full refresh interval (100 ticks) plus setup slack. The passive applies when
    // mob.tickCount % REAPPLY_INTERVAL_TICKS == 0, so it lands within the first interval.
    @GameTest(maxTicks = 220)
    public void passiveAppliesToOwnerInRange(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        // The mock player spawns wherever the harness puts it; pin it inside the structure so the
        // 24-block range check is genuinely exercised from in range.
        Vec3 center = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        owner.snapTo(center.x, center.y, center.z, 0.0f, 0.0f);

        Mob rabbit = helper.spawn(EntityTypes.RABBIT, SPAWN);
        CompanionBehavior.tame(rabbit, owner);
        Holder<MobEffect> effect = CompanionRegistry.get(rabbit).passiveEffect();

        helper.succeedWhen(() -> helper.assertTrue(owner.hasEffect(effect),
                "owner should receive the passive within one refresh interval"));
    }

    @GameTest
    public void captureAndReleaseRoundTrip(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Mob rabbit = helper.spawn(EntityTypes.RABBIT, SPAWN);
        CompanionBehavior.tame(rabbit, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_CLUSTER));

        CompanionTaming.onUseEntity(owner, helper.getLevel(), InteractionHand.MAIN_HAND, rabbit, null);
        helper.assertTrue(rabbit.isRemoved(), "capture should discard the live companion");

        ItemStack cluster = findBoundCluster(owner);
        helper.assertTrue(cluster != null, "capture should hand the owner a bound cluster");
        helper.assertTrue(cluster.get(ModComponents.BOUND_ENTITY) != null, "cluster should carry the companion data");

        Entity released = CompanionCapture.release(cluster, helper.getLevel(),
                helper.absolutePos(SPAWN), Direction.UP);
        helper.assertTrue(released instanceof Mob mob && CompanionBehavior.isCompanion(mob),
                "release should rebuild a companion");
        helper.assertValueEqual(CompanionBehavior.getOwnerUuid((Mob) released), owner.getUUID(),
                "ownership must survive the round trip");
        helper.succeed();
    }

    private static ItemStack findBoundCluster(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.BOUND_CLUSTER)) {
                return stack;
            }
        }
        return null;
    }
}
