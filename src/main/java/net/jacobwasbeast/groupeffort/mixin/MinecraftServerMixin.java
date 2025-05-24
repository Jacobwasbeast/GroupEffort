package net.jacobwasbeast.groupeffort.mixin;

import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void groupeffort_onTickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        GroupEffortManager.onServerTick();
    }
}