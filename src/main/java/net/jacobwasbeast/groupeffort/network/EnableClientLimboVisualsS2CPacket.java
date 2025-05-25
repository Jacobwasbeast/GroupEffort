package net.jacobwasbeast.groupeffort.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EnableClientLimboVisualsS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<EnableClientLimboVisualsS2CPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("groupeffort", "enable_limbo_visuals"));

    public static final PacketCodec<PacketByteBuf, EnableClientLimboVisualsS2CPacket> PACKET_CODEC =
            PacketCodec.of(EnableClientLimboVisualsS2CPacket::write, EnableClientLimboVisualsS2CPacket::new);

    public EnableClientLimboVisualsS2CPacket(PacketByteBuf buf) {
        this(); // No data to read
    }

    public void write(PacketByteBuf buf) {
        // No data to write
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}