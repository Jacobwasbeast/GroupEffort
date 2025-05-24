package net.jacobwasbeast.groupeffort.network; // Adjust package as needed

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class UpdateClientLimboVisualsS2CPacket implements FabricPacket {
    public static final Identifier PACKET_ID = new Identifier("groupeffort", "update_limbo_visuals");

    public static final PacketType<UpdateClientLimboVisualsS2CPacket> TYPE = PacketType.create(
            PACKET_ID,
            UpdateClientLimboVisualsS2CPacket::new
    );

    private final boolean shouldRenderEndSky;

    /**
     * Constructor to create the packet for sending.
     * @param shouldRenderEndSky true if the client should render The End sky, false otherwise.
     */
    public UpdateClientLimboVisualsS2CPacket(boolean shouldRenderEndSky) {
        this.shouldRenderEndSky = shouldRenderEndSky;
    }

    /**
     * Constructor for Fabric to use when receiving and creating the packet from a PacketByteBuf.
     * @param buf The buffer to read data from.
     */
    public UpdateClientLimboVisualsS2CPacket(PacketByteBuf buf) {
        this(buf.readBoolean());
    }

    /**
     * Writes the packet data to the buffer.
     * @param buf The buffer to write data to.
     */
    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(this.shouldRenderEndSky);
    }

    /**
     * Gets the PacketType of this packet.
     * @return The PacketType.
     */
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    /**
     * Gets the payload of this packet.
     * @return true if The End sky should be rendered, false otherwise.
     */
    public boolean shouldRenderEndSky() {
        return this.shouldRenderEndSky;
    }
}
