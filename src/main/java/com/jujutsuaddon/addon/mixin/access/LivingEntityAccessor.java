package com.jujutsuaddon.addon.mixin.access;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    // 你的其他 Accessor
    @Accessor("lastHurtByPlayer")
    @Nullable
    Player jujutsu_addon$getLastHurtByPlayer();

    @Accessor("lastHurtByPlayer")
    void jujutsu_addon$setLastHurtByPlayer(@Nullable Player player);

    @Accessor("lastHurtByPlayerTime")
    int jujutsu_addon$getLastHurtByPlayerTime();

    @Accessor("lastHurtByPlayerTime")
    void jujutsu_addon$setLastHurtByPlayerTime(int value);

    // ★★★ 核心：允许访问 protected 的 lastHurt 字段 ★★★
    // 用于重置伤害判定，防止反噬伤害因为数值低被系统吞掉
    @Accessor("lastHurt")
    void jujutsu_addon$setLastHurt(float value);
}
