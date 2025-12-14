package com.jujutsuaddon.addon.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 渲染工具类
 * 统一处理技能图标、术式图标的渲染、颜色、动画效果
 */
public class RenderHelper {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Set<ResourceLocation> MISSING_ICONS = new HashSet<>();

    // ==================== 统一颜色常量 ====================

    public static final class Colors {
        // 文字颜色
        public static final int TEXT_NORMAL = 0xFFFFFF;
        public static final int TEXT_ACTIVE = 0x55FF55;
        public static final int TEXT_DISABLED = 0x888888;
        public static final int TEXT_DEAD = 0xFF4444;
        public static final int TEXT_TECHNIQUE_INACTIVE = 0xFF6666;
        public static final int TEXT_SUMMON_CONFLICT = 0xFFAA00;

        // 背景颜色
        public static final int BG_NORMAL = 0xFF2A2A3A;
        public static final int BG_ACTIVE = 0xFF224422;
        public static final int BG_DISABLED = 0xFF333333;
        public static final int BG_DEAD = 0xFF442222;
        public static final int BG_TECHNIQUE_INACTIVE = 0xFF3A2233;
        public static final int BG_SUMMON_CONFLICT = 0xFF443322;

        // 边框颜色
        public static final int BORDER_NORMAL = 0xFF888888;
        public static final int BORDER_DISABLED = 0xFF666666;
        public static final int BORDER_DEAD = 0xFFAA3333;
        public static final int BORDER_TECHNIQUE_INACTIVE = 0xFF8844AA;
        public static final int BORDER_SUMMON_CONFLICT = 0xFFAA3333;
        public static final int BORDER_USABLE = 0xFF00CC00;
        public static final int BORDER_NOT_OWNED = 0xFFCC0000;
    }

    // ==================== 脉动效果 ====================

    /**
     * 计算脉动值（用于激活状态的边框闪烁）
     * @return 0.4 ~ 1.0 之间的脉动值
     */
    public static float getPulseValue() {
        long time = System.currentTimeMillis();
        return (float) (Math.sin(time / 200.0) * 0.3 + 0.7);
    }

    /**
     * 获取脉动的绿色边框颜色（激活状态）
     */
    public static int getPulsingActiveBorderColor() {
        float pulse = getPulseValue();
        int green = (int) (255 * pulse);
        return 0xFF000000 | (green << 8);
    }

    /**
     * 获取脉动的红色边框颜色（冲突状态）
     */
    public static int getPulsingConflictBorderColor() {
        float pulse = getPulseValue();
        int red = (int) (180 + 75 * pulse);
        return 0xFF000000 | (red << 16) | 0x3333;
    }

    // ==================== 统一状态颜色获取 ====================

    /**
     * 根据技能状态获取文字颜色
     */
    public static int getTextColor(boolean isDead, boolean techniqueNotActive,
                                   boolean canUse, boolean isActive, boolean hasSummon,
                                   boolean summonConflict) {
        if (isDead) return Colors.TEXT_DEAD;
        if (techniqueNotActive) return Colors.TEXT_TECHNIQUE_INACTIVE;
        if (summonConflict) return Colors.TEXT_SUMMON_CONFLICT;
        if (!canUse) return Colors.TEXT_DISABLED;
        if (isActive || hasSummon) return Colors.TEXT_ACTIVE;
        return Colors.TEXT_NORMAL;
    }

    /**
     * 根据技能状态获取图标背景颜色
     */
    public static int getIconBgColor(boolean isDead, boolean techniqueNotActive,
                                     boolean canUse, boolean isActive, boolean hasSummon,
                                     boolean summonConflict) {
        if (isDead) return Colors.BG_DEAD;
        if (techniqueNotActive) return Colors.BG_TECHNIQUE_INACTIVE;
        if (isActive || hasSummon) return Colors.BG_ACTIVE;
        if (summonConflict) return Colors.BG_SUMMON_CONFLICT;
        if (!canUse) return Colors.BG_DISABLED;
        return Colors.BG_NORMAL;
    }

    /**
     * 根据技能状态获取边框颜色（含脉动效果）
     */
    public static int getBorderColor(boolean isDead, boolean techniqueNotActive,
                                     boolean canUse, boolean isActive, boolean hasSummon,
                                     boolean summonConflict, boolean playerOwns) {
        if (isActive || hasSummon) {
            return getPulsingActiveBorderColor();
        }
        if (isDead) return Colors.BORDER_DEAD;
        if (techniqueNotActive) {
            return getPulsingConflictBorderColor();
        }
        if (summonConflict) return Colors.BORDER_SUMMON_CONFLICT;
        if (!playerOwns) return Colors.BORDER_NOT_OWNED;
        if (canUse) return Colors.BORDER_USABLE;
        return Colors.BORDER_NORMAL;
    }

    // ==================== 图标检查 ====================

