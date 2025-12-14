package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.util.helper.AdaptationUtil; // 导入我们的新工具类
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.DomainExpansion.IOpenDomain;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.Adaptation;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class AdaptationReinforcementHandler {

    private static final int DISRUPTION_DURATION = 20;

    // ==========================================
    // 1. 防御端：减伤与自动反击
    // ==========================================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide || !AddonConfig.COMMON.playerAdaptationEnabled.get()) return;

        ISorcererData sorcererCap = getSorcererCap(player);
        if (sorcererCap == null || !sorcererCap.hasToggled(JJKAbilities.WHEEL.get())) return;

        ITenShadowsData shadowCap = getShadowCap(player);
        if (shadowCap == null) return;

        DamageSource source = event.getSource();
        float progress = shadowCap.getAdaptationProgress(source);

        // 自动反击逻辑
        if (progress > 0 && AddonConfig.COMMON.playerAutoCounter.get() &&
                shadowCap.getAdaptationType(source) == Adaptation.Type.COUNTER) {
            if (HelperMethods.RANDOM.nextInt(Math.max(1, Math.round(20 * (1.0F - progress)))) == 0) {
                performAutoCounter(player, source);
            }
        }

        // 完全免疫逻辑
        if (progress >= 0.999F || shadowCap.isAdaptedTo(source)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide || !AddonConfig.COMMON.playerAdaptationEnabled.get()) return;

        ISorcererData sorcererCap = getSorcererCap(player);
        if (sorcererCap == null || !sorcererCap.hasToggled(JJKAbilities.WHEEL.get())) return;

        ITenShadowsData shadowCap = getShadowCap(player);
        if (shadowCap == null) return;

        float progress = shadowCap.getAdaptationProgress(event.getSource());
        if (progress > 0) {
            event.setAmount(event.getAmount() * (1.0F - progress));
        }
    }

    // ==========================================
    // 2. 进攻端：术式干扰
    // ==========================================
    @SubscribeEvent
    public static void onPlayerAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide || !AddonConfig.COMMON.playerTechniqueDisruption.get()) return;

        LivingEntity victim = event.getEntity();
        ISorcererData attackerCap = getSorcererCap(attacker);
        if (attackerCap == null || !attackerCap.hasToggled(JJKAbilities.WHEEL.get())) return;

        ITenShadowsData shadowCap = getShadowCap(attacker);
        if (shadowCap == null) return;

        ISorcererData victimCap = getSorcererCap(victim);
        if (victimCap == null) return;

        // 干扰已开启的术式
        Set<Ability> toggled = new HashSet<>(victimCap.getToggled());
        for (Ability ability : toggled) {
            if (ability instanceof IOpenDomain) continue;
            if (isStrictlyAdaptedToAbility(shadowCap, ability)) {
                int level = getStrictAdaptationLevel(shadowCap, ability);
                victimCap.disrupt(ability, DISRUPTION_DURATION * level);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        // 干扰正在蓄力的术式
        Ability channeled = victimCap.getChanneled();
        if (channeled != null && isStrictlyAdaptedToAbility(shadowCap, channeled)) {
            int level = getStrictAdaptationLevel(shadowCap, channeled);
            victimCap.disrupt(channeled, DISRUPTION_DURATION * level);
        }

        if (victim instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(victimCap.serializeNBT()), serverPlayer);
        }
    }

    // ==========================================
    // 3. 状态管理：免疫与衰减 (核心重构部分)
    // ==========================================
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player) || !AddonConfig.COMMON.playerAdaptationEnabled.get()) return;

        ISorcererData sorcererCap = getSorcererCap(entity);
        if (sorcererCap == null) return;

        ITenShadowsData shadowCap = getShadowCap(entity);
        if (shadowCap == null) return;

        boolean isWheelActive = sorcererCap.hasToggled(JJKAbilities.WHEEL.get());

        if (isWheelActive) {
            // 轮子开启：检查是否适应了无量空处
            if (isAdaptedToVoid(shadowCap)) {
                // 移除原版混乱效果
                if (entity.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION)) {
                    entity.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
                }

                // 检查是否正在遭受领域攻击
                long lastHitTick = entity.getPersistentData().getLong("JJK_Addon_LastUVHit");
                if (Math.abs(entity.tickCount - lastHitTick) < 20) {
                    // 调用工具类清除脑部损伤
                    AdaptationUtil.clearBrainDamage(entity, sorcererCap);
                }
            }
        } else {
            // 轮子关闭：执行衰减
            if (entity.isAlive()) {
                // 调用工具类执行衰减，如果有变化则同步
                if (AdaptationUtil.performDecay(shadowCap)) {
                    AdaptationUtil.syncShadowData(entity, shadowCap);
                }
            }
        }
    }

    // ==========================================
    // 辅助方法 (Helper Methods)
    // ==========================================

    private static void performAutoCounter(Player player, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker != null) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, attacker.getEyePosition());
            player.swing(InteractionHand.MAIN_HAND);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.resetAttackStrengthTicker();
            player.attack(attacker);
            player.invulnerableTime = 0;
        }
    }

    private static boolean isAdaptedToVoid(ITenShadowsData cap) {
        for (Adaptation adaptation : cap.getAdapted().keySet()) {
            Ability ability = adaptation.getAbility();
            if (ability != null && ability.getClass().getSimpleName().equals("UnlimitedVoid")) {
                return true;
            }
        }
        return false;
    }

    private static ISorcererData getSorcererCap(LivingEntity entity) {
        return entity.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
    }

    private static ITenShadowsData getShadowCap(LivingEntity entity) {
        return entity.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
    }

    private static boolean isStrictlyAdaptedToAbility(ITenShadowsData cap, Ability targetAbility) {
        if (targetAbility == null) return false;
        for (Adaptation adaptation : cap.getAdapted().keySet()) {
            Ability adaptedAbility = adaptation.getAbility();
            if (adaptedAbility != null && adaptedAbility.getClass() == targetAbility.getClass()) {
                return true;
            }
        }
        return false;
    }

    private static int getStrictAdaptationLevel(ITenShadowsData cap, Ability targetAbility) {
        if (targetAbility == null) return 0;
        for (var entry : cap.getAdapted().entrySet()) {
            Adaptation adaptation = entry.getKey();
            Ability adaptedAbility = adaptation.getAbility();
            if (adaptedAbility != null && adaptedAbility.getClass() == targetAbility.getClass()) {
                return entry.getValue();
            }
        }
        return 0;
    }

    // 状态免疫拦截 (保持原样，为了完整性放在这里)
    @SubscribeEvent
    public static void onDomainHit(radon.jujutsu_kaisen.ability.LivingHitByDomainEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !AddonConfig.COMMON.playerAdaptationEnabled.get()) return;
        if (event.getAbility() == null || !event.getAbility().getClass().getSimpleName().equals("UnlimitedVoid")) return;

        ISorcererData sorcererCap = getSorcererCap(entity);
        if (sorcererCap == null || !sorcererCap.hasToggled(JJKAbilities.WHEEL.get())) return;
        ITenShadowsData shadowCap = getShadowCap(entity);
        if (shadowCap == null) return;

        if (isAdaptedToVoid(shadowCap)) {
            entity.getPersistentData().putLong("JJK_Addon_LastUVHit", entity.tickCount);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player) || !AddonConfig.COMMON.playerAdaptationEnabled.get()) return;

        ISorcererData sorcererCap = getSorcererCap(entity);
        if (sorcererCap == null || !sorcererCap.hasToggled(JJKAbilities.WHEEL.get())) return;

        net.minecraft.world.effect.MobEffect effect = event.getEffectInstance().getEffect();
        boolean isCoreEffect = effect == radon.jujutsu_kaisen.effect.JJKEffects.UNLIMITED_VOID.get()
                || effect == radon.jujutsu_kaisen.effect.JJKEffects.STUN.get();

        if (isCoreEffect) {
            ITenShadowsData shadowCap = getShadowCap(entity);
            if (shadowCap != null && isAdaptedToVoid(shadowCap)) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                return;
            }
        }
        boolean isSideEffect = effect == net.minecraft.world.effect.MobEffects.BLINDNESS
                || effect == net.minecraft.world.effect.MobEffects.CONFUSION;

        if (isSideEffect) {
            long lastHitTick = entity.getPersistentData().getLong("JJK_Addon_LastUVHit");
            if (Math.abs(entity.tickCount - lastHitTick) <= 1) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        }
    }
}
