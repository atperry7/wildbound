package com.cadaewen.wildbound.companion;

import java.util.UUID;

import com.cadaewen.wildbound.Wildbound;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Persistent state attached to vanilla mobs to make them Wildbound companions.
 *
 * <p>We attach to the existing entity rather than registering a parallel {@code Tamed*Entity} type,
 * so a tamed mob keeps its identity, model, and animations with no renderer wiring.
 */
public final class WildboundAttachments {

    /** Owner player UUID. Presence of this attachment is what marks a mob as a tamed companion. */
    public static final AttachmentType<UUID> OWNER = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, "owner"), UUIDUtil.CODEC);

    /** Behaviour mode (follow / sit / wander). Absent means {@link CompanionMode#FOLLOW}. */
    public static final AttachmentType<CompanionMode> MODE = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, "mode"), CompanionMode.CODEC);

    /**
     * The spot a {@link CompanionMode#WANDER}ing companion is leashed to (set when wander is enabled).
     * Persistent so the leash survives a reload; the live vanilla restriction is re-applied from it each tick.
     */
    public static final AttachmentType<BlockPos> WANDER_ANCHOR = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, "wander_anchor"), BlockPos.CODEC);

    private WildboundAttachments() {
    }

    /** Touching this class from the initializer forces the static fields above to register. */
    public static void init() {
    }
}
