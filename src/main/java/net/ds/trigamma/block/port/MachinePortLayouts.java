package net.ds.trigamma.block.port;

import net.minecraft.core.BlockPos;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry of port layouts per machine type. When a multiblock places its shells,
 * it looks up its layout here and swaps a plain shell block for a MachinePortBlock at each
 * listed offset, pre-configured with that spec's PortIO/PortKind/filter.
 * <p>
 * This is the "automatically configured based on what machine was placed down" piece - the
 * machine itself doesn't need to know anything about ports beyond its MachineType.
 */
public final class MachinePortLayouts {

    private static final Map<MachineType, List<PortSpec>> LAYOUTS = new EnumMap<>(MachineType.class);

    static {
        // --- Boiler ---
        // NOTE: these offsets are placeholders (relative to the master, same space as
        // BoilerBlockEntity#getShellOffsets(): dx/dz in [-1,1], dy in [0,3], excluding
        // (0,0,0)). Adjust them to whichever exterior shell cells you actually want to
        // expose as the boiler's plumbing connections once the model/texture is in.
        LAYOUTS.put(MachineType.BOILER, List.of(
                PortSpec.matter(new BlockPos(0, 0, -1), PortIO.INPUT),
                PortSpec.matter(new BlockPos(0, 0, 1), PortIO.INPUT),
                PortSpec.matter(new BlockPos(-1, 0, 0), PortIO.INPUT),
                PortSpec.matter(new BlockPos(1, 0, 1), PortIO.INPUT),
                PortSpec.matter(new BlockPos(0, 3, 0), PortIO.OUTPUT)
        ));

        // TODO: register additional machines' layouts here as they adopt the port system.
    }

    private MachinePortLayouts() {}

    public static List<PortSpec> get(MachineType type) {
        return LAYOUTS.getOrDefault(type, List.of());
    }
}