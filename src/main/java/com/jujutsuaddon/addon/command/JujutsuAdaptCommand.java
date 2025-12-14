package com.jujutsuaddon.addon.command;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.helper.CommandUtil; // 导入新工具类
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class JujutsuAdaptCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jjkadapt")
                .requires(source -> source.hasPermission(2))

                // 1. 总开关
                .then(Commands.literal("main")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.playerAdaptationEnabled,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "command.jujutsu_addon.adapt.main"))
                        )
                )

                // 2. 自动反击
                .then(Commands.literal("counter")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.playerAutoCounter,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "command.jujutsu_addon.adapt.counter"))
                        )
                )

                // 3. 术式破坏
                .then(Commands.literal("disruption")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> CommandUtil.setBooleanConfig(context,
                                        AddonConfig.COMMON.playerTechniqueDisruption,
                                        BoolArgumentType.getBool(context, "enabled"),
                                        "command.jujutsu_addon.adapt.disruption"))
                        )
                )
        );
    }
}
