package net.ds.trigamma.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Handles radiation exposure from items:
 *
 * 1. INVENTORY  — radioactive items in a player's inventory (including hotbar and off-hand)
 *                 deal their full stack radiation directly to the player each second.
 *
 * 2. DROPPED    — radioactive ItemEntities on the ground act like radioactive blocks.
 *                 Players within DROPPED_ITEM_RADIUS blocks receive attenuated exposure
 *                 based on the inverse-square of distance.
 *
 * 3. CONTAINERS — nearby block entities that implement {@link Container} and hold
 *                 radioactive items leak radiation outward, attenuated by:
 *                   a) distance (inverse-square falloff)
 *                   b) the container's {@link RadiationShieldedContainer#getRadiationLeakFactor()}
 *                      (defaults to {@link #DEFAULT_CONTAINER_LEAK_FACTOR} if not implemented)
 *
 * Register on the NeoForge EVENT_BUS (server-side):
 * <pre>{@code
 *   NeoForge.EVENT_BUS.register(new RadiationItemEvents());
 * }</pre>
 */
public class RadiationItemEvents {

    // ── Configuration ─────────────────────────────────────────────────────────

    /** Tick interval for item radiation checks (20 = once per second). */
    private static final int TICK_RATE = 20;

    /** Radius (blocks) within which dropped items irradiate nearby players. */
    private static final float DROPPED_ITEM_RADIUS = 8f;

    /** Radius (blocks) within which containers leak radiation to players. */
    private static final float CONTAINER_SCAN_RADIUS = 6f;

    /**
     * Default radiation leak factor for containers that don't implement
     * {@link RadiationShieldedContainer}. 0.5 = half the radiation escapes.
     */
    public static final float DEFAULT_CONTAINER_LEAK_FACTOR = 0.5f;

    /**
     * Divisor applied to container radiation before distance attenuation.
     * Containers already at half-dose; this keeps total exposure balanced.
     */
    private static final float CONTAINER_BASE_MULTIPLIER = 1.0f;

    // ── Server-side player tick ───────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % TICK_RATE != 0) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        RadiationCapability cap = RadiationCapability.get(player);
        Level level = player.level();

        float totalItemRads = 0f;

        // ── 1. Inventory exposure ─────────────────────────────────────────────
        totalItemRads += getInventoryRadiation(player);

        // ── 2. Dropped item exposure ──────────────────────────────────────────
        totalItemRads += getDroppedItemRadiation(player, level);

        // ── 3. Container leakage ──────────────────────────────────────────────
        totalItemRads += getContainerRadiation(player, level);

        // Apply to capability (external contamination — items irradiate skin)
        if (totalItemRads > 0f) {
            cap.addExternalRads(totalItemRads);
        }

        // Sync to client for HUD update
        PacketDistributor.sendToPlayer(serverPlayer, new RadiationSyncPacket(
                cap.getExternalRads(),
                cap.getInternalRads()
        ));
    }

    // ── 1. Inventory ──────────────────────────────────────────────────────────

    /**
     * Sums the radiation from every radioactive stack in the player's full inventory
     * (main inventory + hotbar + off-hand). Armour slots are intentionally excluded
     * here — handle armour separately if your mod has radioactive armour.
     *
     * @return total rads/s absorbed from carried items
     */
    private float getInventoryRadiation(Player player) {
        float total = 0f;

        // Main inventory (includes hotbar, indices 0–35) + off-hand (index 40)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            total += RadioactiveItemRegistry.getStackRadiation(stack);
        }

        return total;
    }

    // ── 2. Dropped items ──────────────────────────────────────────────────────

    /**
     * Finds all ItemEntities within {@link #DROPPED_ITEM_RADIUS} of the player and
     * accumulates their radiation with inverse-square distance attenuation.
     *
     * Dropped stacks behave like radioactive blocks: the full stack radiation is the
     * emission strength, then attenuated by distance.
     *
     * @return total rads/s the player receives from dropped items
     */
    private float getDroppedItemRadiation(Player player, Level level) {
        float total = 0f;
        double radius = DROPPED_ITEM_RADIUS;

        AABB searchBox = player.getBoundingBox().inflate(radius);
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, searchBox);

        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            float stackRads = RadioactiveItemRegistry.getStackRadiation(stack);
            if (stackRads <= 0f) continue;

            double dist = player.distanceTo(itemEntity);
            if (dist < 0.5) dist = 0.5; // avoid division by near-zero

            // Inverse-square falloff matching the block raycast system
            float attenuated = stackRads / (float)(dist * dist * 0.1f + 1f);
            total += attenuated;
        }

        return total;
    }

    // ── 3. Container leakage ──────────────────────────────────────────────────

    /**
     * Scans all block entities within {@link #CONTAINER_SCAN_RADIUS} that implement
     * {@link Container}. For each, totals the radioactive items inside and applies:
     *
     *   leakedRads = stackRads × leakFactor × (1 / distanceSquared)
     *
     * Containers implementing {@link RadiationShieldedContainer} control their own
     * leak factor; others default to {@link #DEFAULT_CONTAINER_LEAK_FACTOR}.
     *
     * @return total rads/s received from all nearby containers
     */
    private float getContainerRadiation(Player player, Level level) {
        float total = 0f;

        BlockPos playerPos = player.blockPosition();
        int scanBlocks = (int) Math.ceil(CONTAINER_SCAN_RADIUS);

        for (int dx = -scanBlocks; dx <= scanBlocks; dx++) {
            for (int dy = -scanBlocks; dy <= scanBlocks; dy++) {
                for (int dz = -scanBlocks; dz <= scanBlocks; dz++) {
                    BlockPos checkPos = playerPos.offset(dx, dy, dz);

                    double dist = player.position()
                            .distanceTo(checkPos.getCenter());
                    if (dist > CONTAINER_SCAN_RADIUS) continue;

                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (!(be instanceof Container container)) continue;

                    // Determine leak factor
                    float leakFactor;
                    if (be instanceof RadiationShieldedContainer shielded) {
                        leakFactor = shielded.getRadiationLeakFactor();
                    } else {
                        leakFactor = DEFAULT_CONTAINER_LEAK_FACTOR;
                    }

                    // Fully shielded — skip entirely
                    if (leakFactor <= 0f) continue;

                    // Sum radioactive contents
                    float containerRads = 0f;
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.getItem(slot);
                        containerRads += RadioactiveItemRegistry.getStackRadiation(stack);
                    }

                    if (containerRads <= 0f) continue;

                    // Apply leak factor and inverse-square distance attenuation
                    if (dist < 0.5) dist = 0.5;
                    float attenuated = (containerRads * leakFactor * CONTAINER_BASE_MULTIPLIER)
                            / (float)(dist * dist * 0.1f + 1f);
                    total += attenuated;
                }
            }
        }

        return total;
    }
}