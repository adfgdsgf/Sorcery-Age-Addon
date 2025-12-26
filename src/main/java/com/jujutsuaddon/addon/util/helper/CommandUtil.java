package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.ten_shadows.Adaptation;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;

import java.util.Map;

public class CommandUtil {

    /**
     * 执行适应性检查逻辑
     */
    public static int executeAdaptationCheck(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            // 1. 尝试获取玩家视线内的实体
            LivingEntity targetEntity = getEntityLookedAt(player, 20.0);
            ITenShadowsData cap = null;
            String targetName = player.getScoreboardName();

            // 2. 优先查视线内的实体
            if (targetEntity != null && targetEntity.getCapability(TenShadowsDataHandler.INSTANCE).isPresent()) {
                cap = targetEntity.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
                targetName = targetEntity.getDisplayName().getString();
            }

            // 3. 查玩家自己
            if (cap == null) {
                if (player.getCapability(TenShadowsDataHandler.INSTANCE).isPresent()) {
                    cap = player.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
                    targetName = "You (" + player.getScoreboardName() + ")";
                }
            }

            // 4. 报错
            if (cap == null) {
                context.getSource().sendFailure(Component.translatable("command.jujutsu_addon.adaptation.no_technique"));
                return 0;
            }

            Map<Adaptation, Integer> adaptedMap = cap.getAdapted();
            Map<Adaptation, Integer> adaptingMap = cap.getAdapting();

            if (adaptedMap.isEmpty() && adaptingMap.isEmpty()) {
                String finalTargetName = targetName;
                context.getSource().sendSuccess(() ->
                        Component.literal("Target: " + finalTargetName + "\n").withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("command.jujutsu_addon.adaptation.none_found")), false);
                return 1;
            }

            MutableComponent msg = Component.literal("§6========================================\n");
            msg.append(Component.literal("Target: " + targetName + "\n").withStyle(ChatFormatting.YELLOW));
            msg.append(Component.translatable("command.jujutsu_addon.adaptation.header")).append("\n");

            // 已完全适应
            if (!adaptedMap.isEmpty()) {
                msg.append(Component.translatable("command.jujutsu_addon.adaptation.section.adapted")).append("\n");
                for (Adaptation adaptation : adaptedMap.keySet()) {
                    MutableComponent name = getAdaptationComponent(adaptation);
                    msg.append(Component.literal(" ✔ ")).append(name).append("\n");
                }
            }

            // 正在适应中
            if (!adaptingMap.isEmpty()) {
                msg.append(Component.translatable("command.jujutsu_addon.adaptation.section.adapting")).append("\n");
                for (Adaptation adaptation : adaptingMap.keySet()) {
                    MutableComponent name = getAdaptationComponent(adaptation);
                    float progress = cap.getAdaptationProgress(adaptation) * 100.0f;
                    String progressStr = String.format("%.1f", progress);

                    msg.append(Component.literal(" ⏳ ")).append(name)
                            .append(" ").append(Component.translatable("command.jujutsu_addon.adaptation.progress", progressStr)).append("\n");
                }
            }

            msg.append(Component.literal("§6========================================"));
            context.getSource().sendSuccess(() -> msg, false);
            return 1;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 设置布尔类型的配置项
     */
    public static int setBooleanConfig(CommandContext<CommandSourceStack> context, ForgeConfigSpec.BooleanValue configValue, boolean newValue, String translationKey) {
        configValue.set(newValue);
        configValue.save();
        sendFeedback(context, translationKey, newValue);
        return 1;
    }

