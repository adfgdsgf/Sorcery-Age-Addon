package com.jujutsuaddon.addon.mixin.summon;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import com.jujutsuaddon.addon.util.helper.MobCompatUtils;
import com.jujutsuaddon.addon.util.helper.SummonScalingHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

import java.util.UUID;

@Mixin(SummonEntity.class)
public abstract class MixinSummonScaling extends TamableAnimal {

    private static final UUID HP_MODIFIER_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000001");
    private static final UUID ATK_MODIFIER_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000002");
    private static final UUID JJK_SORCERER_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    @Unique private double lastCalculatedHpBonus = -1.0;
    @Unique private double lastCalculatedAtkBonus = -1.0;
    @Unique private float lastMaxHealth = -1.0f;

    @Unique private double cachedDilutionFactor = 1.0;
    @Unique private int dilutionUpdateTimer = 0;

    @Unique private double lastLoggedTotalHpBonus = -1.0;
    @Unique private double lastLoggedTotalAtkBonus = -1.0;

    protected MixinSummonScaling(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "onAddedToWorld", at = @At("TAIL"), remap = false)
    private void onJJKAddedToWorld(CallbackInfo ci) {
        if (this.level().isClientSide) return;
        if (this.getOwner() instanceof Player owner) {
            com.jujutsuaddon.addon.util.helper.WeaponEffectProxy.registerSummonOwner(this, owner);
        }
        this.updateDilutionFactor();
        this.runScalingLogic();
        this.setHealth(this.getMaxHealth());
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(this,
                    new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(this.getId(),
                            this.getAttributes().getSyncableAttributes()));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onJJKTick(CallbackInfo ci) {
        if (this.level().isClientSide) return;

        if (this.dilutionUpdateTimer-- <= 0) {
            this.updateDilutionFactor();
            this.dilutionUpdateTimer = 100 + this.random.nextInt(20);
        }

        if (this.tickCount < 60) {
            if (this.tickCount % 10 == 0) {
                this.runScalingLogic();
                if (this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
            }
        } else {
            if (this.tickCount % 40 == 0) {
                this.runScalingLogic();
            }
        }
    }

    @Unique
    private void updateDilutionFactor() {
        if (this.getOwner() instanceof Player owner) {
            this.cachedDilutionFactor = SummonScalingHelper.calculateDilutionFactor(this, owner);
        } else {
            this.cachedDilutionFactor = 1.0;
        }
    }

    @Unique
    private void runScalingLogic() {
        if (!AddonConfig.COMMON.enableSummonScaling.get()) return;
        LivingEntity owner = this.getOwner();
        if (owner == null) return;
        if (!MobCompatUtils.isAllowed(owner)) return;
        String className = this.getClass().getName();
        if (!className.startsWith("radon.jujutsu_kaisen")) return;

        if (this.isTame()) {
            if (!this.getTags().contains("jjk_tamed_ally")) {
                this.addTag("jjk_tamed_ally");
                this.removeTag("jjk_ritual_enemy");
            }
        }

        AttributeInstance hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);

        double mainModHpBonus = SummonScalingHelper.scanAndCleanMainModBonus(
                this, hpAttr, HP_MODIFIER_UUID, "jjk_addon_main_mod_hp_bonus");
        double mainModAtkBonus = SummonScalingHelper.scanAndCleanMainModBonus(
                this, atkAttr, ATK_MODIFIER_UUID, "jjk_addon_main_mod_atk_bonus");

        double dilutionFactor = this.cachedDilutionFactor;

        // ★ 简化：直接调用新方法
        double tierMult = SummonScalingHelper.calculateTierMultiplier(this, owner);

        double swarmMult = SummonScalingHelper.calculateSwarmMultiplier(this);
        double finalMultiplier = tierMult * swarmMult * dilutionFactor;

        boolean isTamedAlly = this.isTame();
        boolean allowUntamedScaling = AddonConfig.COMMON.enableUntamedStatScaling.get();
        boolean shouldScale = isTamedAlly || allowUntamedScaling;

        // --- 血量计算 ---
        double rawOwnerHp = owner.getMaxHealth();
        double safeOwnerHp = (rawOwnerHp < 0 || Double.isNaN(rawOwnerHp) || Double.isInfinite(rawOwnerHp))
                ? 20.0 : rawOwnerHp;

        double hpRatio = AddonConfig.COMMON.summonHpRatio.get();
        double scalingHpBonus = shouldScale ? (safeOwnerHp * hpRatio * finalMultiplier) : 0.0;
        double totalHpBonus = scalingHpBonus + mainModHpBonus;
        if (totalHpBonus < 0) totalHpBonus = 0;

        // --- 攻击力计算 ---
        double rawOwnerDmg;
        if (owner instanceof Player player) {
            rawOwnerDmg = calculatePlayerBaseAttack(player);
        } else {
            AttributeInstance mobAtkAttr = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            rawOwnerDmg = (mobAtkAttr != null) ? mobAtkAttr.getValue() : 1.0;
        }
        if (rawOwnerDmg < 0 || Double.isNaN(rawOwnerDmg) || Double.isInfinite(rawOwnerDmg)) {
            rawOwnerDmg = 1.0;
        }

        double externalMultiplier = SummonScalingHelper.calculateOffensiveMultiplier(owner);
        double dpsConfigFactor = AddonConfig.COMMON.summonDpsCompensationFactor.get();

        double ownerAtkSpeed = 4.0;
        AttributeInstance speedAttr = owner.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) {
            ownerAtkSpeed = speedAttr.getValue();
        }
        if (Double.isNaN(ownerAtkSpeed) || ownerAtkSpeed <= 0) ownerAtkSpeed = 4.0;

        double dpsMultiplier = 1.0;
        if (dpsConfigFactor > 0.001) {
            dpsMultiplier = Math.max(4.0, ownerAtkSpeed) * dpsConfigFactor;
        }

        double effectiveOwnerDamage = rawOwnerDmg * externalMultiplier * dpsMultiplier;
        double atkRatio = AddonConfig.COMMON.summonAtkRatio.get();
        double scalingAtkBonus = shouldScale ? (effectiveOwnerDamage * atkRatio * finalMultiplier) : 0.0;
        double totalAtkBonus = scalingAtkBonus + mainModAtkBonus;
        if (totalAtkBonus < 0) totalAtkBonus = 0;

        // 调试日志
        if (owner instanceof Player player && DebugManager.isDebugging(player)) {
            boolean hpChanged = Math.abs(totalHpBonus - lastLoggedTotalHpBonus) > 0.1;
            boolean atkChanged = Math.abs(totalAtkBonus - lastLoggedTotalAtkBonus) > 0.1;
            if (hpChanged || atkChanged) {
                DamageDebugUtil.logFullSummonStatus(player, this, scalingHpBonus, mainModHpBonus, totalHpBonus,
                        this.getHealth(), this.getMaxHealth(), rawOwnerDmg, externalMultiplier,
                        ownerAtkSpeed, dpsConfigFactor, dpsMultiplier, effectiveOwnerDamage,
                        atkRatio, finalMultiplier, scalingAtkBonus, -1.0f);
                lastLoggedTotalHpBonus = totalHpBonus;
                lastLoggedTotalAtkBonus = totalAtkBonus;
            }
        }

        applyAttributes(hpAttr, atkAttr, totalHpBonus, totalAtkBonus);
        SummonScalingHelper.handleAttributeInheritance(this, owner, dilutionFactor, finalMultiplier);
    }

