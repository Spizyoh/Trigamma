package net.ds.trigamma.inventory.fluid;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MatterRegistry {
    private static final Map<ResourceLocation, IMatter> REGISTRY = new HashMap<>();

    // --- Examples of Custom Liquids ---
    public static final FluidData WATER = register(new FluidData(
            ResourceLocation.fromNamespaceAndPath("trigamma", "water"),
            Set.of(),
            0x2424BF, // Soft Blue Tint
            1000
    ));

    public static final FluidData HEAVY_WATER = register(new FluidData(
            ResourceLocation.fromNamespaceAndPath("trigamma", "heavy_water"),
            Set.of(PropertyTag.COOLANT),
            0x00C1D5, // Soft Blue Tint
            1200
    ));

    public static final FluidData SULFURIC_ACID = register(new FluidData(
            ResourceLocation.fromNamespaceAndPath("trigamma", "sulfuric_acid"),
            Set.of(PropertyTag.CORROSIVE, PropertyTag.TOXIC),
            0x959055, // Acid Green
            1000
    ));

    // --- Examples of Custom Gases ---
    public static final GasData RADON_GAS = register(new GasData(
            ResourceLocation.fromNamespaceAndPath("trigamma", "radon_gas"),
            Set.of(PropertyTag.RADIOACTIVE, PropertyTag.TOXIC),
            0xCEAEFA, // Radioactive Purple
            0.5f
    ));

    public static final GasData HYDROGEN_GAS = register(new GasData(
            ResourceLocation.fromNamespaceAndPath("trigamma", "hydrogen_gas"),
            Set.of(PropertyTag.FLAMMABLE, PropertyTag.COMBUSTIBLE),
            0xCEE5ED, // Ghostly White/Gray
            1.2f
    ));

    private static <T extends IMatter> T register(T matter) {
        REGISTRY.put(matter.id(), matter);
        return matter;
    }

    public static Optional<IMatter> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Map<ResourceLocation, IMatter> getAllMatter() {
        return REGISTRY;
    }
}