package com.jujutsuaddon.addon.command.vow;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.manager.DissolveResult;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

/**
 * 誓约管理命令
 * Vow Management Commands
 */
public class VowCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vow")

                // /vow list - 列出自己的誓约
                .then(Commands.literal("list")
                        .executes(VowCommand::listOwnVows)
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(VowCommand::listPlayerVows)))

                // /vow info <vowId> - 查看誓约详情
                .then(Commands.literal("info")
                        .then(Commands.argument("vowId", StringArgumentType.string())
                                .executes(VowCommand::showVowInfo)))

                // /vow activate <vowId> - 激活誓约
                .then(Commands.literal("activate")
                        .then(Commands.argument("vowId", StringArgumentType.string())
                                .executes(VowCommand::activateVow)))

                // /vow dissolve <vowId> - 解除誓约
                .then(Commands.literal("dissolve")
                        .then(Commands.argument("vowId", StringArgumentType.string())
                                .executes(VowCommand::dissolveVow)))

                // /vow remove <player> <vowId> - 强制移除（OP）
                .then(Commands.literal("remove")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("vowId", StringArgumentType.string())
                                        .executes(VowCommand::forceRemoveVow))))

                // /vow status - 显示誓约系统状态
                .then(Commands.literal("status")
                        .executes(VowCommand::showStatus))
        );
    }

    // ==================== 命令处理方法 ====================

    private static int listOwnVows(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            return listVowsFor(ctx.getSource(), player);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_only"));
            return 0;
        }
    }

    private static int listPlayerVows(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            return listVowsFor(ctx.getSource(), target);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_not_found"));
            return 0;
        }
    }

    private static int listVowsFor(CommandSourceStack source, ServerPlayer player) {
        Collection<CustomBindingVow> vows = VowManager.getPlayerVows(player);

        if (vows.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.vow.list.empty", player.getName())
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("command.vow.list.header", player.getName())
                .withStyle(ChatFormatting.GOLD), false);

        for (CustomBindingVow vow : vows) {
            ChatFormatting stateColor = getStateColor(vow.getState());
            String shortId = vow.getVowId().toString().substring(0, 8);

            source.sendSuccess(() -> Component.literal("")
                            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
                            .append(Component.translatable("vow.state." + vow.getState().name().toLowerCase())
                                    .withStyle(stateColor))
                            .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(vow.getName()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" (" + shortId + ")").withStyle(ChatFormatting.DARK_GRAY)),
                    false);
        }

        source.sendSuccess(() -> Component.translatable("command.vow.list.footer", vows.size())
                .withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int showVowInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String vowIdStr = StringArgumentType.getString(ctx, "vowId");

            UUID vowId = parseVowId(player, vowIdStr);
            if (vowId == null) {
                ctx.getSource().sendFailure(Component.translatable("command.vow.error.vow_not_found", vowIdStr));
                return 0;
            }

            CustomBindingVow vow = VowManager.getVow(player, vowId);
            if (vow == null) {
                ctx.getSource().sendFailure(Component.translatable("command.vow.error.vow_not_found", vowIdStr));
                return 0;
            }

            // 显示详情
            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.header")
                    .withStyle(ChatFormatting.GOLD), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.name", vow.getName())
                    .withStyle(ChatFormatting.WHITE), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.type",
                            Component.translatable("vow.type." + vow.getType().name().toLowerCase()))
                    .withStyle(ChatFormatting.WHITE), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.state",
                            Component.translatable("vow.state." + vow.getState().name().toLowerCase())
                                    .withStyle(getStateColor(vow.getState())))
                    .withStyle(ChatFormatting.WHITE), false);

            // 条件数量
            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.conditions_count",
                    vow.getConditions().size()).withStyle(ChatFormatting.AQUA), false);

            // 收益数量
            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.benefits_count",
                    vow.getBenefits().size()).withStyle(ChatFormatting.GREEN), false);

            // 惩罚数量
            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.info.penalties_count",
                    vow.getPenalties().size()).withStyle(ChatFormatting.RED), false);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_only"));
            return 0;
        }
    }

    private static int activateVow(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String vowIdStr = StringArgumentType.getString(ctx, "vowId");

            UUID vowId = parseVowId(player, vowIdStr);
            if (vowId == null) {
                ctx.getSource().sendFailure(Component.translatable("command.vow.error.vow_not_found", vowIdStr));
                return 0;
            }

            boolean success = VowManager.activateVow(player, vowId);

            if (success) {
                ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.activate.success")
                        .withStyle(ChatFormatting.GREEN), true);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.translatable("command.vow.activate.failed"));
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_only"));
            return 0;
        }
    }

    private static int dissolveVow(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String vowIdStr = StringArgumentType.getString(ctx, "vowId");

            UUID vowId = parseVowId(player, vowIdStr);
            if (vowId == null) {
                ctx.getSource().sendFailure(Component.translatable("command.vow.error.vow_not_found", vowIdStr));
                return 0;
            }

            DissolveResult result = VowManager.dissolveVow(player, vowId);

            if (result.isSuccess()) {
                ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.dissolve.success")
                        .withStyle(ChatFormatting.GREEN), true);
                return 1;
            } else {
                ctx.getSource().sendFailure(result.getErrorMessage());
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_only"));
            return 0;
        }
    }

    private static int forceRemoveVow(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            String vowIdStr = StringArgumentType.getString(ctx, "vowId");

            UUID vowId = parseVowId(target, vowIdStr);
            if (vowId == null) {
                ctx.getSource().sendFailure(Component.translatable("command.vow.error.vow_not_found", vowIdStr));
                return 0;
            }

            VowManager.forceRemoveVow(target, vowId);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.remove.success", target.getName())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_not_found"));
            return 0;
        }
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Collection<CustomBindingVow> vows = VowManager.getPlayerVows(player);

            long activeCount = vows.stream().filter(v -> v.getState() == VowState.ACTIVE).count();
            long inactiveCount = vows.stream().filter(v -> v.getState() == VowState.INACTIVE).count();  // ★ 改这里
            long violatedCount = vows.stream().filter(v -> v.getState() == VowState.VIOLATED).count();
            long exhaustedCount = vows.stream().filter(v -> v.getState() == VowState.EXHAUSTED).count();

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.header")
                    .withStyle(ChatFormatting.GOLD), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.total", vows.size())
                    .withStyle(ChatFormatting.WHITE), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.active", activeCount)
                    .withStyle(ChatFormatting.GREEN), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.inactive", inactiveCount)  // ★ 改这里
                    .withStyle(ChatFormatting.YELLOW), false);

            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.violated", violatedCount)
                    .withStyle(ChatFormatting.RED), false);
            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.exhausted", exhaustedCount)
                    .withStyle(ChatFormatting.DARK_GRAY), false);

            // 显示总收益加成
            float totalBonus = VowManager.calculateTotalOutputBonus(player);
            int bonusPercent = Math.round(totalBonus * 100);
            ctx.getSource().sendSuccess(() -> Component.translatable("command.vow.status.bonus", bonusPercent)
                    .withStyle(ChatFormatting.AQUA), false);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("command.vow.error.player_only"));
            return 0;
        }
    }

    // ==================== 辅助方法 ====================

    private static ChatFormatting getStateColor(VowState state) {
        return switch (state) {
            case ACTIVE -> ChatFormatting.GREEN;
            case INACTIVE -> ChatFormatting.YELLOW;      // ★ 改这里
            case VIOLATED -> ChatFormatting.RED;
            case DISSOLVED -> ChatFormatting.GRAY;
            case EXPIRED -> ChatFormatting.DARK_GRAY;    // ★ 添加这个
            case EXHAUSTED -> ChatFormatting.DARK_GRAY;
        };
    }

    private static UUID parseVowId(ServerPlayer player, String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {}

        for (CustomBindingVow vow : VowManager.getPlayerVows(player)) {
            if (vow.getVowId().toString().startsWith(input)) {
                return vow.getVowId();
            }
        }

        return null;
    }
}