    public static boolean textureExists(ResourceLocation location) {
        if (location == null) return false;
        if (MISSING_ICONS.contains(location)) return false;

        try {
            if (MC.getResourceManager().getResource(location).isEmpty()) {
                MISSING_ICONS.add(location);
                return false;
            }
            return true;
        } catch (Exception e) {
            MISSING_ICONS.add(location);
            return false;
        }
    }

    @Nullable
    public static ResourceLocation getAbilityIconPath(Ability ability) {
        if (ability == null) return null;
        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return null;
        return new ResourceLocation(key.getNamespace(),
                "textures/ability/" + key.getPath() + ".png");
    }

    public static boolean hasAbilityIcon(Ability ability) {
        ResourceLocation iconPath = getAbilityIconPath(ability);
        return iconPath != null && textureExists(iconPath);
    }

    // ==================== 技能图标渲染 ====================

    public static void renderAbilityIcon(GuiGraphics graphics, @Nullable Ability ability,
                                         int x, int y, int size, boolean grayed) {
        if (ability == null) return;

        ResourceLocation iconPath = getAbilityIconPath(ability);

        if (iconPath != null && textureExists(iconPath)) {
            try {
                RenderSystem.enableBlend();
                if (grayed) {
                    RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
                }
                graphics.blit(iconPath, x, y, 0, 0, size, size, size, size);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
                return;
            } catch (Exception e) {
                MISSING_ICONS.add(iconPath);
            }
        }

        renderFallbackIcon(graphics, ability.getName().getString(), x, y, size, grayed);
    }

    /**
     * 渲染技能图标（带红色滤镜，用于术式未激活）
     */
    public static void renderAbilityIconWithTint(GuiGraphics graphics, @Nullable Ability ability,
                                                 int x, int y, int size, boolean techniqueNotActive) {
        if (ability == null) return;

        ResourceLocation iconPath = getAbilityIconPath(ability);

        if (iconPath != null && textureExists(iconPath)) {
            try {
                RenderSystem.enableBlend();
                if (techniqueNotActive) {
                    RenderSystem.setShaderColor(0.8f, 0.4f, 0.4f, 1.0f);
                }
                graphics.blit(iconPath, x, y, 0, 0, size, size, size, size);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
                return;
            } catch (Exception e) {
                MISSING_ICONS.add(iconPath);
            }
        }

        int bgColor = techniqueNotActive ? Colors.BG_TECHNIQUE_INACTIVE : Colors.BG_NORMAL;
        renderTextIcon(graphics, ability.getName().getString(), x, y, size, techniqueNotActive, bgColor);
    }

    // ==================== 术式图标渲染 ====================

    public static void renderTechniqueIcon(GuiGraphics graphics, @Nullable CursedTechnique technique,
                                           int x, int y, int size, boolean isActive) {
        if (technique == null) return;

        Font font = MC.font;

        int bgColor = isActive ? 0xDD445599 : 0xDD663399;
        graphics.fill(x, y, x + size, y + size, bgColor);

        if (isActive) {
            graphics.renderOutline(x, y, size, size, getPulsingActiveBorderColor());
        } else {
            graphics.renderOutline(x, y, size, size, 0xFFAA66CC);
        }

        String name = technique.getName().getString();
        String abbrev = getAbbreviation(name, 3);
        graphics.drawCenteredString(font, abbrev, x + size / 2, y + (size - 8) / 2, 0xFFFFFF);
    }

    // ==================== 咒灵管理图标渲染 ====================
    public static void renderCurseManagementIcon(GuiGraphics graphics, int x, int y, int size) {
        Font font = MC.font;

        // 紫色背景
        graphics.fill(x, y, x + size, y + size, 0xDD3A2255);
        graphics.renderOutline(x, y, size, size, 0xFFAA55FF);

        // 骷髅图标
        graphics.drawCenteredString(font, "☠", x + size / 2, y + (size - 8) / 2, 0xAA55FF);
    }

    // ==================== 备用图标渲染 ====================

    public static void renderFallbackIcon(GuiGraphics graphics, String name,
                                          int x, int y, int size, boolean grayed) {
        renderTextIcon(graphics, name, x, y, size, grayed, grayed ? 0xFF333333 : 0xFF444444);
    }

    public static void renderTextIcon(GuiGraphics graphics, String name,
                                      int x, int y, int size, boolean grayed, int bgColor) {
        Font font = MC.font;

        graphics.fill(x, y, x + size, y + size, bgColor);
        graphics.renderOutline(x, y, size, size, 0xFF555555);

        if (name == null || name.isEmpty()) return;

        String abbrev = getAbbreviation(name, size >= 16 ? 3 : 1);
        int textColor = grayed ? 0x888888 : 0xFFFFFF;
        int textWidth = font.width(abbrev);
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size - 8) / 2;

