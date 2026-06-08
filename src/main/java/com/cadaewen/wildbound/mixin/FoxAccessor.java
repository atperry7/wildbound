package com.cadaewen.wildbound.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.animal.fox.Fox;

/** Exposes {@code Fox.setSleeping} (private) so a sitting companion fox can take the lying-down pose. */
@Mixin(Fox.class)
public interface FoxAccessor {

    @Invoker("setSleeping")
    void wildbound$setSleeping(boolean sleeping);
}
