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

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
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
                        // 角度权重高，距离权重低
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

        // 预测目标移动（基于当前速度）
        if (predict) {
            Vec3 velocity = target.getDeltaMovement();
            // 预测 2-3 tick 后的位置
            double predictionTicks = 2.5;
            x += velocity.x * predictionTicks;
            z += velocity.z * predictionTicks;
            y += velocity.y * predictionTicks;
        }

        return new Vec3(x, y + heightOffset, z);
    }

    // 重载保持兼容
    public Vec3 getAimPoint(LivingEntity target, String targetPart, double heightOffset) {
        return getAimPoint(target, targetPart, heightOffset, true);
    }

    /**
     * 判断目标是否有效
     */
    public boolean isValidTarget(LivingEntity entity, LocalPlayer player) {
        AddonClientConfig.Client config = AddonClientConfig.CLIENT;

        // ========== 玩家 ==========
        if (entity instanceof Player targetPlayer) {
            if (!config.aimAssistTargetPlayers.get()) return false;
            // 不瞄准自己（理论上已经排除了，双重保险）
            if (targetPlayer == player) return false;
        }
        // ========== 敌对生物（包括史莱姆、女巫等） ==========
        else if (isHostile(entity)) {
            if (!config.aimAssistTargetMonsters.get()) return false;
        }
        // ========== 驯服动物/宠物 ==========
        else if (entity instanceof TamableAnimal tamable) {
            if (!config.aimAssistTargetNeutrals.get()) return false;
            // 忽略自己的宠物
            if (tamable.isTame() && tamable.getOwner() == player) return false;
        }
        // ========== 普通动物 ==========
        else if (entity instanceof Animal) {
            if (!config.aimAssistTargetNeutrals.get()) return false;
        }
        // ========== 其他实体（包括JJK生物、NPC等） ==========
        else {
            // 检查是否是 JJK 的召唤物/式神
            String className = entity.getClass().getName().toLowerCase();
            if (className.contains("jujutsu") || className.contains("summon") || className.contains("shikigami")) {
                if (!config.aimAssistTargetSummons.get()) return false;
                // 检查是否是自己的召唤物
                if (entity instanceof net.minecraft.world.entity.OwnableEntity ownable) {
                    if (ownable.getOwner() == player) return false;
                }
            } else {
                // 其他非标准实体，归类为中立
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

    /**
     * 判断实体是否是敌对的
     * 使用 Enemy 接口而不是 Monster 类，这样可以包含史莱姆、女巫等
     */
    private boolean isHostile(LivingEntity entity) {
        // Enemy 接口：史莱姆、女巫、幻翼等都实现了这个接口
        if (entity instanceof Enemy) {
            return true;
        }

        // 检查生物类别
        if (entity.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }

        // 检查是否是愤怒的中立生物（如被攻击的狼、蜜蜂等）
        if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target instanceof Player) {
                return true; // 正在攻击玩家的生物
            }
        }

        return false;
    }
}
