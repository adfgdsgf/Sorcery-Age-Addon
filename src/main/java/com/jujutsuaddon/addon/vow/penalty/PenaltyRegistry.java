package com.jujutsuaddon.addon.vow.penalty;

import com.jujutsuaddon.addon.api.vow.IPenalty;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 惩罚类型注册表
 */
public class PenaltyRegistry {

    private static final Map<ResourceLocation, IPenalty> PENALTIES = new LinkedHashMap<>();

    public static void register(IPenalty penalty) {
        ResourceLocation id = penalty.getId();
        if (PENALTIES.containsKey(id)) {
            throw new IllegalArgumentException("Penalty already registered: " + id);
        }
        PENALTIES.put(id, penalty);
    }

    public static @Nullable IPenalty get(ResourceLocation id) {
        return PENALTIES.get(id);
    }

    public static Collection<IPenalty> getAll() {
        return Collections.unmodifiableCollection(PENALTIES.values());
    }

    public static List<IPenalty> getByType(PenaltyType type) {
        return PENALTIES.values().stream()
                .filter(p -> p.getType() == type)
                .toList();
    }

    public static List<IPenalty> getBySeverity(PenaltySeverity severity) {
        return PENALTIES.values().stream()
                .filter(p -> p.getSeverity() == severity)
                .toList();
    }
}
