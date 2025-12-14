package com.jujutsuaddon.addon.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererData;

import java.util.Set;

@Mixin(value = SorcererData.class, remap = false)
public interface SorcererDataAccess {

    @Accessor(value = "brainDamage", remap = false)
    void setBrainDamage(int value);

    @Accessor(value = "brainDamage", remap = false)
    int getBrainDamage();

    // ★★★ 新增：直接访问 copied 字段 ★★★
    @Accessor(value = "copied", remap = false)
    Set<CursedTechnique> getCopiedSet();

    // ★★★ 新增：直接访问 stolen 字段 ★★★
    @Accessor(value = "stolen", remap = false)
    Set<CursedTechnique> getStolenSet();
}
