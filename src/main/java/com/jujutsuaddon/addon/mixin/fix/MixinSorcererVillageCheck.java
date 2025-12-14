/*
package com.jujutsuaddon.addon.mixin.fix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import radon.jujutsu_kaisen.entity.sorcerer.base.SorcererEntity;

@Mixin(SorcererEntity.class)
public class MixinSorcererVillageCheck {

    */
/**
     * @author JJK_Addon_Dev
     * @reason 使用 @Redirect 精准替换 isInVillage 调用，修复崩溃并拓展兼容性
     *//*

    @Redirect(
            method = "checkSpawnRules",
            at = @At(
                    value = "INVOKE",
                    target = "Lradon/jujutsu_kaisen/entity/sorcerer/base/SorcererEntity;isInVillage()Z"
            )
            // 关键修正：去掉了 remap = false
            // 这样 Mixin 就能正确找到混淆后的 checkSpawnRules 方法
    )
    private boolean redirectVillageCheck(SorcererEntity instance) {
        // 将原模组的 isInVillage() 替换为我们更强大的 isExtendedVillage()
        return this.isExtendedVillage(instance);
    }

    @Unique
    private boolean isExtendedVillage(SorcererEntity entity) {
        // 确保是在服务端世界
        if (!(entity.level() instanceof ServerLevel serverLevel)) return false;

        BlockPos pos = entity.blockPosition();
        Registry<Structure> structureRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

        // 遍历所有结构，寻找村庄或类似城镇的结构
        for (Holder.Reference<Structure> holder : structureRegistry.holders().toList()) {
            Structure structure = holder.value();

            boolean isVanillaVillage = holder.is(StructureTags.VILLAGE);
            boolean isModVillage = false;

            // 如果不是原版村庄，检查名字里是否包含 village/town/city
            if (!isVanillaVillage) {
                ResourceLocation key = holder.key().location();
                String path = key.getPath().toLowerCase();
                if (path.contains("village") || path.contains("town") || path.contains("city")) {
                    isModVillage = true;
                }
            }

            // 如果既不是原版村庄也不是模组城镇，跳过
            if (!isVanillaVillage && !isModVillage) continue;

            // 检查当前位置是否在结构范围内
            StructureStart start = serverLevel.structureManager().getStructureAt(pos, structure);
            if (start != null && start.isValid()) {
                return true;
            }
        }
        return false;
    }
}
*/
