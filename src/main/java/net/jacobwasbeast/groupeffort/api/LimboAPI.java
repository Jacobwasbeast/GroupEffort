package net.jacobwasbeast.groupeffort.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.BitSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.jacobwasbeast.groupeffort.config.GroupEffortConfig;

import net.jacobwasbeast.groupeffort.network.UpdateClientLimboVisualsS2CPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.BiomeKeys;


public class LimboAPI {
    private static final int RENDER_DISTANCE = 8;
    private static final Map<UUID, GroupEffortConfig.LimboType> IN_LIMBO = new HashMap<>();

    /**
     * Send the player a ring of completely empty chunks (via packets only),
     * with lighting set according to LimboType.
     */
    public static void enterLimbo(ServerPlayerEntity player, GroupEffortConfig.LimboType type) {
        ServerWorld world = player.getServerWorld();
        ServerChunkManager chunkManager = world.getChunkManager();
        ChunkPos playerChunkPos = player.getChunkPos();
        int px = playerChunkPos.x;
        int pz = playerChunkPos.z;

        IN_LIMBO.put(player.getUuid(), type);

        List<Packet<?>> packets = new ArrayList<>();
        LightingProvider lightingProvider = world.getLightingProvider();

        int sectionCount = world.countVerticalSections();

        BitSet allSectionsSetMask = new BitSet(sectionCount);
        for (int i = 0; i < sectionCount; i++) {
            allSectionsSetMask.set(i);
        }
        BitSet emptyMask = new BitSet(sectionCount);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int cx = px + dx;
                int cz = pz + dz;
                ChunkPos chunkPos = new ChunkPos(cx, cz);

                RegistryEntry<Biome> biomeEntry = world.getRegistryManager().get(RegistryKeys.BIOME)
                        .getEntry(BiomeKeys.THE_VOID).orElseThrow(() -> new IllegalStateException("THE_VOID biome not found"));

                EmptyChunk emptyChunk = new EmptyChunk(world, chunkPos, biomeEntry);

                BitSet skyLightMaskForPacket;
                BitSet blockLightMaskForPacket;

                if (type == GroupEffortConfig.LimboType.THE_VOID) {
                    skyLightMaskForPacket = null;
                    blockLightMaskForPacket = null;

                } else {
                    skyLightMaskForPacket = emptyMask;
                    blockLightMaskForPacket = null;
                }
                packets.add(new ChunkDataS2CPacket(emptyChunk, lightingProvider, skyLightMaskForPacket, blockLightMaskForPacket));
            }
        }

        for (Packet<?> pkt : packets) {
            player.networkHandler.sendPacket(pkt);
        }

        ServerPlayNetworking.send(player, new UpdateClientLimboVisualsS2CPacket(type == GroupEffortConfig.LimboType.THE_VOID));
    }

    /**
     * Forget all the fake chunks on the client and re-send the real ones
     * so the player “snaps back” to the actual world.
     */
    public static void exitLimbo(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        ServerChunkManager chunkManager = world.getChunkManager();
        ChunkPos playerChunkPos = player.getChunkPos();
        int px = playerChunkPos.x;
        int pz = playerChunkPos.z;

        GroupEffortConfig.LimboType type = IN_LIMBO.remove(player.getUuid());
        if (type == null) return;

        List<Packet<?>> packets = new ArrayList<>();
        LightingProvider lightingProvider = world.getLightingProvider();

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                packets.add(new UnloadChunkS2CPacket(px + dx, pz + dz));
            }
        }

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int cx = px + dx;
                int cz = pz + dz;
                ChunkPos chunkPos = new ChunkPos(cx, cz);

                Chunk chunk = chunkManager.getChunk(cx, cz, ChunkStatus.FULL, true);

                if (chunk instanceof WorldChunk realChunk) {
                    packets.add(new ChunkDataS2CPacket(realChunk, lightingProvider, null, null));
                    packets.add(new LightUpdateS2CPacket(chunkPos, lightingProvider, null, null));
                } else if (chunk != null) {
                    System.err.println("LimboAPI: Chunk at " + cx + ", " + cz + " is not a WorldChunk, type: " + chunk.getClass().getName());
                } else {
                    System.err.println("LimboAPI: Could not load chunk at " + cx + ", " + cz + " for exiting limbo.");
                }
            }
        }

        for (Packet<?> pkt : packets) {
            player.networkHandler.sendPacket(pkt);
        }

        ServerPlayNetworking.send(player, new UpdateClientLimboVisualsS2CPacket(false));
    }
}
