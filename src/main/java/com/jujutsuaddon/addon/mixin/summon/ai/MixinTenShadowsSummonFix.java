package com.jujutsuaddon.addon.mixin.summon.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import java.util.List;
import java.util.UUID;

// 【关键修改】必须继承 TamableAnimal 才能重写 getTeam 并调用 super
@Mixin(TenShadowsSummon.class)
public abstract class MixinTenShadowsSummonFix extends TamableAnimal {

    @Shadow(remap = false) protected List<UUID> participants;

    // 必须添加构造函数以匹配父类
    protected MixinTenShadowsSummonFix(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /**
     * @author JJK_Addon_Dev
     * @reason 修复无法通过指令(Shift+右键)让式神攻击未调伏式神的问题
     */
    @Overwrite(remap = false)
    public void changeTarget(LivingEntity target) {
        // 修复逻辑：如果目标不是十影式神，或者目标是“未调伏”的十影式神（敌人），则允许攻击
        if (!(target instanceof TenShadowsSummon) || !((TenShadowsSummon) target).isTame()) {
            ((TenShadowsSummon) (Object) this).setTarget(target);
        }
    }

    /**
     * @author JJK_Addon_Dev
     * @reason 核心攻击判定逻辑修复 & 合并
     */
    @Override // 这里用 Override 比较安全，因为 canAttack 是父类方法
    public boolean canAttack(LivingEntity pTarget) {
        TenShadowsSummon self = (TenShadowsSummon) (Object) this;

        // 1. 如果未调伏 (仪式中)
        if (!self.isTame()) {
            // 允许攻击主人 (仪式发起者)
            if (self.isOwnedBy(pTarget)) return true;
            // 允许攻击参与者
            if (pTarget instanceof Player && this.participants.contains(pTarget.getUUID())) return true;
        }

        // 2. 如果已调伏，绝对不能攻击主人
        if (self.isTame() && self.getOwner() == pTarget) {
            return false;
        }

        // 3. 基础检查 (模拟 TamableAnimal 的逻辑)
        if (pTarget instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            if (tamable.isTame() && tamable.getOwner() == self.getOwner()) {
                // 除非是未调伏的同门师兄弟，否则不打自家的狗
                if (tamable instanceof TenShadowsSummon ts && !ts.isTame()) {
                    return true;
                }
                return false;
            }
        }

        // 4. 同类检查修正
        if (pTarget.getType() == self.getType()) {
            if (pTarget instanceof TenShadowsSummon targetSummon) {
                // 如果对方未调伏，允许攻击！
                if (!targetSummon.isTame()) return true;
                // 如果对方是克隆体，允许攻击！
                if (targetSummon.isClone()) return true;
                // 否则（对方是已调伏的同类），禁止攻击
                return false;
            }
        }

        return true;
    }

    /**
     * @author JJK_Addon_Dev
     * @reason 修复自瞄模组失效问题：未调伏时，不视为盟友
     */
    @Override
    public boolean isAlliedTo(Entity entity) {
        TenShadowsSummon self = (TenShadowsSummon) (Object) this;
        // 如果未调伏
        if (!self.isTame()) {
            // 1. 检查是否为主人 (需要先判断类型)
            if (entity instanceof LivingEntity living && self.isOwnedBy(living)) {
                return false; // 是主人，但未调伏 -> 视为敌人
            }

            // 2. 检查是否为参与者
            if (entity instanceof Player && this.participants.contains(entity.getUUID())) {
                return false; // 是参与者 -> 视为敌人
            }
        }

        // 默认逻辑
        return super.isAlliedTo(entity);
    }

    /**
     * @author JJK_Addon_Dev
     * @reason 【关键修复】修复自瞄模组失效问题
     * 这里去掉了 @Overwrite，直接使用 @Override，因为目标类没有重写这个方法。
     */
    @Override
    public Team getTeam() {
        TenShadowsSummon self = (TenShadowsSummon) (Object) this;

        // 如果未调伏，假装自己没有队伍 (返回 null)
        // 这样自瞄模组就不会因为它继承了你的队伍而把它当队友
        if (!self.isTame()) {
            return null;
        }

        return super.getTeam();
    }
}
