package com.jujutsuaddon.addon.compat.mob;

import com.jujutsuaddon.addon.config.AddonConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import radon.jujutsu_kaisen.capability.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.config.ConfigHolder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理生物特质 (Trait) 的核心逻辑类
 * 包含特质随机、重随、以及全存档唯一性限制
 */
public class MobTraitHelper {

    // 用于标记生物是否已经初始化过特质的 NBT 标签
    private static final String NBT_TRAITS_INITIALIZED = "jujutsu_addon_traits_initialized";

    /**
     * 判断生物是否兼容特质系统
     * (只要在兼容列表里，就允许随机特质)
     */
    public static boolean isCompatibleMob(LivingEntity entity) {
        return MobConfigManager.getMobConfig(entity) != null;
    }

    /**
     * 处理生物生成时的特质初始化
     * 在 EntityJoinLevelEvent 或 FinalizeSpawn 中调用
     */
    public static void handleSpawn(Mob mob) {
        if (isCompatibleMob(mob)) {
            // 如果没有初始化过特质，则进行随机
            if (!mob.getPersistentData().contains(NBT_TRAITS_INITIALIZED)) {
                randomizeTraits(mob);
                // 标记为已初始化，防止重复随机
                mob.getPersistentData().putBoolean(NBT_TRAITS_INITIALIZED, true);
            }
        }
    }

    /**
     * 处理玩家交互（重随特质）
     * @return true 表示交互成功，需要取消原事件
     */
    public static boolean handleInteract(Mob target, ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (!isCompatibleMob(target)) return false;

        // 1. 获取物品注册名
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;

        // 2. [修改] 获取配置列表 (支持多个物品)
        // 注意：这里对应 AddonConfig 中改为 List 的 traitRerollItems
        List<? extends String> allowedItems = AddonConfig.COMMON.traitRerollItems.get();

        // 3. [修改] 检查手持物品是否在白名单列表中
        if (allowedItems.contains(itemId.toString())) {
            // 执行重随逻辑，并获取新特质列表
            List<Trait> newTraits = randomizeTraits(target);

            // 构建特质名称字符串
            String traitNames;
            if (newTraits.isEmpty()) {
                traitNames = Component.translatable("message.jujutsu_addon.trait.none").getString();
            } else {
                // 将特质列表转换为逗号分隔的字符串 (例如: "六眼")
                traitNames = newTraits.stream()
                        .map(t -> Component.translatable("trait.jujutsu_kaisen." + t.name().toLowerCase()).getString())
                        .collect(Collectors.joining(", "));
            }

            // 发送包含具体特质的消息
            player.sendSystemMessage(Component.translatable("message.jujutsu_addon.reroll.trait_result", target.getName(), traitNames));

            // 非创造模式消耗物品
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return true;
        }
        return false;
    }