    /**
     * 设置双精度类型的配置项
     */
    public static int setDoubleConfig(CommandContext<CommandSourceStack> context, ForgeConfigSpec.DoubleValue configValue, double newValue, String translationKey) {
        configValue.set(newValue);
        configValue.save();
        context.getSource().sendSuccess(() ->
                Component.translatable(translationKey, String.format("%.2f", newValue)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * 设置调试模式
     */
    public static int setDebugMode(CommandContext<CommandSourceStack> context, boolean enabled) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            DebugManager.setDebugging(player, enabled);
            sendFeedback(context, "command.jujutsu_addon.debug_mode", enabled);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 设置计算模式 (加算/乘算)
     */
    public static int setCalcMode(CommandContext<CommandSourceStack> context, boolean isAdditive) {
        AddonConfig.COMMON.useAdditiveExternalAttributes.set(isAdditive);
        String modeKey = isAdditive ? "command.jujutsu_addon.mode_additive" : "command.jujutsu_addon.mode_multiplicative";
        MutableComponent modeText = Component.translatable(modeKey);
        modeText.withStyle(isAdditive ? ChatFormatting.YELLOW : ChatFormatting.LIGHT_PURPLE);
        context.getSource().sendSuccess(() ->
                Component.translatable("command.jujutsu_addon.calc_mode_set", modeText).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * 显示血量转化状态 (已修复：添加了 mobStatusText 定义)
     */
    public static int showHealthConversionStatus(CommandContext<CommandSourceStack> context) {
        // 1. 获取玩家配置
        boolean enabled = AddonConfig.COMMON.enableHealthToArmor.get();
        double hrA = AddonConfig.COMMON.hrHealthToArmorRatio.get();
        double hrT = AddonConfig.COMMON.hrHealthToToughnessRatio.get();
        double sA = AddonConfig.COMMON.sorcererHealthToArmorRatio.get();
        double sT = AddonConfig.COMMON.sorcererHealthToToughnessRatio.get();

        // 2. 获取生物配置
        boolean mobEnabled = AddonConfig.COMMON.enableMobHealthToArmor.get();
        double mobA = AddonConfig.COMMON.mobHealthToArmorRatio.get();
        double mobT = AddonConfig.COMMON.mobHealthToToughnessRatio.get();

        // 3. 构建玩家状态文本
        MutableComponent statusText = Component.translatable(enabled ? "command.jujutsu_addon.status_on" : "command.jujutsu_addon.status_off");
        statusText.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);

        // 4. 构建生物状态文本 (这里就是之前报错缺少的变量！)
        MutableComponent mobStatusText = Component.translatable(mobEnabled ? "command.jujutsu_addon.status_on" : "command.jujutsu_addon.status_off");
        mobStatusText.withStyle(mobEnabled ? ChatFormatting.GREEN : ChatFormatting.RED);

        // 5. 拼接消息
        MutableComponent message = Component.translatable("command.jujutsu_addon.status_header", statusText).withStyle(ChatFormatting.GOLD);

        // 玩家部分
        message.append("\n").append(Component.translatable("command.jujutsu_addon.status_hr", hrA, hrT).withStyle(ChatFormatting.AQUA));
        message.append("\n").append(Component.translatable("command.jujutsu_addon.status_sorcerer", sA, sT).withStyle(ChatFormatting.AQUA));

        // 生物部分
        message.append("\n§6----------------");
        message.append("\n").append(Component.literal("Mob/Maid Conversion: ").append(mobStatusText));
        message.append("\n").append(Component.literal(String.format("Ratio: Armor %.2f | Toughness %.2f", mobA, mobT)).withStyle(ChatFormatting.LIGHT_PURPLE));

        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    /**
     * 显示未调伏式神缩放状态
     */
    public static int showUntamedScalingStatus(CommandContext<CommandSourceStack> context) {
        boolean stats = AddonConfig.COMMON.enableUntamedStatScaling.get();
        boolean sync = AddonConfig.COMMON.enableUntamedEquipSync.get();

        MutableComponent statsText = Component.translatable(stats ? "command.jujutsu_addon.value_true" : "command.jujutsu_addon.value_false")
                .withStyle(stats ? ChatFormatting.GREEN : ChatFormatting.RED);

        MutableComponent syncText = Component.translatable(sync ? "command.jujutsu_addon.value_true" : "command.jujutsu_addon.value_false")
                .withStyle(sync ? ChatFormatting.GREEN : ChatFormatting.RED);

        MutableComponent msg = Component.literal("§6[Untamed Scaling Status]\n");
        msg.append(Component.translatable("command.jujutsu_addon.untamed_stat_scaling")).append(": ").append(statsText).append("\n");
        msg.append(Component.translatable("command.jujutsu_addon.untamed_equip_sync")).append(": ").append(syncText);

        context.getSource().sendSuccess(() -> msg, false);
        return 1;
    }

    // ==========================================
    // 私有辅助方法
    // ==========================================

    private static void sendFeedback(CommandContext<CommandSourceStack> context, String translationKey, boolean value) {
        MutableComponent valueText = Component.translatable(value ? "command.jujutsu_addon.value_true" : "command.jujutsu_addon.value_false");
        valueText.withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
        MutableComponent nameText = Component.translatable(translationKey);
        context.getSource().sendSuccess(() ->
                Component.translatable("command.jujutsu_addon.generic_feedback", nameText, valueText).withStyle(ChatFormatting.GREEN), true);
    }

    private static LivingEntity getEntityLookedAt(ServerPlayer player, double range) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 endPosition = eyePosition.add(viewVector.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(viewVector.scale(range)).inflate(1.0D);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                eyePosition,
                endPosition,
                searchBox,
                (entity) -> entity instanceof LivingEntity && !entity.isSpectator(),
                range * range
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static MutableComponent getAdaptationComponent(Adaptation adaptation) {
        if (adaptation == null) return Component.translatable("command.jujutsu_addon.adaptation.type.unknown");

        Ability ability = adaptation.getAbility();
        if (ability != null) {
            String simpleName = ability.getClass().getSimpleName();
            return Component.translatable("ability.jujutsu_kaisen." + simpleName.toLowerCase())
                    .withStyle(ChatFormatting.WHITE);
        }

        if (adaptation.getKey() != null) {
            String path = adaptation.getKey().getPath();
            return Component.translatable("adaptation.source." + path)
                    .withStyle(ChatFormatting.WHITE);
        }

        return Component.translatable("command.jujutsu_addon.adaptation.type.unknown");
    }
}
