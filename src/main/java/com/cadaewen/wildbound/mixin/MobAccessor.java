package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/**
 * Exposes protected {@link Mob} members: {@code goalSelector} (so companion goals can be added to
 * subclasses) and the mob's own {@code getAmbientSound} (its natural voice — used as the default mode-toggle
 * cue). NB only members <em>declared on</em> {@code Mob} resolve here; inherited ones (e.g.
 * {@code LivingEntity.makeSound}) do not — the caller voices the sound via public API instead.
 */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("goalSelector")
    GoalSelector wildbound$goalSelector();

    /** The mob's current ambient sound, or {@code null} if it has none (e.g. a turtle) → particles-only. */
    @Invoker("getAmbientSound")
    SoundEvent wildbound$getAmbientSound();
}
