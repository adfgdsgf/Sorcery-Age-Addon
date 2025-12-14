package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import javax.annotation.Nullable;

/**
 * 技能列表条目
 */
public class AbilityEntry {

    public enum EntryType {
        // 第一层：原生
        NATIVE_TECHNIQUE_HEADER,
        NATIVE_ABILITY,

        // 分类标题
        MAIN_TECHNIQUE_HEADER,

        // 第二层：偷来/复制的母术式
        SUB_TECHNIQUE_HEADER,
        SUB_TECHNIQUE_ABILITY,

        // 第三层：通过第二层再获得的母术式
        THIRD_TECHNIQUE_HEADER,
        THIRD_TECHNIQUE_ABILITY,

        // ★★★ 咒灵管理入口（作为子条目显示）★★★
        CURSE_MANAGEMENT_ENTRY,

        @Deprecated
        CURSE_MANAGEMENT_HEADER,  // 保留旧类型用于兼容

        @Deprecated
        NORMAL_ABILITY
    }

    public final EntryType type;

    @Nullable public final Ability ability;
    @Nullable public final CursedTechnique technique;
    @Nullable public final CursedTechnique parentTechnique;
    @Nullable public final AbilityStatus status;

    public final TechniqueHelper.TechniqueSource sourceType;
    public final boolean isCollapsed;
    public final boolean isActive;

    // ★★★ 咒灵数量 ★★★
    public int curseCount = 0;

    // ★★★ 自定义缩进级别（-1 表示使用默认计算）★★★
    public int customIndentLevel = -1;

    // ========== 静态工厂方法 ==========

    /** 原生母术式标题 */
    public static AbilityEntry nativeHeader(CursedTechnique technique, boolean collapsed) {
        return new AbilityEntry(EntryType.NATIVE_TECHNIQUE_HEADER,
                null, technique, null, null,
                TechniqueHelper.TechniqueSource.NATIVE, collapsed, true);
    }

    /** 原生子术式 */
    public static AbilityEntry nativeAbility(Ability ability, AbilityStatus status) {
        return new AbilityEntry(EntryType.NATIVE_ABILITY,
                ability, null, null, status,
                TechniqueHelper.TechniqueSource.NATIVE, false, false);
    }

    /** 主分类标题 */
    public static AbilityEntry mainHeader(TechniqueHelper.TechniqueSource sourceType, boolean collapsed) {
        return new AbilityEntry(EntryType.MAIN_TECHNIQUE_HEADER,
                null, null, null, null,
                sourceType, collapsed, false);
    }

    /** 第二层母术式 */
    public static AbilityEntry subHeader(CursedTechnique technique,
                                         TechniqueHelper.TechniqueSource sourceType,
                                         boolean collapsed, boolean active) {
        return new AbilityEntry(EntryType.SUB_TECHNIQUE_HEADER,
                null, technique, null, null,
                sourceType, collapsed, active);
    }

    /** 第二层母术式的子术式 */
    public static AbilityEntry subAbility(Ability ability, CursedTechnique fromTechnique,
                                          TechniqueHelper.TechniqueSource sourceType,
                                          AbilityStatus status) {
        return new AbilityEntry(EntryType.SUB_TECHNIQUE_ABILITY,
                ability, fromTechnique, null, status,
                sourceType, false, false);
    }

    /** 第三层母术式 */
    public static AbilityEntry thirdHeader(CursedTechnique technique,
                                           CursedTechnique parentTechnique,
                                           boolean collapsed, boolean active) {
        return new AbilityEntry(EntryType.THIRD_TECHNIQUE_HEADER,
                null, technique, parentTechnique, null,
                TechniqueHelper.TechniqueSource.COPIED, collapsed, active);
    }

    /** 第三层母术式的子术式 */
    public static AbilityEntry thirdAbility(Ability ability,
                                            CursedTechnique fromTechnique,
                                            CursedTechnique parentTechnique,
                                            AbilityStatus status) {
        return new AbilityEntry(EntryType.THIRD_TECHNIQUE_ABILITY,
                ability, fromTechnique, parentTechnique, status,
                TechniqueHelper.TechniqueSource.COPIED, false, false);
    }

