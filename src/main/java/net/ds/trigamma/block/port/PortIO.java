package net.ds.trigamma.block.port;

/**
 * Whether a MachinePort feeds INTO the machine's buffer (accepts fill(), rejects drain())
 * or draws OUT of it (accepts drain(), rejects fill()).
 */
public enum PortIO {
    INPUT,
    OUTPUT,
    BOTH
}