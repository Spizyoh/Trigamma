package net.ds.trigamma.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ds.trigamma.TriGamma;
import net.ds.trigamma.block.entity.TankBlockEntity;
import net.ds.trigamma.inventory.fluid.IMatter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class TankBlockEntityRenderer implements BlockEntityRenderer<TankBlockEntity> {
    public TankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            TankBlockEntity tank,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ResourceLocation liquidTexture = getLiquidTexture(tank);
        ResourceLocation labelTexture = getLabelTexture(tank);

        renderDynamicFace(
                poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucent(liquidTexture)),
                packedLight,
                packedOverlay,
                0.25f, 0.15f, 0.01f,
                0.75f, 0.85f, 0.01f
        );

        renderDynamicFace(
                poseStack,
                bufferSource.getBuffer(RenderType.entityCutout(labelTexture)),
                packedLight,
                packedOverlay,
                0.30f, 0.88f, 0.00f,
                0.70f, 0.98f, 0.00f
        );
    }

    private ResourceLocation getLiquidTexture(TankBlockEntity tank) {
        return tank.getLockedMatter()
                .map(IMatter::id)
                .map(id -> ResourceLocation.fromNamespaceAndPath(
                        TriGamma.MODID,
                        "textures/block/tank/liquid/" + id.getPath() + ".png"
                ))
                .orElse(ResourceLocation.fromNamespaceAndPath(
                        TriGamma.MODID,
                        "textures/block/tank/liquid/empty.png"
                ));
    }

    private ResourceLocation getLabelTexture(TankBlockEntity tank) {
        return tank.getLockedMatter()
                .map(IMatter::id)
                .map(id -> ResourceLocation.fromNamespaceAndPath(
                        TriGamma.MODID,
                        "textures/block/tank/label/" + id.getPath() + ".png"
                ))
                .orElse(ResourceLocation.fromNamespaceAndPath(
                        TriGamma.MODID,
                        "textures/block/tank/label/empty.png"
                ));
    }

    private void renderDynamicFace(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float minX,
            float minY,
            float z,
            float maxX,
            float maxY,
            float ignoredZ
    ) {
        Matrix4f matrix = poseStack.last().pose();

        vertex(consumer, matrix, minX, minY, z, 0, 1, packedLight, packedOverlay);
        vertex(consumer, matrix, maxX, minY, z, 1, 1, packedLight, packedOverlay);
        vertex(consumer, matrix, maxX, maxY, z, 1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, minX, maxY, z, 0, 0, packedLight, packedOverlay);
    }

    private void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight,
            int packedOverlay
    ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(0, 0, -1);
    }
}