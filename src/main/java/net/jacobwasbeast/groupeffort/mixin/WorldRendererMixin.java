package net.jacobwasbeast.groupeffort.mixin;
import net.jacobwasbeast.groupeffort.client.ClientLimboState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    // Shadow method to call the original renderEndSky.
    // Ensure this method exists in WorldRenderer and is accessible.
    // In 1.20.1, renderEndSky(MatrixStack matrices) is public.
    @Shadow public abstract void renderEndSky(MatrixStack matrices);

    /**
     * Injects into the main sky rendering method of WorldRenderer.
     * If our custom flag indicates that The End sky should be rendered,
     * this method calls renderEndSky and then cancels the original sky rendering.
     */
    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void groupeffort_onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (ClientLimboState.shouldRenderEndSkyForVoidLimbo) {
            ci.cancel();
            if (MinecraftClient.getInstance().currentScreen instanceof DownloadingTerrainScreen) {
                MinecraftClient.getInstance().setScreen(null);
            }
            this.renderEndSky(matrices);
        }
    }
}
