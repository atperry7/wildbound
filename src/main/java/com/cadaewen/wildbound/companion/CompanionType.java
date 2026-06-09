package com.cadaewen.wildbound.companion;

import com.cadaewen.wildbound.mixin.MobAccessor;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

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

    /** Default leash radius (blocks) for a wandering companion. Per-mob config may change it. */
    public static final int DEFAULT_WANDER_LEASH_RADIUS = 12;

    /**
     * The animal's signature food, used purely as the icon for its advancement. Taming itself goes through
     * the single universal item ({@link CompanionTaming#TAMING_ITEM}), so this no longer gates taming — it
     * just keeps each advancement reading as that animal's flavour (bamboo for panda, a flower for bee, …).
     */
    public abstract Item tamingItem();

    /** Status effect granted while following, or {@code null} for special-case companions (e.g. Fox XP). */
    public abstract Holder<MobEffect> passiveEffect();

    /**
     * Sound played when this companion's mode is toggled (sit/wander/follow), or {@code null} to fall back to
     * the particle cue alone. Defaults to the mob's own vanilla ambient voice — a sheep baas, a frog croaks —
     * so the feedback fits each animal instead of a shared synthetic chime. A type may override to pick a
     * cleaner or more distinctive sound. The caller voices it from the mob's own sound source/position.
     */
    public SoundEvent modeToggleSound(Mob mob) {
        return ((MobAccessor) mob).wildbound$getAmbientSound();
    }

    /** Default effect amplifier (Level I). Per-mob config may raise it via {@link #setPassiveAmplifier}. */
    public static final int DEFAULT_AMPLIFIER = 0;

    /** Default taming odds (1-in-3). Per-mob config may change it via {@link #setTamingChanceOneInN}. */
    public static final int DEFAULT_TAMING_CHANCE_ONE_IN_N = 3;

    private int passiveAmplifier = DEFAULT_AMPLIFIER;
    private int tamingChanceOneInN = DEFAULT_TAMING_CHANCE_ONE_IN_N;
    private int wanderLeashRadius = DEFAULT_WANDER_LEASH_RADIUS;

    /** Effect amplifier (0 = Level I). Configurable per mob. */
    public int passiveAmplifier() {
        return passiveAmplifier;
    }

    /** Config hook: set the effect amplifier; clamped to {@code >= 0}. */
    public void setPassiveAmplifier(int amplifier) {
        this.passiveAmplifier = Math.max(0, amplifier);
    }

    /** Taming succeeds with a 1-in-N chance. Configurable per mob. */
    public int tamingChanceOneInN() {
        return tamingChanceOneInN;
    }

    /** Config hook: set the taming odds; clamped to {@code >= 1} (guaranteed if 1). */
    public void setTamingChanceOneInN(int oneInN) {
        this.tamingChanceOneInN = Math.max(1, oneInN);
    }

    /** How far this companion may drift from its anchor while wandering before it's pulled back. */
    public int wanderLeashRadius() {
        return wanderLeashRadius;
    }

    /** Config hook: set the wander leash radius; clamped to {@code >= 1}. */
    public void setWanderLeashRadius(int radius) {
        this.wanderLeashRadius = Math.max(1, radius);
    }

    /**
     * Per-tick movement/animation override for a tamed companion, server-side only. Receives the current
     * {@link CompanionMode} so a non-goal companion (the bat) can drive follow/sit/wander itself.
     *
     * @return {@code true} to suppress the mob's vanilla AI step this tick.
     */
    public boolean serverTickBehavior(Mob mob, ServerLevel level, Player owner, CompanionMode mode) {
        return false;
    }

    /**
     * Attaches companion-type-specific goals on entity load, after the shared follow/sit/tick goals. Most
     * companions need none; the fox overrides this to add its item-fetch goal. Goals stay dormant until the
     * mob is tamed (they gate on companion state), so attaching to an untamed mob is harmless.
     */
    public void attachGoals(PathfinderMob mob, GoalSelector goals) {
    }

    /**
     * Per-type handling of an empty-hand right-click by the owner, evaluated before the generic SIT/WANDER
     * toggle (server-side only). Return a non-{@link InteractionResult#PASS} result to consume the gesture;
     * return {@code PASS} to let the framework toggle as usual. {@code sneaking} mirrors the toggle split
     * (sneak ⇒ WANDER, otherwise SIT) so a type can override only the plain-RC half. The rideable sheep uses
     * this to mount its owner on a plain right-click while leaving sneak-RC to park it via WANDER.
     */
    public InteractionResult onOwnerEmptyHandUse(Mob mob, ServerPlayer owner, boolean sneaking) {
        return InteractionResult.PASS;
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
        // ambient = true (gentle border), particles off, icon ON — the HUD/inventory effect icon is the
        // indicator that the companion's buff is active. NOTE: the 5-arg constructor sets showIcon = visible,
        // so we must use the 6-arg form to keep the icon while suppressing particles.
        owner.addEffect(
                new MobEffectInstance(effect, EFFECT_DURATION_TICKS, passiveAmplifier(), true, false, true), null);
    }
}
