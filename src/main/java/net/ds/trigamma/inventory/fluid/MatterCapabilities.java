package net.ds.trigamma.inventory.fluid;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public class MatterCapabilities {
    // NOTE: this used to be typed to the concrete PipeMatterTank class. It's now typed to
    // the IMatterHandler interface so that anything - pipes, machine ports, tanks - can
    // expose this capability without being a literal PipeMatterTank.
    public static final BlockCapability<IMatterHandler, @Nullable Direction> MATTER_HANDLER =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath("trigamma", "matter_handler"),
                    IMatterHandler.class
            );
}