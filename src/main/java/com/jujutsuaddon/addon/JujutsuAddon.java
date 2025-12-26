package com.jujutsuaddon.addon;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.render.RenderHelper;
import com.jujutsuaddon.addon.config.AbilityConfig;
import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.config.VowConfig;
import com.jujutsuaddon.addon.damage.cache.DamageUtil;
import com.jujutsuaddon.addon.init.AddonBenefits;
import com.jujutsuaddon.addon.init.AddonConditions;
import com.jujutsuaddon.addon.init.AddonPenalties;
import com.jujutsuaddon.addon.inventory.ShadowStorageMenu;
import com.jujutsuaddon.addon.network.AddonNetwork;
import net.minecraftforge.api.distmarker.Dist; // 新增导入
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment; // 新增导入
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(JujutsuAddon.MODID)
public class JujutsuAddon {
    public static final String MODID = "jujutsu_addon";

    /** 日志记录器 */
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public JujutsuAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册 commonSetup 事件
        modEventBus.addListener(this::commonSetup);

        ShadowStorageMenu.MENUS.register(modEventBus);

        // 注册配置加载和重载事件
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        MinecraftForge.EVENT_BUS.register(this);

        // ==================== 配置注册 ====================
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AddonConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AbilityConfig.COMMON_SPEC, "jujutsu_addon-abilities.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AddonClientConfig.CLIENT_SPEC);

        // ★★★ 新增：誓约系统配置 ★★★
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VowConfig.COMMON_SPEC, "jujutsu_addon-vow.toml");

        LOGGER.info("[JujutsuAddon] Mod initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册网络包
            AddonNetwork.register();

            // ★★★ 新增：初始化誓约系统 ★★★
            initVowSystem();
        });
    }

    /**
     * 初始化誓约系统
     * 注册所有条件、收益、惩罚类型
     */
    private void initVowSystem() {
        if (!VowConfig.isEnabled()) {
            LOGGER.info("[JujutsuAddon] Vow system is disabled in config");
            return;
        }

        LOGGER.info("[JujutsuAddon] Initializing Custom Binding Vow system...");

        // 注册条件类型
        AddonConditions.register();
        LOGGER.info("[JujutsuAddon] Registered vow conditions");

        // 注册收益类型
        AddonBenefits.register();
        LOGGER.info("[JujutsuAddon] Registered vow benefits");

        // 注册惩罚类型
        AddonPenalties.register();
        LOGGER.info("[JujutsuAddon] Registered vow penalties");

        LOGGER.info("[JujutsuAddon] Custom Binding Vow system initialized successfully!");
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(MODID)) {
            clearAllCaches();
        }
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MODID)) {
            clearAllCaches();
        }
    }

    private void clearAllCaches() {
        DamageUtil.reload();
        com.jujutsuaddon.addon.util.helper.ProjectileHitTracker.reloadConfig();
        com.jujutsuaddon.addon.compat.mob.MobConfigManager.markDirty();

        // 修改处：添加了环境判断，防止服务端加载 RenderHelper 导致崩溃
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderHelper.clearIconCache();
        }
    }
}
