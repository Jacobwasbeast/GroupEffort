package net.jacobwasbeast.groupeffort.mixin; // Ensure your package is correct

import net.jacobwasbeast.groupeffort.GroupEffort;
import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void groupeffort_onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        // Use this.server from PlayerManager context
        GroupEffortManager.init(this.server, GroupEffort.getConfig());
        GroupEffortManager.onPlayerJoin(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void groupeffort_onPlayerRemove(ServerPlayerEntity player, CallbackInfo ci) {
        GroupEffortManager.init(this.server, GroupEffort.getConfig());
        GroupEffortManager.onPlayerLeave(player);
    }
}