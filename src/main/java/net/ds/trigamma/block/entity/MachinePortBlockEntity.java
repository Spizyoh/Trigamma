package net.ds.trigamma.block.entity;

import net.ds.trigamma.block.ModBlockEntities;
import net.ds.trigamma.block.port.*;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.IMatterBuffer;
import net.ds.trigamma.inventory.fluid.IMatterHandler;
import net.ds.trigamma.inventory.fluid.MatterPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Reusable, invisible "port" block entity. A single instance of this can stand in for a
 * matter intake or outlet on any multiblock/pseudo-multiblock, as long as that machine's
 * master BlockEntity implements {@link IPortableMachine}.
 * <p>
 * The port itself holds no matter - it's a thin, configured adapter that forwards
 * fill()/drain() calls to whichever buffer its master exposes for its PortIO direction.
 * This is what lets Universal Matter Ducts plug directly into a machine's internal
 * buffers without the machine needing any duct-specific code of its own.
 * <p>
 * TODO: Pneumatic Tubes don't exist yet. When they do, this class (or a sibling) needs to
 * also implement whatever IPneumaticHandler capability they use, gated on
 * {@code portKind == PortKind.PNEUMATIC}.
 */
public class MachinePortBlockEntity extends BlockEntity implements IMatterHandler {

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<MachinePortBlockEntity>> TYPE =
            ModBlockEntities.MACHINE_PORT;

    @Nullable
    private BlockPos masterPos;

    public MachinePortBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
    }

    /**
     * The placement configuration now only needs to link the structural master position.
     */
    public void configure(BlockPos masterPos) {
        this.masterPos = masterPos;
        setChanged();
    }

    @Nullable
    public BlockPos getMasterPos() {
        return masterPos;
    }

    private Optional<IPortableMachine> getMasterMachine() {
        if (masterPos == null || level == null) return Optional.empty();
        if (level.getBlockEntity(masterPos) instanceof IPortableMachine machine) {
            return Optional.of(machine);
        }
        return Optional.empty();
    }

    // --- IMatterHandler Capabilities (Driven Dynamically) ---

    @Override
    public int fill(IMatter resource, int amount, boolean simulate) {
        if (resource == null || amount <= 0) return 0;

        var masterOpt = getMasterMachine();
        if (masterOpt.isEmpty()) return 0;
        IPortableMachine machine = masterOpt.get();

        DynamicPortConfig config = machine.getPortConfig(this.worldPosition);

        System.out.println("Port " + worldPosition);
        System.out.println("Config: " + config);
        System.out.println("Matter: " + resource.id());

        if (config.kind() != PortKind.MATTER) {
            System.out.println("Rejected: wrong kind");
            return 0;
        }

        if (config.io() != PortIO.INPUT) {
            System.out.println("Rejected: wrong IO");
            return 0;
        }

        if (config.phaseFilter() != null &&
                resource.phase() != config.phaseFilter()) {

            System.out.println("Rejected: wrong phase");
            return 0;
        }

        // 2. Fetch the corresponding buffer mapped by the machine
        Optional<IMatterBuffer> bufferOpt = machine.getBufferForPort(this.worldPosition, config);
        if (bufferOpt.isEmpty()) return 0;
        IMatterBuffer buffer = bufferOpt.get();

        if (buffer.getMatter().isPresent() && !buffer.getMatter().get().id().equals(resource.id())) {
            return 0;
        }

        int space = Math.min(amount, buffer.getCapacity() - buffer.getAmount());
        if (space <= 0) return 0;

        if (!simulate) {
            int accepted = buffer.fill(resource, space);

            if (accepted > 0 && machine instanceof BoilerBlockEntity boiler) {
                boiler.sync();
            }

            setChanged();
            return accepted;
        }
        return space;
    }

    @Override
    public int drain(int amount, boolean simulate) {
        if (amount <= 0) return 0;

        var masterOpt = getMasterMachine();
        if (masterOpt.isEmpty()) return 0;
        IPortableMachine machine = masterOpt.get();

        DynamicPortConfig config = machine.getPortConfig(this.worldPosition);
        if (config.kind() != PortKind.MATTER || config.io() != PortIO.OUTPUT) return 0;

        Optional<IMatterBuffer> bufferOpt = machine.getBufferForPort(this.worldPosition, config);
        if (bufferOpt.isEmpty()) return 0;
        IMatterBuffer buffer = bufferOpt.get();
        if (buffer.isEmpty()) return 0;

        int drained = Math.min(amount, buffer.getAmount());
        if (!simulate && drained > 0) {
            buffer.drain(drained);
            setChanged();
        }
        return drained;
    }

    public Optional<IMatterBuffer> getBuffer() {
        return getMasterMachine()
                .flatMap(machine ->
                        machine.getBufferForPort(
                                this.worldPosition,
                                machine.getPortConfig(this.worldPosition)
                        )
                );
    }

    @Override
    public Optional<IMatter> getCurrentMatter() {
        return getBuffer().flatMap(IMatterBuffer::getMatter);
    }

    @Override
    public MatterPhase getPhase() {
        return getCurrentMatter()
                .map(IMatter::phase)
                .orElse(MatterPhase.EMPTY);
    }

    @Override
    public int getAmount() {
        return getBuffer().map(IMatterBuffer::getAmount).orElse(0);
    }

    @Override
    public int getCapacity() {
        return getBuffer().map(IMatterBuffer::getCapacity).orElse(0);
    }

    // --- Persistence ---

    // --- Cleaned up Persistence ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (masterPos != null) tag.putLong("MasterPos", masterPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("MasterPos")) masterPos = BlockPos.of(tag.getLong("MasterPos"));
    }
}