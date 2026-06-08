package com.cadaewen.wildbound.companion.fox;

import com.cadaewen.wildbound.companion.CompanionBehavior;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/** The Fox passive: doubles XP a player receives while a following (non-sitting) tamed fox is nearby. */
public final class FoxXpBonus {

    private FoxXpBonus() {
    }

    public static int boosted(Player player, int amount) {
        if (amount > 0 && CompanionBehavior.hasActiveCompanion(player, EntityType.FOX)) {
            return amount * 2;
        }
        return amount;
    }
}
