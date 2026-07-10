package net.ds.trigamma.block.port;

import net.ds.trigamma.inventory.fluid.IMatterBuffer;
import net.minecraft.core.BlockPos;
import java.util.Optional;

public interface IPortableMachine {
    /**
     * Dynamically determines what a specific port location should be doing right now.
     * @param portPos The global position of the port block querying the machine.
     * @return The dynamic configuration for this port.
     */
    DynamicPortConfig getPortConfig(BlockPos portPos);

    /**
     * Grabs the buffer associated with a specific port configuration.
     */
    Optional<IMatterBuffer> getBufferForPort(BlockPos portPos, DynamicPortConfig config);
}