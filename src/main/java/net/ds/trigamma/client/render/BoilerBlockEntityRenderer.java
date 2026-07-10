// net/ds/trigamma/client/renderer/BoilerBlockEntityRenderer.java
package net.ds.trigamma.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ds.trigamma.block.entity.BoilerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * @deprecated The boiler now renders through its normal block model.
 */
@Deprecated(since = "0.0.1", forRemoval = true)
public class BoilerBlockEntityRenderer implements BlockEntityRenderer<BoilerBlockEntity> {

    // Model registered separately (e.g. via a plain block/item model json, or loaded
    // as a standalone model — see note below).
    private static final ModelResourceLocation BOILER_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("trigamma", "block/boiler")
    );

    public BoilerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BoilerBlockEntity boiler, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // The boiler occupies 3x3 horizontally and 4 vertically, centered on the
        // placed block's X/Z. Adjust these offsets to match your model's actual pivot.
        poseStack.translate(-1.0, 0.0, -1.0);

        var model = Minecraft.getInstance().getModelManager().getModel(BOILER_MODEL);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), buffer.getBuffer(net.minecraft.client.renderer.RenderType.solid()),
                null, model, 1f, 1f, 1f, packedLight, packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BoilerBlockEntity boiler) {
        BlockPos pos = boiler.getBlockPos();
        // Inflate the culling box to the full 3x3x4 visual footprint so the
        // renderer doesn't get frustum-culled when the camera is near the edge
        // but the anchor block itself is offscreen.
        return new AABB(
                pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 4, pos.getZ() + 2
        );
    }
}