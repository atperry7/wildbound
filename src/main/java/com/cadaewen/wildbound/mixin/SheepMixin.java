package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Makes a tamed sheep a controllable mount. A vanilla {@link Sheep} never overrides the ridden-control hooks,
 * so — like the bat needs its own AI-step mixin — the rideable sheep needs these overrides merged in. Pattern
 * mirrors the pig/horse: once {@link #getControllingPassenger()} returns the rider, vanilla routes movement
 * through {@code travelRidden} in {@code LivingEntity}, which calls the three methods below.
 *
 * <p>Declaring {@code extends Animal} gives compile-time access to the inherited control hooks and fields
 * (it's stripped at apply time; mixin classes are never instantiated). Control is gated purely on the first
 * passenger being a {@link Player}: the only way a player rides a sheep is our owner-gated mount interaction,
 * and this gate works client-side too (no dependence on the server-only owner attachment, which the client
 * lacks — without that the client wouldn't treat itself as the controller and movement would desync).
 *
 * <p><b>Step height</b> is intentionally absent here: {@code LivingEntity.maxUpStep()} already returns at
 * least 1.0 (a horse's step height) whenever a player controls the entity, so a ridden sheep clears single
 * blocks for free.
 *
 * <p><b>Charged jump (two-block ledges).</b> Single blocks step for free, but anything taller stops the sheep.
 * Implementing {@link PlayerRideableJumping} makes the client treat the ridden sheep like a horse: it shows
 * the jump-charge bar and, on release, sends the charge to the server (and calls {@link #onPlayerJump(int)}
 * locally). The impulse itself is applied client-authoritatively in {@link #tickRidden} exactly as
 * {@code AbstractHorse} does — a full charge clears a two-block ledge, a tap is a low hop. The owner gate is
 * the same player-controlled check as steering: only our owner-gated mount interaction ever seats a player.
 *
 * <p><b>Floating on water</b> is <i>not</i> here — it's the vanilla {@code floatInWaterWhileRidden} mechanic,
 * gated on the {@code minecraft:can_float_while_ridden} entity-type tag (horses, camels, …). Adding
 * {@code minecraft:sheep} to that tag (see {@code data/minecraft/tags/entity_type/can_float_while_ridden.json})
 * makes a ridden sheep bob to the surface and paddle across instead of sinking, with no code at all.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin extends Animal implements PlayerRideableJumping {

    /**
     * Ridden speed as a fraction of the sheep's movement-speed attribute (0.23). 0.85 lands near 0.195 — a
     * brisk pace clearly faster than a sprinting player (~0.13), without being twitchy at 1-block step height.
     * Tunable by playtest.
     */
    private static final double WILDBOUND_RIDE_SPEED_FACTOR = 0.85;

    /**
     * Vertical impulse applied at <b>full</b> jump charge (partial charges scale this down to 0.4×). 0.6 mirrors
     * a strong horse's jump-strength attribute and clears ~2.2 blocks — comfortably onto a two-high ledge with
     * a little margin. Tunable by playtest.
     */
    private static final float WILDBOUND_JUMP_STRENGTH = 0.6F;

    /** Pending jump charge [0.4..1.0], set when the rider releases the jump key; consumed next on-ground tick. */
    @Unique
    private float wildbound$jumpPendingScale;

    private SheepMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
        // The mob's own AI input is zero while ridden; read the rider's movement keys instead (like the horse).
        float sideways = controller.xxa * 0.5F;
        float forward = controller.zza;
        if (forward < 0.0F) {
            forward *= 0.25F; // reverse is a slow shuffle, matching vanilla mounts
        }
        return new Vec3(sideways, 0.0, forward);
    }

    @Override
    protected void tickRidden(final Player controller, final Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        // Steer with the rider's look; snap body/head so the sheep faces where it travels.
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

        // Charged-jump release: the controlling client owns the physics (mirrors AbstractHorse.tickRidden).
        // The charge persists in the air and only fires once we're back on the ground.
        if (this.isLocalInstanceAuthoritative() && this.onGround()) {
            if (this.wildbound$jumpPendingScale > 0.0F && !this.isJumping()) {
                this.wildbound$executeJump(this.wildbound$jumpPendingScale, riddenInput);
            }
            this.wildbound$jumpPendingScale = 0.0F;
        }
    }

    @Override
    protected float getRiddenSpeed(final Player controller) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * WILDBOUND_RIDE_SPEED_FACTOR);
    }

    // --- PlayerRideableJumping: horse-style charged jump so the rider can clear two-block ledges ---

    @Override
    public boolean canJump() {
        // Same gate as steering: only a player-controlled (i.e. owner-mounted) sheep is jumpable.
        return this.getControllingPassenger() instanceof Player;
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
        if (jumpAmount < 0) {
            jumpAmount = 0;
        }
        // 0.4..1.0 charge curve (interface default): a tap hops low, a full hold clears two blocks.
        this.wildbound$jumpPendingScale = this.getPlayerJumpPendingScale(jumpAmount);
    }

    @Override
    public void handleStartJump(final int jumpScale) {
        // Server-side ack only; the impulse is applied client-authoritatively in tickRidden.
    }

    @Override
    public void handleStopJump() {
    }

    @Unique
    private void wildbound$executeJump(final float scale, final Vec3 riddenInput) {
        double impulse = WILDBOUND_JUMP_STRENGTH * scale * this.getBlockJumpFactor();
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x, impulse, movement.z);
        this.needsSync = true;
        if (riddenInput.z > 0.0) {
            // Forward boost so a jump taken while moving carries the rider onto the ledge, not straight up.
            float sin = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            float cos = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * sin * scale, 0.0, 0.4F * cos * scale));
        }
    }
}
