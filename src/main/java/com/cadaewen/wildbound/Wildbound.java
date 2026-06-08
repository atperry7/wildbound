package com.cadaewen.wildbound;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cadaewen.wildbound.companion.CompanionRegistry;
import com.cadaewen.wildbound.companion.CompanionTaming;
import com.cadaewen.wildbound.companion.WildboundAttachments;
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

		LOGGER.info("Wildbound initialised with {} companion type(s).", CompanionRegistry.count());
	}
}
