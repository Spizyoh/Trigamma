// net/ds/trigamma/client/BufferHudOverlay.java
package net.ds.trigamma.client;

import net.ds.trigamma.block.entity.BoilerShellBlockEntity;
import net.ds.trigamma.block.entity.IMatterBufferHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = net.neoforged.api.distmarker.Dist.CLIENT)
public class BufferHudOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
        IMatterBufferHolder holder = null;
        if (be instanceof IMatterBufferHolder h) {
            holder = h;
        } else if (be instanceof BoilerShellBlockEntity shell && shell.getMasterPos() != null) {
            if (mc.level.getBlockEntity(shell.getMasterPos()) instanceof IMatterBufferHolder h) {
                holder = h;
            }
        }
        if (holder == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int startY = mc.getWindow().getGuiScaledHeight() / 2 - 40;

        int lineY = startY;
        for (IMatterBufferHolder.BufferSlot slot : holder.getDisplayBuffers()) {
            System.out.println(
                    "CLIENT BUFFER: " + slot.buffer().getAmount()
            );

            String matterName = slot.buffer().getMatter()
                    .map(m -> Component.translatable(m.translationKey()).getString())
                    .orElse(Component.translatable("buffer.trigamma.empty").getString());

            Component line = Component.translatable(
                    "buffer.trigamma.line",
                    slot.label(),
                    slot.buffer().getAmount(),
                    slot.buffer().getCapacity(),
                    matterName
            );

            graphics.drawCenteredString(mc.font, line, centerX, lineY, 0xFFFFFF);
            lineY += mc.font.lineHeight + 2;
        }
    }
}