package com.cadaewen.wildbound.companion;

import java.util.UUID;

import com.cadaewen.wildbound.Wildbound;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
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

    /** Sit/follow toggle. Absent or {@code false} means following. */
    public static final AttachmentType<Boolean> SITTING = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(Wildbound.MOD_ID, "sitting"), Codec.BOOL);

    private WildboundAttachments() {
    }

    /** Touching this class from the initializer forces the static fields above to register. */
    public static void init() {
    }
}
