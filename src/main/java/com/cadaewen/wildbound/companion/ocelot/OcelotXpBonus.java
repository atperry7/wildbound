package com.cadaewen.wildbound.companion.ocelot;

import java.util.UUID;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** The Ocelot passive: doubles XP a player receives while a following (non-sitting) tamed ocelot is nearby. */
public final class OcelotXpBonus {

    // Single-entry cache so several XP awards in the same server tick (a burst of orbs, grinding) share one
    // nearby-entity scan instead of rescanning per award. XP is granted only on the server thread, so a
    // plain static cache is safe; it is re-resolved whenever the player or game-time changes.
    private static UUID cachedPlayerId;
    private static long cachedGameTime = Long.MIN_VALUE;
    private static Mob cachedOcelot;

    private OcelotXpBonus() {
    }

    public static int boosted(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return amount;
        }
        Mob ocelot = activeOcelot(player);
        if (ocelot == null) {
            return amount;
        }
        // Sparkle at the ocelot so the otherwise-invisible bonus is visible. Throttled so XP grinding
        // doesn't spam particles.
        if (player.getRandom().nextInt(4) == 0 && ocelot.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ocelot.getX(), ocelot.getY() + 0.6, ocelot.getZ(),
                    3, 0.3, 0.4, 0.3, 0.0);
        }
        return amount * 2;
    }

    /** The owner's active ocelot, resolved at most once per server tick (see the cache fields above). */
    private static Mob activeOcelot(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now != cachedGameTime || !player.getUUID().equals(cachedPlayerId)) {
            cachedGameTime = now;
            cachedPlayerId = player.getUUID();
            cachedOcelot = CompanionBehavior.findActiveCompanion(player, EntityType.OCELOT);
        }
        return cachedOcelot;
    }
}
