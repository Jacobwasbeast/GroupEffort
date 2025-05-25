package net.jacobwasbeast.groupeffort.mixin;

import net.jacobwasbeast.groupeffort.GroupEffort;
import net.jacobwasbeast.groupeffort.api.Utils;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;
import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Collections;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    private void groupeffort_cancelIfInLimbo(Packet<?> incomingPacket, CallbackInfo ci, String actionTypeDescription) {
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
            GroupEffortManager.OriginalPlayerData originalData = GroupEffortManager.originalPlayerDataMap.get(player.getUuid());

            if (originalData != null) {
                double targetX = originalData.position.getX();
                double targetY = (GroupEffort.getConfig().generalSettings.limboType == GroupEffortConfig.LimboType.LOCALIZED_FREEZE) ?
                        originalData.position.getY() : 300.0;
                double targetZ = originalData.position.getZ();
                float targetYaw = (GroupEffort.getConfig().generalSettings.limboType == GroupEffortConfig.LimboType.LOCALIZED_FREEZE) ?
                        originalData.yaw : 0;
                float targetPitch = (GroupEffort.getConfig().generalSettings.limboType == GroupEffortConfig.LimboType.LOCALIZED_FREEZE) ? // Typo here in original? limbo_type vs limboType
                        originalData.pitch : 0;

                Utils.requestTeleport(this.player,
                        targetX, targetY, targetZ,
                        targetYaw, targetPitch,
                        Collections.emptySet()
                );
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