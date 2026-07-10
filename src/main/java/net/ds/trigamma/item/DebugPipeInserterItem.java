package net.ds.trigamma.item;

import net.ds.trigamma.block.entity.UniversalMatterDuctBlockEntity;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.MatterRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class DebugPipeInserterItem extends Item {

    public DebugPipeInserterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(clickedPos);

        if (be instanceof UniversalMatterDuctBlockEntity duct) {
            // 1. Dynamically read what the pipe is assigned to (Filter first, then active contents)
            Optional<IMatter> activeMatterOpt = duct.getFilterMatter();
            IMatter targetMatter;

            if (activeMatterOpt.isPresent()) {
                targetMatter = activeMatterOpt.get();
            } else {
                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(Component.literal(
                            "§cThis pipe is empty/unfiltered, please set a filter!"
                    ));
                }
                return InteractionResult.FAIL;
            }

            // 2. Execute the fill transaction on the duct's tank
            int accepted = duct.getTank().fill(targetMatter, 1000, false);

            if (accepted > 0) {
                // Ensure the structural filter updates to lock the choice in place
                duct.setFilterMatter(targetMatter);

                // Triggers structural data saving and immediate block updates for client rendering
                duct.setChanged();

                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(Component.literal(
                            "§a[Debug] Added " + accepted + "mB of " + targetMatter.id().getPath() + " (" + targetMatter.phase() + ")"
                    ));
                }
                return InteractionResult.CONSUME;
            } else {
                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(Component.literal(
                            "§c[Debug] Unable to insert. The pipe tank is already completely full ("
                                    + duct.getTank().getAmount() + "/" + duct.getTank().getCapacity() + "mB)."
                    ));
                }
            }
        }

        return InteractionResult.PASS;
    }
}