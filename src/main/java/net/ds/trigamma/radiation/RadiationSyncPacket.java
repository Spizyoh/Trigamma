package net.ds.trigamma.radiation;

import io.netty.buffer.ByteBuf;
import net.ds.trigamma.TriGamma;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server → client every second to keep the HUD in sync.
 *
 * Register in TriGamma.java:
 *   event.register(RadiationSyncPacket.class, RadiationSyncPacket.STREAM_CODEC, RadiationSyncPacket.TYPE);
 * and
 *   PayloadRegistrar::playToClient
 */
public record RadiationSyncPacket(float externalRads, float internalRads) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RadiationSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TriGamma.MODID, "radiation_sync"));

    public static final StreamCodec<ByteBuf, RadiationSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RadiationSyncPacket::externalRads,
            ByteBufCodecs.FLOAT, RadiationSyncPacket::internalRads,
            RadiationSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Called on the client thread when the packet is received. */
    public static void handle(RadiationSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientRadiationData.externalRads = packet.externalRads();
            ClientRadiationData.internalRads = packet.internalRads();
        });
    }

    // ── Simple static client cache (read by HUD overlay) ─────────────────────
    public static final class ClientRadiationData {
        public static float externalRads = 0f;
        public static float internalRads = 0f;

        public static float getEffectiveDose() {
            return externalRads + internalRads * 2f;
        }
    }
}