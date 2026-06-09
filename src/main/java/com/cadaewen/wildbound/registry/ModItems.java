package com.cadaewen.wildbound.registry;

import com.cadaewen.wildbound.Wildbound;
import com.cadaewen.wildbound.item.BoundClusterItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Registers Wildbound's items. The mod is otherwise pure attach-to-vanilla; the only item is the
 * {@code bound_cluster} that carries a captured companion. It is deliberately kept out of every creative
 * tab — an <em>empty</em> bound cluster is meaningless, the item only ever exists already loaded with a mob.
 */
public final class ModItems {

    public static final Item BOUND_CLUSTER = register("bound_cluster",
            properties -> new BoundClusterItem(properties.stacksTo(1)));

    private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, name));
        Item item = factory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private ModItems() {
    }

    /** Touching this class from the initializer forces the item above to register. */
    public static void init() {
    }
}
