package com.cadaewen.wildbound;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.entity.PathfinderMob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cadaewen.wildbound.companion.CompanionRegistry;
import com.cadaewen.wildbound.companion.CompanionTaming;
import com.cadaewen.wildbound.companion.WildboundAttachments;
import com.cadaewen.wildbound.companion.goal.CompanionGoals;
import com.cadaewen.wildbound.registry.ModCriteria;

public class Wildbound implements ModInitializer {
	public static final String MOD_ID = "wildbound";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		WildboundAttachments.init();
		ModCriteria.init();
		CompanionRegistry.init();
		UseEntityCallback.EVENT.register(CompanionTaming::onUseEntity);

		// Goal-driven companions (ground/flying/swimming) get their follow/sit/tick goals on load —
		// no per-animal mixin. The bat is excluded (not a PathfinderMob; it steers via its own mixin).
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof PathfinderMob mob && CompanionRegistry.get(mob) != null) {
				CompanionGoals.attachTo(mob);
			}
		});

		LOGGER.info("Wildbound initialised with {} companion type(s).", CompanionRegistry.count());
	}
}
