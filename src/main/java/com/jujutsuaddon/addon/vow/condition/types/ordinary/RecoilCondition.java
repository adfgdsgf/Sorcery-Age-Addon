package com.jujutsuaddon.addon.vow.condition.types.ordinary;

import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.capability.vow.VowDataProvider;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 后坐力条件
 * Recoil Condition
 *
 * 逻辑修正：
 * 1. 基础反噬 = 造成伤害 * 百分比
 * 2. 上限 = 最大生命 * 百分比 * 2
 * 3. 最终伤害 = min(基础反噬, 上限)
 */
public class RecoilCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "recoil");

    // 改用 Int 存储百分比整数
    public static final String PARAM_DAMAGE_PERCENT_INT = "damagePercentInt";

    // 用于 Mixin 判断是否正在进行反噬处理 (穿透无下限 & 消除抖动)
    private static final ThreadLocal<Boolean> IS_RECOIL_ACTIVE = ThreadLocal.withInitial(() -> false);

    // AOE 防护：记录玩家最后一次触发反噬的 tick，防止一秒暴毙
    private static final Map<LivingEntity, Long> LAST_TRIGGER_TICK = new WeakHashMap<>();

    private static boolean registered = false;

    public RecoilCondition() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(this);
            registered = true;
        }
    }

    /** 供 Mixin 调用 */
    public static boolean isRecoilActive() {
        return IS_RECOIL_ACTIVE.get();
    }

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("condition.jujutsu_addon.recoil");
    }

    @Override
    public Component getDescription(ConditionParams params) {
        // 更新描述以匹配你的截图，直接用整数
        int percent = params.getInt(PARAM_DAMAGE_PERCENT_INT, 5);
        return Component.translatable("condition.jujutsu_addon.recoil.desc", percent);
    }

    @Override
    public float calculateWeight(ConditionParams params) {
        int percent = params.getInt(PARAM_DAMAGE_PERCENT_INT, 5);
        // 这里 percent 已经是 5, 10 这种整数了，直接乘系数即可
        // 例如 5% -> 权重 7.5
        return percent * 1.5f;
    }

    @Override
    public boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context) {
        // 这里的触发时机无法获取造成的伤害数值
        // 所以我们只在 Event 中处理，这里返回 false (不违约)
        return false;
    }

    /**
     * 核心逻辑：监听造成伤害的事件
     */
    @SubscribeEvent
    public void onDamageDealt(LivingDamageEvent event) {
        // 1. 基础检查
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;

        // 2. 防止递归 (自己反噬自己 -> 再次触发反噬)
        if (IS_RECOIL_ACTIVE.get()) return;

        // 3. AOE 限制 (同一 Tick 只触发一次)
        long currentTick = attacker.tickCount;
        Long lastTick = LAST_TRIGGER_TICK.get(attacker);
        if (lastTick != null && lastTick == currentTick) {
            return;
        }

        // 4. 获取玩家束缚数据
        attacker.getCapability(VowDataProvider.VOW_DATA).ifPresent(data -> {
            ConditionParams params = data.getActiveConditionParams(ID);
            if (params != null) {

                // ★★★ 公式计算 (修正后) ★★★
                // 获取整数参数并转回浮点 (5 -> 0.05)
                int percentInt = params.getInt(PARAM_DAMAGE_PERCENT_INT, 5);
                float percent = percentInt / 100.0f;

                // A. 基础反噬 = 实际造成伤害 * 百分比
                // 例如打出 100 伤害，10% -> 10点反噬
                float actualDamageDealt = event.getAmount();
                float baseRecoil = actualDamageDealt * percent;

                // B. 上限阈值 = 最大生命 * 百分比 * 2
                // 例如 20 血，10% -> 20 * 0.1 * 2 = 4点上限
                float maxLimit = attacker.getMaxHealth() * (percent * 2.0f);

                // C. 最终伤害 = 取较小值
                // min(10, 4) = 4点实际扣血
                float finalDamage = Math.min(baseRecoil, maxLimit);

                if (finalDamage > 0.01f) {
                    // 标记当前 tick 已处理
                    LAST_TRIGGER_TICK.put(attacker, currentTick);

                    // 执行反噬
                    applyRecoil(attacker, finalDamage);
                }
            }
        });
    }

    private void applyRecoil(Player player, float amount) {
        // 使用 通用伤害类型
        DamageSource source = player.damageSources().generic();

        // 设置标记，通知 MixinLivingEntity 和 MixinInfinityEvents 接管
        IS_RECOIL_ACTIVE.set(true);
        try {
            // 调用 hurt，Mixin 会拦截它并处理无下限穿透和静默
            player.hurt(source, amount);
        } finally {
            IS_RECOIL_ACTIVE.set(false);
        }
    }

    @Override
    public void onActivate(LivingEntity owner, ConditionParams params) {}
    @Override
    public CheckTrigger[] getTriggers() { return new CheckTrigger[] { CheckTrigger.ABILITY_EXECUTED }; }
    @Override
    public boolean requiresTickCheck() { return false; }
    @Override
    public boolean isConfigurable() { return true; }
    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        // 使用 addInt，范围 1% - 50%，默认 5%
        return new ParamDefinition()
                .addInt(PARAM_DAMAGE_PERCENT_INT,
                        Component.translatable("condition.jujutsu_addon.recoil.param.damage"),
                        1, 50, 5);
    }
    @Override
    public ConditionParams createDefaultParams() {
        return new ConditionParams().setInt(PARAM_DAMAGE_PERCENT_INT, 5);
    }
    @Override
    public CompoundTag serializeParams(ConditionParams params) { return params.serializeNBT(); }
    @Override
    public ConditionParams deserializeParams(CompoundTag nbt) { return ConditionParams.fromNBT(nbt); }
}
