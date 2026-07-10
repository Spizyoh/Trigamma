package net.ds.trigamma.block.port;

import net.ds.trigamma.inventory.fluid.MatterPhase;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Describes one port slot in a multiblock's layout.
 *
 * @param offset      position of this port relative to the master block (same coordinate
 *                    space as e.g. BoilerBlockEntity#getShellOffsets()).
 * @param kind        MATTER for now; PNEUMATIC is reserved (see PortKind).
 * @param io          whether this port feeds INTO or draws OUT OF the machine's buffer.
 * @param phaseFilter optional restriction (FLUID or GAS only) - null means "accept whatever
 *                    the connected buffer is currently holding/locked to".
 */
public record PortSpec(BlockPos offset, PortKind kind, PortIO io, @Nullable MatterPhase phaseFilter) {

    public static PortSpec matter(BlockPos offset, PortIO io) {
        return new PortSpec(offset, PortKind.MATTER, io, null);
    }

    public static PortSpec matter(BlockPos offset, PortIO io, MatterPhase phaseFilter) {
        return new PortSpec(offset, PortKind.MATTER, io, phaseFilter);
    }
}