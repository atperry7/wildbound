package com.cadaewen.wildbound.companion;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * A companion's behaviour mode. Stored as a persistent attachment ({@code WildboundAttachments.MODE}) and
 * the single source of truth for what a tamed mob is doing:
 *
 * <ul>
 *   <li>{@link #FOLLOW} — trails the owner and grants its passive while in range (the default on taming).</li>
 *   <li>{@link #SIT} — stays put in a natural pose, still granting its passive while the owner is in
 *       range (a parked buff that stays out of the way).</li>
 *   <li>{@link #WANDER} — roams freely on vanilla AI; no follow, no passive — the "off duty" mode. Still
 *       owned (persists, won't flee its owner, won't be hunted by other companions).</li>
 * </ul>
 *
 * {@code FOLLOW} and {@code SIT} are both "active" for passives ({@link #grantsPassive}); only
 * {@code WANDER} is inert. The deliberate off-switch for the buff itself is the milk-bucket quiet.
 */
public enum CompanionMode implements StringRepresentable {
    FOLLOW("follow"),
    SIT("sit"),
    WANDER("wander");

    /** Whether a companion in this mode grants its passive (range and milk-quiet checks still apply). */
    public boolean grantsPassive() {
        return this != WANDER;
    }

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
