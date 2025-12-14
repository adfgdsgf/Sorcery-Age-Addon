package com.jujutsuaddon.addon.util.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DamageDebugUtil {

    // =================================================================================
    // 1. 核心缓冲与状态管理
    // =================================================================================

    private static final Set<Class<?>> analyzedClasses = Collections.synchronizedSet(new HashSet<>());
    private static final Map<UUID, Map<Integer, Runnable>> pendingLogs = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> isBuffering = new ConcurrentHashMap<>();

    // 统一的日志去重Map（支持技能级别和类型级别去重）
    private static final Map<UUID, Map<String, Long>> logCooldown = new ConcurrentHashMap<>();

    // 去重间隔（tick）
    private static final long LOG_COOLDOWN_TICKS = 10;
    private static final long BUFFER_WINDOW = 500;

    // =================================================================================
    // 2. 日志去重方法（统一实现）
    // =================================================================================

    /**
     * 通用去重检查
     */
    private static boolean shouldLog(Player player, String key) {
        if (!DebugManager.isDebugging(player)) return false;

        long currentTick = player.level().getGameTime();
        Map<String, Long> playerMap = logCooldown.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        Long lastTick = playerMap.get(key);

        if (lastTick != null && currentTick - lastTick < LOG_COOLDOWN_TICKS) {
            return false;
        }

        playerMap.put(key, currentTick);
        return true;
    }

    public static boolean shouldLogBalancerForSkill(Player player, String skillName) {
        return shouldLog(player, "balancer_" + (skillName != null ? skillName : "_default_"));
    }

    public static boolean shouldLogCalculationForSkill(Player player, String skillName) {
        return shouldLog(player, "calc_" + (skillName != null ? skillName : "_default_"));
    }

    public static boolean shouldLogCritForType(Player player, String type) {
        return shouldLog(player, "crit_" + type);
    }

    // 兼容旧调用
    public static boolean shouldLogBalancer(Player player) {
        return shouldLogBalancerForSkill(player, null);
    }

    public static boolean shouldLogCalculation(Player player) {
        return shouldLogCalculationForSkill(player, null);
    }

    public static void clearPlayerCache(UUID playerUuid) {
        logCooldown.remove(playerUuid);
    }

    // =================================================================================
    // 3. 召唤物缓冲逻辑
    // =================================================================================

    private static void bufferLog(Player player, int entityId, Runnable logAction) {
        UUID playerId = player.getUUID();
        Map<Integer, Runnable> playerBuffer = pendingLogs.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerBuffer.put(entityId, logAction);

        if (!isBuffering.getOrDefault(playerId, false)) {
            isBuffering.put(playerId, true);
            new Thread(() -> {
                try { Thread.sleep(BUFFER_WINDOW); } catch (InterruptedException ignored) {}
                if (player.getServer() != null) {
                    player.getServer().execute(() -> flushBuffer(player));
                } else {
                    isBuffering.put(playerId, false);
                    pendingLogs.remove(playerId);
                }
            }).start();
        }
    }

    private static void flushBuffer(Player player) {
        UUID playerId = player.getUUID();
        Map<Integer, Runnable> buffer = pendingLogs.remove(playerId);
        isBuffering.put(playerId, false);

        if (buffer == null || buffer.isEmpty()) return;

        try {
            AABB box = player.getBoundingBox().inflate(30.0);
            List<TamableAnimal> nearby = player.level().getEntitiesOfClass(TamableAnimal.class, box,
                    e -> e.getOwnerUUID() != null && e.getOwnerUUID().equals(playerId));
            for (TamableAnimal minion : nearby) {
                if (!buffer.containsKey(minion.getId()) && !minion.isRemoved()) {
                    buffer.put(minion.getId(), () -> printSimpleStatus(player, minion, false));
                }
            }
        } catch (Exception ignored) {}

        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.log.global_start"));
        new ArrayList<>(buffer.keySet()).stream().sorted().forEach(id -> buffer.get(id).run());
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.log.global_end"));
    }

    // =================================================================================
    // 4. 召唤物调试逻辑
    // =================================================================================

    public static void logFullSummonStatus(Player player, TamableAnimal summon,
                                           double scalingHp, double mainModHp, double totalHp, float currentHp, float maxHp,
                                           double rawOwnerDmg, double extMult, double playerSpeed, double configFactor, double dpsMult, double effectiveDmg,
                                           double ratio, double finalMult, double finalBonus, float cost) {
        if (!DebugManager.isDebugging(player)) return;
        bufferLog(player, summon.getId(), () -> {
            if (summon.isRemoved()) return;
            boolean isNewClass = !analyzedClasses.contains(summon.getClass());
            if (isNewClass) {
                analyzedClasses.add(summon.getClass());
                player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.log.new_class", summon.getClass().getSimpleName()));
                printSimpleStatus(player, summon, false);

                MutableComponent hpLine = Component.translatable("debug.jujutsu_addon.log.hp_detail_prefix")
                        .append(Component.translatable("debug.jujutsu_addon.summon.scaling_bonus", String.format("%.1f", scalingHp)));
                if (mainModHp > 0) {
                    hpLine.append(" ").append(Component.translatable("debug.jujutsu_addon.summon.main_mod_bonus_short", String.format("%.1f", mainModHp)));
                }
                player.sendSystemMessage(hpLine);

                String speedStr = playerSpeed < 4.0 ?
                        Component.translatable("debug.jujutsu_addon.summon.atk_speed_clamped", String.format("%.1f", playerSpeed)).getString() :
                        Component.translatable("debug.jujutsu_addon.summon.atk_speed_normal", String.format("%.1f", playerSpeed)).getString();
                String panelStr = Component.translatable("debug.jujutsu_addon.summon.panel_label").getString();
                String extStr = Component.translatable("debug.jujutsu_addon.summon.external_short").getString();

                player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.log.atk_detail_prefix")
                        .append(Component.translatable("debug.jujutsu_addon.log.atk_formula",
                                String.format("%.1f", rawOwnerDmg), String.format("%.2f", extMult), extStr,
                                speedStr, String.format("%.2f", configFactor), String.format("%.1f", effectiveDmg), panelStr)));

                MutableComponent finalLine = Component.translatable("debug.jujutsu_addon.log.final_prefix")
                        .append(Component.translatable("debug.jujutsu_addon.summon.final_label")).append(" §7")
                        .append(Component.translatable("debug.jujutsu_addon.summon.final_formula",
                                panelStr, String.format("%.2f", ratio), String.format("%.2f", finalMult), String.format("%.1f", finalBonus)));
                if (cost > 0) {
                    finalLine.append(Component.translatable("debug.jujutsu_addon.log.cost_suffix", String.format("%.1f", cost)));
                }
                player.sendSystemMessage(finalLine);
                printAttributes(player, summon, true);
            } else {
                printSimpleStatus(player, summon, true);
            }
        });
    }

    public static void logSyncUpdate(Player player, TamableAnimal summon) {
        if (!DebugManager.isDebugging(player)) return;
        bufferLog(player, summon.getId(), () -> printSimpleStatus(player, summon, true));
    }

    private static void printSimpleStatus(Player player, TamableAnimal summon, boolean showAttributes) {
        player.sendSystemMessage(Component.literal(String.format("§7[JJK] [ID:%d] %s §8| §aHP:%.0f/%.0f §8| §cAtk:%.1f",
                summon.getId(), summon.getName().getString(), summon.getHealth(), summon.getMaxHealth(),
                summon.getAttributeValue(Attributes.ATTACK_DAMAGE))));
        if (showAttributes) printAttributes(player, summon, false);
    }

    private static void printAttributes(Player player, LivingEntity entity, boolean detailed) {
        StringBuilder sb = new StringBuilder(Component.translatable(detailed ?
                "debug.jujutsu_addon.log.attr_detailed" : "debug.jujutsu_addon.log.attr_simple").getString());

        entity.getAttributes().getSyncableAttributes().forEach(attr -> {
            double value = attr.getValue();
            if (value > 0 || attr.getAttribute().equals(Attributes.ARMOR)) {
                String name = Component.translatable(attr.getAttribute().getDescriptionId()).getString();
                if (!name.contains("Follow") && !name.contains("Knockback")) {
                    sb.append(String.format("%s:%.1f | ", name, value));
                }
            }
        });

        String res = sb.toString();
        player.sendSystemMessage(Component.literal(res.endsWith(" | ") ? res.substring(0, res.length() - 3) : res));
    }

    // =================================================================================
    // 5. Tier 与 ItemSync 调试
    // =================================================================================

    public static void logTierSuccess(Player player, Entity entity, String ability, float cost) {
        if (!DebugManager.isDebugging(player) || DebugState.hasLoggedTier(entity.getId())) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.tier.success",
                entity.getName().getString(), ability, String.format("%.1f", cost)));
        DebugState.markTierLogged(entity.getId());
    }

    public static void logTierFail(Player player, Entity entity, String reason) {
        if (!DebugManager.isDebugging(player) || DebugState.hasLoggedTier(entity.getId())) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.tier.fail_match", entity.getClass().getSimpleName()));
        DebugState.markTierLogged(entity.getId());
    }

    public static void logItemSync(Player player, TamableAnimal summon, EquipmentSlot slot, ItemStack stack) {
        if (!DebugManager.isDebugging(player)) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.sync.item",
                Component.translatable("debug.jujutsu_addon.slot." + slot.name().toLowerCase()),
                summon.getDisplayName(), stack.getDisplayName()));
    }

    public static void logMissingAttribute(Player player, String attrName) {
        if (!DebugManager.isDebugging(player)) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.sync.warn_missing", attrName));
    }

    // =================================================================================
    // 6. Console 调试
    // =================================================================================

    public static boolean shouldLogAttribute() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(e -> e.getClassName().contains("SummonEntity") || e.getClassName().contains("radon") ||
                        e.getClassName().contains("jujutsuaddon") || e.getClassName().contains("MixinSummonSync") ||
                        e.getClassName().contains("Player") || e.getClassName().contains("Inventory") ||
                        e.getClassName().contains("ItemStack") || e.getClassName().contains("AttributeMap"));
    }

    private static String getRelevantTrace() {
        return Arrays.stream(Thread.currentThread().getStackTrace()).skip(3)
                .filter(e -> e.getClassName().contains("radon") || e.getClassName().contains("jujutsuaddon") ||
                        e.getClassName().contains("MixinSummonSync") || e.getClassName().contains("Player") ||
                        e.getClassName().contains("AttributeMap"))
                .limit(4).map(e -> "\n\t -> " + e.getClassName() + "." + e.getMethodName() + ":" + e.getLineNumber())
                .collect(Collectors.joining());
    }

    public static void logAttributeChangeConsole(AttributeInstance instance, AttributeModifier modifier, String actionKey, boolean isAdd) {
        if (!shouldLogAttribute()) return;
        String attrName = Component.translatable(instance.getAttribute().getDescriptionId()).getString();
        StringBuilder sb = new StringBuilder();
        sb.append(Component.translatable("debug.jujutsu_addon.console.header_attr", attrName).getString()).append(" | ");
        sb.append(Component.translatable("debug.jujutsu_addon.console.action", Component.translatable(actionKey).getString()).getString()).append(" | ");
        if (modifier != null) {
            sb.append(Component.translatable("debug.jujutsu_addon.console.modifier",
                    isAdd ? String.format("%.2f", modifier.getAmount()) : "ID",
                    isAdd ? modifier.getOperation().name() : modifier.getId().toString()).getString()).append(" | ");
        }
        sb.append(Component.translatable("debug.jujutsu_addon.console.values", instance.getBaseValue(), instance.getValue()).getString());
        sb.append("\n").append(Component.translatable("debug.jujutsu_addon.console.trace", getRelevantTrace()).getString());
        System.out.println(sb);
    }

    public static void logAttributeRemoveConsole(AttributeInstance instance, UUID uuid) {
        if (!shouldLogAttribute()) return;
        System.out.println(Component.translatable("debug.jujutsu_addon.console.header_attr",
                Component.translatable(instance.getAttribute().getDescriptionId()).getString()).getString() + " | " +
                Component.translatable("debug.jujutsu_addon.console.action",
                        Component.translatable("debug.jujutsu_addon.console.action.remove").getString()).getString() + " | " +
                Component.translatable("debug.jujutsu_addon.console.uuid", uuid.toString()).getString() + "\n" +
                Component.translatable("debug.jujutsu_addon.console.trace", getRelevantTrace()).getString());
    }

    public static void logHealthChangeConsole(LivingEntity entity, float oldHp, float newHp) {
        System.out.println(Component.translatable("debug.jujutsu_addon.console.header_health", entity.getName().getString()).getString() + " | " +
                Component.translatable("debug.jujutsu_addon.console.health_info", oldHp, newHp, entity.getMaxHealth()).getString() + "\n" +
                Component.translatable("debug.jujutsu_addon.console.trace", getRelevantTrace()).getString());
    }

    // =================================================================================
    // 7. 伤害计算调试 (Chat)
    // =================================================================================

    public static void accumulateDamage(Player player, float amount) {
        DebugState.accumulate(player.getUUID(), amount);
        if (DebugManager.isDebugging(player)) {
            player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.accumulate",
                            String.format("%.1f", amount), String.format("%.1f", DebugState.getCurrentDamage(player.getUUID())))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static void flushTickDamage(Player player) {
        float totalDamage = DebugState.getAndClearDamage(player.getUUID());
        if (totalDamage > 0.1f) {
            player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.tick_actual",
                    String.format("%.1f", totalDamage)).withStyle(ChatFormatting.RED));
        }
    }

    public static void logSmartSplit(Player player, LivingEntity target, float rawPanel, float defense, float finalDmg, boolean isJJK) {
        if (!DebugManager.isDebugging(player) || target == null || player.level().isClientSide()) return;
        player.sendSystemMessage(Component.literal("§8----------------------------------"));
        player.sendSystemMessage(Component.translatable(isJJK ? "debug.jujutsu_addon.split.header_jjk" : "debug.jujutsu_addon.split.header_vanilla", target.getName()));
        player.sendSystemMessage(isJJK ?
                Component.translatable("debug.jujutsu_addon.split.math", String.format("%.1f", rawPanel), String.format("%.1f", defense), String.format("§c%.1f§r", finalDmg)) :
                Component.translatable("debug.jujutsu_addon.split.raw", String.format("§c%.1f§r", finalDmg)));
    }

    public static void logSSKCorrection(Player player, float rawDamage, float fixedDamage, double factor) {
        if (!DebugManager.isDebugging(player)) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.ssk_correction",
                String.format("%.2f", factor), String.format("%.1f", rawDamage), String.format("%.1f", fixedDamage)));
    }

    public static void logCalculation(Player player, String sourceKey, float originalBase, float preservationRatio,
                                      double totalPanel, float classMult, float speedMult, float panelMult, float balancerMult,
                                      float finalDamage, boolean isCrit, float critChance, float critMult, boolean isMelee,
                                      String extraInfo, boolean isAdditive, double weaponRatio, float baseMultiplier, String skillName) {
        if (!shouldLogCalculationForSkill(player, skillName)) return;

        String modeKey = isAdditive ? "debug.jujutsu_addon.mode.additive_full" : "debug.jujutsu_addon.mode.multiplicative_full";
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.header",
                Component.translatable(modeKey), Component.translatable(getTranslationKeyForSource(sourceKey)).withStyle(ChatFormatting.AQUA)));
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.base_line", String.format("%.2f", originalBase), String.format("%.2f", preservationRatio)));
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.panel_line", String.format("%.2f", totalPanel), String.format("%.2f", weaponRatio)));
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.factors_line",
                String.format("%.2f", classMult), String.format("%.2f", speedMult), String.format("%.2f", balancerMult), String.format("%.2f", panelMult)));
        if (extraInfo != null && !extraInfo.isEmpty()) {
            player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.external_details", extraInfo));
        }
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.formula_label"));
        player.sendSystemMessage(Component.literal(String.format(
                "   §8(§f%.2f §7* §f%.2f §7* §f%.2f§8) §7+ §8(§f%.2f §7* §a%.2f §7* §b%.2f §7* §6%.2f §7* §d%.2f§8)",
                originalBase, preservationRatio, baseMultiplier, totalPanel, classMult, speedMult, panelMult, balancerMult)));
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.calc.crit_stats",
                String.format("%.1f", critChance * 100), String.format("%.2f", critMult), isCrit ? "§a✓" : "§7✗"));
        MutableComponent finalLine = Component.translatable("debug.jujutsu_addon.calc.final_output", String.format("%.2f", finalDamage));
        if (isCrit) finalLine.append(Component.translatable("debug.jujutsu_addon.calc.crit_triggered"));
        player.sendSystemMessage(finalLine);
    }

    // 兼容旧版调用
    public static void logCalculation(Player player, String sourceKey, float originalBase, float preservationRatio,
                                      double totalPanel, float classMult, float speedMult, float panelMult, float balancerMult,
                                      float finalDamage, boolean isCrit, float critChance, float critMult, boolean isMelee,
                                      String extraInfo, boolean isAdditive, double weaponRatio, float baseMultiplier) {
        logCalculation(player, sourceKey, originalBase, preservationRatio, totalPanel, classMult, speedMult, panelMult,
                balancerMult, finalDamage, isCrit, critChance, critMult, isMelee, extraInfo, isAdditive, weaponRatio, baseMultiplier, sourceKey);
    }

    public static void logSimple(Player player, String sourceKey, String id, float original, float finalDmg) {
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.damage_log",
                Component.translatable(getTranslationKeyForSource(sourceKey)), id, String.format("%.1f", original), String.format("%.1f", finalDmg)));
    }

    public static void logMainModBonus(Player player, String reasonKey, float multiplier, float damageBefore, float damageAfter) {
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.main_mod_bonus",
                Component.translatable(reasonKey), String.format("%.2f", multiplier), String.format("%.1f", damageAfter)));
    }
