package net.ds.trigamma.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntFunction;

/**
 * The "family" a stamp belongs to - plate, wire, gear, rod, etc.
 * Add more here as your mod grows; recipes reference this by name in JSON.
 */
public enum StampType implements StringRepresentable {
    PLATE("plate"),
    WIRE("wire"),
    GEAR("gear"),
    ROD("rod");

    public static final Codec<StampType> CODEC = StringRepresentable.fromEnum(StampType::values);

    private static final IntFunction<StampType> BY_ID = i -> values()[i];

    public static final StreamCodec<RegistryFriendlyByteBuf, StampType> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, StampType::ordinal).cast();

    private final String name;

    StampType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Does the given stack (in the stamp slot) match this type? */
    public boolean matches(ItemStack stampStack) {
        return !stampStack.isEmpty()
                && stampStack.getItem() instanceof StampItem stampItem
                && stampItem.getStampType() == this;
    }
}
