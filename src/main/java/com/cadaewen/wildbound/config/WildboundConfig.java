package com.cadaewen.wildbound.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cadaewen.wildbound.Wildbound;
import com.cadaewen.wildbound.companion.CompanionRegistry;
import com.cadaewen.wildbound.companion.CompanionType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * Per-mob settings read once at init from {@code config/wildbound.json}. Each companion can be enabled or
 * disabled, and have its taming odds and effect amplifier overridden. Internal mechanics (the flicker-safe
 * effect duration / reapply interval, follow range) are deliberately <i>not</i> exposed — they are not
 * gameplay knobs and exposing them invites broken states (e.g. an effect duration below the reapply
 * interval would strobe).
 *
 * <p>The file is keyed by vanilla entity id. Values are applied onto the registered {@link CompanionType}
 * singletons (taming chance / amplifier) and the registry's enable set. If the file is missing it is
 * generated from the built-in defaults; if it is malformed the defaults stand and the error is logged.
 */
public final class WildboundConfig {

    private static final String FILE_NAME = Wildbound.MOD_ID + ".json";
    // disableHtmlEscaping keeps '=' and the like literal in the human-edited file (no = noise).
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String KEY_COMPANIONS = "companions";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_TAMING_CHANCE = "tamingChanceOneInN";
    private static final String KEY_AMPLIFIER = "effectAmplifier";
    private static final String KEY_WANDER_RADIUS = "wanderRadius";

    private WildboundConfig() {
    }

    /** Reads the config (generating it if absent) and applies it to the registered companions. */
    public static void loadAndApply() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.notExists(path)) {
            writeDefault(path);
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject companions = root.has(KEY_COMPANIONS) ? root.getAsJsonObject(KEY_COMPANIONS) : new JsonObject();
            applyAll(companions);
        } catch (Exception e) {
            Wildbound.LOGGER.error("Could not read {} — using built-in companion defaults.", path, e);
        }
    }

    private static void applyAll(JsonObject companions) {
        Set<String> known = new HashSet<>();
        for (Map.Entry<EntityType<?>, CompanionType> e : CompanionRegistry.all().entrySet()) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getKey()).toString();
            known.add(id);
            if (companions.has(id) && companions.get(id).isJsonObject()) {
                applyEntry(e.getKey(), e.getValue(), id, companions.getAsJsonObject(id));
            }
        }
        // Flag stray keys (typo'd or non-companion ids) so a silently-ignored setting is visible.
        for (String key : companions.keySet()) {
            if (!known.contains(key)) {
                Wildbound.LOGGER.warn("Ignoring unknown companion id \"{}\" in {}.", key, FILE_NAME);
            }
        }
    }

    private static void applyEntry(EntityType<?> type, CompanionType companion, String id, JsonObject entry) {
        try {
            if (entry.has(KEY_ENABLED)) {
                CompanionRegistry.setEnabled(type, entry.get(KEY_ENABLED).getAsBoolean());
            }
            if (entry.has(KEY_TAMING_CHANCE)) {
                companion.setTamingChanceOneInN(entry.get(KEY_TAMING_CHANCE).getAsInt());
            }
            if (entry.has(KEY_AMPLIFIER)) {
                companion.setPassiveAmplifier(entry.get(KEY_AMPLIFIER).getAsInt());
            }
            if (entry.has(KEY_WANDER_RADIUS)) {
                companion.setWanderLeashRadius(entry.get(KEY_WANDER_RADIUS).getAsInt());
            }
        } catch (RuntimeException e) {
            Wildbound.LOGGER.warn("Bad config entry for \"{}\" in {} — keeping defaults for it.", id, FILE_NAME, e);
        }
    }

    private static void writeDefault(Path path) {
        JsonObject companions = new JsonObject();
        for (Map.Entry<EntityType<?>, CompanionType> e : CompanionRegistry.all().entrySet()) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getKey());
            CompanionType companion = e.getValue();
            JsonObject entry = new JsonObject();
            entry.addProperty(KEY_ENABLED, true);
            entry.addProperty(KEY_TAMING_CHANCE, companion.tamingChanceOneInN());
            entry.addProperty(KEY_AMPLIFIER, companion.passiveAmplifier());
            entry.addProperty(KEY_WANDER_RADIUS, companion.wanderLeashRadius());
            companions.add(id.toString(), entry);
        }

        JsonObject root = new JsonObject();
        root.addProperty("_comment", "Wildbound per-mob settings. enabled gates new taming (existing pets "
                + "persist); tamingChanceOneInN is 1-in-N odds (1 = always); effectAmplifier is the buff "
                + "level (0 = I) and is ignored for the fox, whose passive is an XP bonus; wanderRadius is "
                + "how far a wandering companion may roam from where you set it (blocks).");
        root.add(KEY_COMPANIONS, companions);

        try {
            Files.writeString(path, GSON.toJson(root));
            Wildbound.LOGGER.info("Wrote default config to {}.", path);
        } catch (IOException e) {
            Wildbound.LOGGER.error("Could not write default config to {}.", path, e);
        }
    }
}
