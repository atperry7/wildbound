package com.cadaewen.wildbound.companion;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * A companion's behaviour mode. Stored as a persistent attachment ({@code WildboundAttachments.MODE}) and
 * the single source of truth for what a tamed mob is doing:
 *
 * <ul>
 *   <li>{@link #FOLLOW} — trails the owner and grants its passive while in range (the default on taming).</li>
 *   <li>{@link #SIT} — stays put in a natural pose; no passive.</li>
 *   <li>{@link #WANDER} — roams freely on vanilla AI; no follow, no passive. Still owned (persists, won't
 *       flee its owner, won't be hunted by other companions).</li>
 * </ul>
 *
 * Only {@code FOLLOW} is "active" for passive effects — both {@code SIT} and {@code WANDER} are inactive.
 */
public enum CompanionMode implements StringRepresentable {
    FOLLOW("follow"),
    SIT("sit"),
    WANDER("wander");

    public static final Codec<CompanionMode> CODEC = StringRepresentable.fromEnum(CompanionMode::values);

    private final String serializedName;

    CompanionMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