// =================================================================================
// 8. 平衡器调试 (Balancer Debug)
// =================================================================================
    /**
     * 记录平衡器排除信息
     */
    public static void logBalancerExcluded(Player player, String skillName, String category, String reason) {
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.balancer.excluded",
                skillName, category, reason));
    }
    /**
     * 记录平衡器详情（统一版本，支持术式+类型的自动基准）
     *
     * @param technique 可为 null，如果 null 则不显示术式信息
     * @param benchmarkInfo 可为 null，如果 null 则不显示基准来源
     */
    public static void logBalancerDetails(Player player, String skillName, String category,
                                          String technique, String costFormula,
                                          float currentCost, float benchmarkCost,
                                          String benchmarkInfo, float rawRatio, float finalMult) {
        MutableComponent header = Component.translatable("debug.jujutsu_addon.balancer.header", skillName);
        player.sendSystemMessage(header);
        // Category + Technique 行
        MutableComponent catLine = Component.translatable("debug.jujutsu_addon.balancer.category", category);
        if (technique != null && !technique.isEmpty() && !"NONE".equals(technique)) {
            catLine.append(Component.translatable("debug.jujutsu_addon.balancer.technique_suffix", technique));
        }
        player.sendSystemMessage(catLine);
        // Formula 行
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.balancer.formula", costFormula));
        // Cost / Benchmark 行
        MutableComponent costLine = Component.translatable("debug.jujutsu_addon.balancer.cost_compare",
                String.format("%.2f", currentCost),
                String.format("%.2f", benchmarkCost));
        if (benchmarkInfo != null && !benchmarkInfo.isEmpty()) {
            costLine.append(Component.translatable("debug.jujutsu_addon.balancer.benchmark_source", benchmarkInfo));
        }
        player.sendSystemMessage(costLine);
        // Ratio -> Multiplier 行
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.balancer.result",
                String.format("%.2f", rawRatio),
                String.format("%.2fx", finalMult)));
    }
    /**
     * 简化版（向后兼容，不显示术式信息）
     */
    public static void logBalancerDetails(Player player, String skillName, String category,
                                          String costFormula, float currentCost, float benchmarkCost,
                                          float rawRatio, float finalMult) {
        logBalancerDetails(player, skillName, category, null, costFormula,
                currentCost, benchmarkCost, null, rawRatio, finalMult);
    }

    private static String getTranslationKeyForSource(String key) {
        if (key == null) return "debug.jujutsu_addon.source.unknown";
        return switch (key) {
            case "mba_melee" -> "debug.jujutsu_addon.source.mba_melee";
            case "mba_tech" -> "debug.jujutsu_addon.source.mba_tech";
            case "todo_melee" -> "debug.jujutsu_addon.source.todo_melee";
            case "todo_tech" -> "debug.jujutsu_addon.source.todo_tech";
            case "no_tech_melee" -> "debug.jujutsu_addon.source.no_tech_melee";
            case "no_tech_tech" -> "debug.jujutsu_addon.source.no_tech_tech";
            case "hr" -> "debug.jujutsu_addon.source.hr";
            case "sorcerer_melee" -> "debug.jujutsu_addon.source.sorcerer_melee";
            case "sorcerer_tech" -> "debug.jujutsu_addon.source.sorcerer_tech";
            case "special_melee" -> "debug.jujutsu_addon.source.special_melee";
            default -> "debug.jujutsu_addon.source.unknown";
        };
    }

    // =================================================================================
    // 9. 暴击系统调试
    // =================================================================================

    public static void logCritChanceDetails(Player player, double baseChance, List<CritContribution> contributions, double finalChance) {
        if (!shouldLogCritForType(player, "chance")) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.crit.base", String.format("%.2f", baseChance * 100)));
        for (CritContribution c : contributions) {
            if (c.value() != 0) {
                player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.crit.contrib", c.attrId(),
                        Component.translatable(c.isMultiplicative() ? "debug.jujutsu_addon.crit.type_mult" : "debug.jujutsu_addon.crit.type_add"),
                        String.format("%.2f", c.value() * 100)));
            }
        }
    }

    public static void logCritDamageDetails(Player player, double baseDamage, List<CritContribution> contributions, double finalDamage) {
        if (!shouldLogCritForType(player, "damage")) return;
        player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.crit.base_dmg", String.format("%.2f", baseDamage)));
        for (CritContribution c : contributions) {
            if (c.value() != 0) {
                player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.crit.contrib_dmg", c.attrId(),
                        Component.translatable(c.isMultiplicative() ? "debug.jujutsu_addon.crit.type_mult" : "debug.jujutsu_addon.crit.type_add"),
                        String.format("%.2f", c.value())));
            }
        }
    }

    public record CritContribution(String attrId, double value, boolean isMultiplicative) {}
}
