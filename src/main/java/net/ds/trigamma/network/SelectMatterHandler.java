package net.ds.trigamma.network;

import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.MatterRegistry;
import net.ds.trigamma.item.IdentifierTabletItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public class SelectMatterHandler {
    public static void handle(SelectMatterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof IdentifierTabletItem)) return;

            Optional<IMatter> matterOpt = MatterRegistry.get(payload.matterId());
            if (matterOpt.isEmpty()) return;
            IMatter matter = matterOpt.get();

            stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData -> customData.update(tag ->
                    tag.putString("SelectedMatter", payload.matterId().toString())
            ));

            player.sendSystemMessage(Component.translatable("message.trigamma.tablet_mode_set",
                    Component.translatable(matter.translationKey())));
        });
    }
}