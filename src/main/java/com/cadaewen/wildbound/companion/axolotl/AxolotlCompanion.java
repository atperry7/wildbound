package com.cadaewen.wildbound.companion.axolotl;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Axolotl companion: grants Night Vision I while following — a cave-dweller's gift that lights up the dark
 * underwater depths the bat's land-bound Night Vision can't reach (the bat won't dive). Pairs naturally with
 * the turtle's Water Breathing for an underwater-explorer duo.
 */
public class AxolotlCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        // Advancement icon only — a tropical fish for the axolotl's flavour. Taming is the universal item.
        return Items.TROPICAL_FISH;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return MobEffects.NIGHT_VISION;
    }

    /**
     * Swim at the axolotl's own pace — no follow boost. Its water move control sets speed to
     * {@code followSpeed × MOVEMENT_SPEED(1.0) × 0.1}, so the shared 1.2 boost gives 0.12 b/t, faster than a
     * swimming player; capped to a ~10°/tick turn it then overshoots into wide arcs. 1.0 → 0.10 b/t keeps it
     * at the player's pace (the 16-block follow-teleport still backstops a sprinting owner). Tunable: nudge
     * down if it still overshoots, up if it lags.
     */
    @Override
    public double followSpeed() {
        return 1.0;
    }
}
