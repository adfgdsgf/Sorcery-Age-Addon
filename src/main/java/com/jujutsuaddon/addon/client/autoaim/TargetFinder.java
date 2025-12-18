package com.jujutsuaddon.addon.client.autoaim;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class TargetFinder {

    private final Minecraft mc = Minecraft.getInstance();

    @Nullable
    public LivingEntity findTarget(double maxDistance, double fovAngle,
                                   Predicate<LivingEntity> filter,
                                   String priority,
                                   @Nullable LivingEntity currentTarget,
                                   boolean stickyTarget) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();

        AABB searchBox = player.getBoundingBox().inflate(maxDistance);

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
            if (entity == player) return false;
            if (!entity.isAlive()) return false;

            // 隐身检查
            if (AddonClientConfig.CLIENT.aimAssistIgnoreInvisible.get() && entity.isInvisible()) {
                return false;
            }

            // 类型过滤
            if (!filter.test(entity)) {
                return false;
            }

            // 距离检查
            double dist = entity.distanceTo(player);
            if (dist > maxDistance) return false;

            // FOV 检查
            Vec3 toTarget = entity.getEyePosition().subtract(eyePos).normalize();
            double dot = lookVec.dot(toTarget);
            dot = Math.max(-1.0, Math.min(1.0, dot));
            double angle = Math.toDegrees(Math.acos(dot));

            if (angle > fovAngle / 2.0) return false;

            // 视线检查
            if (AddonClientConfig.CLIENT.aimAssistRequireInitialSight.get()) {
                if (!hasLineOfSight(player, entity)) {
                    if (!(entity == currentTarget && AddonClientConfig.CLIENT.aimAssistThroughWalls.get())) {
                        return false;
                    }
                }
            }

            return true;
        });

        if (candidates.isEmpty()) return null;

        // 粘性目标：如果当前目标仍然有效，优先保持
        if (stickyTarget && currentTarget != null && candidates.contains(currentTarget)) {
            return currentTarget;
        }

        // 根据优先级排序
        Comparator<LivingEntity> comparator = getComparator(player, eyePos, lookVec, priority);

        return candidates.stream()
                .min(comparator)
                .orElse(null);
    }

    private Comparator<LivingEntity> getComparator(LocalPlayer player, Vec3 eyePos, Vec3 lookVec, String priority) {
        return switch (priority.toUpperCase()) {
            case "DISTANCE" -> Comparator.comparingDouble(e -> e.distanceTo(player));
            case "HEALTH" -> Comparator.comparingDouble(LivingEntity::getHealth);
            default -> // "ANGLE" - 优先准星附近
                    Comparator.comparingDouble(e -> {
                        Vec3 toTarget = e.getEyePosition().subtract(eyePos).normalize();
                        double dot = lookVec.dot(toTarget);
                        dot = Math.max(-1.0, Math.min(1.0, dot));
                        double angle = Math.acos(dot);
                        return angle * 1000 + e.distanceTo(player) * 0.01;
                    });
        };
    }

    public boolean hasLineOfSight(LocalPlayer player, LivingEntity target) {
        Vec3 start = player.getEyePosition();
        Vec3 end = target.getEyePosition();

        BlockHitResult result = mc.level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        return result.getType() == HitResult.Type.MISS ||
                result.getLocation().distanceToSqr(end) < 1.0;
    }

    /**
     * 获取目标的瞄准点（带预测）
     */
    public Vec3 getAimPoint(LivingEntity target, String targetPart, double heightOffset, boolean predict) {
        AABB box = target.getBoundingBox();

        double y = switch (targetPart.toUpperCase()) {
            case "FEET" -> box.minY;
            case "BODY" -> (box.minY + box.maxY) / 2.0;
            default -> target.getEyeY();
        };

        double x = target.getX();
        double z = target.getZ();

        if (predict) {
            Vec3 velocity = target.getDeltaMovement();
            double predictionTicks = 2.5;
            x += velocity.x * predictionTicks;
            z += velocity.z * predictionTicks;
            y += velocity.y * predictionTicks;
        }

        return new Vec3(x, y + heightOffset, z);
    }

    public Vec3 getAimPoint(LivingEntity target, String targetPart, double heightOffset) {
        return getAimPoint(target, targetPart, heightOffset, true);
    }

    /**
     * 判断目标是否有效
     */
    public boolean isValidTarget(LivingEntity entity, LocalPlayer player) {
        AddonClientConfig.Client config = AddonClientConfig.CLIENT;
        UUID playerUUID = player.getUUID();

        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        // ★★★ 优先检查：十影式神（最高优先级判断）★★★
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        if (entity instanceof TenShadowsSummon shikigami) {
            return handleTenShadowsTarget(shikigami, playerUUID, config);
        }

        // ★★★ 检查其他 JJK 召唤物（里香等）★★★
        if (entity instanceof SummonEntity summon) {
            return handleSummonTarget(summon, playerUUID, config);
        }

        // ========== 玩家 ==========
        if (entity instanceof Player targetPlayer) {
            if (!config.aimAssistTargetPlayers.get()) return false;
            if (targetPlayer == player) return false;
        }
        // ========== 敌对生物 ==========
        else if (isHostile(entity)) {
            if (!config.aimAssistTargetMonsters.get()) return false;
        }
        // ========== 驯服动物/宠物 ==========
        else if (entity instanceof TamableAnimal tamable) {
            if (!config.aimAssistTargetNeutrals.get()) return false;
            // ★★★ 修复：使用 UUID 比较 ★★★
            if (tamable.isTame()) {
                LivingEntity petOwner = tamable.getOwner();
                if (petOwner != null && petOwner.getUUID().equals(playerUUID)) {
                    return false;
                }
            }
        }
        // ========== 普通动物 ==========
        else if (entity instanceof Animal) {
            if (!config.aimAssistTargetNeutrals.get()) return false;
        }
        // ========== 其他实体 ==========
        else {
            String className = entity.getClass().getName().toLowerCase();
            if (className.contains("jujutsu") || className.contains("summon") || className.contains("shikigami")) {
                if (!config.aimAssistTargetSummons.get()) return false;
                // ★★★ 修复：使用 UUID 比较 ★★★
                if (entity instanceof net.minecraft.world.entity.OwnableEntity ownable) {
                    LivingEntity ownableOwner = ownable.getOwner();
                    if (ownableOwner != null && ownableOwner.getUUID().equals(playerUUID)) {
                        return false;
                    }
                }
            } else {
                if (!config.aimAssistTargetNeutrals.get()) return false;
            }
        }

        // ========== 队友检查 ==========
        if (config.aimAssistIgnoreTeammates.get()) {
            if (entity.isAlliedTo(player)) return false;
            if (player.getTeam() != null && entity.getTeam() == player.getTeam()) return false;
        }

        return true;
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ 处理十影式神（使用 UUID 比较）★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    private boolean handleTenShadowsTarget(TenShadowsSummon shikigami, UUID playerUUID, AddonClientConfig.Client config) {
        LivingEntity owner = shikigami.getOwner();
        boolean isTamed = shikigami.isTame();

        // ★★★ 关键修复：使用 UUID 比较 ★★★
        boolean isMyShikigami = owner != null && owner.getUUID().equals(playerUUID);

        // ========== 情况1：自己的式神 ==========
        if (isMyShikigami) {
            if (isTamed) {
                // 自己的已调伏式神 = 友方，永远不瞄准
                return false;
            } else {
                // 自己的未调伏式神 = 敌对！（调伏战）
                return config.aimAssistTargetMonsters.get();
            }
        }

        // ========== 情况2：别人的式神 ==========
        if (owner != null) {
            // 检查队友的式神
            if (config.aimAssistIgnoreTeammates.get()) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    if (owner.isAlliedTo(player)) return false;
                    if (player.getTeam() != null && owner.getTeam() == player.getTeam()) return false;
                }
            }

            if (isTamed) {
                // 别人的已调伏式神 = 按召唤物设置
                return config.aimAssistTargetSummons.get();
            } else {
                // 别人的未调伏式神 = 敌对
                return config.aimAssistTargetMonsters.get();
            }
        }

        // ========== 情况3：无主式神 ==========
        return config.aimAssistTargetMonsters.get();
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ 处理其他 JJK 召唤物（里香等）★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    private boolean handleSummonTarget(SummonEntity summon, UUID playerUUID, AddonClientConfig.Client config) {
        LivingEntity owner = summon.getOwner();
        boolean isTamed = summon.isTame();

        // ★★★ 使用 UUID 比较 ★★★
        boolean isMySummon = owner != null && owner.getUUID().equals(playerUUID);

        // 自己的已调伏召唤物 = 友方
        if (isMySummon && isTamed) {
            return false;
        }

        // 自己的未调伏召唤物 = 敌对
        if (isMySummon && !isTamed) {
            return config.aimAssistTargetMonsters.get();
        }

        // 别人的召唤物
        if (owner != null) {
            if (config.aimAssistIgnoreTeammates.get()) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    if (owner.isAlliedTo(player)) return false;
                    if (player.getTeam() != null && owner.getTeam() == player.getTeam()) return false;
                }
            }

            if (isTamed) {
                return config.aimAssistTargetSummons.get();
            } else {
                return config.aimAssistTargetMonsters.get();
            }
        }

        // 无主召唤物
        return config.aimAssistTargetMonsters.get();
    }

    /**
     * 判断实体是否是敌对的
     */
    private boolean isHostile(LivingEntity entity) {
        // 未调伏的式神视为敌对（这里不需要再判断，因为已经在前面处理了）

        if (entity instanceof Enemy) {
            return true;
        }

        if (entity.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }

        if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target instanceof Player) {
                return true;
            }
        }

        return false;
    }
}
