package net.ds.trigamma.inventory.fluid;

import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.IMatterHandler;
import net.ds.trigamma.inventory.fluid.MatterCapabilities;
import net.ds.trigamma.inventory.fluid.MatterPhase;
import net.ds.trigamma.inventory.fluid.PipeMatterTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class MatterNetwork {
    private static final int TRANSFER_PER_ENDPOINT_PER_TICK = 250;

    private final List<BlockPos> ducts;
    private final List<Endpoint> endpoints;
    private final BlockPos leader;
    private final int capacity;

    public MatterNetwork(List<BlockPos> ducts, List<Endpoint> endpoints) {
        this.ducts = List.copyOf(ducts);
        this.endpoints = List.copyOf(endpoints);
        this.leader = this.ducts.stream()
                .min(MatterNetwork::compareCoordinates)
                .orElse(BlockPos.ZERO)
                .immutable();
        this.capacity = this.ducts.size() * UniversalMatterDuctBlockEntity.BUCKET_CAPACITY;
    }

    public static MatterNetwork empty(BlockPos fallbackLeader) {
        return new MatterNetwork(List.of(fallbackLeader.immutable()), List.of());
    }

    public List<BlockPos> ducts() {
        return ducts;
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    public BlockPos leader() {
        return leader;
    }

    public int capacity() {
        return capacity;
    }

    public void tick(Level level) {
        IMatter matter = chooseMatter(level).orElse(null);
        if (matter == null) {
            return;
        }

        List<BlockPos> compatibleDucts = compatibleDucts(level, matter);
        int compatibleCapacity = compatibleDucts.size() * UniversalMatterDuctBlockEntity.BUCKET_CAPACITY;
        int stored = collectDuctMatter(level, compatibleDucts, matter);
        int spare = Math.max(0, compatibleCapacity - stored);
        Set<Endpoint> drainedEndpoints = new HashSet<>();

        if (spare > 0) {
            PullResult pulled = pullFromEndpoints(level, matter, spare);
            stored += pulled.amount();
            drainedEndpoints.addAll(pulled.endpoints());
        }

        if (stored > 0) {
            int pushed = pushToEndpoints(level, matter, stored, drainedEndpoints);
            stored -= pushed;
        }

        distributeToDucts(level, compatibleDucts, matter, stored);
    }

    private Optional<IMatter> chooseMatter(Level level) {
        IMatter best = null;
        int bestAmount = 0;

        for (BlockPos pos : ducts) {
            UniversalMatterDuctBlockEntity duct = getDuct(level, pos);
            if (duct == null) continue;

            PipeMatterTank tank = duct.getTank();
            Optional<IMatter> tankMatter = tank.getCurrentMatter();
            if (tankMatter.isPresent() && tank.getAmount() > bestAmount) {
                best = tankMatter.get();
                bestAmount = tank.getAmount();
            }
        }

        if (best != null) {
            return Optional.of(best);
        }

        for (BlockPos pos : ducts) {
            UniversalMatterDuctBlockEntity duct = getDuct(level, pos);
            if (duct == null) continue;

            Optional<IMatter> filterMatter = duct.getFilterMatter();
            if (filterMatter.isPresent()) {
                return filterMatter;
            }
        }

        for (Endpoint endpoint : endpoints) {
            IMatterHandler handler = endpoint.resolve(level);
            if (handler == null || handler.getAmount() <= 0) continue;

            Optional<IMatter> endpointMatter = handler.getCurrentMatter();
            if (endpointMatter.isPresent()) {
                return endpointMatter;
            }
        }

        return Optional.empty();
    }

    private List<BlockPos> compatibleDucts(Level level, IMatter matter) {
        List<BlockPos> compatible = new ArrayList<>();
        ResourceLocation id = matter.id();

        for (BlockPos pos : ducts) {
            UniversalMatterDuctBlockEntity duct = getDuct(level, pos);
            if (duct == null) continue;

            Optional<IMatter> tankMatter = duct.getTank().getCurrentMatter();
            if (tankMatter.isEmpty() || tankMatter.get().id().equals(id)) {
                compatible.add(pos);
            }
        }

        return compatible;
    }

    private int collectDuctMatter(Level level, List<BlockPos> ductPositions, IMatter matter) {
        int total = 0;
        ResourceLocation id = matter.id();

        for (BlockPos pos : ductPositions) {
            UniversalMatterDuctBlockEntity duct = getDuct(level, pos);
            if (duct == null) continue;

            PipeMatterTank tank = duct.getTank();
            Optional<IMatter> tankMatter = tank.getCurrentMatter();
            if (tankMatter.isPresent() && tankMatter.get().id().equals(id)) {
                total += tank.getAmount();
            }
        }

        return total;
    }

    private PullResult pullFromEndpoints(Level level, IMatter matter, int maxAmount) {
        int pulled = 0;
        Set<Endpoint> drainedEndpoints = new HashSet<>();

        for (Endpoint endpoint : endpoints) {
            if (pulled >= maxAmount) break;

            IMatterHandler handler = endpoint.resolve(level);
            if (handler == null || handler.getAmount() <= 0) continue;

            Optional<IMatter> endpointMatter = handler.getCurrentMatter();
            if (endpointMatter.isEmpty() || !endpointMatter.get().id().equals(matter.id())) {
                continue;
            }

            int request = Math.min(TRANSFER_PER_ENDPOINT_PER_TICK, maxAmount - pulled);
            int drained = handler.drain(request, false);
            if (drained > 0) {
                pulled += drained;
                drainedEndpoints.add(endpoint);
                markChanged(level, endpoint.pos());
            }
        }

        return new PullResult(pulled, drainedEndpoints);
    }

    private int pushToEndpoints(Level level, IMatter matter, int maxAmount, Set<Endpoint> skip) {
        int pushed = 0;

        for (Endpoint endpoint : endpoints) {
            if (pushed >= maxAmount) break;
            if (skip.contains(endpoint)) continue;

            IMatterHandler handler = endpoint.resolve(level);
            if (handler == null) continue;

            int request = Math.min(TRANSFER_PER_ENDPOINT_PER_TICK, maxAmount - pushed);
            int accepted = handler.fill(matter, request, false);
            if (accepted > 0) {
                pushed += accepted;
                markChanged(level, endpoint.pos());
            }
        }

        return pushed;
    }

    private void distributeToDucts(Level level, List<BlockPos> ductPositions, IMatter matter, int amount) {
        List<BlockPos> sortedDucts = new ArrayList<>(ductPositions);
        sortedDucts.sort(distributionOrder(matter.phase()));

        int remaining = Math.min(amount, ductPositions.size() * UniversalMatterDuctBlockEntity.BUCKET_CAPACITY);

        for (BlockPos pos : sortedDucts) {
            UniversalMatterDuctBlockEntity duct = getDuct(level, pos);
            if (duct == null) continue;

            PipeMatterTank tank = duct.getTank();
            Optional<IMatter> beforeMatter = tank.getCurrentMatter();
            int beforeAmount = tank.getAmount();
            int targetAmount = Math.min(UniversalMatterDuctBlockEntity.BUCKET_CAPACITY, remaining);

            setTankContents(tank, matter, targetAmount);
            remaining -= targetAmount;

            Optional<IMatter> afterMatter = tank.getCurrentMatter();
            if (beforeAmount != tank.getAmount() || !sameMatter(beforeMatter, afterMatter)) {
                duct.setChanged();
            }
        }
    }

    private static void setTankContents(PipeMatterTank tank, IMatter matter, int amount) {
        tank.drain(tank.getCapacity(), false);
        if (amount > 0) {
            tank.fill(matter, amount, false);
        }
    }

    private static boolean sameMatter(Optional<IMatter> left, Optional<IMatter> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() == right.isEmpty();
        }
        return left.get().id().equals(right.get().id());
    }

    private static UniversalMatterDuctBlockEntity getDuct(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof UniversalMatterDuctBlockEntity duct) {
            return duct;
        }
        return null;
    }

    private static void markChanged(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null) {
            entity.setChanged();
        }
    }

    private static Comparator<BlockPos> distributionOrder(MatterPhase phase) {
        Comparator<BlockPos> vertical = phase == MatterPhase.GAS
                ? Comparator.<BlockPos>comparingInt(pos -> pos.getY()).reversed()
                : Comparator.comparingInt(pos -> pos.getY());

        return vertical
                .thenComparingInt(pos -> pos.getX())
                .thenComparingInt(pos -> pos.getZ());
    }

    private static int compareCoordinates(BlockPos left, BlockPos right) {
        int x = Integer.compare(left.getX(), right.getX());
        if (x != 0) return x;

        int y = Integer.compare(left.getY(), right.getY());
        if (y != 0) return y;

        return Integer.compare(left.getZ(), right.getZ());
    }

    public record Endpoint(BlockPos pos, Direction side) {
        public IMatterHandler resolve(Level level) {
            return level.getCapability(MatterCapabilities.MATTER_HANDLER, pos, side);
        }
    }

    private record PullResult(int amount, Set<Endpoint> endpoints) {
    }
}
