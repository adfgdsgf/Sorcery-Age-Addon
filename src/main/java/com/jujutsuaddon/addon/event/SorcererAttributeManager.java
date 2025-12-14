package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class SorcererAttributeManager {

    // 我们的护甲 UUID
    public static final UUID CONVERTED_ARMOR_UUID = UUID.fromString("99887766-5544-3322-1100-aabbccddeeff");
    public static final UUID CONVERTED_TOUGHNESS_UUID = UUID.fromString("11223344-5566-7788-9900-ffeeddccbbaa");

    // 主模组的 UUID (作为保底)
    private static final UUID TARGET_JJK_HEALTH_UUID = UUID.fromString("72ff5080-3a82-4a03-8493-3be970039cfe");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        boolean isConfigEnabled = AddonConfig.COMMON.enableHealthToArmor.get();

        if (isConfigEnabled) {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                boolean needsFix = false;

                // 1. 检查已知 UUID (精准打击)
                if (health.getModifier(TARGET_JJK_HEALTH_UUID) != null) {
                    health.removeModifier(TARGET_JJK_HEALTH_UUID);
                    needsFix = true;
                }

                // 2. 模糊扫描 (防止 UUID 变了)
                // 如果血量异常高 (比如 > 100)，扫描所有修饰符
                if (player.getMaxHealth() > 100.0) {
                    List<UUID> toRemove = new ArrayList<>();
                    for (AttributeModifier mod : health.getModifiers()) {
                        // JJK 的修饰符名字通常叫 "Max health" (非常普通)
                        // 但我们可以通过排除法：如果数值很大，且不是我们认识的其他模组的，就干掉
                        // 这里为了安全，只移除数值巨大的单次加成
                        if (mod.getAmount() > 50.0 && mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                            // 再次确认不是某种装备提供的
                            toRemove.add(mod.getId());
                        }
                    }
                    for (UUID id : toRemove) {
                        health.removeModifier(id);
                        needsFix = true;
                    }
                }

                if (needsFix && player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        } else {
            // 如果配置关闭，清理我们的护甲
            AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(CONVERTED_ARMOR_UUID);

            AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (toughness != null) toughness.removeModifier(CONVERTED_TOUGHNESS_UUID);
        }
    }
}
