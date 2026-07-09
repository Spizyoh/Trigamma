package net.ds.trigamma.block.entity;

import net.ds.trigamma.inventory.fluid.IMatterBuffer;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface IMatterBufferHolder {
    List<BufferSlot> getDisplayBuffers();

    record BufferSlot(Component label, IMatterBuffer buffer) {}
}