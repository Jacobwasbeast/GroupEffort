package net.jacobwasbeast.groupeffort.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jacobwasbeast.groupeffort.network.DisableClientLimboVisualsS2CPacket;
import net.jacobwasbeast.groupeffort.network.EnableClientLimboVisualsS2CPacket;
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
        ClientPlayNetworking.registerGlobalReceiver(
                EnableClientLimboVisualsS2CPacket.PACKET_ID,
                (packet, context) -> {
                    context.client().execute(() -> {
                        ClientLimboState.shouldRenderEndSkyForVoidLimbo = true;
                        LOGGER.info("Received UpdateClientLimboVisualsS2CPacket, shouldRenderEndSky: {}", true);
                    });
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                DisableClientLimboVisualsS2CPacket.PACKET_ID,
                (packet, context) -> {
                    context.client().execute(() -> {
                        ClientLimboState.shouldRenderEndSkyForVoidLimbo = false;
                        LOGGER.info("Received UpdateClientLimboVisualsS2CPacket, shouldRenderEndSky: {}", false);
                    });
                }
        );
        LOGGER.info("Registered S2C payload and handler for UpdateClientLimboVisualsS2CPacket.");
    }
}
