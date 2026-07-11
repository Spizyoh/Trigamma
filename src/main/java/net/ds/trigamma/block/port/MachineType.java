package net.ds.trigamma.block.port;

/**
 * Identifies which multiblock/pseudo-multiblock a port layout belongs to.
 * Add an entry here for every machine that adopts the port system.
 */
public enum MachineType {
    BOILER,
    TANK
    // TODO: add future machines here (e.g. DISTILLATION_TOWER, ELECTROLYZER, ...)
}