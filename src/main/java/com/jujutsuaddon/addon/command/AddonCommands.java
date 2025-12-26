package com.jujutsuaddon.addon.command;

import com.jujutsuaddon.addon.command.vow.VowCommand;
import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.helper.CommandUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AddonCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
        JujutsuAdaptCommand.register(event.getDispatcher());

        // ★★★ 新增：注册誓约命令 ★★★
        VowCommand.register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jjkaddon")

                // 1. 适应性检查
                .then(Commands.literal("adaptation_check")
                        .executes(CommandUtil::executeAdaptationCheck)
                )

                // 2. 死亡清除适应性
                .then(Commands.literal("clear_adaptation_on_death")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.clearAdaptationOnDeath,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "command.jujutsu_addon.clear_adaptation_on_death"))
                        )
                )

                // 3. 调试模式
                .then(Commands.literal("debug")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setDebugMode(context,
                                        BoolArgumentType.getBool(context, "enabled")))
                        )
                )

                // 4. 计算模式
                .then(Commands.literal("calc_mode")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("additive", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setCalcMode(context,
                                        BoolArgumentType.getBool(context, "additive")))
                        )
                )

                // 5. 伤害倍率调整
                .then(Commands.literal("hr_multiplier")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(context -> CommandUtil.setDoubleConfig(context,
                                        AddonConfig.COMMON.hrMeleeMultiplier,
                                        DoubleArgumentType.getDouble(context, "value"),
                                        "command.jujutsu_addon.set_hr_melee"))
                        )
                )
                .then(Commands.literal("sorcerer_melee")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(context -> CommandUtil.setDoubleConfig(context,
                                        AddonConfig.COMMON.sorcererMeleeMultiplier,
                                        DoubleArgumentType.getDouble(context, "value"),
                                        "command.jujutsu_addon.set_sorcerer_melee"))
                        )
                )
                .then(Commands.literal("sorcerer_tech")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(context -> CommandUtil.setDoubleConfig(context,
                                        AddonConfig.COMMON.sorcererTechniqueMultiplier,
                                        DoubleArgumentType.getDouble(context, "value"),
                                        "command.jujutsu_addon.set_sorcerer_tech"))
                        )
                )

                // 6. 血量转护甲
                .then(Commands.literal("health_conversion")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.enableHealthToArmor,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "command.jujutsu_addon.health_conversion"))
                        )

                        .then(Commands.literal("mob_enabled")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> CommandUtil.setBooleanConfig(context,
                                                AddonConfig.COMMON.enableMobHealthToArmor,
                                                BoolArgumentType.getBool(context, "enabled"),
                                                "command.jujutsu_addon.mob_health_conversion"))
                                )
                        )

                        .then(Commands.literal("set")
                                .then(Commands.literal("hr")
                                        .then(Commands.argument("armor", DoubleArgumentType.doubleArg(0.0))
                                                .then(Commands.argument("toughness", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(context -> {
                                                            double a = DoubleArgumentType.getDouble(context, "armor");
                                                            double t = DoubleArgumentType.getDouble(context, "toughness");
                                                            AddonConfig.COMMON.hrHealthToArmorRatio.set(a);
                                                            AddonConfig.COMMON.hrHealthToToughnessRatio.set(t);
                                                            context.getSource().sendSuccess(() ->
                                                                    Component.translatable("command.jujutsu_addon.set_hr_conversion", a, t).withStyle(ChatFormatting.GREEN), true);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("sorcerer")
                                        .then(Commands.argument("armor", DoubleArgumentType.doubleArg(0.0))
                                                .then(Commands.argument("toughness", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(context -> {
                                                            double a = DoubleArgumentType.getDouble(context, "armor");
                                                            double t = DoubleArgumentType.getDouble(context, "toughness");
                                                            AddonConfig.COMMON.sorcererHealthToArmorRatio.set(a);
                                                            AddonConfig.COMMON.sorcererHealthToToughnessRatio.set(t);
                                                            context.getSource().sendSuccess(() ->
                                                                    Component.translatable("command.jujutsu_addon.set_sorcerer_conversion", a, t).withStyle(ChatFormatting.GREEN), true);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("mob")
                                        .then(Commands.argument("armor", DoubleArgumentType.doubleArg(0.0))
                                                .then(Commands.argument("toughness", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(context -> {
                                                            double a = DoubleArgumentType.getDouble(context, "armor");
                                                            double t = DoubleArgumentType.getDouble(context, "toughness");
                                                            AddonConfig.COMMON.mobHealthToArmorRatio.set(a);
                                                            AddonConfig.COMMON.mobHealthToToughnessRatio.set(t);
                                                            context.getSource().sendSuccess(() ->
                                                                    Component.translatable("command.jujutsu_addon.set_mob_conversion", a, t).withStyle(ChatFormatting.GREEN), true);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )

                        .then(Commands.literal("status")
                                .executes(CommandUtil::showHealthConversionStatus)
                        )
                )

                // 7. 未调伏式神缩放
                .then(Commands.literal("untamed_scaling")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("stats")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> CommandUtil.setBooleanConfig(context,
                                                AddonConfig.COMMON.enableUntamedStatScaling,
                                                BoolArgumentType.getBool(context, "enabled"),
                                                "command.jujutsu_addon.untamed_stat_scaling"))
                                )
                        )
                        .then(Commands.literal("equip_sync")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> CommandUtil.setBooleanConfig(context,
                                                AddonConfig.COMMON.enableUntamedEquipSync,
                                                BoolArgumentType.getBool(context, "enabled"),
                                                "command.jujutsu_addon.untamed_equip_sync"))
                                )
                        )
                        .then(Commands.literal("status")
                                .executes(CommandUtil::showUntamedScalingStatus)
                        )
                )

                // 8. 术式重随
                .then(Commands.literal("reroll")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.enableTechniqueReroll,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "config.jujutsu_addon.enable_technique_reroll"))
                        )
                )

                // 9. 生物兼容性
                .then(Commands.literal("mob_compat")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.enableMobCompatibility,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "config.jujutsu_addon.enable_mob_compatibility"))
                        )
                )

                // 10. 生物 AI
                .then(Commands.literal("mob_ai")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.enableMobAI,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "config.jujutsu_addon.enable_mob_ai"))
                        )
                )
        );
    }
}
