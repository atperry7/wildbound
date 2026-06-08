package com.cadaewen.wildbound.companion.fox;

import java.util.UUID;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** The Fox passive: doubles XP a player receives while a following (non-sitting) tamed fox is nearby. */
public final class FoxXpBonus {

    // Single-entry cache so several XP awards in the same server tick (a burst of orbs, grinding) share one
    // nearby-entity scan instead of rescanning per award. XP is granted only on the server thread, so a
    // plain static cache is safe; it is re-resolved whenever the player or game-time changes.
    private static UUID cachedPlayerId;
    private static long cachedGameTime = Long.MIN_VALUE;
    private static Mob cachedFox;

    private FoxXpBonus() {
    }

    public static int boosted(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return amount;
        }
        Mob fox = activeFox(player);
        if (fox == null) {
            return amount;
        }
        // Sparkle at the fox so the otherwise-invisible bonus is visible. Throttled so XP grinding
        // doesn't spam particles.
        if (player.getRandom().nextInt(4) == 0 && fox.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fox.getX(), fox.getY() + 0.6, fox.getZ(),
                    3, 0.3, 0.4, 0.3, 0.0);
        }
        return amount * 2;
    }

    /** The owner's active fox, resolved at most once per server tick (see the cache fields above). */
    private static Mob activeFox(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now != cachedGameTime || !player.getUUID().equals(cachedPlayerId)) {
            cachedGameTime = now;
            cachedPlayerId = player.getUUID();
            cachedFox = CompanionBehavior.findActiveCompanion(player, EntityType.FOX);
        }
        return cachedFox;
    }
}
