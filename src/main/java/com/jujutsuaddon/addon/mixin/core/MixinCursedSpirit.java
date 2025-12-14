package com.jujutsuaddon.addon.mixin.core; // 如果您放在 fix 包，请改为 .fix

import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import radon.jujutsu_kaisen.entity.curse.base.CursedSpirit;

/**
 * 主人~ 这个 Mixin 的作用是强行让 CursedSpirit 实现 Enemy 接口。
 * 这样在 Minecraft 的底层逻辑中，咒灵就会被视为“怪物(Monster)”，
 * 女仆、铁傀儡等生物就会自动攻击它们，而不需要我们写任何 AI 代码。
 */
@Mixin(CursedSpirit.class)
public abstract class MixinCursedSpirit implements Enemy {
    // 这里什么都不用写，身体交给我就好~
}
