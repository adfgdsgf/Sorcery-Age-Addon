package com.jujutsuaddon.addon.compat.mob;

import com.jujutsuaddon.addon.config.AddonConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.entity.JJKEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MobTechniqueHelper {
    private static final Random RANDOM = new Random();

    public static CursedTechnique getRandomTechnique() {
        List<? extends String> blacklist = AddonConfig.COMMON.randomTechniqueBlacklist.get();
        List<CursedTechnique> validTechniques = new ArrayList<>();
        for (CursedTechnique t : CursedTechnique.values()) {
            if (!blacklist.contains(t.name())) {
                validTechniques.add(t);
            }
        }
        return !validTechniques.isEmpty() ? validTechniques.get(RANDOM.nextInt(validTechniques.size())) : CursedTechnique.SHRINE;
    }

    public static void applyTechnique(LivingEntity mob, ISorcererData cap, CursedTechnique technique) {
        cap.setTechnique(technique);
        if (technique != null) {
            // 1. 解锁术式对应的所有技能
            for (radon.jujutsu_kaisen.ability.base.Ability ability : technique.getAbilities()) {
                cap.unlock(ability);
            }

            // 2. 解锁领域（如果有）
            if (technique.getDomain() != null) {
                cap.unlock(technique.getDomain());
            }

            // 3. 十种影法术特判：自动调伏所有式神
            if (technique == CursedTechnique.TEN_SHADOWS) {
                mob.getCapability(TenShadowsDataHandler.INSTANCE).ifPresent(shadowCap -> {
                    Registry<EntityType<?>> registry = mob.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
                    try {
                        shadowCap.tame(registry, JJKEntities.DIVINE_DOG_WHITE.get());
                        shadowCap.tame(registry, JJKEntities.DIVINE_DOG_BLACK.get());
                        shadowCap.tame(registry, JJKEntities.DIVINE_DOG_TOTALITY.get());
                        shadowCap.tame(registry, JJKEntities.TOAD.get());
                        shadowCap.tame(registry, JJKEntities.TOAD_FUSION.get());
                        shadowCap.tame(registry, JJKEntities.GREAT_SERPENT.get());
                        shadowCap.tame(registry, JJKEntities.NUE.get());
                        shadowCap.tame(registry, JJKEntities.NUE_TOTALITY.get());
                        shadowCap.tame(registry, JJKEntities.MAX_ELEPHANT.get());
                        shadowCap.tame(registry, JJKEntities.RABBIT_ESCAPE.get());
                        shadowCap.tame(registry, JJKEntities.TRANQUIL_DEER.get());
                        shadowCap.tame(registry, JJKEntities.PIERCING_BULL.get());
                        shadowCap.tame(registry, JJKEntities.AGITO.get());
                        shadowCap.tame(registry, JJKEntities.MAHORAGA.get());
                    } catch (Exception e) {
                        // 忽略异常
                    }
                });
            }
        }
    }

    /**
     * [新增] 动态管理被动技能的开关状态
     * 应在 LivingTickEvent 中低频调用 (例如每秒一次)
     */
    public static void updateToggles(Mob mob, ISorcererData cap) {
        // 只有当咒力足够时才尝试开启 (防止低咒力时反复开关鬼畜)
        boolean hasEnergy = cap.getEnergy() > 20.0;
        boolean inCombat = mob.getTarget() != null && mob.getTarget().isAlive();

        // 1. 无下限术式 (Infinity) 控制逻辑
        if (cap.getTechnique() == CursedTechnique.LIMITLESS) {
            Ability infinity = JJKAbilities.INFINITY.get();
            if (cap.isUnlocked(infinity)) {
                boolean isOn = cap.hasToggled(infinity);
                boolean shouldBeOn;

                // 判定逻辑：
                // 如果有六眼 (回蓝 > 消耗)，则无条件常驻开启
                // 否则，只在战斗状态下开启
                if (cap.hasTrait(Trait.SIX_EYES)) {
                    shouldBeOn = true;
                } else {
                    shouldBeOn = inCombat;
                }

                // 状态不一致且有蓝时，切换状态
                if (shouldBeOn != isOn && hasEnergy) {
                    cap.toggle(infinity);
                }
                // 如果应该关闭，强制关闭 (不管有没有蓝)
                else if (!shouldBeOn && isOn) {
                    cap.toggle(infinity);
                }
            }
        }

        // 2. 咒力流动 (Cursed Energy Flow) 控制逻辑
        Ability ceFlow = JJKAbilities.CURSED_ENERGY_FLOW.get();
        if (cap.isUnlocked(ceFlow)) {
            boolean isOn = cap.hasToggled(ceFlow);
            // 判定逻辑：只在战斗状态开启
            boolean shouldBeOn = inCombat;

            if (shouldBeOn != isOn && hasEnergy) {
                cap.toggle(ceFlow);
            } else if (!shouldBeOn && isOn) {
                cap.toggle(ceFlow);
            }
        }

        // 3. 宿傩 - 捌 (Cleave) 控制逻辑
        if (cap.getTechnique() == CursedTechnique.SHRINE) {
            Ability cleave = JJKAbilities.CLEAVE.get();
            if (cap.isUnlocked(cleave)) {
                boolean isOn = cap.hasToggled(cleave);
                // 判定逻辑：只在战斗状态开启
                boolean shouldBeOn = inCombat;

                if (shouldBeOn != isOn && hasEnergy) {
                    cap.toggle(cleave);
                } else if (!shouldBeOn && isOn) {
                    cap.toggle(cleave);
                }
            }
        }
    }
}
