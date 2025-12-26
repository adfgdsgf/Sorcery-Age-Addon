package com.jujutsuaddon.addon.vow;

import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 违约记录
 * Violation Record
 *
 * 记录一次违约事件的详细信息，用于：
 * - 日志记录和调试
 * - 向玩家显示违约原因
 * - 决定惩罚类型和严重程度
 */
public class ViolationRecord {

    /** 被违反的誓约ID */
    private final UUID vowId;

    /** 被违反的条件ID */
    private final ResourceLocation conditionId;

    /** 触发违约检测的触发器类型 */
    private final CheckTrigger trigger;

    /** 违约发生的游戏时间（游戏刻） */
    private final long gameTime;

    /** 违约发生的真实时间戳 */
    private final long realTimestamp;

    public ViolationRecord(UUID vowId, ResourceLocation conditionId,
                           CheckTrigger trigger, long gameTime) {
        this.vowId = vowId;
        this.conditionId = conditionId;
        this.trigger = trigger;
        this.gameTime = gameTime;
        this.realTimestamp = System.currentTimeMillis();
    }

    private ViolationRecord(UUID vowId, ResourceLocation conditionId,
                            CheckTrigger trigger, long gameTime, long realTimestamp) {
        this.vowId = vowId;
        this.conditionId = conditionId;
        this.trigger = trigger;
        this.gameTime = gameTime;
        this.realTimestamp = realTimestamp;
    }

    // ==================== Getters ====================

    public UUID getVowId() {
        return vowId;
    }

    public ResourceLocation getConditionId() {
        return conditionId;
    }

    public CheckTrigger getTrigger() {
        return trigger;
    }

    public long getGameTime() {
        return gameTime;
    }

    public long getRealTimestamp() {
        return realTimestamp;
    }

    // ==================== NBT序列化 ====================

    /**
     * 序列化为NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("vowId", vowId);
        tag.putString("conditionId", conditionId.toString());
        tag.putString("trigger", trigger.name());
        tag.putLong("gameTime", gameTime);
        tag.putLong("realTimestamp", realTimestamp);
        return tag;
    }

    /**
     * 从NBT反序列化
     */
    public static ViolationRecord deserializeNBT(CompoundTag tag) {
        return new ViolationRecord(
                tag.getUUID("vowId"),
                new ResourceLocation(tag.getString("conditionId")),
                CheckTrigger.valueOf(tag.getString("trigger")),
                tag.getLong("gameTime"),
                tag.getLong("realTimestamp")
        );
    }

    @Override
    public String toString() {
        return String.format("ViolationRecord{vow=%s, condition=%s, trigger=%s, time=%d}",
                vowId, conditionId, trigger, gameTime);
    }
}
