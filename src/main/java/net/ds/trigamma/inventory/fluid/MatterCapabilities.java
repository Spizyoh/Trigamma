package net.ds.trigamma.inventory.fluid;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public class MatterCapabilities {
    public static final BlockCapability<PipeMatterTank, @Nullable Direction> MATTER_HANDLER =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath("trigamma", "matter_handler"),
                    PipeMatterTank.class
            );
}