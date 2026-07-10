package net.ds.trigamma;

import net.ds.trigamma.block.ModBlockEntities;
import net.ds.trigamma.block.ModBlocks;
import net.ds.trigamma.block.entity.MachinePortBlockEntity;
import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.data.CraftingRecipeProvider;
import net.ds.trigamma.inventory.ModMenus;
import net.ds.trigamma.inventory.fluid.MatterCapabilities;
import net.ds.trigamma.inventory.recipes.ModRecipes;
import net.ds.trigamma.item.ModCreativeModeTabs;
import net.ds.trigamma.client.ClientPayloadHandler;
import net.ds.trigamma.command.EntityRadCommand;
import net.ds.trigamma.item.ModItems;
import net.ds.trigamma.item.RadioactiveItem;
import net.ds.trigamma.network.SelectMatterHandler;
import net.ds.trigamma.network.SelectMatterPayload;
import net.ds.trigamma.particle.ModParticles;
import net.ds.trigamma.radiation.*;
import net.ds.trigamma.sound.ModSounds;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TriGamma.MODID)
public class TriGamma {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "trigamma";
    public static final Logger LOGGER = LogUtils.getLogger();
    public TriGamma(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModSounds.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        modEventBus.addListener(TriGamma::registerCapabilities);

        ModMenus.register(modEventBus);

        // ── Radiation: DataAttachment types ──────────────────────────────────
        MobRadiationCapability.MOB_RADIATION.getClass(); // force static init
        RadiationCapability.ATTACHMENT_TYPES.register(modEventBus);

        // ── Network packets ────────────────────────────────────────
        modEventBus.addListener(TriGamma::registerPackets);

        // ── Radiation: Game events (server tick, clone, etc.) ─────────────────
        NeoForge.EVENT_BUS.register(new RadiationEvents());
        NeoForge.EVENT_BUS.register(new RadiationItemEvents());
        NeoForge.EVENT_BUS.register(new RadiationMobEvents());

        NeoForge.EVENT_BUS.register(new VomitEvents());

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0");

        registrar.playToClient(
                RadiationSyncPacket.TYPE,
                RadiationSyncPacket.STREAM_CODEC,
                ClientPayloadHandler::handleData // Direct link to client-only code
        );

        // 2. Add our new Anvil Crafting packet right here! (Client -> Server)
        registrar.playToServer(
                net.ds.trigamma.network.CraftAnvilPayload.TYPE,
                net.ds.trigamma.network.CraftAnvilPayload.CODEC,
                net.ds.trigamma.network.CraftAnvilPayload::handleServer
        );

        registrar.playToServer(
                SelectMatterPayload.TYPE,
                SelectMatterPayload.STREAM_CODEC,
                SelectMatterHandler::handle
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        //something goes here i think

        // Vanilla Radioactive Items
        RadioactiveItemRegistry.register(Items.ANCIENT_DEBRIS, 1f);
        RadioactiveItemRegistry.register(Items.GLOWSTONE_DUST, 0.025f);

        // Radioactive Items from other Mods

        // Modded Radioactive Items
        RadioactiveItemRegistry.register(ModBlocks.NATURAL_URANIUM_BLOCK.asItem(), 1.25f);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                MatterCapabilities.MATTER_HANDLER,
                UniversalMatterDuctBlockEntity.TYPE.get(),
                (blockEntity, direction) -> blockEntity.getTank() // Give out our engine tank references
        );

        event.registerBlockEntity(
                MatterCapabilities.MATTER_HANDLER,
                MachinePortBlockEntity.TYPE.get(),
                (blockEntity, direction) -> blockEntity
        );
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EntityRadCommand.register(event.getDispatcher());
    }

    // Keep it non-static so it doesn't cause instance registration issues!
    private void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();

        generator.addProvider(
                event.includeServer(),
                new CraftingRecipeProvider(generator.getPackOutput(), event.getLookupProvider())
        );
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        //The creative tab im pretty sure
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Does something when the server starts i think
    }
}
