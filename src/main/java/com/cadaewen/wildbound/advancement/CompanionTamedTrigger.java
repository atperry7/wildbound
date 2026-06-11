package com.cadaewen.wildbound.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Fires when a player tames a Wildbound companion. A single trigger serves every advancement:
 * the per-animal ones filter on {@code animal_type}, the root matches any.
 */
public class CompanionTamedTrigger extends SimpleCriterionTrigger<CompanionTamedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, EntityType<?> tamedType) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(tamedType);
        this.trigger(player, instance -> instance.matches(typeId));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Identifier> animalType)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Identifier.CODEC.optionalFieldOf("animal_type").forGetter(TriggerInstance::animalType)
        ).apply(i, TriggerInstance::new));

        public boolean matches(Identifier tamedType) {
            return animalType.isEmpty() || animalType.get().equals(tamedType);
        }
    }
}
