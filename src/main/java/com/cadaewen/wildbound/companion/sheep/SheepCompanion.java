package com.cadaewen.wildbound.companion.sheep;

import com.cadaewen.wildbound.companion.CompanionBehavior;
import com.cadaewen.wildbound.companion.CompanionMode;
import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Sheep companion: tamed with an <b>apple</b>. Its "passive" is not a status effect but the ability to be
 * <b>ridden</b> — an empty-hand right-click by the owner hops them on (no saddle required), and from there the
 * sheep steers with the rider's movement keys and look. The actual steering — making the sheep a controllable
 * vehicle, faster than a sprinting player, with a horse's step height so single blocks need no jump — lives in
 * {@code SheepMixin}; this type only wires the taming item and the mount gesture.
 *
 * <p>{@link #passiveEffect()} is therefore {@code null} (the per-tick effect path does nothing for the sheep).
 */
public class SheepCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        return Items.APPLE;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return null;
    }

    /**
     * Empty-hand right-click mounts the sheep (no sneak, not already riding). Sneak right-click is left to the
     * framework so it still parks the mount via WANDER; a rideable companion has no use for a SIT pose, so the
     * plain-RC slot is repurposed for mounting instead.
     */
    @Override
    public InteractionResult onOwnerEmptyHandUse(Mob mob, ServerPlayer owner, boolean sneaking) {
        if (sneaking || owner.isPassenger()) {
            // Sneak ⇒ let the generic toggle handle WANDER. Already riding ⇒ nothing to do (sneak dismounts).
            return InteractionResult.PASS;
        }
        // A ridden sheep should stay in the "active" FOLLOW mode, not lurk in SIT/WANDER while carrying you.
        CompanionBehavior.setMode(mob, CompanionMode.FOLLOW);
        owner.startRiding(mob);
        return InteractionResult.SUCCESS;
    }
}
