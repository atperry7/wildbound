package com.cadaewen.wildbound.companion.ocelot;

import com.cadaewen.wildbound.companion.CompanionType;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Ocelot companion: its passive is not a status effect but a <b>x2 XP bonus</b> while following, applied by
 * {@link OcelotXpBonus} via the experience mixin. So {@link #passiveEffect()} is null and the per-tick effect
 * path does nothing for the ocelot.
 *
 * <p>Ocelots have no natural sit pose (unlike cats), so no sit-pose hooks are overridden — a sitting ocelot
 * simply holds still.
 */
public class OcelotCompanion extends CompanionType {

    @Override
    public Item tamingItem() {
        // Advancement icon only — raw cod for the ocelot's flavour. Taming is the universal item.
        return Items.COD;
    }

    @Override
    public Holder<MobEffect> passiveEffect() {
        return null;
    }
}
