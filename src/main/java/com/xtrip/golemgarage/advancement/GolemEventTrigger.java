package com.xtrip.golemgarage.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * One trigger for every Golem Garage milestone. Advancements pick the milestone with "event", for example
 * {"trigger": "golemgarage:golem_event", "conditions": {"event": "garage_repair"}}.
 */
public class GolemEventTrigger extends SimpleCriterionTrigger<GolemEventTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        this.trigger(player, inst -> inst.event().map(event::equals).orElse(true));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<String> event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.optionalFieldOf("event").forGetter(TriggerInstance::event)
        ).apply(i, TriggerInstance::new));
    }
}
