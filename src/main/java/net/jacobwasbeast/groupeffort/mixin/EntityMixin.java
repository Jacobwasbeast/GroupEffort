package net.jacobwasbeast.groupeffort.mixin;

import net.jacobwasbeast.groupeffort.manager.GroupEffortManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void groupeffort_tickEntity(CallbackInfo ci) {
        if (!((Object)this instanceof ServerPlayerEntity) && !GroupEffortManager.isServerTickAllowed()) {
            ci.cancel();
        }
    }
}