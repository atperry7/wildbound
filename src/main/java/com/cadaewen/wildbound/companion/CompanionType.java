package com.cadaewen.wildbound.companion;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Describes one tameable animal. Adding a companion means subclassing this and registering it in
 * {@link CompanionRegistry} — all shared taming, follow, sit, and effect logic lives in the framework.
 */
public abstract class CompanionType {

    /**
     * Passive-effect duration in ticks (16s). Refreshing every {@link #REAPPLY_INTERVAL_TICKS} keeps the
     * remaining time oscillating between 220 and 320 ticks — always above Night Vision's end-of-effect
     * flicker window (~200 ticks), so an active effect never strobes. When a companion stops refreshing
     * (sits / out of range), the effect simply fades on its own.
     */
    public static final int EFFECT_DURATION_TICKS = 320;

    /** How often the passive effect is refreshed while in range (5s). */
    public static final int REAPPLY_INTERVAL_TICKS = 100;

    /** Owner must be within this many blocks for the passive effect to apply. */
    public static final double FOLLOW_RANGE = 24.0;
    public static final double FOLLOW_RANGE_SQR = FOLLOW_RANGE * FOLLOW_RANGE;

    /** The vanilla item used to tame this animal (also the representative item for tag-based tamers). */
    public abstract Item tamingItem();

    /** Whether the given held stack can tame this animal. Override for tag-based items (e.g. any flower). */
    public boolean isTamingItem(ItemStack stack) {
        return stack.is(tamingItem());
    }

    /** Status effect granted while following, or {@code null} for special-case companions (e.g. Fox XP). */
    public abstract Holder<MobEffect> passiveEffect();

    /** Effect amplifier — always 0 (Level I) by design. */
    public int passiveAmplifier() {
        return 0;
    }

    /** Taming succeeds with a 1-in-N chance. */
    public int tamingChanceOneInN() {
        return 3;
    }

    /**
     * Per-tick movement/animation override for a tamed companion, server-side only.
     *
     * @return {@code true} to suppress the mob's vanilla AI step this tick.
     */
    public boolean serverTickBehavior(Mob mob, ServerLevel level, Player owner, boolean sitting) {
        return false;
    }

    /** Called once when a goal-driven companion enters sit mode. Override to apply a natural sit pose. */
    public void onStartSitting(Mob mob) {
    }

    /** Called once when a goal-driven companion leaves sit mode. Override to clear the sit pose. */
    public void onStopSitting(Mob mob) {
    }

    /** Called every tick while a goal-driven companion sits. Override to hold/re-assert a pose. */
    public void onSitTick(Mob mob) {
    }

    /**
     * If true, the sit goal won't stop navigation or zero velocity — the companion drives its own sit
     * movement in {@link #onSitTick} (e.g. a bee flying down to land rather than freezing mid-air).
     */
    public boolean controlsSitMovement() {
        return false;
    }

    /** Applies the passive bonus to the owner. Default grants the status effect; Fox-style companions override. */
    public void applyPassiveBonus(ServerPlayer owner) {
        Holder<MobEffect> effect = passiveEffect();
        if (effect == null) {
            return;
        }
        // ambient = true, visible = false: no particles, clean HUD — only the inventory effect icon shows.
        owner.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, passiveAmplifier(), true, false), null);
    }
}
