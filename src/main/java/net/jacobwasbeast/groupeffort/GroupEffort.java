package net.jacobwasbeast.groupeffort;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.jacobwasbeast.groupeffort.command.GroupEffortCommands;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;
import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.jacobwasbeast.groupeffort.network.DisableClientLimboVisualsS2CPacket;
import net.jacobwasbeast.groupeffort.network.EnableClientLimboVisualsS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupEffort implements ModInitializer {

    public static final String MOD_ID = "groupeffort";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static GroupEffortConfig CONFIG_INSTANCE;

    @Override
    public void onInitialize() {
        AutoConfig.register(GroupEffortConfig.class, GsonConfigSerializer::new);
        CONFIG_INSTANCE = AutoConfig.getConfigHolder(GroupEffortConfig.class).getConfig();
        LOGGER.info("GroupEffort initialized. Minimum players: {}", CONFIG_INSTANCE.generalSettings.minimumPlayers);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GroupEffortManager.init(server, getConfig());
            GroupEffortManager.updateGlobalLimboState();
        });

        PayloadTypeRegistry.playS2C().register(
                DisableClientLimboVisualsS2CPacket.PACKET_ID,
                DisableClientLimboVisualsS2CPacket.PACKET_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                EnableClientLimboVisualsS2CPacket.PACKET_ID,
                EnableClientLimboVisualsS2CPacket.PACKET_CODEC
        );

        CommandRegistrationCallback.EVENT.register(GroupEffortCommands::register);
    }

    public static GroupEffortConfig getConfig() {
        if (CONFIG_INSTANCE == null) {
            CONFIG_INSTANCE = AutoConfig.getConfigHolder(GroupEffortConfig.class).getConfig();
        }
        return CONFIG_INSTANCE;
    }
}