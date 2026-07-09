package net.ds.trigamma.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record SelectMatterPayload(InteractionHand hand, ResourceLocation matterId) implements CustomPacketPayload {

    public static final Type<SelectMatterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("trigamma", "select_matter"));

    private static final StreamCodec<ByteBuf, InteractionHand> HAND_CODEC = ByteBufCodecs.BOOL.map(
            isOffhand -> isOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
            hand -> hand == InteractionHand.OFF_HAND
    );

    public static final StreamCodec<ByteBuf, SelectMatterPayload> STREAM_CODEC = StreamCodec.composite(
            HAND_CODEC, SelectMatterPayload::hand,
            ResourceLocation.STREAM_CODEC, SelectMatterPayload::matterId,
            SelectMatterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}