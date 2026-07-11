package net.ds.trigamma.inventory.fluid;

import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.inventory.fluid.IMatterHandler;
import net.ds.trigamma.inventory.fluid.MatterCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MatterNetworkScanner {
    private static final int CACHE_TICKS = 20;
    private static final MatterNetworkScanner INSTANCE = new MatterNetworkScanner();

    private final Map<Level, LevelCache> caches = new WeakHashMap<>();

    private MatterNetworkScanner() {
    }

    public static MatterNetwork getNetwork(Level level, BlockPos origin) {
        return INSTANCE.scan(level, origin);
    }

    public static void invalidate(Level level, BlockPos pos) {
        if (level == null) return;

        LevelCache cache = INSTANCE.caches.get(level);
        if (cache != null) {
            cache.invalidate(pos);
        }
    }

    private MatterNetwork scan(Level level, BlockPos origin) {
        if (level == null) {
            return MatterNetwork.empty(origin);
        }

        long now = level.getGameTime();
        LevelCache cache = caches.computeIfAbsent(level, ignored -> new LevelCache());
        MatterNetwork cached = cache.get(origin, now);
        if (cached != null) {
            return cached;
        }

        MatterNetwork network = scanUncached(level, origin);
        cache.put(network, now + CACHE_TICKS);
        return network;
    }

    private MatterNetwork scanUncached(Level level, BlockPos origin) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> ducts = new ArrayList<>();
        List<MatterNetwork.Endpoint> endpoints = new ArrayList<>();
        Set<MatterNetwork.Endpoint> seenEndpoints = new HashSet<>();

        open.add(origin.immutable());

        while (!open.isEmpty()) {
            BlockPos current = open.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            BlockEntity currentEntity = level.getBlockEntity(current);
            if (!(currentEntity instanceof UniversalMatterDuctBlockEntity)) {
                continue;
            }

            ducts.add(current.immutable());

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = current.relative(direction);
                BlockEntity neighborEntity = level.getBlockEntity(neighborPos);

                if (neighborEntity instanceof UniversalMatterDuctBlockEntity) {
                    if (!visited.contains(neighborPos)) {
                        open.add(neighborPos.immutable());
                    }
                    continue;
                }

                IMatterHandler handler = level.getCapability(
                        MatterCapabilities.MATTER_HANDLER,
                        neighborPos,
                        direction.getOpposite()
                );

                if (handler != null) {
                    MatterNetwork.Endpoint endpoint = new MatterNetwork.Endpoint(
                            neighborPos.immutable(),
                            direction.getOpposite()
                    );
                    if (seenEndpoints.add(endpoint)) {
                        endpoints.add(endpoint);
                    }
                }
            }
        }

        return new MatterNetwork(ducts, endpoints);
    }

    private static final class LevelCache {
        private final Map<BlockPos, CachedNetwork> byDuct = new HashMap<>();

        MatterNetwork get(BlockPos pos, long now) {
            CachedNetwork cached = byDuct.get(pos);
            if (cached == null || cached.expiresAt < now) {
                return null;
            }
            return cached.network;
        }

        void put(MatterNetwork network, long expiresAt) {
            CachedNetwork cached = new CachedNetwork(network, expiresAt);
            for (BlockPos duct : network.ducts()) {
                byDuct.put(duct, cached);
            }
        }

        void invalidate(BlockPos pos) {
            CachedNetwork cached = byDuct.remove(pos);
            if (cached == null) return;

            for (BlockPos duct : cached.network.ducts()) {
                byDuct.remove(duct);
            }
        }
    }

    private record CachedNetwork(MatterNetwork network, long expiresAt) {
    }
}
