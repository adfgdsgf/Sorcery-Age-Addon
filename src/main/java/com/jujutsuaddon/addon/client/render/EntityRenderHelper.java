package com.jujutsuaddon.addon.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体渲染工具类 - 用于在 GUI 中渲染实体预览
 */
public class EntityRenderHelper {

    private static final Map<EntityType<?>, Entity> ENTITY_CACHE = new HashMap<>();

    // ==================== JJK 模组检测 ====================

    private static final String JJK_NAMESPACE = "jujutsu_kaisen";

    /**
     * 检查实体类型是否来自 JJK 模组
     */
    public static boolean isJJKEntity(EntityType<?> entityType) {
        if (entityType == null) return false;
        ResourceLocation key = EntityType.getKey(entityType);
        return key != null && JJK_NAMESPACE.equals(key.getNamespace());
    }

    // ==================== 实体缓存 ====================

    /**
     * 获取或创建实体实例用于渲染
     */
    @Nullable
    public static Entity getOrCreateEntity(EntityType<?> entityType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        return ENTITY_CACHE.computeIfAbsent(entityType, type -> {
            try {
                return type.create(mc.level);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * 清除实体缓存
     */
    public static void clearCache() {
        ENTITY_CACHE.clear();
    }

    // ==================== 实体渲染 ====================

    /**
     * 在 GUI 中渲染实体
     *
     * @param poseStack 渲染矩阵
     * @param x         中心 X 坐标
     * @param y         底部 Y 坐标
     * @param scale     缩放
     * @param angleX    水平旋转角度
     * @param angleY    垂直旋转角度
     * @param entity    要渲染的实体
     */
    public static void renderEntityInGui(PoseStack poseStack,
                                         int x, int y, int scale,
                                         float angleX, float angleY,
                                         Entity entity) {
        if (entity == null) return;

        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float) Math.PI);
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(angleY * 20.0F * ((float) Math.PI / 180.0F));
        quaternionf.mul(quaternionf1);

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = 180.0F + angleX * 20.0F;
        }
        entity.setYRot(180.0F + angleX * 40.0F);
        entity.setXRot(-angleY * 20.0F);

        if (entity instanceof LivingEntity living) {
            living.yHeadRot = entity.getYRot();
            living.yHeadRotO = entity.getYRot();
        }

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(0.0D, 0.0D, 1000.0D);
        RenderSystem.applyModelViewMatrix();

        poseStack.pushPose();
        poseStack.translate(x, y, -950.0D);
        poseStack.mulPoseMatrix((new Matrix4f()).scaling((float) scale, (float) scale, (float) (-scale)));
        poseStack.mulPose(quaternionf);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternionf1.conjugate();
        dispatcher.overrideCameraOrientation(quaternionf1);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.runAsFancy(() ->
                dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, buffer, 15728880));

        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        poseStack.popPose();
        Lighting.setupFor3DItems();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * 计算实体在格子中的合适缩放
     */
    public static int calculateEntityScale(Entity entity, int cellSize) {
        if (entity == null) return cellSize / 5;

        float entityHeight = entity.getBbHeight();
        float entityWidth = entity.getBbWidth();
        float maxDim = Math.max(entityHeight, entityWidth);

        return (int) Math.max(2, Math.min(cellSize / 5.0f, (cellSize * 0.35f) / maxDim));
    }
}
