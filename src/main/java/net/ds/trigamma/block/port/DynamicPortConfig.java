package net.ds.trigamma.block.port;

import net.ds.trigamma.inventory.fluid.MatterPhase;
import org.jetbrains.annotations.Nullable;

public record DynamicPortConfig(PortKind kind, PortIO io, @Nullable MatterPhase phaseFilter) {
    // A fallback config for when a machine is idle or unconfigured
    public static final DynamicPortConfig OMNI_MATTER = new DynamicPortConfig(PortKind.MATTER, PortIO.INPUT, null);
}