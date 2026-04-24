package net.ds.trigamma.radiation;

import net.ds.trigamma.TriGamma;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RadiationSyncPacket(float externalRads, float internalRads) implements CustomPacketPayload {

    public static final Type<RadiationSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TriGamma.MODID, "radiation_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadiationSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RadiationSyncPacket::externalRads,
            ByteBufCodecs.FLOAT, RadiationSyncPacket::internalRads,
            RadiationSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}