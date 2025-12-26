package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.client.cache.ClientVowDataCache;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * 同步誓约列表包 (S2C)
 * Sync Vow List Packet (Server to Client)
 */
public class SyncVowListS2CPacket {

    private final List<CustomBindingVow> vows;
    // ★ 改名：从 usedPairOwners 改为 occupiedConditionOwners
    private final Map<String, UUID> occupiedConditionOwners;
    private final long penaltyEndTime;

    // ==================== 构造 ====================

    /**
     * 服务端构造
     */
    public SyncVowListS2CPacket(Collection<CustomBindingVow> vows, Map<String, UUID> occupiedConditionOwners, long penaltyEndTime) {
        this.vows = new ArrayList<>(vows);
        this.occupiedConditionOwners = occupiedConditionOwners != null ? new HashMap<>(occupiedConditionOwners) : new HashMap<>();
        this.penaltyEndTime = penaltyEndTime;
    }

    /**
     * 兼容旧调用
     */
    public SyncVowListS2CPacket(Collection<CustomBindingVow> vows, Map<String, UUID> occupiedConditionOwners) {
        this(vows, occupiedConditionOwners, 0L);
    }

    /**
     * 客户端解码
     */
    public SyncVowListS2CPacket(FriendlyByteBuf buf) {
        this.vows = new ArrayList<>();
        this.occupiedConditionOwners = new HashMap<>();

        CompoundTag tag = buf.readNbt();

        // 读取惩罚时间
        this.penaltyEndTime = buf.readLong();

        if (tag == null) return;

        // 读取誓约列表
        if (tag.contains("vows", Tag.TAG_LIST)) {
            ListTag list = tag.getList("vows", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                try {
                    CustomBindingVow vow = CustomBindingVow.deserializeNBT(list.getCompound(i));
                    if (vow != null) {
                        vows.add(vow);
                    }
                } catch (Exception ignored) {}
            }
        }

        // ★ 读取 occupiedConditionOwners (NBT key 也改一下，保持一致)
        if (tag.contains("occupiedConditions", Tag.TAG_COMPOUND)) {
            CompoundTag mapTag = tag.getCompound("occupiedConditions");
            for (String key : mapTag.getAllKeys()) {
                try {
                    UUID vowId = mapTag.getUUID(key);
                    occupiedConditionOwners.put(key, vowId);
                } catch (Exception ignored) {}
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();

        // 写入誓约列表
        ListTag vowList = new ListTag();
        for (CustomBindingVow vow : vows) {
            vowList.add(vow.serializeNBT());
        }
        tag.put("vows", vowList);

        // ★ 写入 occupiedConditionOwners
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<String, UUID> entry : occupiedConditionOwners.entrySet()) {
            mapTag.putUUID(entry.getKey(), entry.getValue());
        }
        tag.put("occupiedConditions", mapTag);

        buf.writeNbt(tag);

        // 写入惩罚时间
        buf.writeLong(penaltyEndTime);
    }

    // ==================== 处理 ====================

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(this::handleClient);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        // ★ 更新客户端缓存
        // 注意：你需要去 ClientVowDataCache 把 update 方法的参数名也改一下，或者直接传进去
        ClientVowDataCache.update(vows, occupiedConditionOwners, penaltyEndTime);

        // 更新界面
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof VowListScreenAccess screen) {
            screen.updateVowList(vows);
        }
    }

    // ==================== 访问接口 ====================

    public interface VowListScreenAccess {
        void updateVowList(List<CustomBindingVow> vows);
    }
}
