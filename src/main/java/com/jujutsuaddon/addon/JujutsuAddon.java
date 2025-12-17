package com.jujutsuaddon.addon;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.damage.cache.DamageUtil;
import com.jujutsuaddon.addon.inventory.ShadowStorageMenu;
import com.jujutsuaddon.addon.network.AddonNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent; // ★ 新增导入
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JujutsuAddon.MODID)
public class JujutsuAddon {
    public static final String MODID = "jujutsu_addon";

    public JujutsuAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册 commonSetup 事件
        modEventBus.addListener(this::commonSetup);

        ShadowStorageMenu.MENUS.register(modEventBus);

        // ★★★ 新增：注册配置加载和重载事件 ★★★
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AddonConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AddonClientConfig.CLIENT_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AddonNetwork.register();
        });
    }

    // ★★★ 新增：配置首次加载时的回调 ★★★
    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(MODID)) {
            clearAllCaches();
        }
    }

    // ★★★ 新增：配置热重载时的回调（修改文件保存后触发） ★★★
    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MODID)) {
            clearAllCaches();
        }
    }

    // ★★★ 新增：统一清理缓存的方法 ★★★
    private void clearAllCaches() {
        // 刷新伤害计算相关的缓存
        DamageUtil.reload();
        // 刷新投射物判定缓存
        com.jujutsuaddon.addon.util.helper.ProjectileHitTracker.reloadConfig();
        // 标记生物配置为脏数据（下次使用时自动重载）
        com.jujutsuaddon.addon.compat.mob.MobConfigManager.markDirty();
        // 刷新图标缓存（客户端）
        com.jujutsuaddon.addon.client.util.RenderHelper.clearIconCache();
    }
}
