package net.jacobwasbeast.groupeffort.manager;

import net.jacobwasbeast.groupeffort.GroupEffort;
import net.jacobwasbeast.groupeffort.api.LimboAPI;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupEffortManager {
    private static GroupEffortConfig CONFIG;
    private static MinecraftServer server;
    private static boolean limboActiveStateForServerTicks = false;
    private static long gracePeriodEndTime = 0L;
    private static long lastPeriodicMessageTime = 0L;

    public static final Map<UUID, OriginalPlayerData> originalPlayerDataMap = new ConcurrentHashMap<>();
    private static final Map<UUID, GroupEffortConfig.LimboType> playerLimboTypes = new ConcurrentHashMap<>();

    public static class OriginalPlayerData {
        public final RegistryKey<World> dimensionKey;
        public final Vec3d position;
        public final float yaw;
        public final float pitch;
        public final PlayerAbilities abilities;
        public final net.minecraft.world.GameMode gameMode;
        @Nullable
        public final net.minecraft.world.GameMode previousGameMode;

        OriginalPlayerData(ServerPlayerEntity player) {
            this.dimensionKey = player.getServerWorld().getRegistryKey();
            this.position = player.getPos();
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.abilities = new PlayerAbilities();
            NbtCompound nbt = new NbtCompound();
            player.getAbilities().writeNbt(nbt);
            this.abilities.readNbt(nbt);
            this.gameMode = player.interactionManager.getGameMode();
            this.previousGameMode = player.interactionManager.getPreviousGameMode();
        }
    }

    public static void init(MinecraftServer serverInstance, GroupEffortConfig configInstance) {
        server = serverInstance;
        CONFIG = configInstance;
        GroupEffort.LOGGER.info("GroupEffort Manager initialized.");
    }

    public static boolean isServerTickAllowed() { return !limboActiveStateForServerTicks; }
    public static boolean isPlayerInAnyLimbo(UUID playerUuid) { return playerLimboTypes.containsKey(playerUuid); }
    public static boolean isPlayerInSkyOnlyLimbo(UUID playerUuid) {
        GroupEffortConfig.LimboType type = playerLimboTypes.get(playerUuid);
        return type == GroupEffortConfig.LimboType.THE_VOID;
    }
    @Nullable public static GroupEffortConfig.LimboType getPlayerLimboType(UUID playerUuid) { return playerLimboTypes.get(playerUuid); }

    public static void onServerTick() {
        if (server == null || CONFIG == null) return;
        updateGlobalLimboState();
    }

    public static void updateGlobalLimboState() {
        if (server == null || CONFIG == null) return;
        int currentPlayers = server.getPlayerManager().getCurrentPlayerCount();
        int minimumPlayers = CONFIG.generalSettings.minimumPlayers;
        boolean currentlyBelowMinimum = currentPlayers < minimumPlayers;

        if (currentlyBelowMinimum) {
            if (!limboActiveStateForServerTicks) {
                if (gracePeriodEndTime > 0L) {
                    if (System.currentTimeMillis() > gracePeriodEndTime) {
                        GroupEffort.LOGGER.info("Grace period ended. Player count ({}/{}) still low. Activating server restrictions.", currentPlayers, minimumPlayers);
                        limboActiveStateForServerTicks = true;
                        gracePeriodEndTime = 0L;
                        broadcastMessage(formatMessage(CONFIG.gracePeriod.endMessageRestrictionsActive, null, 0, currentPlayers, minimumPlayers, -1));
                        server.getPlayerManager().getPlayerList().forEach(GroupEffortManager::ensurePlayerInCorrectLimboState);
                    }
                } else if (CONFIG.gracePeriod.enabled) {
                    gracePeriodEndTime = System.currentTimeMillis() + (long) CONFIG.gracePeriod.durationSeconds * 1000L;
                    GroupEffort.LOGGER.info("Player count ({}/{}) low, starting grace period: {}s", currentPlayers, minimumPlayers, CONFIG.gracePeriod.durationSeconds);
                    broadcastMessage(formatMessage(CONFIG.gracePeriod.startMessage, null, 0, currentPlayers, minimumPlayers, CONFIG.gracePeriod.durationSeconds));
                    lastPeriodicMessageTime = System.currentTimeMillis();
                } else {
                    GroupEffort.LOGGER.info("Player count ({}/{}) low (no grace period). Activating server restrictions.", currentPlayers, minimumPlayers);
                    limboActiveStateForServerTicks = true;
                    broadcastMessage(formatMessage(CONFIG.gracePeriod.endMessageRestrictionsActive, null, 0, currentPlayers, minimumPlayers, -1));
                    server.getPlayerManager().getPlayerList().forEach(GroupEffortManager::ensurePlayerInCorrectLimboState);
                }
            }

            if (limboActiveStateForServerTicks || gracePeriodEndTime > 0L) {
                if (System.currentTimeMillis() - lastPeriodicMessageTime > 60000L) {
                    int needed = Math.max(0, minimumPlayers - currentPlayers);
                    broadcastMessage(formatMessage(CONFIG.chatMessages.waitingForPlayersPeriodic, null, needed, currentPlayers, minimumPlayers, -1));
                    lastPeriodicMessageTime = System.currentTimeMillis();
                }
            }
        } else {
            boolean wasInLimboOrGrace = limboActiveStateForServerTicks || gracePeriodEndTime > 0L;
            if (limboActiveStateForServerTicks) {
                GroupEffort.LOGGER.info("Minimum player count ({}/{}) met. Deactivating server restrictions.", currentPlayers, minimumPlayers);
                broadcastMessage(formatMessage(CONFIG.chatMessages.minimumPlayersMet, null, 0, currentPlayers, minimumPlayers, -1));
            } else if (gracePeriodEndTime > 0L) {
                GroupEffort.LOGGER.info("Minimum player count ({}/{}) met during grace period. Restrictions averted.", currentPlayers, minimumPlayers);
                broadcastMessage(formatMessage(CONFIG.chatMessages.minimumPlayersMet, null, 0, currentPlayers, minimumPlayers, -1));
            }

            limboActiveStateForServerTicks = false;
            gracePeriodEndTime = 0L;
            if (wasInLimboOrGrace) {
                server.getPlayerManager().getPlayerList().forEach(p -> removePlayerFromLimboInternal(p, true, true));
            }
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (server == null || CONFIG == null) init(player.getServer(), GroupEffort.getConfig());
        updateGlobalLimboState();
        int currentPlayers = server.getPlayerManager().getCurrentPlayerCount();
        int minimumPlayers = CONFIG.generalSettings.minimumPlayers;
        if (currentPlayers < minimumPlayers) {
            int needed = Math.max(0, minimumPlayers - currentPlayers);
            sendMessageToPlayer(player, formatMessage(CONFIG.chatMessages.playerJoinBelowMinimum, player.getGameProfile().getName(), needed, currentPlayers, minimumPlayers, -1));
        }
        if (limboActiveStateForServerTicks) {
            ensurePlayerInCorrectLimboState(player);
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        removePlayerFromLimboInternal(player, false, true);
        if (server == null || CONFIG == null) return;
        server.execute(GroupEffortManager::updateGlobalLimboState);
    }

    private static void ensurePlayerInCorrectLimboState(ServerPlayerEntity player) {
        final UUID playerUuid = player.getUuid();
        final GroupEffortConfig.LimboType targetLimboType = CONFIG.generalSettings.limboType;

        if (playerLimboTypes.get(playerUuid) == targetLimboType && originalPlayerDataMap.containsKey(playerUuid)) {
            if (targetLimboType == GroupEffortConfig.LimboType.LOCALIZED_FREEZE) {
                OriginalPlayerData oData = originalPlayerDataMap.get(playerUuid);
                if (oData != null) {
                    player.networkHandler.requestTeleport(oData.position.getX(), oData.position.getY(), oData.position.getZ(), oData.yaw, oData.pitch);
                }
            }
            return;
        }
        GroupEffort.LOGGER.info("Putting player {} into limbo type: {}", player.getName().getString(), targetLimboType);

        if (!originalPlayerDataMap.containsKey(playerUuid)) {
            originalPlayerDataMap.put(playerUuid, new OriginalPlayerData(player));
        }
        OriginalPlayerData originalData = originalPlayerDataMap.get(playerUuid);

        PlayerAbilities packetAbilities = new PlayerAbilities();
        NbtCompound originalAbilitiesNbtForPacket = new NbtCompound();
        originalData.abilities.writeNbt(originalAbilitiesNbtForPacket);
        packetAbilities.readNbt(originalAbilitiesNbtForPacket);

        packetAbilities.invulnerable = true;
        packetAbilities.setWalkSpeed(0.0f);

       if (targetLimboType == GroupEffortConfig.LimboType.THE_VOID) {
            packetAbilities.flying = true;
            packetAbilities.allowFlying = true;
            player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(packetAbilities));

            LimboAPI.enterLimbo(player, GroupEffortConfig.LimboType.THE_VOID);

            double limboY = player.getServerWorld().getTopY() + 100.0;
            if (limboY > 1000 || limboY < player.getServerWorld().getBottomY() + 50) limboY = 350;
            player.networkHandler.requestTeleport(originalData.position.getX(), limboY, originalData.position.getZ(), originalData.yaw, originalData.pitch);

            playerLimboTypes.put(playerUuid, targetLimboType);
            GroupEffort.LOGGER.info("Visual limbo (LimboAPI with current sky) and high teleport initiated for {} for type {}", player.getName().getString(), targetLimboType);

        } else if (targetLimboType == GroupEffortConfig.LimboType.LOCALIZED_FREEZE) {
            packetAbilities.flying = false;
            packetAbilities.allowFlying = originalData.abilities.allowFlying;
            player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(packetAbilities));

            playerLimboTypes.put(playerUuid, targetLimboType);
            player.networkHandler.requestTeleport(originalData.position.getX(), originalData.position.getY(), originalData.position.getZ(), originalData.yaw, originalData.pitch);
        }
        sendMessageToPlayer(player, formatMessage(CONFIG.chatMessages.playerEntersLimbo, player.getName().getString(),0,0,0, -1));
    }

    public static void removePlayerFromLimbo(ServerPlayerEntity player) {
        removePlayerFromLimboInternal(player, true, true);
    }

    private static void removePlayerFromLimboInternal(ServerPlayerEntity player, boolean sendLeaveMessage, boolean restoreFully) {
        final UUID playerUuid = player.getUuid();
        GroupEffort.LOGGER.info("Attempting to remove player {} from limbo tracking.", player.getName().getString());

        final GroupEffortConfig.LimboType limboTypeExited = playerLimboTypes.remove(playerUuid);
        final OriginalPlayerData originalData = originalPlayerDataMap.remove(playerUuid);

        if (!restoreFully) {
            if (originalData != null) {
                NbtCompound tempNbt = new NbtCompound();
                originalData.abilities.writeNbt(tempNbt);
                player.getAbilities().readNbt(tempNbt);
                player.sendAbilitiesUpdate();
            }
            if (limboTypeExited == GroupEffortConfig.LimboType.THE_VOID) {
                LimboAPI.exitLimbo(player);
                GroupEffort.LOGGER.info("ExitLimbo called for {} during partial removal.", player.getName().getString());
            }
            GroupEffort.LOGGER.info("Partial removal for {} (likely disconnect/type switch), limbo type was: {}", player.getName().getString(), limboTypeExited);
            return;
        }

        if (originalData == null) {
            GroupEffort.LOGGER.warn("Player {} was not in originalPlayerDataMap, cannot fully remove from limbo. Limbo type was: {}", player.getName().getString(), limboTypeExited);
            if (limboTypeExited != null) {
                if (limboTypeExited == GroupEffortConfig.LimboType.THE_VOID) {
                    LimboAPI.exitLimbo(player);
                }
                player.sendAbilitiesUpdate();
            }
            return;
        }
        GroupEffort.LOGGER.info("Restoring player {} from limbo (was type: {})", player.getName().getString(), limboTypeExited);

        // 1. Restore Abilities
        NbtCompound originalAbilitiesNbt = new NbtCompound();
        originalData.abilities.writeNbt(originalAbilitiesNbt);
        player.getAbilities().readNbt(originalAbilitiesNbt);
        player.sendAbilitiesUpdate();
        GroupEffort.LOGGER.info("Restored abilities for {}. WalkSpeed now: {}", player.getName().getString(), player.getAbilities().getWalkSpeed());

        // 2. Determine original world
        ServerWorld originalWorld = server.getWorld(originalData.dimensionKey);
        if (originalWorld == null) {
            GroupEffort.LOGGER.error("Original world {} not found for {}. Defaulting to Overworld.", originalData.dimensionKey.getValue(), player.getName().getString());
            originalWorld = server.getOverworld();
        }
        final ServerWorld finalOriginalWorld = originalWorld;
        RegistryKey<DimensionType> originalDimensionTypeKey = finalOriginalWorld.getDimensionEntry().getKey()
                .orElseThrow(() -> new IllegalStateException("DimensionType key missing for world: " + finalOriginalWorld.getRegistryKey().getValue()));

        // 3. Send PlayerRespawnS2CPacket to switch client back to original dimension FIRST
        PlayerRespawnS2CPacket respawnPacket = new PlayerRespawnS2CPacket(
                originalDimensionTypeKey, finalOriginalWorld.getRegistryKey(), finalOriginalWorld.getSeed(),
                originalData.gameMode, originalData.previousGameMode, finalOriginalWorld.isDebugWorld(),
                finalOriginalWorld.isFlat(), PlayerRespawnS2CPacket.KEEP_ALL,
                player.getLastDeathPos(), player.getPortalCooldown()
        );
        player.networkHandler.sendPacket(respawnPacket);
        GroupEffort.LOGGER.info("Sent PlayerRespawnS2CPacket to {} to return to original dimension: {}", player.getName().getString(), finalOriginalWorld.getRegistryKey().getValue());

        // 4. Teleport player server-side to original position AFTER client knows it's in the right dimension
        server.execute(() -> {
            if (player.isRemoved()) {
                GroupEffort.LOGGER.warn("Player {} removed before server-side teleport in removePlayerFromLimboInternal.", player.getName().getString());
                return;
            }
            player.teleport(finalOriginalWorld,
                    originalData.position.getX(), originalData.position.getY(), originalData.position.getZ(),
                    originalData.yaw, originalData.pitch
            );
            GroupEffort.LOGGER.info("Teleported {} to original position in {}.", player.getName().getString(), finalOriginalWorld.getRegistryKey().getValue());

            // 5. Call LimboAPI.exitLimbo AFTER client is in original dimension and at original position
            LimboAPI.enterLimbo(player, GroupEffortConfig.LimboType.THE_VOID);
            LimboAPI.exitLimbo(player);
            GroupEffort.LOGGER.info("ExitLimbo called for {}.", player.getName().getString());

            // 6. Further state sync
            server.execute(() -> {
                if (player.isRemoved()) return;

                for (StatusEffectInstance effectInstance : player.getStatusEffects()) {
                    player.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), effectInstance));
                }

                DefaultedList<ItemStack> currentInventoryStacks = DefaultedList.ofSize(player.playerScreenHandler.getStacks().size(), ItemStack.EMPTY);
                for(int i = 0; i < player.playerScreenHandler.getStacks().size(); ++i) {
                    currentInventoryStacks.set(i, player.playerScreenHandler.getStacks().get(i).copy());
                }
                player.networkHandler.sendPacket(new InventoryS2CPacket(
                        player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(),
                        currentInventoryStacks, player.playerScreenHandler.getCursorStack().copy()
                ));
                player.networkHandler.sendPacket(new HealthUpdateS2CPacket(player.getHealth(), player.getHungerManager().getFoodLevel(), player.getHungerManager().getSaturationLevel()));
                player.getInventory().updateItems();

                finalOriginalWorld.getChunkManager().updatePosition(player);
                if (sendLeaveMessage) {
                    sendMessageToPlayer(player, formatMessage(CONFIG.chatMessages.playerLeavesLimbo, player.getName().getString(),0,0,0, -1));
                }
                GroupEffort.LOGGER.info("Player {} restoration process complete.", player.getName().getString());
            });
        });
    }


    public static void updateLimboTypeOfActivePlayers() {
        if (server == null || CONFIG == null) return;
        GroupEffort.LOGGER.info("Global limbo type changed, re-evaluating state for players currently in limbo.");
        Set<UUID> currentLimboPlayers = ConcurrentHashMap.newKeySet();
        currentLimboPlayers.addAll(playerLimboTypes.keySet());

        for (UUID playerUuid : currentLimboPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                GroupEffortConfig.LimboType oldType = playerLimboTypes.get(playerUuid);

                if (oldType != null && oldType != CONFIG.generalSettings.limboType) {
                    GroupEffort.LOGGER.info("Limbo type changing for {}. Exiting old type: {}", player.getName().getString(), oldType);
                    removePlayerFromLimboInternal(player, false, false);
                }
                ensurePlayerInCorrectLimboState(player);
            } else {
                playerLimboTypes.remove(playerUuid);
                originalPlayerDataMap.remove(playerUuid);
            }
        }
    }

    public static int getPlayersInLimboCount() { return playerLimboTypes.size(); }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        if (textToTranslate == null) return null;
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = '\u00A7';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    private static String formatMessage(String template, @Nullable String playerName, int needed, int current, int minimum, int graceDurationSeconds) {
        String message = template;
        if (playerName != null) message = message.replace("%player%", playerName);
        message = message.replace("%needed%", String.valueOf(needed));
        message = message.replace("%current%", String.valueOf(current));
        message = message.replace("%minimum%", String.valueOf(minimum));
        if (graceDurationSeconds > -1) message = message.replace("%duration%", String.valueOf(graceDurationSeconds));
        return translateAlternateColorCodes('&', message);
    }

    private static void sendMessageToPlayer(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(message), false);
    }

    private static void broadcastMessage(String message) {
        if (server != null && server.getPlayerManager() != null) {
            server.getPlayerManager().getPlayerList().forEach(serverPlayerEntity -> {
                serverPlayerEntity.sendMessage(Text.literal(message), false);
            });
        }
    }
}
