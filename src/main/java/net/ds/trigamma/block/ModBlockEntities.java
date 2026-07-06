package net.ds.trigamma.block;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.block.entity.PressBlockEntity;
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

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
