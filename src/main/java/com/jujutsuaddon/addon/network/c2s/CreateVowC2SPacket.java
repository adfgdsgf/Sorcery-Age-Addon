package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.calculation.ValidationResult;
import com.jujutsuaddon.addon.vow.calculation.VowCalculator;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import com.jujutsuaddon.addon.vow.manager.CreateVowResult;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 创建誓约请求包 (C2S)
 * Create Vow Request Packet (Client to Server)
 *
 * 客户端请求创建新誓约时发送。
 * 服务端验证后创建誓约。
 */
public class CreateVowC2SPacket {

    private final String name;
    private final VowType type;
    private final List<ConditionEntry> conditions;
    private final List<BenefitEntry> benefits;

    // ==================== 构造 ====================

    public CreateVowC2SPacket(String name, VowType type,
                              List<ConditionEntry> conditions,
                              List<BenefitEntry> benefits) {
        this.name = name;
        this.type = type;
        this.conditions = new ArrayList<>(conditions);
        this.benefits = new ArrayList<>(benefits);
    }

    // ==================== 序列化 ====================

    public CreateVowC2SPacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf(32);
        this.type = buf.readEnum(VowType.class);

        // 读取条件列表
        this.conditions = new ArrayList<>();
        CompoundTag conditionsTag = buf.readNbt();
        if (conditionsTag != null && conditionsTag.contains("list", Tag.TAG_LIST)) {
            ListTag list = conditionsTag.getList("list", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ConditionEntry entry = ConditionEntry.deserializeNBT(list.getCompound(i));
                if (entry != null) {
                    conditions.add(entry);
                }
            }
        }

        // 读取收益列表
        this.benefits = new ArrayList<>();
        CompoundTag benefitsTag = buf.readNbt();
        if (benefitsTag != null && benefitsTag.contains("list", Tag.TAG_LIST)) {
            ListTag list = benefitsTag.getList("list", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                BenefitEntry entry = BenefitEntry.deserializeNBT(list.getCompound(i));
                if (entry != null) {
                    benefits.add(entry);
                }
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name, 32);
        buf.writeEnum(type);

        // 写入条件列表
        CompoundTag conditionsTag = new CompoundTag();
        ListTag conditionList = new ListTag();
        for (ConditionEntry entry : conditions) {
            conditionList.add(entry.serializeNBT());
        }
        conditionsTag.put("list", conditionList);
        buf.writeNbt(conditionsTag);

        // 写入收益列表
        CompoundTag benefitsTag = new CompoundTag();
        ListTag benefitList = new ListTag();
        for (BenefitEntry entry : benefits) {
            benefitList.add(entry.serializeNBT());
        }
        benefitsTag.put("list", benefitList);
        buf.writeNbt(benefitsTag);
    }

    // ==================== 处理 ====================

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 验证名称
            if (name == null || name.trim().isEmpty()) {
                sendError(player, "vow.error.empty_name");
                return;
            }

            // 验证条件
            if (conditions.isEmpty()) {
                sendError(player, "vow.error.no_conditions");
                return;
            }

            // 验证收益
            if (benefits.isEmpty()) {
                sendError(player, "vow.error.no_benefits");
                return;
            }

            // 构建誓约（使用玩家UUID）
            CustomBindingVow.Builder builder = CustomBindingVow.builder(player.getUUID())
                    .name(name.trim())
                    .type(type);

            for (ConditionEntry entry : conditions) {
                builder.addCondition(entry);
            }

            for (BenefitEntry entry : benefits) {
                builder.addBenefit(entry);
            }

            CustomBindingVow vow = builder.build();

            // 验证权重平衡
            ValidationResult validation = VowCalculator.validateVowBalance(vow);

            if (!validation.isValid()) {
                player.sendSystemMessage(validation.getErrorMessage().copy()
                        .withStyle(s -> s.withColor(0xFF5555)));
                return;
            }

            // 通过 VowManager 创建誓约
            CreateVowResult result = VowManager.createVow(player, vow);

            if (result.isSuccess()) {
                player.sendSystemMessage(
                        Component.translatable("vow.message.created", name.trim())
                );
            } else {
                player.sendSystemMessage(result.getErrorMessage().copy()
                        .withStyle(s -> s.withColor(0xFF5555)));
            }
        });

        ctx.get().setPacketHandled(true);
    }

    private void sendError(ServerPlayer player, String errorKey) {
        player.sendSystemMessage(
                Component.translatable(errorKey).withStyle(s -> s.withColor(0xFF5555))
        );
    }
}
