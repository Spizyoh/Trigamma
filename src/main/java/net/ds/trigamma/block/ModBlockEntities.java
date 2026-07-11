package net.ds.trigamma.block;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.block.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TriGamma.MODID); // <-- your modid

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressBlockEntity>> PRESS =
            BLOCK_ENTITIES.register("metalworking_press",
                    () -> BlockEntityType.Builder.of(PressBlockEntity::new, ModBlocks.METALWORKING_PRESS.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UniversalMatterDuctBlockEntity>> UNIVERSAL_MATTER_DUCT =
            BLOCK_ENTITIES.register("universal_matter_duct", () ->
                    BlockEntityType.Builder.of(UniversalMatterDuctBlockEntity::new,
                            ModBlocks.UNIVERSAL_MATTER_DUCT.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER =
            BLOCK_ENTITIES.register("boiler", () ->
                    BlockEntityType.Builder.of(BoilerBlockEntity::new,
                            ModBlocks.BOILER.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK =
            BLOCK_ENTITIES.register("tank", () ->
                    BlockEntityType.Builder.of(TankBlockEntity::new,
                            ModBlocks.TANK.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankShellBlockEntity>> TANK_SHELL =
            BLOCK_ENTITIES.register("tank_shell", () ->
                    BlockEntityType.Builder.of(TankShellBlockEntity::new,
                            ModBlocks.TANK_SHELL.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerShellBlockEntity>> BOILER_SHELL =
            BLOCK_ENTITIES.register("boiler_shell", () ->
                    BlockEntityType.Builder.of(BoilerShellBlockEntity::new,
                            ModBlocks.BOILER_SHELL.get()).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachinePortBlockEntity>> MACHINE_PORT =
            BLOCK_ENTITIES.register("machine_port", () ->
                    BlockEntityType.Builder.of(MachinePortBlockEntity::new,
                            ModBlocks.MACHINE_PORT.get()).build(null)
            );

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
