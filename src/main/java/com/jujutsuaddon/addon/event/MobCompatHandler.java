package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.compat.mob.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererGrade;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class MobCompatHandler {

    private static final String NBT_FACTION_INITIALIZED = "jujutsu_addon_faction_initialized";

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!AddonConfig.COMMON.enableMobCompatibility.get()) return;
        if (event.getObject() instanceof LivingEntity entity) {
            if (MobConfigManager.getMobConfig(entity) != null) {
                if (!event.getCapabilities().containsKey(SorcererDataHandler.SorcererDataProvider.IDENTIFIER)) {
                    SorcererDataHandler.SorcererDataProvider provider = new SorcererDataHandler.SorcererDataProvider();
                    provider.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> cap.init(entity));
                    event.addCapability(SorcererDataHandler.SorcererDataProvider.IDENTIFIER, provider);
                }
                if (!event.getCapabilities().containsKey(TenShadowsDataHandler.TenShadowsDataProvider.IDENTIFIER)) {
                    TenShadowsDataHandler.TenShadowsDataProvider provider = new TenShadowsDataHandler.TenShadowsDataProvider();
                    provider.getCapability(TenShadowsDataHandler.INSTANCE).ifPresent(cap -> cap.init(entity));
                    event.addCapability(TenShadowsDataHandler.TenShadowsDataProvider.IDENTIFIER, provider);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();

        if (!(sourceEntity instanceof TamableAnimal attackerTamable) || !attackerTamable.isTame()) {
            return;
        }

        if (MobConfigManager.getMobConfig(attackerTamable) == null) {
            return;
        }

        if (attackerTamable.isOwnedBy(victim)) {
            event.setCanceled(true);
            return;
        }

        if (victim instanceof TamableAnimal victimTamable && victimTamable.isTame()) {
            if (attackerTamable.getOwnerUUID() != null && attackerTamable.getOwnerUUID().equals(victimTamable.getOwnerUUID())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity() instanceof PathfinderMob mob) {
            if (MobConfigManager.getMobConfig(mob) != null) {
                mob.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                    cap.tick(mob);
                    if (mob.tickCount % 20 == mob.getId() % 20) {
                        MobLevelingHelper.handleLevelingAndStats(mob, cap);
                        MobTechniqueHelper.updateToggles(mob, cap);
                    }
                });
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof PathfinderMob mob) {
            if (MobConfigManager.getMobConfig(mob) != null) {
                initSorcererData(mob);
                MobTraitHelper.handleSpawn(mob);
                if (AddonConfig.COMMON.enableMobAI.get()) {
                    MobAIHelper.injectAI(mob);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() instanceof PathfinderMob mob) {
            if (MobConfigManager.getMobConfig(mob) != null) {
                initSorcererData(mob);
                MobTraitHelper.handleSpawn(mob);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof PathfinderMob attacker) {
            if (MobConfigManager.getMobConfig(attacker) != null) {
                attacker.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                    MobLevelingHelper.handleKillExperience(attacker, event.getEntity(), cap);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!event.getEntity().isShiftKeyDown()) return;

        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;

        // ★ 木棍查看信息（含阵营）★
        if (stack.getItem() == net.minecraft.world.item.Items.STICK) {
            if (event.getTarget() instanceof LivingEntity target) {
                target.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                    double exp = cap.getExperience();
                    double maxEnergy = cap.getMaxEnergy();
                    double currentEnergy = cap.getEnergy();
                    String technique = cap.getTechnique() != null ? cap.getTechnique().getName().getString() : Component.translatable("message.jujutsu_addon.technique.none").getString();

                    SorcererGrade calculatedGrade = SorcererGrade.GRADE_4;
                    for (SorcererGrade g : SorcererGrade.values()) {
                        if (exp >= g.getRequiredExperience()) {
                            if (g.getRequiredExperience() >= calculatedGrade.getRequiredExperience()) calculatedGrade = g;
                        }
                    }
                    String grade = calculatedGrade.toString();
                    float maxHealth = target.getMaxHealth();
                    float currentHealth = target.getHealth();

                    java.util.Set<Trait> traits = cap.getTraits();
                    String traitStr;
                    if (traits.isEmpty()) {
                        traitStr = Component.translatable("message.jujutsu_addon.trait.none").getString();
                    } else {
                        traitStr = traits.stream()
                                .map(t -> Component.translatable("trait.jujutsu_kaisen." + t.name().toLowerCase()).getString())
                                .collect(Collectors.joining(", "));
                    }

                    // ★ 获取阵营信息 ★
                    JujutsuType factionType = cap.getType();
                    String factionStr = Component.translatable(MobFactionHelper.getTypeTranslationKey(factionType)).getString();

                    Component msg = Component.translatable("message.jujutsu_addon.report.header")
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.target", target.getName()))
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.grade", grade))
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.technique", technique))
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.traits", traitStr))
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.faction", factionStr))  // ★ 新增阵营显示 ★
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.exp", exp))
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.energy", currentEnergy, maxEnergy))
                            .append("\n").append(Component.translatable("message.jujutsu_addon.report.health", currentHealth, maxHealth));

                    event.getEntity().sendSystemMessage(msg);
                    event.setCanceled(true);
                });
            }
        }
        // ★ 阵营重随 ★
        else if (event.getTarget() instanceof PathfinderMob mob && MobConfigManager.getMobConfig(mob) != null) {
            List<? extends String> factionItems = AddonConfig.COMMON.factionRerollItems.get();
            if (factionItems.contains(itemId.toString())) {
                mob.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                    JujutsuType newType = MobFactionHelper.getRandomType();
                    cap.setType(newType);

                    String factionName = Component.translatable(MobFactionHelper.getTypeTranslationKey(newType)).getString();
                    event.getEntity().sendSystemMessage(Component.translatable("message.jujutsu_addon.reroll.faction_result", mob.getName(), factionName));

                    if (!event.getEntity().isCreative()) stack.shrink(1);
                    event.setCanceled(true);
                });
                return;
            }
        }
        // 特质重随
        if (event.getTarget() instanceof PathfinderMob mob && MobTraitHelper.handleInteract(mob, stack, event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        // 术式重随
        if (AddonConfig.COMMON.enableTechniqueReroll.get()) {
            List<? extends String> allowedItems = AddonConfig.COMMON.rerollItems.get();
            if (allowedItems.contains(itemId.toString())) {
                if (event.getTarget() instanceof LivingEntity target) {
                    if (MobConfigManager.getMobConfig(target) != null) {
                        target.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                            CursedTechnique newTechnique = MobTechniqueHelper.getRandomTechnique();
                            MobTechniqueHelper.applyTechnique(target, cap, newTechnique);
                            cap.setEnergy(cap.getMaxEnergy());
                            String techName = newTechnique.getName().getString();
                            event.getEntity().sendSystemMessage(Component.translatable("message.jujutsu_addon.reroll.success", techName));
                            if (!event.getEntity().isCreative()) stack.shrink(1);
                            event.setCanceled(true);
                        });
                    }
                }
            }
        }
    }
    private static void initSorcererData(PathfinderMob mob) {
        MobConfigManager.MobConfigData data = MobConfigManager.getMobConfig(mob);
        if (data == null) return;
        mob.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            // 术式初始化（只初始化一次）
            if (cap.getTechnique() == null) {
                CursedTechnique technique = null;
                if ("RANDOM".equalsIgnoreCase(data.techniqueName())) {
                    technique = MobTechniqueHelper.getRandomTechnique();
                } else if (!"NONE".equalsIgnoreCase(data.techniqueName())) {
                    try {
                        technique = CursedTechnique.valueOf(data.techniqueName().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        technique = CursedTechnique.SHRINE;
                    }
                }
                MobTechniqueHelper.applyTechnique(mob, cap, technique);
                try {
                    cap.unlock(JJKAbilities.SIMPLE_DOMAIN.get());
                    cap.unlock(JJKAbilities.RCT1.get());
                    cap.unlock(JJKAbilities.RCT2.get());
                    cap.unlock(JJKAbilities.CURSED_ENERGY_FLOW.get());
                } catch (Exception e) { /* ignore */ }
                SorcererGrade grade;
                try {
                    grade = SorcererGrade.valueOf(data.gradeName().toUpperCase());
                } catch (IllegalArgumentException e) {
                    grade = SorcererGrade.GRADE_4;
                }
                cap.setGrade(grade);
                mob.getPersistentData().putString("jujutsu_addon_last_grade", grade.name());
                cap.setEnergy(cap.getMaxEnergy());
            }
            // ★ 阵营初始化（只初始化一次）★
            if (!mob.getPersistentData().getBoolean(NBT_FACTION_INITIALIZED)) {
                JujutsuType factionType = MobFactionHelper.parseType(data.jujutsuTypeName());
                cap.setType(factionType);
                mob.getPersistentData().putBoolean(NBT_FACTION_INITIALIZED, true);
            }
            cap.tick(mob);
        });
    }
}