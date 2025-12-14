package com.jujutsuaddon.addon.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;

import javax.annotation.Nullable;
import java.util.List;

public class ShadowStorageMenuProvider implements MenuProvider {

    public static void open(ServerPlayer player) {
        NetworkHooks.openScreen(player, new ShadowStorageMenuProvider(), buf -> {
            // 写入所有影子库存数据
            ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (data != null) {
                List<ItemStack> items = data.getShadowInventory();
                buf.writeVarInt(items.size());
                for (ItemStack item : items) {
                    buf.writeItem(item);
                }
            } else {
                buf.writeVarInt(0);
            }
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.jujutsu_addon.shadow_storage.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        List<ItemStack> items = ShadowStorageMenu.loadFromCapability(player);
        return new ShadowStorageMenu(containerId, inventory, items);
    }
}
