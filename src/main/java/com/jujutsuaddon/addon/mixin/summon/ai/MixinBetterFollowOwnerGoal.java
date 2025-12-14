package com.jujutsuaddon.addon.mixin.summon.ai;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.entity.ai.goal.BetterFollowOwnerGoal;

@Mixin(BetterFollowOwnerGoal.class)
public abstract class MixinBetterFollowOwnerGoal extends Goal {

    @Shadow(remap = false) @Final private TamableAnimal tamable;
    @Shadow(remap = false) private LivingEntity owner;
    @Shadow(remap = false) @Final private boolean canFly;

    @Unique private double lastX;
    @Unique private double lastY;
    @Unique private double lastZ;
    @Unique private int stuckTimer;

    // ★★★ 修复：删除 remap = false ★★★
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void onCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (this.tamable == null) return;

        // 如果有攻击目标，直接返回 false，禁止触发跟随逻辑
        if (this.tamable.getTarget() != null && this.tamable.getTarget().isAlive()) {
            cir.setReturnValue(false);
        }
    }

    // ★★★ 修复：删除 remap = false ★★★
    @Inject(method = "start", at = @At("TAIL"))
    private void onStart(CallbackInfo ci) {
        this.stuckTimer = 0;
        this.updateLastPos();
    }

    // ★★★ 修复：删除 remap = false ★★★
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (this.owner == null || this.tamable == null) return;

        // 战斗状态 - 直接取消
        if (this.tamable.getTarget() != null && this.tamable.getTarget().isAlive()) {
            double distSqr = this.tamable.distanceToSqr(this.owner);

            double baseDist = AddonConfig.COMMON.summonRegroupDistance.get();
            double multiplier = AddonConfig.COMMON.manualTargetDistanceMultiplier.get();
            double combatThresholdSqr = (baseDist * multiplier) * (baseDist * multiplier);

            if (distSqr > combatThresholdSqr) {
                this.addonTryTeleportToOwner();
                this.tamable.setTarget(null);
            }

            ci.cancel();
            return;
        }

        // 非战斗状态
        double distSqr = this.tamable.distanceToSqr(this.owner);

        if (distSqr > 400.0D) {
            this.addonTryTeleportToOwner();
            this.stuckTimer = 0;
            ci.cancel();
            return;
        }

        if (distSqr > 100.0D) {
            double moveDist = this.tamable.distanceToSqr(this.lastX, this.lastY, this.lastZ);
            if (moveDist < 0.05D) {
                this.stuckTimer++;
            } else {
                this.stuckTimer = 0;
                this.updateLastPos();
            }
            if (this.stuckTimer > 60) {
                this.addonTryTeleportToOwner();
                this.stuckTimer = 0;
            }
        } else {
            this.stuckTimer = 0;
        }
    }

    @Unique
    private void updateLastPos() {
        this.lastX = this.tamable.getX();
        this.lastY = this.tamable.getY();
        this.lastZ = this.tamable.getZ();
    }

    @Unique
    private void addonTryTeleportToOwner() {
        if (this.owner == null) return;
        BlockPos ownerPos = this.owner.blockPosition();

        for (int i = 0; i < 10; ++i) {
            int dx = this.getRandomInt(-2, 2);
            int dy = this.getRandomInt(-1, 1);
            int dz = this.getRandomInt(-2, 2);

            if (this.addonTeleportTo(ownerPos.getX() + dx, ownerPos.getY() + dy, ownerPos.getZ() + dz)) {
                this.tamable.getNavigation().stop();
                this.tamable.setTarget(null);
                return;
            }
        }
    }

    @Unique
    private boolean addonTeleportTo(int x, int y, int z) {
        if (Math.abs((double)x - this.owner.getX()) < 1.0D && Math.abs((double)z - this.owner.getZ()) < 1.0D) {
            return false;
        }

        BlockPos targetPos = new BlockPos(x, y, z);
        if (!this.addonCanTeleportTo(targetPos)) return false;

        this.tamable.moveTo((double)x + 0.5D, (double)y, (double)z + 0.5D, this.tamable.getYRot(), this.tamable.getXRot());
        return true;
    }

    @Unique
    private boolean addonCanTeleportTo(BlockPos pos) {
        BlockPathTypes pathType = WalkNodeEvaluator.getBlockPathTypeStatic(this.tamable.level(), pos.mutable());

        if (!this.canFly) {
            if (pathType == BlockPathTypes.DANGER_FIRE || pathType == BlockPathTypes.LAVA || pathType == BlockPathTypes.DAMAGE_OTHER) {
                return false;
            }
            BlockState stateBelow = this.tamable.level().getBlockState(pos.below());
            if (stateBelow.isAir() || stateBelow.getBlock() instanceof LeavesBlock) {
                return false;
            }
        }

        BlockPos offset = pos.subtract(this.tamable.blockPosition());
        return this.tamable.level().noCollision(this.tamable, this.tamable.getBoundingBox().move(offset));
    }

    @Unique
    private int getRandomInt(int min, int max) {
        return this.tamable.getRandom().nextInt(max - min + 1) + min;
    }
}
