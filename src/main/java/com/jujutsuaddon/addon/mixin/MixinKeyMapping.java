package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.client.input.JJKKeyBlocker;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * KeyMapping 按键映射类的 Mixin
 *
 * 目的：拦截 JJK 模组的按键检测，当我们的技能栏启用时，
 *       让 JJK 的按键（如 C、V、R 等）返回"未按下"状态，
 *       从而阻止 JJK 的轮盘菜单和技能触发。
 *
 * 原理：
 *   - JJK 通过 KeyMapping.isDown() 检测按键是否按住（用于轮盘菜单）
 *   - JJK 通过 KeyMapping.consumeClick() 检测按键是否被点击（用于技能触发）
 *   - 我们在这两个方法的开头注入代码，如果是需要屏蔽的 JJK 按键，
 *     直接返回 false，让 JJK 认为按键没有被按下
 *
 * @author JujutsuAddon
 */
@Mixin(KeyMapping.class)
public class MixinKeyMapping {

    /**
     * 注入到 isDown() 方法的开头
     *
     * isDown() 方法用于检测按键是否"正在被按住"
     * JJK 的轮盘菜单（按住 C/V 显示）就是用这个方法检测的
     *
     * @param cir CallbackInfoReturnable - 用于修改返回值
     *            调用 cir.setReturnValue(false) 可以让方法直接返回 false
     *            并跳过原本的方法逻辑
     */
    @Inject(
            method = "isDown",           // 目标方法名
            at = @At("HEAD"),            // 注入位置：方法开头
            cancellable = true           // 允许取消/修改返回值
    )
    private void jujutsuAddon$blockIsDown(CallbackInfoReturnable<Boolean> cir) {
        // (KeyMapping)(Object)this 是 Mixin 的标准写法
        // 因为 Mixin 类继承的是目标类，this 就是 KeyMapping 实例
        // 但需要先转成 Object 再转成 KeyMapping 才能作为参数传递
        KeyMapping self = (KeyMapping)(Object)this;

        // 检查这个按键是否应该被屏蔽
        if (JJKKeyBlocker.shouldBlock(self)) {
            // 设置返回值为 false，表示"按键未按下"
            // 这样 JJK 调用 isDown() 时会得到 false，轮盘菜单就不会显示
            cir.setReturnValue(false);
        }
        // 如果不需要屏蔽，不做任何事，让原方法正常执行
    }

    /**
     * 注入到 consumeClick() 方法的开头
     *
     * consumeClick() 方法用于检测按键是否"被点击了一下"
     * 调用后会消耗掉这次点击，下次调用返回 false
     * JJK 的技能触发（按 R 释放技能）就是用这个方法检测的
     *
     * @param cir CallbackInfoReturnable - 用于修改返回值
     */
    @Inject(
            method = "consumeClick",     // 目标方法名
            at = @At("HEAD"),            // 注入位置：方法开头
            cancellable = true           // 允许取消/修改返回值
    )
    private void jujutsuAddon$blockConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping)(Object)this;

        if (JJKKeyBlocker.shouldBlock(self)) {
            // 返回 false 表示"没有点击"
            // 这样 JJK 调用 consumeClick() 时会得到 false，技能就不会触发
            cir.setReturnValue(false);
        }
    }
}
