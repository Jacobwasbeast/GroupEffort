package net.jacobwasbeast.groupeffort.client;

public class ClientLimboState {
    /**
     * This flag should be updated by your custom S2C packet handler.
     * true = render The End sky (when server indicates THE_VOID limbo).
     * false = render normal sky.
     */
    public static volatile boolean shouldRenderEndSkyForVoidLimbo = false;
}