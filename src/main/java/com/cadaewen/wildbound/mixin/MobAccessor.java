package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/** Exposes {@code Mob.goalSelector} (declared on {@link Mob}) so companion goals can be added to subclasses. */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("goalSelector")
    GoalSelector wildbound$goalSelector();
}
