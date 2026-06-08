package com.cadaewen.wildbound.registry;

import com.cadaewen.wildbound.Wildbound;
import com.cadaewen.wildbound.advancement.CompanionTamedTrigger;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Registers Wildbound's advancement criterion triggers. */
public final class ModCriteria {

    public static final CompanionTamedTrigger COMPANION_TAMED = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, "companion_tamed"),
            new CompanionTamedTrigger());

    private ModCriteria() {
    }

    /** Touching this class from the initializer forces the trigger above to register. */
    public static void init() {
    }
}
