package net.nima.cu;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.nima.cu.charm.CharmComponent;

public class CharmUploader implements ModInitializer {

	public static final String ID = "charm-uploader";
	private static ConfigHolder<CharmUploaderConfig> configHolder;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		configHolder = AutoConfig.register(CharmUploaderConfig.class, GsonConfigSerializer::new);
		CharmComponent.getInstance();
	}

	public static ConfigHolder<CharmUploaderConfig> getConfigHolder() {
		return configHolder;
	}
}