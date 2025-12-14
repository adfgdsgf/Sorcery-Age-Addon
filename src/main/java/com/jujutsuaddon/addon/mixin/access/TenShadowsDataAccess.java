package com.jujutsuaddon.addon.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import radon.jujutsu_kaisen.capability.data.ten_shadows.Adaptation;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsData;

import java.util.Map;

@Mixin(value = TenShadowsData.class, remap = false)
public interface TenShadowsDataAccess {
    @Accessor(value = "adapting", remap = false)
    Map<Adaptation, Integer> getAdaptingMap();

    @Accessor(value = "adapted", remap = false)
    Map<Adaptation, Integer> getAdaptedMap();
}
