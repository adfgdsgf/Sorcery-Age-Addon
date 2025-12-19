package com.jujutsuaddon.addon.client.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class AddonClientConfig {
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public enum ShadowStorageSortMode {
        NONE, NAME, COUNT, MOD, RARITY
    }

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
        public final ForgeConfigSpec.BooleanValue showSkillBarDamage;
        public final ForgeConfigSpec.BooleanValue showActiveIndicator;
        public final ForgeConfigSpec.BooleanValue skillBarHorizontalLayout;
        public final ForgeConfigSpec.BooleanValue hideSkillBarWhenDisabled;

        // ==========================================
// 投射物反弹 (Projectile Reflection)
// ==========================================
        public final ForgeConfigSpec.BooleanValue enableProjectileReflection;
        public final ForgeConfigSpec.ConfigValue<String> reflectToOwnerModifier;
        public final ForgeConfigSpec.ConfigValue<String> reflectToCursorModifier;
        public final ForgeConfigSpec.DoubleValue reflectSpeedMultiplier;

        // ==========================================
        // 影子库存 (Shadow Storage)
        // ==========================================
        public final ForgeConfigSpec.BooleanValue showShadowStorageHUD;
        public final ForgeConfigSpec.EnumValue<AnchorPoint> shadowStorageHudAnchor;
        public final ForgeConfigSpec.IntValue shadowStorageHudOffsetX;
        public final ForgeConfigSpec.IntValue shadowStorageHudOffsetY;
        public final ForgeConfigSpec.DoubleValue shadowStorageHudScale;
        public final ForgeConfigSpec.IntValue shadowStorageHudMaxItems;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> searchMods;
        public final ForgeConfigSpec.EnumValue<ShadowStorageSortMode> shadowStorageSortMode;

        // ==========================================
        // 自瞄辅助 (Aim Assist)
        // ==========================================
        // 基础
        public final ForgeConfigSpec.BooleanValue aimAssistEnabled;
        public final ForgeConfigSpec.DoubleValue aimAssistMaxDistance;
        public final ForgeConfigSpec.DoubleValue aimAssistLockDistance;
        public final ForgeConfigSpec.DoubleValue aimAssistFovAngle;
        // 瞄准行为
        public final ForgeConfigSpec.DoubleValue aimAssistSpeed;
        public final ForgeConfigSpec.DoubleValue aimAssistMaxTurnSpeed;
        public final ForgeConfigSpec.ConfigValue<String> aimAssistTargetPart;
        public final ForgeConfigSpec.DoubleValue aimAssistHeightOffset;
        // 穿墙与视线
        public final ForgeConfigSpec.BooleanValue aimAssistThroughWalls;
        public final ForgeConfigSpec.BooleanValue aimAssistRequireInitialSight;
        // 目标选择
        public final ForgeConfigSpec.BooleanValue aimAssistTargetPlayers;
        public final ForgeConfigSpec.BooleanValue aimAssistTargetMonsters;
        public final ForgeConfigSpec.BooleanValue aimAssistTargetNeutrals;
        public final ForgeConfigSpec.BooleanValue aimAssistTargetSummons;
        public final ForgeConfigSpec.BooleanValue aimAssistIgnoreTeammates;
        public final ForgeConfigSpec.BooleanValue aimAssistIgnoreInvisible;
        // 优先级
        public final ForgeConfigSpec.ConfigValue<String> aimAssistPriority;
        public final ForgeConfigSpec.BooleanValue aimAssistStickyTarget;
        // 击杀切换
        public final ForgeConfigSpec.BooleanValue aimAssistAutoSwitch;
        public final ForgeConfigSpec.IntValue aimAssistSwitchDelay;
        // 触发模式
        public final ForgeConfigSpec.ConfigValue<String> aimAssistTriggerMode;
        public final ForgeConfigSpec.BooleanValue aimAssistOnlyOnAttack;
        // 视觉效果
        public final ForgeConfigSpec.BooleanValue aimAssistGlowingTarget;
        public final ForgeConfigSpec.ConfigValue<String> aimAssistGlowColor;
        public final ForgeConfigSpec.BooleanValue aimAssistShowIndicator;



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

            showSkillBarDamage = builder
                    .comment(" ",
                            "================================================================",
                            " [Show Damage Prediction]",
                            " Display predicted damage on skill bar slots.",
                            " Color meanings:",
                            " - Orange: Base JJK damage",
                            " - Red: With addon bonus (attack attributes, crit, etc.)",
                            " - Blue: Summon attack power",
                            " - Purple: Domain power",
                            " - Gray (?): Cannot predict",
                            " - Gray (-): Utility skill (no damage)",
                            "----------------------------------------------------------------",
                            " [显示伤害预测]",
                            " 在技能栏槽位显示预测伤害。",
                            " 颜色含义：",
                            " - 橙色：JJK基础伤害",
                            " - 红色：附属加成后（攻击属性、暴击等）",
                            " - 蓝色：召唤物攻击力",
                            " - 紫色：领域威力",
                            " - 灰色(?)：无法预测",
                            " - 灰色(-)：功能性技能（无伤害）",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.show_skill_bar_damage")
                    .define("ShowSkillBarDamage", false);  // ★★★ 改为 false ★★★

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

            // ===== 投射物反弹配置 =====
            builder.push("Projectile_Reflection");
            enableProjectileReflection = builder
                    .comment(" ",
                            "================================================================",
                            " [Enable Projectile Reflection]",
                            " Allow reflecting frozen projectiles back by pressing",
                            " modifier key + Infinity skill key.",
                            "----------------------------------------------------------------",
                            " [启用投射物反弹]",
                            " 允许通过按住修饰键+无下限技能键来反弹静止的投射物。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.enable_projectile_reflection")
                    .define("EnableProjectileReflection", true);
            reflectToOwnerModifier = builder
                    .comment(" ",
                            "================================================================",
                            " [Reflect to Original Owner - Modifier Key]",
                            " Which modifier key to hold for reflecting toward the",
                            " projectile's original shooter.",
                            " Options: SHIFT, CTRL, ALT, NONE (disabled)",
                            "----------------------------------------------------------------",
                            " [反弹至原发射者 - 修饰键]",
                            " 按住哪个键+技能键，将投射物反弹回原发射者。",
                            " 选项：SHIFT, CTRL, ALT, NONE（禁用）",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.reflect_to_owner_modifier")
                    .define("ReflectToOwnerModifier", "SHIFT");
            reflectToCursorModifier = builder
                    .comment(" ",
                            "================================================================",
                            " [Reflect to Cursor - Modifier Key]",
                            " Which modifier key to hold for reflecting toward your",
                            " crosshair/look direction.",
                            " Options: SHIFT, CTRL, ALT, NONE (disabled)",
                            "----------------------------------------------------------------",
                            " [反弹至准星方向 - 修饰键]",
                            " 按住哪个键+技能键，将投射物反弹至准星方向。",
                            " 选项：SHIFT, CTRL, ALT, NONE（禁用）",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.reflect_to_cursor_modifier")
                    .define("ReflectToCursorModifier", "CTRL");
            reflectSpeedMultiplier = builder
                    .comment(" ",
                            " [Reflect Speed Multiplier]",
                            " How fast the reflected projectile travels.",
                            " 1.0 = Original speed, 2.0 = Double speed.",
                            " [反弹速度倍率] 反弹后投射物的速度倍率。")
                    .translation("config.jujutsu_addon.client.reflect_speed_multiplier")
                    .defineInRange("ReflectSpeedMultiplier", 1.5, 0.5, 5.0);
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

            searchMods = builder
                    .comment(" ",
                            "================================================================",
                            " [Search Enhancement Mods]",
                            " List of mod IDs that provide pinyin/romaji search.",
                            " Just add the mod ID, no complicated class names needed!",
                            " ",
                            " [Supported Mods]",
                            " - jecharacters     (Chinese Pinyin / 中文拼音)",
                            " - searchables      (Multi-language search)",
                            " ",
                            " [How it works]",
                            " If you have one of these mods installed, shadow storage",
                            " search will automatically support that language input.",
                            " ",
                            " [Adding New Mods]",
                            " If you know a search mod not listed here, add its mod ID.",
                            " We'll try common method patterns to detect it.",
                            "----------------------------------------------------------------",
                            " [搜索增强模组]",
                            " 提供拼音/罗马字搜索功能的模组ID列表。",
                            " 只需填写模组ID，无需复杂的类名！",
                            " ",
                            " [已支持的模组]",
                            " - jecharacters     (中文拼音)",
                            " - searchables      (多语言搜索)",
                            " ",
                            " [工作原理]",
                            " 如果你安装了这些模组之一，影子库存搜索",
                            " 将自动支持对应语言的输入法搜索。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.search_mods")
                    .defineList("SearchMods", Arrays.asList(
                            "jecharacters",
                            "searchables"
                    ), entry -> entry instanceof String);

            shadowStorageSortMode = builder
                    .comment(" ",
                            " [Last Used Sort Mode]",
                            " Automatically saved when you change sort mode.",
                            " [上次使用的排序模式] 切换时自动保存。")
                    .translation("config.jujutsu_addon.client.shadow_storage_sort_mode")
                    .defineEnum("ShadowStorageSortMode", ShadowStorageSortMode.NONE);

            builder.pop();

            // ===== 自瞄辅助配置 =====
            builder.push("Aim_Assist");
            aimAssistEnabled = builder
                    .comment(" ",
                            "================================================================",
                            " [Master Switch]",
                            " Enable or disable the entire Aim Assist system.",
                            " Use the keybind (default: `) to toggle in-game.",
                            "----------------------------------------------------------------",
                            " [总开关]",
                            " 启用或禁用整个自瞄系统。",
                            " 游戏内使用快捷键（默认：`）切换。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_enabled")
                    .define("Enabled", true);
            aimAssistMaxDistance = builder
                    .comment(" Maximum detection distance (blocks).",
                            " 最大检测距离（格）。")
                    .translation("config.jujutsu_addon.client.aim_assist_max_distance")
                    .defineInRange("MaxDistance", 64.0, 1.0, 444.0);
            aimAssistLockDistance = builder
                    .comment(" ",
                            "================================================================",
                            " [Lock Keep Distance]",
                            " Maximum distance to KEEP tracking a locked target.",
                            " Set higher than MaxDistance to maintain lock even when target moves away.",
                            " Set to 0 to never lose lock due to distance (only death breaks lock).",
                            "----------------------------------------------------------------",
                            " [锁定保持距离]",
                            " 保持锁定的最大距离。设为0表示永不因距离丢失锁定。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_lock_distance")
                    .defineInRange("LockKeepDistance", 0.0, 0.0, 512.0);


            aimAssistFovAngle = builder
                    .comment(" ",
                            "================================================================",
                            " [FOV Detection Angle]",
                            " The cone angle (in degrees) around your crosshair.",
                            " 60 = targets within 30° left/right of crosshair.",
                            " 180 = half of your screen.",
                            "----------------------------------------------------------------",
                            " [视野检测角度]",
                            " 准星周围的锥形检测角度（度数）。",
                            " 60 = 准星左右各30°范围内的目标。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_fov")
                    .defineInRange("FovAngle", 60.0, 10.0, 360.0);
            // ========== 瞄准行为 ==========
            aimAssistSpeed = builder
                    .comment(" ",
                            "================================================================",
                            " [Aim Speed]",
                            " How fast your crosshair moves toward the target.",
                            " 0.1 = Slow, smooth tracking.",
                            " 0.5 = Medium speed.",
                            " 1.0 = Instant snap (not recommended).",
                            "----------------------------------------------------------------",
                            " [瞄准速度]",
                            " 准星向目标移动的速度。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_speed")
                    .defineInRange("AimSpeed", 0.7, 0.05, 100.0);
            aimAssistMaxTurnSpeed = builder
                    .comment(" ",
                            "================================================================",
                            " [Max Turn Speed] (Degrees per tick)",
                            " Limits how fast your view can rotate.",
                            " Prevents unnatural 180° instant turns.",
                            " 0 = No limit. | 15 = Smooth, human-like.",
                            "----------------------------------------------------------------",
                            " [最大转向速度]（度/tick）",
                            " 限制视角旋转的最大速度，防止不自然的瞬间转头。",
                            " 0 = 无限制。| 15 = 平滑。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_max_turn")
                    .defineInRange("MaxTurnSpeed", 15.0, 0.0, 180.0);
            aimAssistTargetPart = builder
                    .comment(" ",
                            "================================================================",
                            " [Target Part]",
                            " Which part of the entity to aim at.",
                            " HEAD = Eye position (headshots).",
                            " BODY = Center of hitbox.",
                            " FEET = Bottom of hitbox.",
                            "----------------------------------------------------------------",
                            " [瞄准部位]",
                            " HEAD = 眼睛位置。BODY = 中心。FEET = 底部。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_target_part")
                    .define("TargetPart", "HEAD");
            aimAssistHeightOffset = builder
                    .comment(" Fine-tune the aim height. Positive = higher, Negative = lower.",
                            " 微调瞄准高度。正数更高，负数更低。")
                    .translation("config.jujutsu_addon.client.aim_assist_height_offset")
                    .defineInRange("HeightOffset", 0.0, -2.0, 2.0);
            // ========== 穿墙与视线 ==========
            aimAssistThroughWalls = builder
                    .comment(" ",
                            "================================================================",
                            " [Through Walls Tracking]",
                            " If TRUE, continues tracking a locked target through walls.",
                            " If FALSE, loses target when line of sight is blocked.",
                            "----------------------------------------------------------------",
                            " [穿墙跟踪]",
                            " 若为真，锁定目标后即使被墙挡住也会继续跟踪。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_through_walls")
                    .define("ThroughWalls", false);
            aimAssistRequireInitialSight = builder
                    .comment(" ",
                            "================================================================",
                            " [Require Initial Line of Sight]",
                            " If TRUE, you must SEE the target first before locking on.",
                            " Works with ThroughWalls: Lock requires sight, then tracks through walls.",
                            "----------------------------------------------------------------",
                            " [首次锁定需要视线]",
                            " 若为真，必须先看到目标才能锁定。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_require_sight")
                    .define("RequireInitialSight", true);
            // ========== 目标选择 ==========
            aimAssistTargetPlayers = builder
                    .comment(" Target other players. | 瞄准其他玩家。")
                    .translation("config.jujutsu_addon.client.aim_assist_target_players")
                    .define("TargetPlayers", true);
            aimAssistTargetMonsters = builder
                    .comment(" Target hostile mobs. | 瞄准敌对生物。")
                    .translation("config.jujutsu_addon.client.aim_assist_target_monsters")
                    .define("TargetMonsters", true);
            aimAssistTargetNeutrals = builder
                    .comment(" Target neutral mobs (Wolves, Iron Golems, etc.). | 瞄准中立生物。")
                    .translation("config.jujutsu_addon.client.aim_assist_target_neutrals")
                    .define("TargetNeutrals", false);
            aimAssistTargetSummons = builder
                    .comment(" Target summons/shikigami. | 瞄准召唤物/式神。")
                    .translation("config.jujutsu_addon.client.aim_assist_target_summons")
                    .define("TargetSummons", true);
            aimAssistIgnoreTeammates = builder
                    .comment(" ",
                            "================================================================",
                            " [Ignore Teammates]",
                            " Skip entities on the same team (scoreboard team or JJK faction).",
                            "----------------------------------------------------------------",
                            " [忽略队友]",
                            " 跳过同一队伍的实体（记分板队伍或JJK阵营）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_ignore_teammates")
                    .define("IgnoreTeammates", true);
            aimAssistIgnoreInvisible = builder
                    .comment(" Ignore invisible entities. | 忽略隐身实体。")
                    .translation("config.jujutsu_addon.client.aim_assist_ignore_invisible")
                    .define("IgnoreInvisible", true);
            // ========== 优先级 ==========
            aimAssistPriority = builder
                    .comment(" ",
                            "================================================================",
                            " [Target Priority]",
                            " How to choose the best target when multiple are in range.",
                            " ANGLE = Closest to crosshair (default).",
                            " DISTANCE = Closest to you.",
                            " HEALTH = Lowest health (finish off weak enemies).",
                            "----------------------------------------------------------------",
                            " [目标优先级]",
                            " ANGLE = 离准星最近。DISTANCE = 离你最近。HEALTH = 血量最低。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_priority")
                    .define("Priority", "ANGLE");
            aimAssistStickyTarget = builder
                    .comment(" ",
                            "================================================================",
                            " [Sticky Target]",
                            " Prefer to keep tracking the current target instead of switching.",
                            " Prevents aim from jumping between targets rapidly.",
                            "----------------------------------------------------------------",
                            " [粘性目标]",
                            " 优先保持跟踪当前目标，防止准星在目标之间快速跳跃。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_sticky")
                    .define("StickyTarget", true);
            // ========== 击杀切换 ==========
            aimAssistAutoSwitch = builder
                    .comment(" ",
                            "================================================================",
                            " [Auto Switch on Kill]",
                            " Automatically find a new target after killing the current one.",
                            "----------------------------------------------------------------",
                            " [击杀自动切换]",
                            " 击杀当前目标后自动寻找新目标。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_auto_switch")
                    .define("AutoSwitchOnKill", true);
            aimAssistSwitchDelay = builder
                    .comment(" Delay (ticks) before switching after kill. 20 = 1 second.",
                            " 击杀后切换延迟（tick）。20 = 1秒。")
                    .translation("config.jujutsu_addon.client.aim_assist_switch_delay")
                    .defineInRange("SwitchDelay", 5, 0, 100);
            // ========== 触发模式 ==========
            aimAssistTriggerMode = builder
                    .comment(" ",
                            "================================================================",
                            " [Trigger Mode]",
                            " TOGGLE = Press key to enable/disable.",
                            " HOLD = Only active while holding the key.",
                            "----------------------------------------------------------------",
                            " [触发模式]",
                            " TOGGLE = 按键切换开关。HOLD = 按住时才生效。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_trigger_mode")
                    .define("TriggerMode", "TOGGLE");
            aimAssistOnlyOnAttack = builder
                    .comment(" ",
                            "================================================================",
                            " [Only When Attacking]",
                            " If TRUE, only tracks when you're holding attack button.",
                            " Prevents aim from moving while you're just looking around.",
                            "----------------------------------------------------------------",
                            " [仅攻击时生效]",
                            " 若为真，只有按住攻击键时才跟踪。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_only_attack")
                    .define("OnlyOnAttack", false);
            // ========== 视觉效果 ==========
            aimAssistGlowingTarget = builder
                    .comment(" ",
                            "================================================================",
                            " [Glowing Target]",
                            " Make the current target glow (outline effect).",
                            "----------------------------------------------------------------",
                            " [目标发光]",
                            " 让当前目标发光（轮廓高亮效果）。",
                            "================================================================")
                    .translation("config.jujutsu_addon.client.aim_assist_glow")
                    .define("GlowingTarget", true);
            aimAssistGlowColor = builder
                    .comment(" Glow color in HEX format (e.g., FF0000 = Red).",
                            " 发光颜色，HEX格式（如 FF0000 = 红色）。")
                    .translation("config.jujutsu_addon.client.aim_assist_glow_color")
                    .define("GlowColor", "FF5555");
            aimAssistShowIndicator = builder
                    .comment(" Show a lock-on indicator on the target.",
                            " 在目标上显示锁定指示器。")
                    .translation("config.jujutsu_addon.client.aim_assist_indicator")
                    .define("ShowLockIndicator", true);
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
