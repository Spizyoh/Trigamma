package net.ds.trigamma.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ds.trigamma.block.entity.PressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PressBlockEntityRenderer implements BlockEntityRenderer<PressBlockEntity> {
    private final ItemRenderer itemRenderer;

    public PressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PressBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

        // Prioritize displaying the output stack. If empty, show the input ingot.
        ItemStack stackToRender = !blockEntity.getOutputStack().isEmpty()
                ? blockEntity.getOutputStack()
                : blockEntity.getIngotStack();

        if (stackToRender.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // 1. Move to the center of the block horizontally, and slightly above the top face vertically (y = 1.0)
        // Adjust the Y coordinate (e.g., 0.5 or 1.0) depending on your lower block model's exact height.
        poseStack.translate(0.5D, 1.01D, 0.5D);

        // 2. Rotate the item flat onto its back (90 degrees around X-axis)
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // 3. Optional: rotate it slightly if you want a dynamic, loose look like Create Mod
        // poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));

        // 4. Scale down the item so it looks natural on the depot surface
        poseStack.scale(0.6F, 0.6F, 0.6F);

        // 5. Render the item using the fixed context
        BakedModel bakedModel = this.itemRenderer.getModel(stackToRender, blockEntity.getLevel(), null, 0);
        this.itemRenderer.render(
                stackToRender,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                bufferSource,
                combinedLight,
                combinedOverlay,
                bakedModel
        );

        poseStack.popPose();
    }
}