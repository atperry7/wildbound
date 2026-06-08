package com.cadaewen.wildbound.companion;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.cadaewen.wildbound.Wildbound;
import com.cadaewen.wildbound.companion.armadillo.ArmadilloCompanion;
import com.cadaewen.wildbound.companion.axolotl.AxolotlCompanion;
import com.cadaewen.wildbound.companion.bat.BatCompanion;
import com.cadaewen.wildbound.companion.bee.BeeCompanion;
import com.cadaewen.wildbound.companion.fox.FoxCompanion;
import com.cadaewen.wildbound.companion.frog.FrogCompanion;
import com.cadaewen.wildbound.companion.panda.PandaCompanion;
import com.cadaewen.wildbound.companion.rabbit.RabbitCompanion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Maps vanilla entity types to their Wildbound companion definitions. */
public final class CompanionRegistry {

    // LinkedHashMap so the catalogue (and the generated default config) keeps registration order.
    private static final Map<EntityType<?>, CompanionType> BY_TYPE = new LinkedHashMap<>();

    /**
     * Types disabled by config. Empty by default (everything enabled), so until a config system populates
     * this the behaviour is unchanged. {@link #BY_TYPE} stays the full catalogue — disabling never removes
     * a type, it just gates new taming (see {@link #isEnabled}); already-tamed companions keep working.
     */
    private static final Set<EntityType<?>> DISABLED = new HashSet<>();

    private CompanionRegistry() {
    }

    public static void register(EntityType<?> type, CompanionType companion) {
        CompanionType previous = BY_TYPE.put(type, companion);
        if (previous != null) {
            Wildbound.LOGGER.warn("Duplicate companion registration for {}: {} replaced {}.",
                    type, companion.getClass().getSimpleName(), previous.getClass().getSimpleName());
        }
    }

    public static CompanionType get(EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    public static CompanionType get(Entity entity) {
        return BY_TYPE.get(entity.getType());
    }

    /**
     * Whether this type is a registered companion that is currently enabled. Future per-mob config toggles
     * flip this via {@link #setEnabled}; the taming flow consults it so a disabled mob behaves like a plain
     * wild animal. Read state (effects/goals on existing companions) deliberately does not check this, so
     * disabling a type never strands pets already tamed.
     */
    public static boolean isEnabled(EntityType<?> type) {
        return BY_TYPE.containsKey(type) && !DISABLED.contains(type);
    }

    /** Enables or disables taming of a registered companion type (config hook; no-op for unregistered types). */
    public static void setEnabled(EntityType<?> type, boolean enabled) {
        if (enabled) {
            DISABLED.remove(type);
        } else {
            DISABLED.add(type);
        }
    }

    public static int count() {
        return BY_TYPE.size();
    }

    /** All registered companions in registration order (read-only). Used to generate the default config. */
    public static Map<EntityType<?>, CompanionType> all() {
        return Collections.unmodifiableMap(BY_TYPE);
    }

    /** Registers every companion. Bat is the first; further animals slot in here. */
    public static void init() {
        register(EntityType.BAT, new BatCompanion());
        register(EntityType.RABBIT, new RabbitCompanion());
        register(EntityType.PANDA, new PandaCompanion());
        register(EntityType.ARMADILLO, new ArmadilloCompanion());
        register(EntityType.FROG, new FrogCompanion());
        register(EntityType.BEE, new BeeCompanion());
        register(EntityType.AXOLOTL, new AxolotlCompanion());
        register(EntityType.FOX, new FoxCompanion());
    }
}
