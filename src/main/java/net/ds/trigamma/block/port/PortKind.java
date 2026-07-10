package net.ds.trigamma.block.port;

/**
 * What kind of network a given port plugs into.
 * <p>
 * MATTER ports connect to Universal Matter Ducts (fluids/gases) via IMatterHandler.
 * <p>
 * TODO: Pneumatic Tubes don't exist yet. Once they do, add a PNEUMATIC case here, give it
 * its own capability (e.g. PneumaticCapabilities.PNEUMATIC_HANDLER) and an IPneumaticHandler
 * interface mirroring IMatterHandler, then wire MachinePortBlockEntity to expose that
 * capability for ports of this kind instead of/alongside the matter one.
 */
public enum PortKind {
    MATTER,
    PNEUMATIC // TODO: not implemented - pneumatic tubes don't exist yet
}