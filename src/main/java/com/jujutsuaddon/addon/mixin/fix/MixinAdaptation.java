package com.jujutsuaddon.addon.mixin.fix;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.ten_shadows.Adaptation;

import javax.annotation.Nullable;
import java.util.Objects;

@Mixin(Adaptation.class)
public class MixinAdaptation {

    // 映射原类的私有字段，remap = false 表示不使用混淆名
    @Shadow(remap = false) @Final private ResourceLocation key;
    @Shadow(remap = false) @Nullable private Ability ability;

    /**
     * @author YourName
     * @reason 修复 equals 方法，使用 .equals() 而不是 == 来比较 ResourceLocation
     */
    @Overwrite(remap = false)
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Adaptation)) return false;
        Adaptation other = (Adaptation) obj;

        // 1. 如果是技能伤害，优先比较技能
        if (this.ability != null && other.getAbility() != null) {
            Ability.Classification first = this.ability.getClassification();
            Ability.Classification second = other.getAbility().getClassification();
            // 只要技能分类相同且不是无分类，就视为相等
            if (first != Ability.Classification.NONE && first == second) return true;
            // 否则比较技能对象本身
            return this.ability == other.getAbility();
        }

        // 2. 如果是普通伤害（如岩浆），比较 ResourceLocation Key
        // 使用 Objects.equals 确保内容一致，而不是对象地址一致
        return Objects.equals(this.getKey(), other.getKey());
    }

    /**
     * @author YourName
     * @reason 确保 hashCode 与 equals 逻辑一致，防止在 Map 中丢失
     */
    @Overwrite(remap = false)
    public int hashCode() {
        if (this.ability != null) {
            Ability.Classification classification = this.ability.getClassification();
            if (classification != Ability.Classification.NONE) {
                return classification.hashCode();
            }
            return this.ability.hashCode();
        }
        // 使用 Key 的 hashCode，如果 Key 为空则返回 0
        return this.key != null ? this.key.hashCode() : 0;
    }

    // 影子方法：让我们能访问到 private 的 getKey 方法
    @Shadow(remap = false)
    public ResourceLocation getKey() { return null; }

    // 影子方法：让我们能访问到 private 的 getAbility 方法
    @Shadow(remap = false)
    public Ability getAbility() { return null; }
}