    @Unique
    private void applyAttributes(AttributeInstance hpAttr, AttributeInstance atkAttr,
                                 double totalHpBonus, double totalAtkBonus) {
        if (hpAttr != null) {
            boolean isSignificantChange = Math.abs(lastCalculatedHpBonus - totalHpBonus) >
                    (lastCalculatedHpBonus * 0.01 + 0.1);
            if (isSignificantChange || hpAttr.getModifier(HP_MODIFIER_UUID) == null) {
                lastCalculatedHpBonus = totalHpBonus;
                applyHpModifier(hpAttr, totalHpBonus);
            }
        }

        if (atkAttr != null) {
            boolean isSignificantChange = Math.abs(lastCalculatedAtkBonus - totalAtkBonus) >
                    (lastCalculatedAtkBonus * 0.01 + 0.1);
            if (isSignificantChange || atkAttr.getModifier(ATK_MODIFIER_UUID) == null) {
                lastCalculatedAtkBonus = totalAtkBonus;
                atkAttr.removeModifier(ATK_MODIFIER_UUID);
                if (totalAtkBonus > 0.1) {
                    atkAttr.addTransientModifier(new AttributeModifier(
                            ATK_MODIFIER_UUID, "JJK Addon Summon Atk", totalAtkBonus,
                            AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    @Unique
    private double calculatePlayerBaseAttack(Player player) {
        AttributeInstance att = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att == null) return 1.0;
        double base = att.getBaseValue();
        double flatBonus = 0.0;
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.ADDITION)) {
            if (mod.getId().equals(JJK_SORCERER_DAMAGE_UUID)) continue;
            flatBonus += mod.getAmount();
        }
        return Math.max(1.0, base + flatBonus);
    }

    @Unique
    private void applyHpModifier(AttributeInstance hpAttr, double newBonus) {
        float currentMax = this.getMaxHealth();
        float currentHp = this.getHealth();

        AttributeModifier existing = hpAttr.getModifier(HP_MODIFIER_UUID);

        boolean isFirstRun = (lastMaxHealth < 0);
        if (isFirstRun) lastMaxHealth = currentMax;

        float ratio;
        if (isFirstRun && existing == null) {
            ratio = 1.0f;
        } else {
            ratio = SummonScalingHelper.calculateSmartHealthRatio(currentHp, currentMax, lastMaxHealth);
        }

        boolean attributesChanged = false;
        if (existing == null) {
            if (newBonus > 0.1) attributesChanged = true;
        } else {
            if (Math.abs(existing.getAmount() - newBonus) > (existing.getAmount() * 0.01 + 0.1)) {
                attributesChanged = true;
            }
        }

        if (attributesChanged || Math.abs(currentMax - lastMaxHealth) > (lastMaxHealth * 0.01 + 0.1)
                || (isFirstRun && existing == null)) {

            if (attributesChanged) {
                hpAttr.removeModifier(HP_MODIFIER_UUID);
                if (newBonus > 0.1) {
                    hpAttr.addTransientModifier(new AttributeModifier(
                            HP_MODIFIER_UUID, "JJK Addon Summon HP", newBonus,
                            AttributeModifier.Operation.ADDITION));
                }
                currentMax = this.getMaxHealth();
            }

            float targetHealth = currentMax * ratio;

            if (Math.abs(targetHealth - this.getHealth()) > 0.5 || currentMax < lastMaxHealth) {
                this.setHealth(targetHealth);
            }

            lastMaxHealth = currentMax;
        } else {
            lastMaxHealth = currentMax;
        }
    }

    // ★★★ 移除了整个 updateCostCache 方法 ★★★
    // 因为新系统通过 AbilityBalancer.getSummonMultiplier() 自动处理
}
