package com.jujutsuaddon.addon.vow.condition;

import com.jujutsuaddon.addon.api.vow.ICondition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 条件注册表
 * Condition Registry
 *
 * 管理所有可用的条件类型。
 * 条件需要在mod初始化时注册到这里。
 */
public class ConditionRegistry {

    /** 条件存储映射表（保持注册顺序） */
    private static final Map<ResourceLocation, ICondition> CONDITIONS = new LinkedHashMap<>();

    /**
     * 注册条件
     * @param condition 条件实例
     */
    public static void register(ICondition condition) {
        ResourceLocation id = condition.getId();
        if (CONDITIONS.containsKey(id)) {
            throw new IllegalArgumentException("Condition already registered: " + id);
        }
        CONDITIONS.put(id, condition);
    }

    /**
     * 根据ID获取条件
     * @param id 条件ID
     * @return 条件实例，不存在则返回null
     */
    @Nullable
    public static ICondition get(ResourceLocation id) {
        return CONDITIONS.get(id);
    }

    /**
     * 获取所有已注册的条件
     */
    public static Collection<ICondition> getAll() {
        return Collections.unmodifiableCollection(CONDITIONS.values());
    }
}
