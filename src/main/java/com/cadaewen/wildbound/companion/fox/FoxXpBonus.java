package com.cadaewen.wildbound.companion.fox;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** The Fox passive: doubles XP a player receives while a following (non-sitting) tamed fox is nearby. */
public final class FoxXpBonus {

    private FoxXpBonus() {
    }

    public static int boosted(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return amount;
        }
        Mob fox = CompanionBehavior.findActiveCompanion(player, EntityType.FOX);
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
}
