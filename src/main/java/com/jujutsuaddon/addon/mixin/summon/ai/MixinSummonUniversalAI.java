package com.jujutsuaddon.addon.mixin.summon.ai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

@Mixin(SummonEntity.class)
public abstract class MixinSummonUniversalAI extends TamableAnimal {

    protected MixinSummonUniversalAI(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }


    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void setOnGround(boolean state) {
        // 下面是原有的下潜逻辑，完全保留
        if (shouldDive()) {
            super.setOnGround(false);
        } else {
            super.setOnGround(state);
        }
    }

    // ==============================================================
    // 1. 智能物理锁 (解决下沉慢 + 防查克拉脚)
    // ==============================================================

    @Override
    public void setDeltaMovement(Vec3 vec) {
        // 【终极修复】如果是鵺，或者正在被骑乘，直接放行！
/*        if (isNue() || this.isVehicle()) {
            super.setDeltaMovement(vec);
            return;
        }*/

        // 下面是原有的下潜逻辑，完全保留
        if (shouldDive()) {
            // A. 如果试图往上游 (y > 0)
            if (vec.y > 0) {
                boolean forceSink = true;
                LivingEntity target = this.getTarget();
                if (target != null) {
                    double surfaceY = this.level().getSeaLevel();
                    boolean isDeepEnough = this.getY() < surfaceY - 2.0;
                    boolean targetIsAbove = target.getY() > this.getY() + 1.0;

                    if (isDeepEnough && targetIsAbove) {
                        forceSink = false; // 允许上浮
                    }
                }
                if (forceSink) {
                    // 强制下沉：这里不再用死板的 -0.05，而是调用计算好的下潜速度
                    super.setDeltaMovement(new Vec3(vec.x, getSinkSpeed(), vec.z));
                    return;
                }
            }
            // B. 如果本来就是往下游 (y <= 0)
            // 我们也要干预！防止原版水的阻力把速度拖慢
            else {
                // 如果当前下潜速度太慢 (比如只有 -0.01)，我们强制给它加速
                double desiredSinkSpeed = getSinkSpeed();
                if (vec.y > desiredSinkSpeed) { // 注意负数比较：-0.01 > -0.2
                    super.setDeltaMovement(new Vec3(vec.x, desiredSinkSpeed, vec.z));
                    return;
                }
            }
        }
        super.setDeltaMovement(vec);
    }

    @Override
    public void setDeltaMovement(double x, double y, double z) {
        // 【终极修复】同上
/*        if (isNue() || this.isVehicle()) {
            super.setDeltaMovement(x, y, z);
            return;
        }*/

        // 下面是原有的下潜逻辑，完全保留
        if (shouldDive()) {
            if (y > 0) {
                boolean forceSink = true;
                LivingEntity target = this.getTarget();
                if (target != null) {
                    double surfaceY = this.level().getSeaLevel();
                    boolean isDeepEnough = this.getY() < surfaceY - 2.0;
                    boolean targetIsAbove = target.getY() > this.getY() + 1.0;

                    if (isDeepEnough && targetIsAbove) {
                        forceSink = false;
                    }
                }
                if (forceSink) {
                    super.setDeltaMovement(x, getSinkSpeed(), z);
                    return;
                }
            } else {
                double desiredSinkSpeed = getSinkSpeed();
                if (y > desiredSinkSpeed) {
                    super.setDeltaMovement(x, desiredSinkSpeed, z);
                    return;
                }
            }
        }
        super.setDeltaMovement(x, y, z);
    }

    // ==============================================================
    // 辅助方法：计算“应该有多快下沉”
    // ==============================================================
    @Unique
    private double getSinkSpeed() {
        LivingEntity target = this.getTarget();
        // 1. 如果有目标，且目标在下面 -> 极速俯冲
        if (target != null && target.getY() < this.getY()) {
            // 基础下潜速度 -0.1 (比之前的 -0.05 快一倍)
            // 加上俯冲加成：根据高度差加速，最大限制在 -0.4
            double divePower = (target.getY() - this.getY()) * 0.1;
            // 限制最大下潜速度，防止穿模 (-0.4 已经很快了)
            return Math.max(-0.4, -0.1 + divePower);
        }
        // 2. 如果没目标，或者目标在上面(但被强制下沉) -> 快速下沉
        // -0.15 的速度相当于铁块入水，干脆利落
        return -0.15;
    }

    // ==============================================================
    // 2. 自由移动逻辑 (保持刚才的丝滑感)
    // ==============================================================

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) return;

/*        // 【终极修复】如果是鵺，或者正在被骑乘，直接退出，不执行任何额外 AI
        if (isNue() || this.isVehicle()) return;*/

        // 下面是原有的下潜逻辑，完全保留
        if (shouldDive()) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                // 极速转向
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double dy = target.getEyeY() - this.getEyeY();
                double dist = Math.sqrt(dx * dx + dz * dz);

                float targetYaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                float targetPitch = (float)(-(Math.atan2(dy, dist) * (180D / Math.PI)));

                this.setYRot(targetYaw);
                this.setXRot(targetPitch);
                this.yBodyRot = targetYaw;
                this.yHeadRot = targetYaw;

                // 无阻力推进
                Vec3 myPos = this.getEyePosition();
                Vec3 targetPos = target.getEyePosition();
                Vec3 direction = targetPos.subtract(myPos).normalize();

                Vec3 currentMotion = this.getDeltaMovement();
                double inertia = 0.96;
                double speed = 0.1;

                if (this.distanceTo(target) < 2.0) {
                    inertia = 0.6;
                }

                // 计算新速度
                Vec3 newMotion = currentMotion.scale(inertia).add(direction.scale(speed));

                // 【关键】：这里不再手动干预 Y 轴，全权交给 setDeltaMovement 去判断是该沉还是该浮
                super.setDeltaMovement(newMotion);

                this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
            }
        } else {
            this.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        }
    }

    @Unique
    private boolean shouldDive() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (target.isInWater() && this.isInWater()) {
                return true;
            }
        }
        return false;
    }
}
