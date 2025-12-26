package com.jujutsuaddon.addon.vow.benefit;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 收益注册表
 * Benefit Registry
 *
 * 管理所有可用的收益类型。
 * 收益需要在模组初始化时注册到这里，
 * 之后可以通过ID获取收益实例。
 */
public class BenefitRegistry {

    /** 收益存储映射表（使用LinkedHashMap保持注册顺序） */
    private static final Map<ResourceLocation, IBenefit> BENEFITS = new LinkedHashMap<>();

    /**
     * 注册收益
     * @param benefit 要注册的收益实例
     */
    public static void register(IBenefit benefit) {
        BENEFITS.put(benefit.getId(), benefit);
    }

    /**
     * 根据ID获取收益
     * @param id 收益的资源位置ID
     * @return 收益实例，如果不存在则返回null
     */
    @Nullable
    public static IBenefit get(ResourceLocation id) {
        return BENEFITS.get(id);
    }

    /**
     * 获取所有已注册的收益
     * @return 不可修改的收益集合
     */
    public static Collection<IBenefit> getAll() {
        return Collections.unmodifiableCollection(BENEFITS.values());
    }

    /**
     * 根据类别获取收益列表
     * @param category 收益类别
     * @return 属于该类别的收益列表
     */
    public static List<IBenefit> getByCategory(BenefitCategory category) {
        List<IBenefit> result = new ArrayList<>();
        for (IBenefit benefit : BENEFITS.values()) {
            if (benefit.getCategory() == category) {
                result.add(benefit);
            }
        }
        return result;
    }
}
