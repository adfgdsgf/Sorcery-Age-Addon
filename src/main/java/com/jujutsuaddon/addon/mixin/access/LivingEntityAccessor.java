// src/main/java/com/jujutsuaddon/addon/mixin/accessor/LivingEntityAccessor.java
package com.jujutsuaddon.addon.mixin.access;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("lastHurtByPlayer")
    @Nullable
    Player jujutsu_addon$getLastHurtByPlayer();
    @Accessor("lastHurtByPlayer")
    void jujutsu_addon$setLastHurtByPlayer(@Nullable Player player);

    @Accessor("lastHurtByPlayerTime")
    int jujutsu_addon$getLastHurtByPlayerTime();

    @Accessor("lastHurtByPlayerTime")
    void jujutsu_addon$setLastHurtByPlayerTime(int value);
}
