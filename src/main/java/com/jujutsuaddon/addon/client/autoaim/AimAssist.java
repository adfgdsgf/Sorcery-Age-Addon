package com.jujutsuaddon.addon.client.autoaim;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AimAssist {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final TargetFinder finder = new TargetFinder();

    private static boolean enabled = false;
    private static boolean keyHeld = false;
    private static LivingEntity currentTarget = null;

    // 状态
    private static boolean isFirstLock = true;

    // ★★★ 新增：标记目标是否因死亡丢失 ★★★
    private static boolean targetDied = false;

    // 平滑角度缓存
    private static float smoothYaw = 0;
    private static float smoothPitch = 0;
    private static boolean hasSmoothedAngles = false;

    // ★ 帧率无关用
    private static long lastFrameTime = 0;

    // 阈值
    private static final float DEADZONE = 0.1f;

    /**
     * ClientTick: 寻找目标
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.isPaused()) return;
        if (mc.screen != null) return;
        if (AddonClientConfig.CLIENT == null) return;

        AddonClientConfig.Client config = AddonClientConfig.CLIENT;

        if (!config.aimAssistEnabled.get()) {
            clearTarget();
            return;
        }

        String triggerMode = config.aimAssistTriggerMode.get();
        boolean shouldBeActive;

        if ("HOLD".equalsIgnoreCase(triggerMode)) {
            shouldBeActive = keyHeld;
        } else {
            shouldBeActive = enabled;
        }

        if (!shouldBeActive) {
            clearTarget();
            return;
        }

        LocalPlayer player = mc.player;

        // 验证当前目标
        if (currentTarget != null) {
            if (!currentTarget.isAlive() || currentTarget.isRemoved()) {
                currentTarget = null;
                isFirstLock = true;
                hasSmoothedAngles = false;
                targetDied = true;  // ★★★ 标记：目标死亡 ★★★
            } else {
                double lockDist = config.aimAssistLockDistance.get();
                if (lockDist > 0 && currentTarget.distanceTo(player) > lockDist) {
                    currentTarget = null;
                    isFirstLock = true;
                    hasSmoothedAngles = false;
                    // 注意：超出距离不算死亡，不设置 targetDied
                }
            }
        }

        // 寻找目标
        if (currentTarget == null) {
            // ★★★ 如果上一个目标死了，使用距离优先 ★★★
            String effectivePriority = targetDied ? "DISTANCE" : config.aimAssistPriority.get();

            currentTarget = finder.findTarget(
                    config.aimAssistMaxDistance.get(),
                    config.aimAssistFovAngle.get(),
                    entity -> finder.isValidTarget(entity, player),
                    effectivePriority,  // ★★★ 使用有效优先级 ★★★
                    null,
                    false
            );

            if (currentTarget != null) {
                isFirstLock = true;
                hasSmoothedAngles = false;
                targetDied = false;  // ★★★ 重置标记 ★★★
            }
        }
    }

    /**
     * RenderTick: 执行瞄准
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.isPaused()) return;
        if (mc.screen != null) return;
        if (AddonClientConfig.CLIENT == null) return;

        AddonClientConfig.Client config = AddonClientConfig.CLIENT;

        if (!config.aimAssistEnabled.get()) return;

        String triggerMode = config.aimAssistTriggerMode.get();
        boolean shouldBeActive = "HOLD".equalsIgnoreCase(triggerMode) ? keyHeld : enabled;

        if (!shouldBeActive) return;

        if (config.aimAssistOnlyOnAttack.get() && !mc.options.keyAttack.isDown()) {
            return;
        }

        if (currentTarget != null && currentTarget.isAlive()) {
            aimAtTarget(currentTarget, event.renderTickTime);
        }
    }

    /**
     * 核心瞄准逻辑
     */
    private static void aimAtTarget(LivingEntity target, float partialTick) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        AddonClientConfig.Client config = AddonClientConfig.CLIENT;

        // ========== 1. 计算 deltaTime ==========
        long currentTime = System.nanoTime();
        float deltaTime;
        if (lastFrameTime == 0) {
            deltaTime = 1.0f / 60.0f;  // 首帧假设60fps
        } else {
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            // 限制范围，防止暂停后突然跳很多
            deltaTime = Mth.clamp(deltaTime, 0.001f, 0.1f);
        }
        lastFrameTime = currentTime;

        // ========== 2. 获取目标的插值位置 ==========
        Vec3 aimPoint = getInterpolatedAimPoint(target, config, partialTick);

        // ========== 3. 计算目标角度 ==========
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 diff = aimPoint.subtract(eyePos);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontalDist));
        targetPitch = Mth.clamp(targetPitch, -90.0F, 90.0F);

        // ========== 4. 初始化平滑角度 ==========
        if (!hasSmoothedAngles) {
            smoothYaw = player.getYRot();
            smoothPitch = player.getXRot();
            hasSmoothedAngles = true;
        }

        // ========== 5. 计算角度差 ==========
        float yawDiff = Mth.wrapDegrees(targetYaw - smoothYaw);
        float pitchDiff = targetPitch - smoothPitch;
        float totalDiff = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // ========== 6. 死区 ==========
        if (totalDiff < DEADZONE) {
            player.setYRot(smoothYaw);
            player.setXRot(smoothPitch);
            return;
        }

        // ========== 7. 第一次锁定：瞬间对准 ==========
        if (isFirstLock) {
            smoothYaw = targetYaw;
            smoothPitch = targetPitch;
            player.setYRot(targetYaw);
            player.setXRot(targetPitch);
            isFirstLock = false;
            return;
        }

        // ========== 8. 帧率无关的平滑追踪 ==========
        float speedConfig = config.aimAssistSpeed.get().floatValue();

        float smoothFactor;
        if (speedConfig >= 1.0f) {
            smoothFactor = 1.0f;
        } else {
            float frameMultiplier = deltaTime * 60.0f;
            smoothFactor = 1.0f - (float) Math.pow(1.0f - speedConfig, frameMultiplier);
        }

        // 计算移动量
        float yawMove = yawDiff * smoothFactor;
        float pitchMove = pitchDiff * smoothFactor;

        // 防止过冲
        if (Math.abs(yawMove) > Math.abs(yawDiff)) yawMove = yawDiff;
        if (Math.abs(pitchMove) > Math.abs(pitchDiff)) pitchMove = pitchDiff;

        // 更新平滑角度
        smoothYaw += yawMove;
        smoothPitch = Mth.clamp(smoothPitch + pitchMove, -90.0F, 90.0F);

        // 应用
        player.setYRot(smoothYaw);
        player.setXRot(smoothPitch);
    }

    /**
     * 获取目标的插值瞄准点
     */
    private static Vec3 getInterpolatedAimPoint(LivingEntity target, AddonClientConfig.Client config, float partialTick) {
        String targetPart = config.aimAssistTargetPart.get();
        double heightOffset = config.aimAssistHeightOffset.get();

        double x = Mth.lerp(partialTick, target.xOld, target.getX());
        double y = Mth.lerp(partialTick, target.yOld, target.getY());
        double z = Mth.lerp(partialTick, target.zOld, target.getZ());

        double aimY;
        switch (targetPart.toUpperCase()) {
            case "FEET" -> aimY = y;
            case "BODY" -> aimY = y + target.getBbHeight() * 0.5;
            default -> aimY = y + target.getEyeHeight();
        }

        return new Vec3(x, aimY + heightOffset, z);
    }

    private static void clearTarget() {
        currentTarget = null;
        isFirstLock = true;
        hasSmoothedAngles = false;
        lastFrameTime = 0;
        targetDied = false;  // ★★★ 清理时也重置 ★★★
    }

    // ==================== 公共 API ====================

    public static void toggle() {
        enabled = !enabled;

        if (mc.player != null) {
            String key = enabled ?
                    "message.jujutsu_addon.aim_assist_enabled" :
                    "message.jujutsu_addon.aim_assist_disabled";
            mc.player.displayClientMessage(Component.translatable(key), true);
        }

        if (!enabled) {
            clearTarget();
        }
    }

    public static void setKeyHeld(boolean held) {
        keyHeld = held;
        if (!held) {
            clearTarget();
        }
    }

    public static boolean isEnabled() {
        if (AddonClientConfig.CLIENT == null) return false;
        String triggerMode = AddonClientConfig.CLIENT.aimAssistTriggerMode.get();
        if ("HOLD".equalsIgnoreCase(triggerMode)) {
            return keyHeld;
        }
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            clearTarget();
        }
    }

    @Nullable
    public static LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public static boolean hasTarget() {
        return currentTarget != null && currentTarget.isAlive();
    }
}
