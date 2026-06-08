package com.cadaewen.wildbound.companion;

import java.util.HashMap;
import java.util.Map;

import com.cadaewen.wildbound.companion.armadillo.ArmadilloCompanion;
import com.cadaewen.wildbound.companion.axolotl.AxolotlCompanion;
import com.cadaewen.wildbound.companion.bat.BatCompanion;
import com.cadaewen.wildbound.companion.bee.BeeCompanion;
import com.cadaewen.wildbound.companion.frog.FrogCompanion;
import com.cadaewen.wildbound.companion.panda.PandaCompanion;
import com.cadaewen.wildbound.companion.rabbit.RabbitCompanion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Maps vanilla entity types to their Wildbound companion definitions. */
public final class CompanionRegistry {

    private static final Map<EntityType<?>, CompanionType> BY_TYPE = new HashMap<>();

    private CompanionRegistry() {
    }

    public static void register(EntityType<?> type, CompanionType companion) {
        BY_TYPE.put(type, companion);
    }

    public static CompanionType get(EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    public static CompanionType get(Entity entity) {
        return BY_TYPE.get(entity.getType());
    }

    public static int count() {
        return BY_TYPE.size();
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
    }
}
