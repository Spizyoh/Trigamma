package net.ds.trigamma.radiation;

/**
 * Implement this interface on a {@link net.minecraft.world.level.block.entity.BlockEntity}
 * to control how much radiation leaks through its container to nearby players.
 *
 * If a container's block entity does NOT implement this interface, it will use the
 * default leakage factor defined in {@link RadiationItemEvents#DEFAULT_CONTAINER_LEAK_FACTOR}.
 *
 * Examples:
 * <pre>{@code
 *   // A lead-lined chest — blocks all radiation
 *   public class LeadChestBlockEntity extends BlockEntity implements RadiationShieldedContainer {
 *       @Override public float getRadiationLeakFactor() { return 0f; }
 *   }
 *
 *   // A regular wooden barrel — partial shielding
 *   public class BarrelBlockEntity extends BlockEntity implements RadiationShieldedContainer {
 *       @Override public float getRadiationLeakFactor() { return 0.3f; }
 *   }
 *
 *   // An open crate — almost no shielding (same as default container behaviour)
 *   public class OpenCrateBlockEntity extends BlockEntity implements RadiationShieldedContainer {
 *       @Override public float getRadiationLeakFactor() { return 0.5f; }
 *   }
 * }</pre>
 */
public interface RadiationShieldedContainer {

    /**
     * Returns the fraction of item radiation that leaks out of this container to nearby players.
     *
     * <ul>
     *   <li>{@code 0.0f} — completely shielded, no radiation escapes</li>
     *   <li>{@code 0.5f} — half the radiation escapes (default for unshielded containers)</li>
     *   <li>{@code 1.0f} — fully transparent, radiation passes through as if no container</li>
     * </ul>
     *
     * @return a value in [0, 1]
     */
    float getRadiationLeakFactor();
}