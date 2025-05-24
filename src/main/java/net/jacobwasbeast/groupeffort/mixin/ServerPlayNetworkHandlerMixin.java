package net.jacobwasbeast.groupeffort.mixin;

import net.jacobwasbeast.groupeffort.GroupEffort;
import net.jacobwasbeast.groupeffort.api.Utils;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;
import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;
    @Shadow @Final private MinecraftServer server;

    @Shadow public abstract ServerPlayerEntity getPlayer();

    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), cancellable = true)
    private void groupeffort_interceptClientBoundPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        if (GroupEffortManager.isPlayerInSkyOnlyLimbo(this.player.getUuid())) {
            if (packet instanceof ChatMessageS2CPacket && GroupEffort.getConfig().generalSettings.blockChatMessages) {
                ci.cancel();
                return;
            }

            if (packet instanceof KeepAliveS2CPacket ||
                    packet instanceof PlayerAbilitiesS2CPacket ||
                    packet instanceof PlayerRespawnS2CPacket ||
                    packet instanceof PlayerPositionLookS2CPacket ||
                    packet instanceof WorldTimeUpdateS2CPacket ||
                    packet instanceof ChatMessageS2CPacket ||
                    packet instanceof GameMessageS2CPacket ||
                    packet instanceof DisconnectS2CPacket ||
                    packet instanceof ResourcePackSendS2CPacket ||
                    packet instanceof NbtQueryResponseS2CPacket ||
                    packet instanceof CommandTreeS2CPacket ||
                    packet instanceof UnlockRecipesS2CPacket ||
                    packet instanceof ServerMetadataS2CPacket ||
                    packet instanceof DifficultyS2CPacket ||
                    packet instanceof ExperienceBarUpdateS2CPacket ||
                    packet instanceof HealthUpdateS2CPacket ||
                    packet instanceof SetCameraEntityS2CPacket ||
                    packet instanceof PlayerListS2CPacket ||
                    (GroupEffort.getConfig().generalSettings.blockChatMessages && packet instanceof ChatMessageS2CPacket) ||
                    (packet instanceof EntityStatusS2CPacket && ((EntityStatusS2CPacket) packet).getEntity(player.getWorld()) == player)) {
                return;
            }

            if (packet instanceof ChunkDataS2CPacket ||
                    packet instanceof UnloadChunkS2CPacket ||
                    packet instanceof LightUpdateS2CPacket ||
                    packet instanceof BlockUpdateS2CPacket ||
                    packet instanceof ChunkRenderDistanceCenterS2CPacket ||
                    packet instanceof BlockBreakingProgressS2CPacket ||
                    packet instanceof EntityAnimationS2CPacket ||
                    packet instanceof EntityDamageS2CPacket ||
                    packet instanceof EntityS2CPacket ||
                    packet instanceof EntitySetHeadYawS2CPacket ||
                    packet instanceof EntityTrackerUpdateS2CPacket ||
                    packet instanceof EntityVelocityUpdateS2CPacket ||
                    packet instanceof EntityEquipmentUpdateS2CPacket ||
                    packet instanceof EntitySpawnS2CPacket || // Corrected name
                    packet instanceof PlayerSpawnS2CPacket ||
                    packet instanceof ParticleS2CPacket ||
                    packet instanceof WorldEventS2CPacket ||
                    packet instanceof GameStateChangeS2CPacket ||
                    packet instanceof BlockEntityUpdateS2CPacket ||
                    packet instanceof SignEditorOpenS2CPacket ||
                    packet instanceof OpenScreenS2CPacket ||
                    packet instanceof ExplosionS2CPacket ||
                    packet instanceof BossBarS2CPacket) {
                ci.cancel();
            }
        }
    }

    private void groupeffort_cancelIfInLimbo(Packet<?> incomingPacket, CallbackInfo ci, String actionType) {
        if (incomingPacket instanceof TeleportConfirmC2SPacket) {
            return;
        }
        if (GroupEffortManager.isPlayerInAnyLimbo(this.player.getUuid())) {
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (GroupEffortManager.isPlayerInAnyLimbo(this.player.getUuid())) {
            ci.cancel();
            GroupEffortConfig.LimboType type = GroupEffortManager.getPlayerLimboType(this.player.getUuid());
            GroupEffortManager.OriginalPlayerData originalData = GroupEffortManager.originalPlayerDataMap.get(player.getUuid());
            if (originalData != null) {
                if (GroupEffort.getConfig().generalSettings.limboType == GroupEffortConfig.LimboType.LOCALIZED_FREEZE) {
                    Utils.requestTeleport(this.player,
                            originalData.position.getX(), originalData.position.getY(), originalData.position.getZ(),
                            originalData.yaw, originalData.pitch,
                            Collections.emptySet()
                    );
                }
                else {
                    Utils.requestTeleport(this.player,
                            originalData.position.getX(), 300, originalData.position.getZ(),
                            0, 0,
                            Collections.emptySet()
                    );
                }
            }
        }
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "perform actions");
    }

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "interact with blocks");
    }

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onPlayerInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "interact with entities");
    }

    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onPlayerInteractItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "use items");
    }

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "access creative inventory");
    }

    @Inject(method = "onPickFromInventory", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onPickFromInventory(PickFromInventoryC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "pick items from inventory");
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "interact with inventory slots");
    }

    @Inject(method = "onCraftRequest", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onCraftRequest(CraftRequestC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "craft items");
    }

    @Inject(method = "onUpdatePlayerAbilities", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onUpdatePlayerAbilities(UpdatePlayerAbilitiesC2SPacket packet, CallbackInfo ci) {
        groupeffort_cancelIfInLimbo(packet, ci, "change abilities");
    }

    @Inject(method = "onClientStatus", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onClientStatus(ClientStatusC2SPacket packet, CallbackInfo ci) {
        if (packet.getMode() == ClientStatusC2SPacket.Mode.REQUEST_STATS) {
            groupeffort_cancelIfInLimbo(packet, ci, "request stats");
        }
    }
}