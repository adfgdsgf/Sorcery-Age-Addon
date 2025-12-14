package com.jujutsuaddon.addon.mixin.fix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.Adaptation;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(TenShadowsData.class)
public class MixinTenShadowsData {

    @Shadow(remap = false) private Map<Adaptation, Integer> adapted;
    @Shadow(remap = false) private Map<Adaptation, Integer> adapting;
    @Shadow(remap = false) private LivingEntity owner;

    // =================================================================
    // 修复 1: 阻止运行时自动清空
    // =================================================================
    @Inject(method = "updateAdaptation", at = @At("HEAD"), cancellable = true, remap = false)
    private void preventAutoClear(CallbackInfo ci) {
        if (this.owner == null) return;

        Optional<ISorcererData> capOpt = this.owner.getCapability(SorcererDataHandler.INSTANCE).resolve();

        if (capOpt.isPresent()) {
            ISorcererData cap = capOpt.get();
            if (!cap.hasToggled(JJKAbilities.WHEEL.get())) {
                ci.cancel();
            }
        }
    }

    // =================================================================
    // 修复 2: 保存逻辑
    // =================================================================
    @Inject(method = "serializeNBT", at = @At("RETURN"), cancellable = true, remap = false)
    private void injectSerialize(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag nbt = cir.getReturnValue();

        if (this.adapted != null) {
            nbt.put("adapted", saveMap(this.adapted));
        }

        if (this.adapting != null) {
            nbt.put("adapting", saveMap(this.adapting));
        }

        cir.setReturnValue(nbt);
    }

    private ListTag saveMap(Map<Adaptation, Integer> map) {
        ListTag listTag = new ListTag();
        for (Map.Entry<Adaptation, Integer> entry : map.entrySet()) {
            CompoundTag data = new CompoundTag();
            Adaptation adaptation = entry.getKey();
            Ability ability = adaptation.getAbility();
            ResourceLocation key = adaptation.getKey();

            if (ability != null) {
                data.put("original_nbt", adaptation.serializeNBT());
                data.putString("type", "ability");
            } else if (key != null) {
                data.putString("source_key", key.toString());
                data.putString("type", "source");
            } else {
                continue;
            }

            data.putInt("stage", entry.getValue());
            listTag.add(data);
        }
        return listTag;
    }

    // =================================================================
    // 修复 3: 读取逻辑 (关键修复：不再尝试赋值 final 字段)
    // =================================================================
    @Inject(method = "deserializeNBT", at = @At("TAIL"), remap = false)
    private void injectDeserialize(CompoundTag nbt, CallbackInfo ci) {
        // 崩溃修复点：
        // 之前写了 this.adapted = new HashMap<>(); -> 报错 IllegalAccessError
        // 因为 adapted 是 final 的，不能重新赋值。
        // 改为直接 clear()，因为对象在构造函数里已经创建了。

        if (this.adapted != null) {
            this.adapted.clear(); // 清空原模组读的残缺数据
            if (nbt.contains("adapted", Tag.TAG_LIST)) {
                loadMap(nbt.getList("adapted", Tag.TAG_COMPOUND), this.adapted);
            }
        }

        if (this.adapting != null) {
            this.adapting.clear();
            if (nbt.contains("adapting", Tag.TAG_LIST)) {
                loadMap(nbt.getList("adapting", Tag.TAG_COMPOUND), this.adapting);
            }
        }
    }

    private void loadMap(ListTag list, Map<Adaptation, Integer> targetMap) {
        for (Tag tag : list) {
            CompoundTag compound = (CompoundTag) tag;
            int stage = compound.getInt("stage");
            String type = compound.getString("type");
            Adaptation adaptation = null;

            try {
                if ("source".equals(type)) {
                    String keyStr = compound.getString("source_key");
                    ResourceLocation key = new ResourceLocation(keyStr);
                    adaptation = new Adaptation(key, null);
                } else if ("ability".equals(type)) {
                    if (compound.contains("original_nbt")) {
                        adaptation = new Adaptation(compound.getCompound("original_nbt"));
                    }
                } else {
                    if (compound.contains("adaptation")) {
                        adaptation = new Adaptation(compound.getCompound("adaptation"));
                    }
                }
            } catch (Exception e) {
                continue;
            }

            if (adaptation != null) {
                targetMap.put(adaptation, stage);
            }
        }
    }

    // =================================================================
    // 修复 4: 死亡/重置逻辑
    // =================================================================
    @Inject(method = "resetAdaptations", at = @At("TAIL"), remap = false)
    private void injectReset(CallbackInfo ci) {
        if (this.adapting != null) {
            this.adapting.clear();
        }
    }
}
