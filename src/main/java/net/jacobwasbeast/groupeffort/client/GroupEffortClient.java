package net.jacobwasbeast.groupeffort.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jacobwasbeast.groupeffort.network.UpdateClientLimboVisualsS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupEffortClient implements ClientModInitializer {
    public static final String MOD_ID = "groupeffort";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing GroupEffort client-side components.");
        registerPacketHandlers();
    }

    public static void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(UpdateClientLimboVisualsS2CPacket.PACKET_ID, (client, handler, buf, responseSender) -> {
            boolean shouldRenderEndSky = buf.readBoolean();
            client.execute(() -> {
                ClientLimboState.shouldRenderEndSkyForVoidLimbo = shouldRenderEndSky;
                LOGGER.info("Received UpdateClientLimboVisualsS2CPacket, shouldRenderEndSky: {}", shouldRenderEndSky);
            });
        });
        LOGGER.info("Registered S2C packet handler for UpdateClientLimboVisualsS2CPacket.");
    }
}
