package com.cadaewen.wildbound.companion.ocelot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** The Ocelot passive: doubles XP a player receives while an active (following or sitting) tamed ocelot is nearby. */
public final class OcelotXpBonus {

    // Per-tick cache so several XP awards in the same server tick (a burst of orbs, grinding) share one
    // nearby-entity scan per player. Keyed per player so two players grinding in the same tick don't evict
    // each other's entry; cleared whenever the game-time advances, so no Mob reference outlives its tick.
    // XP is granted only on the server thread, so the plain static state is safe.
    private static long cachedGameTime = Long.MIN_VALUE;
    private static final Map<UUID, Mob> cachedOcelots = new HashMap<>();

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
        // Sparkle at the ocelot so the otherwise-invisible bonus is visible. Particle-only and throttled:
        // XP is awarded too often for a sound (it would be grind-spam), and 1-in-4 keeps a burst of orbs
        // reading as a flurry without flooding. The count/spread are tuned up from a near-invisible 3 so the
        // cue actually lands; the bonus has no other indicator.
        if (player.getRandom().nextInt(4) == 0 && ocelot.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ocelot.getX(), ocelot.getY() + 0.7, ocelot.getZ(),
                    8, 0.4, 0.5, 0.4, 0.0);
        }
        return amount * 2;
    }

    /** The player's active ocelot, resolved at most once per player per server tick (see the cache above). */
    private static Mob activeOcelot(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now != cachedGameTime) {
            cachedGameTime = now;
            cachedOcelots.clear();
        }
        UUID id = player.getUUID();
        // containsKey rather than computeIfAbsent: "no ocelot nearby" (null) is the common case and must
        // cache too, or every award without an ocelot would rescan.
        if (!cachedOcelots.containsKey(id)) {
            cachedOcelots.put(id, CompanionBehavior.findActiveCompanion(player, EntityType.OCELOT));
        }
        return cachedOcelots.get(id);
    }
}