        graphics.drawString(font, abbrev, textX, textY, textColor, false);
    }

    // ==================== 工具方法 ====================

    public static String getAbbreviation(String name, int maxChars) {
        if (name == null || name.isEmpty()) return "";
        // 1. CJK (中文/日文/韩文) 检查
        char firstChar = name.charAt(0);
        boolean isCJK = Character.UnicodeBlock.of(firstChar) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                Character.UnicodeBlock.of(firstChar) == Character.UnicodeBlock.HIRAGANA ||
                Character.UnicodeBlock.of(firstChar) == Character.UnicodeBlock.KATAKANA;
        if (isCJK) {
            return name.substring(0, 1);
        }
        String[] parts = name.split(" ");
        // 2. 单个单词的情况 (例如 "Fireball")
        // ★★★ 智能点 1：强制限制为 2 个字母 ★★★
        // 即使 maxChars 传进来是 3，这里也只返回 2，避免 "FIR" 这种太宽的情况
        if (parts.length == 1) {
            int len = Math.min(name.length(), 2);
            return name.substring(0, len).toUpperCase();
        }
        // 3. 2 到 3 个单词的情况 (例如 "Hollow Wicker Basket" 或 "Curse Absorption")
        // ★★★ 智能点 2：尝试生成首字母缩写 (HWB / CA) ★★★
        if (parts.length <= 3) {
            StringBuilder acronym = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    acronym.append(part.charAt(0));
                }
            }
            String result = acronym.toString().toUpperCase();
            // 如果生成的缩写长度 (比如 HWB 是 3) 超过了 UI 允许的最大长度 (maxChars)
            // 就截断它。但只要 UI 允许 3，这里就会返回 HWB。
            if (result.length() > maxChars) {
                return result.substring(0, maxChars);
            }
            return result;
        }
        // 4. 4个及以上单词的情况 (超长名字)
        // 回退到取前两个字母，避免缩写过长
        int len = Math.min(name.length(), 2);
        return name.substring(0, len).toUpperCase();
    }

    public static String formatCooldown(int ticks) {
        float seconds = ticks / 20.0f;
        if (seconds >= 60) {
            int mins = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format("%d:%02d", mins, secs);
        } else if (seconds >= 10) {
            return String.format("%.0f", seconds);
        } else {
            return String.format("%.1f", seconds);
        }
    }

    // ==================== 槽位背景颜色（带透明度） ====================
    /**
     * 获取 HUD 技能槽位背景颜色（半透明版本）
     */
    public static int getSlotBgColor(boolean isDead, boolean techniqueNotActive,
                                     boolean isActive, boolean hasSummon,
                                     boolean summonConflict, boolean playerOwns,
                                     boolean canUse, boolean onCooldown) {
        if (summonConflict) return 0x80552222;
        if (techniqueNotActive) return 0x80332244;
        if (isActive || hasSummon) return 0x80004400;
        if (!playerOwns) return 0x80550000;
        if (!canUse || onCooldown) return 0x80333333;
        return 0x80000000;
    }
    /**
     * 获取 HUD 技能槽位边框颜色
     */
    public static int getSlotBorderColor(boolean isActive, boolean hasSummon,
                                         boolean techniqueNotActive, boolean summonConflict,
                                         boolean playerOwns, boolean usable) {
        if (isActive || hasSummon) return getPulsingActiveBorderColor();
        if (techniqueNotActive) return getPulsingConflictBorderColor();
        if (summonConflict) return 0xFFAA3333;
        if (!playerOwns) return 0xFFCC0000;
        if (usable) return 0xFF00CC00;
        return 0xFF888888;
    }
    // ==================== Header 颜色常量 ====================
    public static final class HeaderColors {
        // 原生术式
        public static final int NATIVE_BG = 0xFF2A3A2A;
        public static final int NATIVE_BORDER = 0xFF55AA55;
        public static final int NATIVE_TEXT = 0x88FF88;
        // 主分类（偷取/复制）
        public static final int MAIN_BG = 0xFF3A3A5A;
        public static final int MAIN_BORDER = 0xFFAA88FF;
        public static final int MAIN_TEXT = 0xFFDD99;
        // 子术式（第二层）
        public static final int SUB_BG_NORMAL = 0xFF2A2A3A;
        public static final int SUB_BG_ACTIVE = 0xFF2A4A2A;
        public static final int SUB_BORDER_NORMAL = 0xFF6666AA;
        public static final int SUB_TEXT_NORMAL = 0xAAAAFF;
        public static final int SUB_TEXT_ACTIVE = 0x55FF55;
        // 第三层
        public static final int THIRD_BG_NORMAL = 0xFF2A2A3A;
        public static final int THIRD_BG_ACTIVE = 0xFF2A3A4A;
        public static final int THIRD_BORDER_NORMAL = 0xFF5566AA;
        public static final int THIRD_TEXT_NORMAL = 0x8888FF;
        public static final int THIRD_TEXT_ACTIVE = 0x55AAFF;
        // 术式标签
        public static final int TAG_STOLEN = 0xFF6666;
        public static final int TAG_COPIED = 0x66AAFF;
    }

    public static void clearIconCache() {
        MISSING_ICONS.clear();
    }
}
