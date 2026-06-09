package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * <p>Step height is intentionally absent here: {@code LivingEntity.maxUpStep()} already returns at least 1.0
 * (a horse's step height) whenever a player controls the entity, so a ridden sheep clears single blocks for
 * free.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin extends Animal {

    /**
     * Ridden speed as a fraction of the sheep's movement-speed attribute (0.23). 0.85 lands near 0.195 — a
     * brisk pace clearly faster than a sprinting player (~0.13), without being twitchy at 1-block step height.
     * Tunable by playtest.
     */
    private static final double WILDBOUND_RIDE_SPEED_FACTOR = 0.85;

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
    }

    @Override
    protected float getRiddenSpeed(final Player controller) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * WILDBOUND_RIDE_SPEED_FACTOR);
    }
}
