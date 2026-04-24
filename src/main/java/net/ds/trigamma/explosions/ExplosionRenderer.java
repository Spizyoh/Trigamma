package net.ds.trigamma.explosions;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(value = Dist.CLIENT)
public class ExplosionRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        // only draw during this specific stage
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        // get all the tools we need
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance()
                .renderBuffers().bufferSource();

        // loop through all active effects and render each one
        for (ExplosionRenderData effect : ExplosionEffectManager.activeEffects) {
            renderEffect(poseStack, bufferSource, camera, effect);
        }

        // flush everything to the GPU
        bufferSource.endBatch();
    }

    private static void renderEffect(PoseStack poseStack, MultiBufferSource bufferSource,
                                     Camera camera, ExplosionRenderData effect) {

        poseStack.pushPose(); // always save first

        // move origin to explosion center
        Vec3 cam = camera.getPosition();
        poseStack.translate(effect.x - cam.x, effect.y - cam.y, effect.z - cam.z);

        // get pencil
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        // draw your shape here using consumer...

        poseStack.popPose(); // always restore after
    }
}
