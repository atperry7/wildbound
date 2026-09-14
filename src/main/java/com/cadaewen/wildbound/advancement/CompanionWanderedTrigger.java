package com.cadaewen.wildbound.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Fires the first time a player sets a companion to {@link com.cadaewen.wildbound.companion.CompanionMode#WANDER}.
 * Backs the "Home on the Range" advancement, which teaches the wander control in-tree.
 */
public class CompanionWanderedTrigger extends SimpleCriterionTrigger<CompanionWanderedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(i, TriggerInstance::new));
    }
}
