package net.nima.cu.util;

import net.minecraft.client.MinecraftClient;
import net.nima.cu.CharmUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CharmUploader.ID);
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Chatter getChatter(Class<?> cls) {
        return new Chatter(cls, mc);
    }
}