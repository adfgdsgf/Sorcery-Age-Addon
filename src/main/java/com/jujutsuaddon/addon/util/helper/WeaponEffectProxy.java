package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.compat.mob.MobConfigManager;
import com.jujutsuaddon.addon.mixin.access.LivingEntityAccessor;
import com.jujutsuaddon.addon.util.context.WeaponProxyContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class WeaponEffectProxy {

    private static final Map<UUID, WeaponContext> pendingAbilityDamage = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> summonOwnerCache = new ConcurrentHashMap<>();
    private static final Set<UUID> processingTargets = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, Set<UUID>> entityEffectTracker = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> entityTimestamps = new ConcurrentHashMap<>();

    public record WeaponContext(
            ItemStack weapon,
            @Nullable Player owner,        // 可能为 null（独立的白名单生物）
            LivingEntity attacker,          // 实际攻击者（玩家或白名单生物）
            long timestamp,
            @Nullable UUID entityId
    ) {}

    // ==================== 核心判断逻辑 ====================

    /**
     * 检查实体是否是有效的攻击者（玩家或白名单生物）
     */
    private static boolean isValidAttacker(Entity entity) {
        // 玩家始终有效
        if (entity instanceof Player) {
            return true;
        }

        // 白名单生物（需要配置开启）
        if (AddonConfig.COMMON.enableWhitelistMobWeaponProxy.get()) {
            if (entity instanceof LivingEntity living) {
                if (MobConfigManager.getMobConfig(living) != null) {
                    return true;
                }
            }
        }

        return false;
    }
    /**
     * 判断是否应该触发武器效果
     */
    public static boolean shouldTriggerWeaponEffect(DamageSource source) {
        if (WeaponProxyContext.isActive()) {
            return false;
        }

        Entity directEntity = source.getDirectEntity();
        Entity sourceEntity = source.getEntity();

        if (directEntity == null) {
            return false;
        }

        // 情况1：式神攻击（要放在 direct == source 检查之前！）
        if (directEntity instanceof LivingEntity livingDirect && isJJKEntity(directEntity)) {
            if (AddonConfig.COMMON.enableShikigamiWeaponProxy.get() && isTamedShikigami(directEntity)) {
                Player owner = getOwner(livingDirect);
                return owner != null;
            }
            return false;
        }

        // 情况2：技能投射物攻击（direct != source）
        if (directEntity == sourceEntity) {
            return false;
        }

        if (!isValidAttacker(sourceEntity)) {
            return false;
        }

        return isJJKEntity(directEntity);
    }

    public static boolean isJJKEntity(Entity entity) {
        if (entity == null) return false;
        String className = entity.getClass().getName();
        return className.contains("jujutsu_kaisen");
    }
    /**
     * 检查是否是已调伏的式神
     */
    private static boolean isTamedShikigami(Entity entity) {
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame();
        }
        return false;
    }

    // ==================== Mixin 调用入口 ====================

    public static void markAbilityDamage(DamageSource source, LivingEntity target) {
        if (!AddonConfig.COMMON.enableWeaponEffectProxy.get()) return;
        if (source == null || target == null) return;
        if (!shouldTriggerWeaponEffect(source)) return;
        if (processingTargets.contains(target.getUUID())) return;
        Entity directEntity = source.getDirectEntity();
        Entity sourceEntity = source.getEntity();
        LivingEntity attacker = null;
        Player owner = null;
        ItemStack weapon = ItemStack.EMPTY;
        UUID entityId = directEntity != null ? directEntity.getUUID() : null;
        // 情况1：玩家技能攻击
        if (sourceEntity instanceof Player player) {
            attacker = player;
            owner = player;
            weapon = player.getMainHandItem();
        }
        // 情况2：白名单生物技能攻击
        else if (sourceEntity instanceof LivingEntity living
                && AddonConfig.COMMON.enableWhitelistMobWeaponProxy.get()
                && MobConfigManager.getMobConfig(living) != null) {
            attacker = living;
            owner = getOwner(living);
            weapon = living.getMainHandItem();
        }
        // 情况3：式神直接攻击（directEntity 是式神）
        else if (directEntity instanceof LivingEntity shikigami
                && AddonConfig.COMMON.enableShikigamiWeaponProxy.get()
                && isJJKEntity(directEntity)
                && isTamedShikigami(directEntity)) {
            Player shikigamiOwner = getOwner(shikigami);
            if (shikigamiOwner != null) {
                attacker = shikigami;
                owner = shikigamiOwner;
                weapon = shikigamiOwner.getMainHandItem();  // ← 用主人的武器！
                entityId = null;
            }
        }

        if (attacker == null || weapon.isEmpty()) return;



        if (entityId != null && hasEntityTriggeredEffect(entityId, target.getUUID())) {
            return;
        }

        pendingAbilityDamage.put(target.getUUID(), new WeaponContext(
                weapon.copy(),
                owner,      // 可能为 null
                attacker,   // 永远不为 null
                attacker.level().getGameTime(),
                entityId
        ));
    }

    // ==================== 实体效果追踪 ====================

    private static boolean hasEntityTriggeredEffect(UUID entityId, UUID targetId) {
        if (entityId == null) return false;
        Set<UUID> triggeredTargets = entityEffectTracker.get(entityId);
        return triggeredTargets != null && triggeredTargets.contains(targetId);
    }

    private static void markEntityTriggeredEffect(UUID entityId, UUID targetId, long currentTick) {
        if (entityId == null) return;
        entityEffectTracker.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet()).add(targetId);
        entityTimestamps.putIfAbsent(entityId, currentTick);
    }

    // ==================== 召唤物管理 ====================

    public static void registerSummonOwner(Entity summon, Player owner) {
        if (summon != null && owner != null) {
            summonOwnerCache.put(summon.getUUID(), owner.getUUID());
        }
    }

    // ==================== 事件处理 ====================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAbilityDamageEarly(LivingHurtEvent event) {
        if (!AddonConfig.COMMON.enableWeaponEffectProxy.get()) return;
        if (event.getEntity().level().isClientSide()) return;

        markAbilityDamage(event.getSource(), event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!AddonConfig.COMMON.enableWeaponEffectProxy.get()) return;
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        long currentTick = target.level().getGameTime();

        WeaponContext ctx = pendingAbilityDamage.remove(target.getUUID());

        if (ctx == null) {
            trySetKillAttribution(event.getSource(), target);
            return;
        }

        if (currentTick - ctx.timestamp > 5) return;

        if (ctx.entityId != null && hasEntityTriggeredEffect(ctx.entityId, target.getUUID())) {
            return;
        }

        triggerWeaponEffects(ctx.owner, ctx.attacker, target, ctx.weapon,
                event.getAmount(), ctx.entityId, currentTick);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!AddonConfig.COMMON.enableWeaponEffectProxy.get()) return;
        if (event.getEntity().level().isClientSide()) return;

        Player owner = findOwnerFromSource(event.getSource());
        if (owner != null) {
            ensureKillAttribution(event.getEntity(), owner);
        }
    }

    // ==================== 武器效果触发 ====================

    private static void triggerWeaponEffects(@Nullable Player owner, LivingEntity attacker,
                                             LivingEntity target, ItemStack weapon, float damage,
                                             @Nullable UUID entityId, long currentTick) {
        UUID targetId = target.getUUID();
        if (processingTargets.contains(targetId)) return;
        processingTargets.add(targetId);
        if (entityId != null) {
            markEntityTriggeredEffect(entityId, targetId, currentTick);
        }
        try {
            // 触发武器的 hurtEnemy
            if (AddonConfig.COMMON.enableItemHurtEnemyTrigger.get() && !isBlacklisted(weapon)) {
                ItemStack copy = weapon.copy();
                // 决定谁来触发效果
                // 式神情况：用主人触发，这样主人的 Capability 能生效
                LivingEntity effectUser = attacker;
                if (owner != null && attacker != owner) {
                    effectUser = owner;
                }
                WeaponProxyContext.set(true);
                try {
                    copy.getItem().hurtEnemy(copy, target, effectUser);
                } finally {
                    WeaponProxyContext.clear();
                }
            }
            // 设置攻击关系
            attacker.setLastHurtMob(target);
            target.setLastHurtByMob(attacker);
            // 只有存在玩家 owner 时才设置击杀归属
            if (owner != null) {
                ensureKillAttribution(target, owner);
            }
            // 发布自定义事件
            MinecraftForge.EVENT_BUS.post(new WeaponHitEvent(owner, attacker, target, weapon, damage));
        } finally {
            if (target.level().getServer() != null) {
                target.level().getServer().execute(() -> processingTargets.remove(targetId));
            } else {
                processingTargets.remove(targetId);
            }
        }
    }

    // ==================== 击杀归属 ====================

    private static void trySetKillAttribution(DamageSource source, LivingEntity target) {
        Player owner = findOwnerFromSource(source);
        if (owner != null) {
            ensureKillAttribution(target, owner);
        }
    }

    private static void ensureKillAttribution(LivingEntity victim, Player killer) {
        LivingEntityAccessor accessor = (LivingEntityAccessor) victim;
        accessor.jujutsu_addon$setLastHurtByPlayer(killer);
        accessor.jujutsu_addon$setLastHurtByPlayerTime(100);
    }

    // ==================== 所有者查找 ====================

    @Nullable
    public static Player getOwner(LivingEntity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player p) return p;

        UUID ownerId = summonOwnerCache.get(entity.getUUID());
        if (ownerId != null) {
            return getPlayerById(entity.level(), ownerId);
        }
        return null;
    }

    @Nullable
    private static Player findOwnerFromSource(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof Player p) return p;
        if (entity instanceof LivingEntity living) return getOwner(living);

        Entity direct = source.getDirectEntity();
        if (direct instanceof Player p) return p;
        if (direct instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player p) return p;

        return null;
    }

    @Nullable
    private static Player getPlayerById(net.minecraft.world.level.Level level, UUID playerId) {
        if (playerId == null || level == null) return null;
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(playerId);
            if (entity instanceof Player player) return player;
            if (serverLevel.getServer() != null) {
                return serverLevel.getServer().getPlayerList().getPlayer(playerId);
            }
        }
        return null;
    }

    // ==================== 工具方法 ====================

    private static boolean isBlacklisted(ItemStack weapon) {
        String itemId = ForgeRegistries.ITEMS.getKey(weapon.getItem()).toString();
        List<? extends String> blacklist = AddonConfig.COMMON.hurtEnemyBlacklist.get();
        return blacklist.stream().anyMatch(itemId::contains);
    }

    public static void cleanupExpiredCache(long currentTime) {
        pendingAbilityDamage.entrySet().removeIf(e -> currentTime - e.getValue().timestamp > 100);

        entityTimestamps.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > 200) {
                entityEffectTracker.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    // ==================== 自定义事件 ====================

    public static class WeaponHitEvent extends Event {
        @Nullable private final Player owner;
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final ItemStack weapon;
        private final float damage;

        public WeaponHitEvent(@Nullable Player owner, LivingEntity attacker,
                              LivingEntity target, ItemStack weapon, float damage) {
            this.owner = owner;
            this.attacker = attacker;
            this.target = target;
            this.weapon = weapon;
            this.damage = damage;
        }

        @Nullable public Player getOwner() { return owner; }
        public LivingEntity getAttacker() { return attacker; }
        public LivingEntity getTarget() { return target; }
        public ItemStack getWeapon() { return weapon; }
        public float getDamage() { return damage; }
    }
}
