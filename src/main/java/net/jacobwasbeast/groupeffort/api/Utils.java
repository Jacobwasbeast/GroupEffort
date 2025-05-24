package net.jacobwasbeast.groupeffort.api;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.Set;

public class Utils {
    public static void requestTeleport(ServerPlayerEntity player, double x, double y, double z, float yaw, float pitch) {
        requestTeleport(player, x, y, z, yaw, pitch, Collections.emptySet());
    }

    public static void requestTeleport(ServerPlayerEntity player, double x, double y, double z, float yaw, float pitch, Set<PositionFlag> flags) {
        double d = flags.contains(PositionFlag.X) ? player.getX() : 0.0;
        double e = flags.contains(PositionFlag.Y) ? player.getY() : 0.0;
        double f = flags.contains(PositionFlag.Z) ? player.getZ() : 0.0;
        float g = flags.contains(PositionFlag.Y_ROT) ? player.getYaw() : 0.0F;
        float h = flags.contains(PositionFlag.X_ROT) ? player.getPitch() : 0.0F;
        int requestedTeleportId = 0;
        if (++requestedTeleportId == Integer.MAX_VALUE) {
            requestedTeleportId = 0;
        }
        double originalX = player.getX();
        double originalY = player.getY();
        double originalZ = player.getZ();
        float originalYaw = player.getYaw();
        float originalPitch = player.getPitch();
        player.updatePositionAndAngles(x, y, z, yaw, pitch);
        player.networkHandler.sendPacket(new PlayerPositionLookS2CPacket(x - d, y - e, z - f, yaw - g, pitch - h, flags, requestedTeleportId));
        player.updatePositionAndAngles(originalX, originalY, originalZ, originalYaw, originalPitch);
    }

    public static void sendFakeAbilities(ServerPlayerEntity player, PlayerAbilities abilities) {
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(abilities));
    }

}