    /**
     * 核心逻辑：随机分配特质
     * @return 返回本次随机到的所有特质列表
     */
    private static List<Trait> randomizeTraits(Mob mob) {
        List<Trait> addedTraits = new ArrayList<>();

        // 仅在服务端执行
        if (mob.level().isClientSide) return addedTraits;

        mob.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            // 1. 清除旧特质
            cap.getTraits().clear();

            JujutsuType type = cap.getType();

            // [修改] 强制特质数量为 1 (或 0)
            // 我们不再读取 ConfigHolder.SERVER.minTraits/maxTraits，因为那是给玩家用的
            // 生物只允许拥有一个特质，保持纯粹
            int traitCount = 1;

            int traitRolls = ConfigHolder.SERVER.traitRolls.get(); // 每次尝试随机的次数 (重试次数)
            Random random = new Random();

            // 2. 循环抽取特质 (虽然 traitCount 是 1，但保留循环结构方便未来扩展)
            for (int i = 0; i < traitCount; i++) {
                Trait selectedTrait = null;
                // 尝试多次抽取以获得有效特质
                for (int j = 0; j < traitRolls; j++) {
                    Trait candidate = rollSingleTrait(type, random);
                    // 确保不重复添加已有特质 (虽然现在只有1个，但逻辑保留)
                    if (candidate != null && !cap.hasTrait(candidate)) {
                        selectedTrait = candidate;
                        break;
                    }
                }

                if (selectedTrait != null) {
                    // 3. 全存档唯一性检查
                    if (AddonConfig.COMMON.enableUniqueTraitLimit.get()) {
                        if (isUniqueTrait(selectedTrait)) {
                            // 检查全存档数据中是否已被占用
                            if (checkGlobalUniqueExists(mob.level(), selectedTrait)) {
                                continue; // 全局已存在，跳过此特质 (结果就是该生物本次没有特质)
                            } else {
                                // 全局不存在，注册占用
                                registerGlobalUnique(mob.level(), selectedTrait);
                            }
                        }
                    }
                    // 4. 添加特质
                    cap.addTrait(selectedTrait);
                    addedTraits.add(selectedTrait); // 记录到返回列表
                }
            }
            // 注意：ISorcererData 接口没有 sync() 方法，数据同步依赖主模组自身的 tick 或包同步机制
        });

        return addedTraits;
    }

    /**
     * 根据权重抽取单个特质
     */
    private static Trait rollSingleTrait(JujutsuType type, Random random) {
        Map<Trait, Integer> weights = ConfigHolder.SERVER.getTraits(type);
        if (weights == null || weights.isEmpty()) return null;

        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int noTraitWeight = ConfigHolder.SERVER.noTraitWeight.get();
        totalWeight += noTraitWeight;

        int roll = random.nextInt(totalWeight);
        int current = 0;

        // 判定是否轮空（无特质）
        if (roll < noTraitWeight) return null;
        current += noTraitWeight;

        for (Map.Entry<Trait, Integer> entry : weights.entrySet()) {
            current += entry.getValue();
            if (roll < current) return entry.getKey();
        }
        return null;
    }

    /**
     * 检查是否为唯一特质（如六眼）
     */
    private static boolean isUniqueTrait(Trait trait) {
        List<Trait> uniqueTraits = ConfigHolder.SERVER.getUniqueTraits();
        return uniqueTraits.contains(trait);
    }

    // ==================================================
    // 全局存档数据逻辑 (Global Save Data)
    // ==================================================

    /**
     * 检查特质在全存档中是否已被占用
     */
    private static boolean checkGlobalUniqueExists(net.minecraft.world.level.Level level, Trait trait) {
        if (level instanceof ServerLevel serverLevel) {
            GlobalTraitData data = GlobalTraitData.get(serverLevel);
            return data.isTraitTaken(trait.name());
        }
        return false;
    }

    /**
     * 在全存档中注册特质占用
     */
    private static void registerGlobalUnique(net.minecraft.world.level.Level level, Trait trait) {
        if (level instanceof ServerLevel serverLevel) {
            GlobalTraitData data = GlobalTraitData.get(serverLevel);
            data.takeTrait(trait.name());
        }
    }

    /**
     * 内部类：用于保存全存档的特质占用情况
     * 存储在 world/data/jujutsu_addon_global_traits.dat 中
     */
    public static class GlobalTraitData extends SavedData {
        private static final String DATA_NAME = "jujutsu_addon_global_traits";
        private final Set<String> takenTraits = new HashSet<>();

        public static GlobalTraitData get(ServerLevel level) {
            // 获取 Overworld 的存储，确保跨维度统一
            return level.getServer().overworld().getDataStorage().computeIfAbsent(GlobalTraitData::load, GlobalTraitData::new, DATA_NAME);
        }

        public GlobalTraitData() {}

        public boolean isTraitTaken(String traitName) {
            return takenTraits.contains(traitName);
        }

        public void takeTrait(String traitName) {
            takenTraits.add(traitName);
            setDirty(); // 标记为脏数据，通知游戏保存
        }

        // 从 NBT 加载数据
        public static GlobalTraitData load(CompoundTag tag) {
            GlobalTraitData data = new GlobalTraitData();
            ListTag list = tag.getList("TakenTraits", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                data.takenTraits.add(list.getString(i));
            }
            return data;
        }

        // 保存数据到 NBT
        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (String trait : takenTraits) {
                list.add(StringTag.valueOf(trait));
            }
            tag.put("TakenTraits", list);
            return tag;
        }
    }
}