    // ★★★ 咒灵管理入口（带自定义缩进级别）★★★
    public static AbilityEntry curseManagementEntry(int curseCount, int indentLevel) {
        AbilityEntry entry = new AbilityEntry(EntryType.CURSE_MANAGEMENT_ENTRY,
                null, null, null, null,
                TechniqueHelper.TechniqueSource.NATIVE, false, curseCount > 0);
        entry.curseCount = curseCount;
        entry.customIndentLevel = indentLevel;
        return entry;
    }

    // ★★★ 保留旧方法用于兼容（顶部显示时使用，缩进级别0）★★★
    @Deprecated
    public static AbilityEntry curseManagementHeader(int curseCount) {
        return curseManagementEntry(curseCount, 0);
    }

    @Deprecated
    public static AbilityEntry normalAbility(Ability ability, AbilityStatus status) {
        return nativeAbility(ability, status);
    }

    // ========== 私有构造函数 ==========

    private AbilityEntry(EntryType type,
                         @Nullable Ability ability,
                         @Nullable CursedTechnique technique,
                         @Nullable CursedTechnique parentTechnique,
                         @Nullable AbilityStatus status,
                         TechniqueHelper.TechniqueSource sourceType,
                         boolean isCollapsed, boolean isActive) {
        this.type = type;
        this.ability = ability;
        this.technique = technique;
        this.parentTechnique = parentTechnique;
        this.status = status;
        this.sourceType = sourceType;
        this.isCollapsed = isCollapsed;
        this.isActive = isActive;
    }

    // ========== 便捷方法 ==========

    public boolean isNativeHeader() {
        return type == EntryType.NATIVE_TECHNIQUE_HEADER;
    }

    public boolean isMainHeader() {
        return type == EntryType.MAIN_TECHNIQUE_HEADER;
    }

    public boolean isSubHeader() {
        return type == EntryType.SUB_TECHNIQUE_HEADER;
    }

    public boolean isThirdHeader() {
        return type == EntryType.THIRD_TECHNIQUE_HEADER;
    }

    public boolean isHeader() {
        return isNativeHeader() || isMainHeader() || isSubHeader() || isThirdHeader();
    }

    public boolean isDraggableTechnique() {
        return type == EntryType.SUB_TECHNIQUE_HEADER ||
                type == EntryType.THIRD_TECHNIQUE_HEADER;
    }

    public boolean isAbility() {
        return type == EntryType.NATIVE_ABILITY ||
                type == EntryType.NORMAL_ABILITY ||
                type == EntryType.SUB_TECHNIQUE_ABILITY ||
                type == EntryType.THIRD_TECHNIQUE_ABILITY;
    }

    public boolean isNativeAbility() {
        return type == EntryType.NATIVE_ABILITY || type == EntryType.NORMAL_ABILITY;
    }

    public boolean isSubAbility() {
        return type == EntryType.SUB_TECHNIQUE_ABILITY;
    }

    public boolean isThirdAbility() {
        return type == EntryType.THIRD_TECHNIQUE_ABILITY;
    }

    // ★★★ 是否是咒灵管理入口 ★★★
    public boolean isCurseManagement() {
        return type == EntryType.CURSE_MANAGEMENT_ENTRY ||
                type == EntryType.CURSE_MANAGEMENT_HEADER;
    }

    /** 获取缩进级别 */
    public int getIndentLevel() {
        // ★★★ 如果有自定义缩进，使用自定义值 ★★★
        if (customIndentLevel >= 0) {
            return customIndentLevel;
        }

        return switch (type) {
            case NATIVE_TECHNIQUE_HEADER, MAIN_TECHNIQUE_HEADER,
                    CURSE_MANAGEMENT_HEADER, CURSE_MANAGEMENT_ENTRY -> 0;
            case NATIVE_ABILITY, NORMAL_ABILITY, SUB_TECHNIQUE_HEADER -> 1;
            case SUB_TECHNIQUE_ABILITY, THIRD_TECHNIQUE_HEADER -> 2;
            case THIRD_TECHNIQUE_ABILITY -> 3;
        };
    }

    public int getIndentPixels() {
        return getIndentLevel() * 10;
    }
}
