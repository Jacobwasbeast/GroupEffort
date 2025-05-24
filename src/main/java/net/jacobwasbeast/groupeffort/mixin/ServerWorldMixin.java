package net.jacobwasbeast.groupeffort.mixin;

import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void groupeffort_tickWorld(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (!GroupEffortManager.isServerTickAllowed()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void groupeffort_tickChunk(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (!GroupEffortManager.isServerTickAllowed()) {
            ci.cancel();
        }
    }
}