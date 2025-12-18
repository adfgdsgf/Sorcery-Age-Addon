package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import com.jujutsuaddon.addon.util.helper.tenshadows.TenShadowsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.util.*;

public class AbilityListRenderer {

    private final Font font;
    private int x, y, width, height;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private final List<AbilityEntry> entries = new ArrayList<>();
    private final Set<TechniqueHelper.TechniqueSource> collapsedMainSections = new HashSet<>();
    private final Set<CursedTechnique> collapsedTechniques = new HashSet<>();

    private EntryRenderer entryRenderer;

    public AbilityListRenderer() {
        this.font = Minecraft.getInstance().font;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.entryRenderer = new EntryRenderer(font, x, width);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean isInBounds(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Nullable public Ability getHoveredToggleAbility() {
        return entryRenderer != null ? entryRenderer.hoveredToggleAbility : null;
    }

    @Nullable public CursedTechnique getHoveredDeleteTechnique() {
        return entryRenderer != null ? entryRenderer.hoveredDeleteTechnique : null;
    }

    @Nullable public CursedTechnique getHoveredSubHeader() {
        return entryRenderer != null ? entryRenderer.hoveredSubHeader : null;
    }

    @Nullable public CursedTechnique getHoveredActivateTechnique() {
        return entryRenderer != null ? entryRenderer.hoveredActivateTechnique : null;
    }

    @Nullable public TechniqueHelper.TechniqueSource getHoveredMainHeader() {
        return entryRenderer != null ? entryRenderer.hoveredMainHeader : null;
    }

    @Nullable public CursedTechnique getHoveredThirdHeader() {
        return entryRenderer != null ? entryRenderer.hoveredThirdHeader : null;
    }

    public boolean isToggleButtonHovered(int mouseX, int mouseY) {
        if (entryRenderer == null || entryRenderer.hoveredToggleAbility == null) return false;
        return mouseX >= entryRenderer.toggleBtnX && mouseX < entryRenderer.toggleBtnX + 22 &&
                mouseY >= entryRenderer.toggleBtnY && mouseY < entryRenderer.toggleBtnY + 18;
    }

    public boolean isTechniqueActive(CursedTechnique technique) {
        if (technique == null) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && TechniqueHelper.isTechniqueActive(player, technique);
    }

    @Nullable
    public CursedTechnique getDraggableTechniqueAt(int mouseX, int mouseY) {
        if (!isInBounds(mouseX, mouseY)) return null;
        int index = scrollOffset + (mouseY - y - 2) / EntryRenderer.ITEM_HEIGHT;
        if (index >= 0 && index < entries.size()) {
            AbilityEntry entry = entries.get(index);
            if (entry.isDraggableTechnique()) {
                return entry.technique;
            }
        }
        return null;
    }

    public boolean isCurseManagementHovered(int mouseX, int mouseY) {
        if (!isInBounds(mouseX, mouseY)) return false;
        int index = scrollOffset + (mouseY - y - 2) / EntryRenderer.ITEM_HEIGHT;
        if (index >= 0 && index < entries.size()) {
            return entries.get(index).isCurseManagement();
        }
        return false;
    }

    public boolean getCurseManagementDraggableAt(int mouseX, int mouseY) {
        if (!isInBounds(mouseX, mouseY)) return false;
        int index = scrollOffset + (mouseY - y - 2) / EntryRenderer.ITEM_HEIGHT;
        if (index >= 0 && index < entries.size()) {
            return entries.get(index).isCurseManagement();
        }
        return false;
    }

    public void toggleMainSectionCollapse(TechniqueHelper.TechniqueSource source) {
        if (source == TechniqueHelper.TechniqueSource.NONE) return;
        if (collapsedMainSections.contains(source)) {
            collapsedMainSections.remove(source);
        } else {
            collapsedMainSections.add(source);
        }
        refresh();
    }

    public void toggleTechniqueCollapse(CursedTechnique technique) {
        if (technique == null) return;
        if (collapsedTechniques.contains(technique)) {
            collapsedTechniques.remove(technique);
        } else {
            collapsedTechniques.add(technique);
        }
        refresh();
    }

    public void refresh() {
        entries.clear();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return;

        CursedTechnique nativeTechnique = data.getTechnique();
        Set<CursedTechnique> stolen = data.getStolen();
        Set<CursedTechnique> copied = data.getCopied();
        CursedTechnique currentStolen = data.getCurrentStolen();
        CursedTechnique currentCopied = data.getCurrentCopied();

        boolean hasNativeCopyAbility = TechniqueHelper.isCopyTechnique(nativeTechnique);
        boolean hasNativeStealAbility = TechniqueHelper.isStealTechnique(nativeTechnique);

        // 获取咒灵数量
        List<AbsorbedCurse> curses = data.getCurses();
        int curseCount = curses.size();

        // 收集额外术式技能Key（防重复）
        Set<ResourceLocation> extraAbilityKeys = new HashSet<>();
        for (CursedTechnique technique : TechniqueHelper.getAllExtraTechniques(player)) {
            if (technique != null) {
                for (Ability ability : technique.getAbilities()) {
                    ResourceLocation key = JJKAbilities.getKey(ability);
                    if (key != null) extraAbilityKeys.add(key);
                }
            }
        }

        Set<ResourceLocation> addedAbilityKeys = new HashSet<>();
        boolean useTenShadows = TenShadowsHelper.isEnabled();
        boolean hasNativeTenShadows = nativeTechnique == CursedTechnique.TEN_SHADOWS;
        List<Ability> tenShadowsAbilities = useTenShadows && TenShadowsHelper.hasTenShadows(player)
                ? TenShadowsHelper.getAllTenShadowsAbilitiesIncludingDead(player) : Collections.emptyList();

        // =====================================================
        // 1. 原生术式
        // =====================================================
        List<Ability> nativeAbilityList = new ArrayList<>();
        for (Ability ability : JJKAbilities.getAbilities(player)) {
            if (ability == null) continue;
            ResourceLocation key = JJKAbilities.getKey(ability);
            if (key == null || extraAbilityKeys.contains(key) || addedAbilityKeys.contains(key)) continue;

            boolean isValid = useTenShadows && TenShadowsHelper.isTenShadowsAbility(ability)
                    ? tenShadowsAbilities.contains(ability) : ability.isValid(player);
            if (isValid) {
                nativeAbilityList.add(ability);
                addedAbilityKeys.add(key);
            }
        }

        if (useTenShadows && hasNativeTenShadows) {
            for (Ability ability : tenShadowsAbilities) {
                if (ability == null) continue;
                ResourceLocation key = JJKAbilities.getKey(ability);
                if (key == null || extraAbilityKeys.contains(key) || addedAbilityKeys.contains(key)) continue;
                nativeAbilityList.add(ability);
                addedAbilityKeys.add(key);
            }
        }

        if (!nativeAbilityList.isEmpty() || nativeTechnique == CursedTechnique.CURSE_MANIPULATION) {
            boolean collapsed = collapsedMainSections.contains(TechniqueHelper.TechniqueSource.NATIVE);
            entries.add(AbilityEntry.nativeHeader(nativeTechnique, collapsed));
            if (!collapsed) {
                for (Ability ability : nativeAbilityList) {
                    AbilityStatus status = AbilityStatus.build(player, ability);
                    entries.add(AbilityEntry.nativeAbility(ability, status));
                }
                // 原生咒灵操术
                if (nativeTechnique == CursedTechnique.CURSE_MANIPULATION) {
                    entries.add(AbilityEntry.curseManagementEntry(curseCount, 1));
                }
            }
        }

        // =====================================================
        // 2. 偷取的术式（或原生偷取能力获得的）
        // =====================================================
        if ((stolen != null && !stolen.isEmpty()) || hasNativeStealAbility) {
            boolean mainCollapsed = collapsedMainSections.contains(TechniqueHelper.TechniqueSource.STOLEN);

            if (stolen != null && !stolen.isEmpty()) {
                entries.add(AbilityEntry.mainHeader(TechniqueHelper.TechniqueSource.STOLEN, mainCollapsed));
            }

            if (!mainCollapsed && stolen != null) {
                for (CursedTechnique technique : stolen) {
                    if (technique == null) continue;

                    // ★★★ 添加第二层术式及其技能 ★★★
                    addSecondLayerTechnique(player, technique,
                            TechniqueHelper.TechniqueSource.STOLEN,
                            currentStolen, currentCopied,
                            copied,  // 如果偷取的是复制能力，第三层来自 copied
                            TechniqueHelper.TechniqueSource.COPIED,  // 第三层的来源类型
                            addedAbilityKeys, useTenShadows, tenShadowsAbilities, curseCount);
                }
            }
        }

        // =====================================================
        // 3. 复制的术式（原生复制能力获得的）
        // =====================================================
        if (hasNativeCopyAbility && copied != null && !copied.isEmpty()) {
            boolean mainCollapsed = collapsedMainSections.contains(TechniqueHelper.TechniqueSource.COPIED);
            entries.add(AbilityEntry.mainHeader(TechniqueHelper.TechniqueSource.COPIED, mainCollapsed));

            if (!mainCollapsed) {
                for (CursedTechnique technique : copied) {
                    if (technique == null) continue;

                    // ★★★ 添加第二层术式及其技能 ★★★
                    addSecondLayerTechnique(player, technique,
                            TechniqueHelper.TechniqueSource.COPIED,
                            currentCopied, currentStolen,
                            stolen,  // 如果复制的是偷取能力，第三层来自 stolen
                            TechniqueHelper.TechniqueSource.STOLEN,  // 第三层的来源类型
                            addedAbilityKeys, useTenShadows, tenShadowsAbilities, curseCount);
                }
            }
        }

        updateScroll();
    }

    /**
     * ★★★ 统一处理第二层术式（完全对称）★★★
     *
     * @param player 玩家
     * @param technique 第二层术式
     * @param sourceType 第二层来源类型（STOLEN 或 COPIED）
     * @param currentOfThisType 当前激活的同类型术式
     * @param currentOfOtherType 当前激活的另一类型术式
     * @param thirdLayerPool 第三层术式池（如果第二层是复制，这里是 stolen；反之亦然）
     * @param thirdLayerSourceType 第三层来源类型
     */
    private void addSecondLayerTechnique(LocalPlayer player, CursedTechnique technique,
                                         TechniqueHelper.TechniqueSource sourceType,
                                         CursedTechnique currentOfThisType,
                                         CursedTechnique currentOfOtherType,
                                         Set<CursedTechnique> thirdLayerPool,
                                         TechniqueHelper.TechniqueSource thirdLayerSourceType,
                                         Set<ResourceLocation> addedKeys,
                                         boolean useTenShadows,
                                         List<Ability> tenShadowsAbilities,
                                         int curseCount) {
        boolean subCollapsed = collapsedTechniques.contains(technique);
        boolean isActive = technique == currentOfThisType;
        entries.add(AbilityEntry.subHeader(technique, sourceType, subCollapsed, isActive));

        if (subCollapsed) return;

        // 添加第二层术式的技能
        addTechniqueAbilities(player, technique, sourceType, addedKeys, useTenShadows, tenShadowsAbilities);

        // 咒灵操术特殊处理
        if (technique == CursedTechnique.CURSE_MANIPULATION) {
            entries.add(AbilityEntry.curseManagementEntry(curseCount, 2));
        }

        // ★★★ 检查是否能产生第三层 ★★★
        boolean canProduceThirdLayer = false;
        if (sourceType == TechniqueHelper.TechniqueSource.STOLEN) {
            // 偷取的术式：如果是复制类型，可以产生第三层（复制得到的术式）
            canProduceThirdLayer = TechniqueHelper.isCopyTechnique(technique);
        } else if (sourceType == TechniqueHelper.TechniqueSource.COPIED) {
            // 复制的术式：如果是偷取类型，可以产生第三层（偷取得到的术式）
            canProduceThirdLayer = TechniqueHelper.isStealTechnique(technique);
        }

        if (canProduceThirdLayer && thirdLayerPool != null && !thirdLayerPool.isEmpty()) {
            for (CursedTechnique thirdTech : thirdLayerPool) {
                if (thirdTech == null) continue;

                boolean thirdCollapsed = collapsedTechniques.contains(thirdTech);
                boolean thirdActive = thirdTech == currentOfOtherType;
                entries.add(AbilityEntry.thirdHeader(thirdTech, technique, thirdCollapsed, thirdActive));

                if (!thirdCollapsed) {
                    // ★★★ 使用正确的来源类型 ★★★
                    addThirdTechniqueAbilities(player, thirdTech, technique, thirdLayerSourceType,
                            addedKeys, useTenShadows, tenShadowsAbilities);

                    // 第三层咒灵操术
                    if (thirdTech == CursedTechnique.CURSE_MANIPULATION) {
                        entries.add(AbilityEntry.curseManagementEntry(curseCount, 3));
                    }
                }
            }
        }
    }

    private void addTechniqueAbilities(LocalPlayer player, CursedTechnique technique,
                                       TechniqueHelper.TechniqueSource sourceType,
                                       Set<ResourceLocation> addedKeys, boolean useTenShadows,
                                       List<Ability> tenShadowsAbilities) {
        List<Ability> abilities = useTenShadows && technique == CursedTechnique.TEN_SHADOWS
                ? tenShadowsAbilities : Arrays.asList(technique.getAbilities());

        for (Ability ability : abilities) {
            if (ability == null) continue;
            if (!useTenShadows || !TenShadowsHelper.isTenShadowsAbility(ability)) {
                if (!ability.isValid(player)) continue;
            }
            ResourceLocation key = JJKAbilities.getKey(ability);
            if (key == null || addedKeys.contains(key)) continue;

            AbilityStatus status = AbilityStatus.build(player, ability, technique, sourceType);
            entries.add(AbilityEntry.subAbility(ability, technique, sourceType, status));
            addedKeys.add(key);
        }
    }

    /**
     * ★★★ 修复：第三层技能使用正确的来源类型 ★★★
     */
    private void addThirdTechniqueAbilities(LocalPlayer player, CursedTechnique technique,
                                            CursedTechnique parentTechnique,
                                            TechniqueHelper.TechniqueSource sourceType,  // ★★★ 新增参数 ★★★
                                            Set<ResourceLocation> addedKeys,
                                            boolean useTenShadows, List<Ability> tenShadowsAbilities) {
        List<Ability> abilities = useTenShadows && technique == CursedTechnique.TEN_SHADOWS
                ? tenShadowsAbilities : Arrays.asList(technique.getAbilities());

        for (Ability ability : abilities) {
            if (ability == null) continue;
            if (!useTenShadows || !TenShadowsHelper.isTenShadowsAbility(ability)) {
                if (!ability.isValid(player)) continue;
            }
            ResourceLocation key = JJKAbilities.getKey(ability);
            if (key == null || addedKeys.contains(key)) continue;

            // ★★★ 使用传入的 sourceType，而不是硬编码 ★★★
            AbilityStatus status = AbilityStatus.build(player, ability, technique, sourceType);
            entries.add(AbilityEntry.thirdAbility(ability, technique, parentTechnique, status));
            addedKeys.add(key);
        }
    }

    private void updateScroll() {
        int visibleCount = (height - 4) / EntryRenderer.ITEM_HEIGHT;
        maxScroll = Math.max(0, entries.size() - visibleCount);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (entryRenderer == null) return;
        entryRenderer.resetHoverState();

        graphics.fill(x, y, x + width, y + height, 0xCC222222);
        graphics.renderOutline(x, y, width, height, 0xFF555555);
        graphics.drawCenteredString(font, Component.translatable("gui.jujutsu_addon.skill_bar_config.available"),
                x + width / 2, y - 12, 0xFFFFFF);

        LocalPlayer player = Minecraft.getInstance().player;
        int visibleCount = (height - 4) / EntryRenderer.ITEM_HEIGHT;

        graphics.enableScissor(x, y, x + width, y + height);
        for (int i = 0; i < visibleCount + 1 && scrollOffset + i < entries.size(); i++) {
            AbilityEntry entry = entries.get(scrollOffset + i);
            int itemY = y + 2 + i * EntryRenderer.ITEM_HEIGHT;
            entryRenderer.renderEntry(graphics, entry, itemY, mouseX, mouseY, player);
        }
        graphics.disableScissor();

        renderScrollbar(graphics, visibleCount);
    }

    private void renderScrollbar(GuiGraphics graphics, int visibleCount) {
        if (maxScroll <= 0) return;
        int barH = height - 4;
        int thumbH = Math.max(20, barH * visibleCount / entries.size());
        int thumbY = y + 2 + (barH - thumbH) * scrollOffset / maxScroll;
        graphics.fill(x + width - 6, y + 2, x + width - 2, y + height - 2, 0x80000000);
        graphics.fill(x + width - 5, thumbY, x + width - 3, thumbY + thumbH, 0xFFAAAAAA);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isInBounds(mouseX, mouseY)) return;

        int index = scrollOffset + (mouseY - y - 2) / EntryRenderer.ITEM_HEIGHT;
        if (index < 0 || index >= entries.size()) return;

        AbilityEntry entry = entries.get(index);

        if (entry.isCurseManagement()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.title"));
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.count", entry.curseCount)
                    .withStyle(s -> s.withColor(0xAAAAAA)));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.click_hint")
                    .withStyle(s -> s.withColor(0x55FF55)));
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.drag_hint")
                    .withStyle(s -> s.withColor(0x8888FF)));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }

        if (entry.isMainHeader()) {
            String key = entry.sourceType == TechniqueHelper.TechniqueSource.STOLEN
                    ? "gui.jujutsu_addon.technique_source.stolen"
                    : "gui.jujutsu_addon.technique_source.copied";
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            return;
        }

        if ((entry.isSubHeader() || entry.isThirdHeader()) && entry.technique != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(entry.technique.getName());
            tooltip.add(Component.translatable(entry.isActive
                            ? "gui.jujutsu_addon.tooltip.technique_active"
                            : "gui.jujutsu_addon.tooltip.technique_inactive")
                    .withStyle(s -> s.withColor(entry.isActive ? 0x55FF55 : 0xAAAAAA)));
            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }

        if (entry.isAbility() && entry.ability != null && entry.status != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(entry.ability.getName());
            AbilityStatus status = entry.status;

            if (status.isTenShadowsSummon) {
                if (status.isDead) {
                    tooltip.add(Component.translatable("gui.jujutsu_addon.tooltip.shikigami_dead")
                            .withStyle(s -> s.withColor(0xFF4444)));
                } else if (status.conditionsNotMet) {  // ★ 条件未满足
                    tooltip.add(Component.translatable("gui.jujutsu_addon.tooltip.shikigami_conditions_not_met")
                            .withStyle(s -> s.withColor(0xFFAA00)));
                } else if (status.isTamed) {
                    tooltip.add(Component.translatable("gui.jujutsu_addon.tooltip.shikigami_tamed")
                            .withStyle(s -> s.withColor(0x44FF44)));
                } else {
                    tooltip.add(Component.translatable("gui.jujutsu_addon.tooltip.shikigami_untamed")
                            .withStyle(s -> s.withColor(0xFFFF44)));
                }
            }
            if (status.techniqueNotActive) {
                tooltip.add(Component.translatable("gui.jujutsu_addon.tooltip.technique_not_active")
                        .withStyle(s -> s.withColor(0xFF6666)));
            }

            graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    public boolean handleScroll(double mouseX, double mouseY, double delta) {
        if (isInBounds((int) mouseX, (int) mouseY)) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            return true;
        }
        return false;
    }

    @Nullable
    public Ability getAbilityAt(int mouseX, int mouseY) {
        if (mouseX < x || mouseX >= x + width - 28 || mouseY < y || mouseY >= y + height) return null;
        int index = scrollOffset + (mouseY - y - 2) / EntryRenderer.ITEM_HEIGHT;
        if (index >= 0 && index < entries.size()) {
            AbilityEntry entry = entries.get(index);
            if (entry.isAbility()) return entry.ability;
        }
        return null;
    }
}