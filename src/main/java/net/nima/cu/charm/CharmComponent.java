package net.nima.cu.charm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.nima.cu.CharmUploader;
import net.nima.cu.charm.dto.AllowedPlotDTO;
import net.nima.cu.charm.dto.AllowedPlotResponse;
import net.nima.cu.charm.dto.ZenithCharmDTO;
import net.nima.cu.charm.dto.ZenithCharmEffectDTO;
import net.nima.cu.charm.utils.CharmRarityMapper;
import net.nima.cu.charm.utils.CharmTypeParser;
import net.nima.cu.CharmUploaderConfig;
import net.nima.cu.util.Chatter;
import net.nima.cu.util.ChatterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CharmComponent {

    private static CharmComponent instance;
    private static final Chatter CHATTER = ChatterFactory.getChatter(CharmComponent.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(CharmComponent.class);

    private final CharmUploaderConfig config;
    private final CharmUpload charmUpload;

    private static Screen prevScreen;

    Set<String> plotSet;
    public boolean serverStatus = true;

    private CharmComponent() {
        charmUpload = new CharmUpload();
        config = CharmUploader.getConfigHolder().getConfig();

        // Get list of plots from players who've given consent
        plotSet = new HashSet<>();
        final Type consentListType = new TypeToken<AllowedPlotResponse>(){}.getType();
        String plotConsentList = "";
        try {
            plotConsentList = charmUpload.getConsentList().get().body();
        } catch (Exception ignored) {}

        // Get consent list
        if (plotConsentList != null && !plotConsentList.isEmpty()) {
            try {
                Gson gson = new Gson();
                AllowedPlotResponse response = gson.fromJson(plotConsentList, consentListType);
                if (response != null && response.data() != null) {
                    for (AllowedPlotDTO item : response.data()) {
                        plotSet.add(item.plot());
                    }
                } else {
                    LOGGER.warn("Failed to parse consent list - response was null");
                    serverStatus = false;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse consent list JSON", e);
                serverStatus = false;
            }
        } else {
            LOGGER.warn("Consent list was empty or null");
            serverStatus = false;
        }

        // If the server status is down then don't do anything
        LOGGER.info("Status of charm server is: {}", serverStatus);

        // On client tick
        ClientTickEvents.END_CLIENT_TICK.register(((minecraftClient) -> {
            if (!serverStatus) return;
            if (!config.charm.charm_report_enable) return;

            Screen screen = minecraftClient.currentScreen;
            if (screen == null || screen == prevScreen) return;

            // Intialize new array list to store charm data
            List<ZenithCharmDTO> payload = new ArrayList<>();
            // isVzc (Is the current GUI vzc)
            boolean isVzc = false;
            if (screen instanceof GenericContainerScreen containerScreen) {
                GenericContainerScreenHandler handler = containerScreen.getScreenHandler();

                for (int i = 0; i < handler.getInventory().size(); i++) {
                    ItemStack item = handler.getInventory().getStack(i);
                    // Read shulkerbox data to see if charms are inside shulker boxes
                    if (isShulkerBox(item)) {
                        // Get list of sub item nbt data
                        NbtList subItems = (NbtList) item.getNbt()
                                .getCompound("BlockEntityTag")
                                .get("Items");
                        // If subItems = null, then it's an empty shulker
                        if (subItems != null) {
                            // For each item inside the shulker
                            for (NbtElement nbtElement : subItems) {
                                NbtCompound subItem = (NbtCompound) nbtElement;
                                if (isZenithCharm(subItem)) {
                                    ZenithCharmDTO charm = parseZenithCharm(subItem);
                                    payload.add(charm);
                                }
                            }
                        }
                    }
                    // Read normal charms
                    if (!isZenithCharm(item)) {
                        isVzc = isVzc || isVzcOrVc(item);
                        continue;
                    }
                    ZenithCharmDTO charm = parseZenithCharm(item);
                    if (charm != null) {
                        payload.add(charm);
                    }
                }
            }

            prevScreen = screen;
            if (payload.isEmpty()) return;

            ClientPlayerEntity player = minecraftClient.player;
            if (player == null) {
                return;
            }

            HitResult hr = minecraftClient.crosshairTarget;
            BlockPos pos = (hr != null && hr.getType() == HitResult.Type.BLOCK) ?
                    ((BlockHitResult) hr).getBlockPos() :
                    player.getBlockPos();
            List<Integer> coords = List.of(pos.getX(), pos.getY(), pos.getZ());

            String dimension = player.getEntityWorld().getRegistryKey().getValue().toString();
            uploadCharms(payload, dimension, coords, isVzc ? "VZC" : "CONTAINER");
        }));
    }

    public void uploadCharms(List<ZenithCharmDTO> charms, String dimension, List<Integer> coords, String source) {
        dimension = dimension.contains("monumenta:") || dimension.contains("plot") ? dimension.replace("monumenta:", "").replace("minecraft:", "") : null;

        if (dimension == null) return;

        // If it's a VZC, always upload
        if (source.equals("VZC")) {
            charmUpload.uploadCharms(charms, dimension, coords, source);
            return;
        }

        // Not a VZC - check if it's a plot dimension
        if (dimension.matches("guildplot\\d+") || dimension.matches("plot\\d+")) {
            // It's a plot - only upload if it's in the consent list
            if (plotSet.contains(dimension)) {
                charmUpload.uploadCharms(charms, dimension, coords, source);
            }
        } else {
            charmUpload.uploadCharms(charms, dimension, coords, source);
        }
    }

    public static CharmComponent getInstance() {
        if (instance == null) {
            instance = new CharmComponent();
        }
        return instance;
    }

    public boolean isShulkerBox(ItemStack item) {
        if (item.isEmpty() || !item.hasNbt() || item.getNbt() == null) {
            return false;
        }
        String id = item.getNbt()
                .getCompound("BlockEntityTag")
                .getString("id");
        return "minecraft:shulker_box".equals(id);
    }

    public boolean isZenithCharm(ItemStack item) {
        if (item.isEmpty() || !item.hasNbt() || item.getNbt() == null) { // that last null check is for the stupid ide
            return false;
        }
        NbtElement el = item.getNbt()
                .getCompound("Monumenta")
                .get("Tier");
        return el != null && el.asString().equals("zenithcharm");
    }

    public boolean isZenithCharm(NbtCompound item) {
        if (item == null || item.isEmpty()) return false;
        NbtCompound tag = item.contains("tag") ? item.getCompound("tag") : item;
        NbtElement el = tag.getCompound("Monumenta")
                .get("Tier");
        return el != null && el.asString().equals("zenithcharm");
    }

    public boolean isVzcOrVc(ItemStack item) {
        try {
            if (item.isEmpty() || !item.hasNbt() || item.getNbt() == null) {
                return false;
            }
            return item.getNbt()
                    .getCompound("plain")
                    .getCompound("display")
                    .getString("Name")
                    .strip().equals("Charm Effect Summary");
        } catch (NullPointerException ignored)  {
            return false;
        }
    }

    public ZenithCharmDTO parseZenithCharm(ItemStack item) {
        try {
            NbtCompound playerModified = item.getNbt().getCompound("Monumenta").getCompound("PlayerModified");
            List<ZenithCharmEffectDTO> effects = new ArrayList<>();

            int index = 1;
            while (!playerModified.getString("DEPTHS_CHARM_EFFECT" + index).isEmpty()) {
                if (index == 1) {
                    effects.add(new ZenithCharmEffectDTO(
                            playerModified.getString("DEPTHS_CHARM_EFFECT" + index),
                            playerModified.getInt("DEPTHS_CHARM_RARITY"),
                            (float) playerModified.getDouble("DEPTHS_CHARM_ROLLS" + index),
                            index - 1
                    ));
                } else {
                    effects.add(new ZenithCharmEffectDTO(
                            playerModified.getString("DEPTHS_CHARM_EFFECT" + index),
                            CharmRarityMapper.getRarity(playerModified.getString("DEPTHS_CHARM_ACTIONS" + (index - 1))),
                            (float) playerModified.getDouble("DEPTHS_CHARM_ROLLS" + index),
                            index - 1
                    ));
                }
                index++;
            }

            return new ZenithCharmDTO(
                    item.getNbt().getCompound("plain").getCompound("display").getString("Name"),
                    playerModified.getLong("DEPTHS_CHARM_UUID"),
                    playerModified.getInt("DEPTHS_CHARM_RARITY"),
                    playerModified.getBoolean("CELESTIAL_GEM_USED"),
                    playerModified.getInt("DEPTHS_CHARM_BUDGET"),
                    CharmTypeParser.getCharmType(playerModified.getString("DEPTHS_CHARM_TYPE_NAME")),
                    item.getNbt().getCompound("Monumenta").getInt("CharmPower"),
                    item.getItem().toString(),
                    effects
            );

        } catch (NullPointerException e) {
            CHATTER.error("Something went wrong! Report to Niam");
            return null;
        }
    }

    // Override for shulkers
    public ZenithCharmDTO parseZenithCharm(NbtCompound item) {
        try {
            // Item either contains tag or is already inside the item itself
            NbtCompound tag = item.contains("tag") ? item.getCompound("tag") : item;

            NbtCompound monumenta = tag.getCompound("Monumenta");
            NbtCompound playerModified = monumenta.getCompound("PlayerModified");
            List<ZenithCharmEffectDTO> effects = new ArrayList<>();

            int index = 1;
            while (!playerModified.getString("DEPTHS_CHARM_EFFECT" + index).isEmpty()) {
                if (index == 1) {
                    effects.add(new ZenithCharmEffectDTO(
                            playerModified.getString("DEPTHS_CHARM_EFFECT" + index),
                            playerModified.getInt("DEPTHS_CHARM_RARITY"),
                            (float) playerModified.getDouble("DEPTHS_CHARM_ROLLS" + index),
                            index - 1
                    ));
                } else {
                    effects.add(new ZenithCharmEffectDTO(
                            playerModified.getString("DEPTHS_CHARM_EFFECT" + index),
                            CharmRarityMapper.getRarity(playerModified.getString("DEPTHS_CHARM_ACTIONS" + (index - 1))),
                            (float) playerModified.getDouble("DEPTHS_CHARM_ROLLS" + index),
                            index - 1
                    ));
                }
                index++;
            }

            return new ZenithCharmDTO(
                    tag.getCompound("plain").getCompound("display").getString("Name"),
                    playerModified.getLong("DEPTHS_CHARM_UUID"),
                    playerModified.getInt("DEPTHS_CHARM_RARITY"),
                    playerModified.getBoolean("CELESTIAL_GEM_USED"),
                    playerModified.getInt("DEPTHS_CHARM_BUDGET"),
                    CharmTypeParser.getCharmType(playerModified.getString("DEPTHS_CHARM_TYPE_NAME")),
                    monumenta.getInt("CharmPower"),
                    item.getString("id"),
                    effects
            );

        } catch (NullPointerException e) {
            CHATTER.error("Something went wrong! Report to the niam");
            return null;
        }
    }
}