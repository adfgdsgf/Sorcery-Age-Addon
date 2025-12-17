package com.jujutsuaddon.addon.client.autoaim;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, value = Dist.CLIENT)
public class AimAssistRenderer {

    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (AddonClientConfig.CLIENT == null) return;
        if (!AddonClientConfig.CLIENT.aimAssistEnabled.get()) return;
        if (!AimAssist.isEnabled()) return;

        LivingEntity target = AimAssist.getCurrentTarget();
        if (target == null || !target.isAlive()) return;

        AddonClientConfig.Client config = AddonClientConfig.CLIENT;

        if (!config.aimAssistShowIndicator.get() && !config.aimAssistGlowingTarget.get()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        // 获取目标位置（插值平滑）
        float partialTick = event.getPartialTick();
        double x = target.xOld + (target.getX() - target.xOld) * partialTick;
        double y = target.yOld + (target.getY() - target.yOld) * partialTick;
        double z = target.zOld + (target.getZ() - target.zOld) * partialTick;

        // 解析颜色
        int color = parseColor(config.aimAssistGlowColor.get());
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.6f;

        // 渲染目标高亮框
        if (config.aimAssistGlowingTarget.get()) {
            AABB box = target.getBoundingBox().move(-target.getX() + x, -target.getY() + y, -target.getZ() + z);
            renderOutlineBox(poseStack, box, r, g, b, a);
        }

        // 渲染锁定指示器
        if (config.aimAssistShowIndicator.get()) {
            renderLockIndicator(poseStack, x, y + target.getBbHeight() + 0.5, z, r, g, b);
        }

        poseStack.popPose();
    }

    private static void renderOutlineBox(PoseStack poseStack, AABB box, float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // 底面
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();

        // 顶面
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();

        // 竖边
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();

        tesselator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderLockIndicator(PoseStack poseStack, double x, double y, double z, float r, float g, float b) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // 始终面向玩家
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());

        // 旋转动画
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float rotation = time * 360.0f;
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));

        Matrix4f matrix = poseStack.last().pose();
        float size = 0.15f;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // 菱形四个顶点
        buffer.vertex(matrix, 0, size * 1.5f, 0).color(r, g, b, 1.0f).endVertex();
        buffer.vertex(matrix, -size, 0, 0).color(r, g, b, 0.8f).endVertex();
        buffer.vertex(matrix, 0, -size * 1.5f, 0).color(r, g, b, 1.0f).endVertex();
        buffer.vertex(matrix, size, 0, 0).color(r, g, b, 0.8f).endVertex();

        tesselator.end();

        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFF5555; // 默认红色
        }
    }
}
