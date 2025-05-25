package net.jacobwasbeast.groupeffort.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;

import net.jacobwasbeast.groupeffort.network.DisableClientLimboVisualsS2CPacket;
import net.jacobwasbeast.groupeffort.network.EnableClientLimboVisualsS2CPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;


public class LimboAPI {
    private static final int RENDER_DISTANCE = 8;
    private static final Map<UUID, GroupEffortConfig.LimboType> IN_LIMBO = new HashMap<>();

    public static void enterLimbo(ServerPlayerEntity player, GroupEffortConfig.LimboType type) {
        ServerPlayNetworking.send(player, new EnableClientLimboVisualsS2CPacket());
        ChunkPos playerChunkPos = player.getChunkPos();
        int px = playerChunkPos.x;
        int pz = playerChunkPos.z;

        IN_LIMBO.put(player.getUuid(), type);

        List<Packet<?>> packetsToSend = new ArrayList<>();

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                packetsToSend.add(new UnloadChunkS2CPacket(new ChunkPos(px + dx, pz + dz)));
            }
        }

        for (Packet<?> pkt : packetsToSend) {
            player.networkHandler.sendPacket(pkt);
        }
    }

    public static void exitLimbo(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new DisableClientLimboVisualsS2CPacket());
        ServerWorld world = player.getServerWorld();
        ServerChunkManager chunkManager = world.getChunkManager();
        ChunkPos playerChunkPos = player.getChunkPos();
        int px = playerChunkPos.x;
        int pz = playerChunkPos.z;

        GroupEffortConfig.LimboType limboType = IN_LIMBO.remove(player.getUuid());
        if (limboType == null) return;



        List<Packet<?>> packetsToSend = new ArrayList<>();
        LightingProvider lightingProvider = world.getLightingProvider();

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int cx = px + dx;
                int cz = pz + dz;
                ChunkPos currentChunkPos = new ChunkPos(cx, cz);

                WorldChunk realChunk = chunkManager.getWorldChunk(cx, cz, false);

                if (realChunk != null) {
                    packetsToSend.add(new ChunkDataS2CPacket(realChunk, lightingProvider, null, null));
                } else {
                    System.err.println("LimboAPI: No WorldChunk at " + cx + ", " + cz + " to resend for exiting limbo. Ensuring client unloads.");
                    packetsToSend.add(new UnloadChunkS2CPacket(currentChunkPos));
                }
            }
        }

        for (Packet<?> pkt : packetsToSend) {
            player.networkHandler.sendPacket(pkt);
        }
    }
}