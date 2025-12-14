package com.jujutsuaddon.addon.client.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class AddonClientConfig {
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static class Client {
        // ==========================================
        // 技能冷却HUD (Ability Cooldown HUD)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableCooldownHUD;
        public final ForgeConfigSpec.EnumValue<AnchorPoint> hudAnchor;
        public final ForgeConfigSpec.IntValue hudOffsetX;
        public final ForgeConfigSpec.IntValue hudOffsetY;
        public final ForgeConfigSpec.BooleanValue showIconBackground;
        public final ForgeConfigSpec.BooleanValue showCooldownText;
        public final ForgeConfigSpec.BooleanValue showProgressOverlay;
        public final ForgeConfigSpec.IntValue maxDisplayCount;
        public final ForgeConfigSpec.BooleanValue prioritizeEquippedSkills;
        public final ForgeConfigSpec.BooleanValue horizontalLayout;
        public final ForgeConfigSpec.DoubleValue hudScale;

        // ==========================================
        // 技能快捷栏 (Skill Bar)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue enableSkillBar;
        public final ForgeConfigSpec.BooleanValue blockJJKKeys;
        public final ForgeConfigSpec.EnumValue<AnchorPoint> skillBarAnchor;
        public final ForgeConfigSpec.IntValue skillBarOffsetX;
        public final ForgeConfigSpec.IntValue skillBarOffsetY;
        public final ForgeConfigSpec.DoubleValue skillBarScale;
        public final ForgeConfigSpec.BooleanValue showSkillBarKeybinds;
        public final ForgeConfigSpec.BooleanValue showSkillBarCooldown;
        public final ForgeConfigSpec.BooleanValue showActiveIndicator;
        public final ForgeConfigSpec.BooleanValue skillBarHorizontalLayout;
        public final ForgeConfigSpec.BooleanValue hideSkillBarWhenDisabled;

        // ==========================================
        // 影子库存 (Shadow Storage)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue showShadowStorageHUD;
        public final ForgeConfigSpec.EnumValue<AnchorPoint> shadowStorageHudAnchor;
        public final ForgeConfigSpec.IntValue shadowStorageHudOffsetX;
        public final ForgeConfigSpec.IntValue shadowStorageHudOffsetY;
        public final ForgeConfigSpec.DoubleValue shadowStorageHudScale;
        public final ForgeConfigSpec.IntValue shadowStorageHudMaxItems;

        public Client(ForgeConfigSpec.Builder builder) {
            // ===== 冷却HUD配置 =====
            builder.push("Cooldown_HUD");

            enableCooldownHUD = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Cooldown HUD] (OPTIONAL)",
                            " Display a separate cooldown list on screen.",
                            " Note: Skill Bar already shows cooldowns, so this is optional.",
                            " Default: FALSE (disabled)",
                            "----------------------------------------------------------------",
                            " [启用冷却HUD] (可选)",
                            " 在屏幕上显示独立的冷却列表。",
                            " 注意：技能栏已经显示冷却，所以这个是可选的。",
                            " 默认：关闭",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.enable_cooldown_hud")
                    .define("EnableCooldownHUD", false);

            hudAnchor = builder
                    .comment(" ",
                            "================================================================",
                            " [HUD Anchor Point]",
                            " Where the HUD is anchored on screen.",
                            " Options: TOP_LEFT, TOP_CENTER, TOP_RIGHT,",
                            "          CENTER_LEFT, CENTER, CENTER_RIGHT,",
                            "          BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT",
                            "----------------------------------------------------------------",
                            " [HUD锚点]",
                            " HUD在屏幕上的锚定位置。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.hud_anchor")
                    .defineEnum("HUDAnchor", AnchorPoint.BOTTOM_LEFT);

            hudOffsetX = builder
                    .comment(" ",
                            " [X Offset] Horizontal offset (permille: -500 to 500 = -50% to +50%).",
                            " [X偏移] 水平偏移（千分比：-500到500 = -50%到+50%）。")
                    .translation("config.jujutsu_addon.client.hud_offset_x")
                    .defineInRange("HUDOffsetX", 10, -500, 500);

            hudOffsetY = builder
                    .comment(" ",
                            " [Y Offset] Vertical offset (permille: -500 to 500 = -50% to +50%).",
                            " [Y偏移] 垂直偏移（千分比：-500到500 = -50%到+50%）。")
                    .translation("config.jujutsu_addon.client.hud_offset_y")
                    .defineInRange("HUDOffsetY", -50, -500, 500);

            hudScale = builder
                    .comment(" ",
                            " [HUD Scale] Size multiplier for the cooldown display.",
                            " [HUD缩放] 冷却显示的大小倍率。")
                    .translation("config.jujutsu_addon.client.hud_scale")
                    .defineInRange("HUDScale", 1.0, 0.5, 3.0);

            horizontalLayout = builder
                    .comment(" ",
                            " [Horizontal Layout] If true, icons are arranged horizontally.",
                            " If false, icons are stacked vertically.",
                            " [水平布局] 若为真，图标水平排列；若为假，图标垂直堆叠。")
                    .translation("config.jujutsu_addon.client.horizontal_layout")
                    .define("HorizontalLayout", false);

            showIconBackground = builder
                    .comment(" Show dark background behind icons.",
                            " 在图标后显示深色背景。")
                    .translation("config.jujutsu_addon.client.show_icon_background")
                    .define("ShowIconBackground", true);

            showCooldownText = builder
                    .comment(" Show remaining time as text below icons.",
                            " 在图标下方显示剩余时间文字。")
                    .translation("config.jujutsu_addon.client.show_cooldown_text")
                    .define("ShowCooldownText", true);

            showProgressOverlay = builder
                    .comment(" Show gray overlay indicating cooldown progress.",
                            " 显示灰色遮罩表示冷却进度。")
                    .translation("config.jujutsu_addon.client.show_progress_overlay")
                    .define("ShowProgressOverlay", true);

            maxDisplayCount = builder
                    .comment(" ",
                            "================================================================",
                            " [Max Display Count]",
                            " Maximum number of cooldowns to display.",
                            " Set to 0 for unlimited (show all).",
                            "----------------------------------------------------------------",
                            " [最大显示数量]",
                            " 最多显示多少个冷却中的技能。设为 0 表示无限制。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.max_display_count")
                    .defineInRange("MaxDisplayCount", 8, 0, 50);

            prioritizeEquippedSkills = builder
                    .comment(" ",
                            " [Prioritize Equipped Skills]",
                            " Always show equipped skills first.",
                            " [优先显示装备技能] 始终优先显示装备中的技能。")
                    .translation("config.jujutsu_addon.client.prioritize_equipped_skills")
                    .define("PrioritizeEquippedSkills", true);

            builder.pop();

            // ===== 技能快捷栏配置 =====
            builder.push("Skill_Bar");

            enableSkillBar = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Skill Bar]",
                            " Display a skill bar for quick ability access.",
                            " - Press configured keys to trigger abilities",
                            " - Hold key for CHANNELED abilities, release to stop",
                            " - Press [ ] to switch presets",
                            " - Press K to open configuration",
                            "----------------------------------------------------------------",
                            " [启用技能快捷栏]",
                            " 显示技能快捷栏以快速使用技能。",
                            " - 按配置的按键触发技能",
                            " - 引导类技能按住释放，松开停止",
                            " - 按 [ ] 切换预设",
                            " - 按 K 打开配置界面",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.enable_skill_bar")
                    .define("EnableSkillBar", true);

            blockJJKKeys = builder
                    .comment(" ",
                            "================================================================",
                            " [Block JJK Original Keys]",
                            " When enabled, block the original JJK mod ability keys",
                            " to prevent conflicts with skill bar.",
                            " Blocked keys: R, T, X, Z, C, V, B, G, H, N, P, etc.",
                            " Use \\ key to toggle skill bar keys on/off if needed.",
                            "----------------------------------------------------------------",
                            " [屏蔽JJK原版按键]",
                            " 启用后，屏蔽JJK模组原版的技能按键以避免冲突。",
                            " 被屏蔽的按键：R, T, X, Z, C, V, B, G, H, N, P 等",
                            " 如需临时使用原版按键，可按 \\ 键切换技能栏开关。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.block_jjk_keys")
                    .define("BlockJJKKeys", true);

            skillBarAnchor = builder
                    .comment(" ",
                            "================================================================",
                            " [Skill Bar Anchor Point]",
                            " Where the skill bar is anchored on screen.",
                            "----------------------------------------------------------------",
                            " [技能栏锚点]",
                            " 技能栏在屏幕上的锚定位置。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.skill_bar_anchor")
                    .defineEnum("SkillBarAnchor", AnchorPoint.BOTTOM_CENTER);

            skillBarOffsetX = builder
                    .comment(" ",
                            " [X Offset] Horizontal offset (permille: -500 to 500 = -50% to +50%).",
                            " [X偏移] 水平偏移（千分比：-500到500 = -50%到+50%）。")
                    .translation("config.jujutsu_addon.client.skill_bar_offset_x")
                    .defineInRange("SkillBarOffsetX", 0, -500, 500);

            skillBarOffsetY = builder
                    .comment(" ",
                            " [Y Offset] Vertical offset (permille: -500 to 500 = -50% to +50%).",
                            " [Y偏移] 垂直偏移（千分比：-500到500 = -50%到+50%）。")
                    .translation("config.jujutsu_addon.client.skill_bar_offset_y")
                    .defineInRange("SkillBarOffsetY", 0, -500, 500);

            skillBarScale = builder
                    .comment(" ",
                            " [Scale] Size multiplier for skill bar.",
                            " [缩放] 技能栏大小倍率。")
                    .translation("config.jujutsu_addon.client.skill_bar_scale")
                    .defineInRange("SkillBarScale", 1.0, 0.5, 2.0);

            showSkillBarKeybinds = builder
                    .comment(" Show keybind hints on skill bar slots.",
                            " 在技能栏槽位显示快捷键提示。")
                    .translation("config.jujutsu_addon.client.show_skill_bar_keybinds")
                    .define("ShowSkillBarKeybinds", true);

            showSkillBarCooldown = builder
                    .comment(" Show cooldown overlay and text on skill bar.",
                            " 在技能栏显示冷却遮罩和文字。")
                    .translation("config.jujutsu_addon.client.show_skill_bar_cooldown")
                    .define("ShowSkillBarCooldown", true);

            showActiveIndicator = builder
                    .comment(" ",
                            " [Show Active Indicator]",
                            " Highlight skills that are currently active (toggled/channeling).",
                            " [显示激活指示器]",
                            " 高亮显示当前激活的技能（切换类/引导中）。")
                    .translation("config.jujutsu_addon.client.show_active_indicator")
                    .define("ShowActiveIndicator", true);

            skillBarHorizontalLayout = builder
                    .comment(" ",
                            " [Horizontal Layout] If true, skill bar is horizontal.",
                            " If false, skill bar is vertical.",
                            " [水平布局] 若为真，技能栏水平排列；若为假，垂直排列。")
                    .translation("config.jujutsu_addon.client.skill_bar_horizontal_layout")
                    .define("SkillBarHorizontalLayout", true);

            hideSkillBarWhenDisabled = builder
                    .comment(" ",
                            "================================================================",
                            " [Hide When Disabled]",
                            " If true, skill bar is completely hidden when disabled",
                            " (press \\ to disable).",
                            " If false, skill bar shows semi-transparent when disabled.",
                            "----------------------------------------------------------------",
                            " [禁用时隐藏]",
                            " 若为真，禁用技能栏时完全隐藏（按 \\ 禁用）。",
                            " 若为假，禁用时技能栏显示为半透明。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.hide_skill_bar_when_disabled")
                    .define("HideSkillBarWhenDisabled", true);

            builder.pop();

            // ===== 影子库存配置 =====
            builder.push("Shadow_Storage");

            showShadowStorageHUD = builder
                    .comment(" ",
                            "================================================================",
                            " [Show Shadow Storage HUD]",
                            " Display a small preview of items stored in shadow inventory",
                            " near the hotbar (requires Ten Shadows technique).",
                            "----------------------------------------------------------------",
                            " [显示影子库存HUD]",
                            " 在快捷栏附近显示影子库存中物品的预览",
                            " （需要拥有十种影法术）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.show_shadow_storage_hud")
                    .define("ShowShadowStorageHUD", true);
            shadowStorageHudAnchor = builder
                    .comment(" ",
                            "================================================================",
                            " [Shadow Storage HUD Anchor Point]",
                            " Where the shadow storage HUD is anchored on screen.",
                            "----------------------------------------------------------------",
                            " [影子库存HUD锚点]",
                            " 影子库存HUD在屏幕上的锚定位置。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.shadow_storage_hud_anchor")
                    .defineEnum("ShadowStorageHUDAnchor", AnchorPoint.BOTTOM_RIGHT);
            shadowStorageHudOffsetX = builder
                    .comment(" ",
                            " [X Offset] Horizontal offset (permille: -500 to 500 = -50% to +50%).",
                            " [X偏移] 水平偏移（千分比：-500到500 = -50%到+50%）。")
                    .translation("config.jujutsu_addon.client.shadow_storage_hud_offset_x")
                    .defineInRange("ShadowStorageHUDOffsetX", 0, -500, 500);
            shadowStorageHudOffsetY = builder
                    .comment(" ",
                            " [Y Offset] Vertical offset (permille: -500 to 500 = -50% to +50%).",
                            " [Y偏移] 垂直偏移（千分比：-500到500 = -50%到+50%）。")
                    .translation("config.jujutsu_addon.client.shadow_storage_hud_offset_y")
                    .defineInRange("ShadowStorageHUDOffsetY", 0, -500, 500);
            shadowStorageHudScale = builder
                    .comment(" ",
                            " [Scale] Size multiplier for shadow storage HUD.",
                            " [缩放] 影子库存HUD大小倍率。")
                    .translation("config.jujutsu_addon.client.shadow_storage_hud_scale")
                    .defineInRange("ShadowStorageHUDScale", 1.0, 0.5, 2.0);
            shadowStorageHudMaxItems = builder
                    .comment(" ",
                            "================================================================",
                            " [Max Display Items]",
                            " Maximum number of items to display in preview.",
                            " Range: 1 to 10",
                            "----------------------------------------------------------------",
                            " [最大显示物品数]",
                            " 预览中最多显示的物品数量。范围：1到10",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.shadow_storage_hud_max_items")
                    .defineInRange("ShadowStorageHUDMaxItems", 5, 1, 10);

            builder.pop();
        }
    }

    public enum AnchorPoint {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }
}
