package net.nima.cu.charm.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.nima.cu.CharmUploader;
import net.nima.cu.CharmUploaderConfig;
import net.nima.cu.charm.CharmComponent;
import net.nima.cu.charm.dto.ZenithCharmDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements TickablePacketListener, ClientPlayPacketListener {

    private final CharmComponent charmComponent = CharmComponent.getInstance();
    private static final CharmUploaderConfig config = CharmUploader.getConfigHolder().getConfig();
    private ClientWorld world;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPlayNetworkHandlerMixin.class);

    @Inject(at = @At("HEAD"), method = "onItemPickupAnimation")
    public void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        // If server is down just don't do anything
        if (!charmComponent.serverStatus) return;

        if (!config.charm.charm_report_enable) return;

        NetworkThreadUtils.forceMainThread(packet, this, MinecraftClient.getInstance());

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        world = client.world;
        if (player == null) return;
        int cId = packet.getCollectorEntityId();
        int pId = player.getId();
        // Is this item picked by "me" or other players?
        if (cId == pId) {

            Entity entity = world.getEntityById(packet.getEntityId());
            // Required for differentiating xp orbs
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack item = itemEntity.getStack();
                if (item == null || !charmComponent.isZenithCharm(item)) return;
                ZenithCharmDTO charm = charmComponent.parseZenithCharm(item);
                if (charm == null) return;
                List<Integer> coordinates = List.of((int) itemEntity.getX(), (int) itemEntity.getY(), (int) itemEntity.getZ());
                charmComponent.uploadCharms(List.of(charm), world.getRegistryKey().getValue().toString(), coordinates, "PICKUP");
            }
        }
    }
}