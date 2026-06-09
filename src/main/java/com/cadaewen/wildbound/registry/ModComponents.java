package com.cadaewen.wildbound.registry;

import com.cadaewen.wildbound.Wildbound;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;

/** Registers Wildbound's data components — the mod's first, introduced by companion capture. */
public final class ModComponents {

    /**
     * The serialized companion stored inside a {@code wildbound:bound_cluster} — the full entity NBT
     * (including the entity-type {@code id} and the persistent Wildbound attachments, so the released
     * companion is byte-for-byte the one that was captured). Persistent only: it's read server-side at
     * release and never on the client (the cluster's title is baked into the vanilla {@code ITEM_NAME}
     * component at capture, so there's no reason to ship the whole entity NBT to every holder's client).
     */
    public static final DataComponentType<CustomData> BOUND_ENTITY = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, "bound_entity"),
            DataComponentType.<CustomData>builder()
                    .persistent(CustomData.CODEC)
                    .build());

    private ModComponents() {
    }

    /** Touching this class from the initializer forces the component above to register. */
    public static void init() {
    }
}
