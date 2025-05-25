package net.jacobwasbeast.groupeffort.mixin;

import net.jacobwasbeast.groupeffort.GroupEffort;
import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin {
    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void groupeffort_onSendPacket_S2CInterceptor(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        ServerCommonNetworkHandler handler = (ServerCommonNetworkHandler) (Object) this;

        if (handler instanceof ServerPlayNetworkHandler playHandler) {
            ServerPlayerEntity player = playHandler.player;

            if (player != null && GroupEffortManager.isPlayerInSkyOnlyLimbo(player.getUuid())) {
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
                        packet instanceof ChangeUnlockedRecipesS2CPacket ||
                        packet instanceof ServerMetadataS2CPacket ||
                        packet instanceof DifficultyS2CPacket ||
                        packet instanceof ExperienceBarUpdateS2CPacket ||
                        packet instanceof HealthUpdateS2CPacket ||
                        packet instanceof SetCameraEntityS2CPacket ||
                        packet instanceof PlayerListS2CPacket ||
                        (packet instanceof EntityStatusS2CPacket && player.getWorld() != null && ((EntityStatusS2CPacket) packet).getEntity(player.getWorld()) == player)) {
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
                        packet instanceof EntitySpawnS2CPacket ||
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
    }
}