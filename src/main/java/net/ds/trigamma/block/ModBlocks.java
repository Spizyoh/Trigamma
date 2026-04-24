package net.ds.trigamma.block;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TriGamma.MODID);

    public static final DeferredBlock<Block> TITANIUM_BLOCK = registerBlock("titanium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0F, 4.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));

    public static final DeferredBlock<Block> LEAD_ORE = registerBlock("lead_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4.0F, 4.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.COLOR_BLUE)));

    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE = registerBlock("deepslate_lead_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(6.5F, 4.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)
                    .mapColor(MapColor.COLOR_BLUE)));
    public static final DeferredBlock<Block> RAW_LEAD_BLOCK = registerBlock("raw_lead_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(7F, 7.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .mapColor(MapColor.COLOR_BLUE)));

    public static final DeferredBlock<RotatedPillarBlock> INSULATION_BLOCK = ModBlocks.<RotatedPillarBlock>registerBlock("insulation_block",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F, 0.9F)
                    .sound(SoundType.WOOL)
                    .mapColor(MapColor.COLOR_BLACK)));

    public static final DeferredBlock<Block> NATURAL_URANIUM_BLOCK = registerBlock("natural_uranium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(6.0F, 4.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name,  () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
