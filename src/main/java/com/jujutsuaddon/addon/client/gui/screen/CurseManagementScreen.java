package com.jujutsuaddon.addon.client.gui.screen;

import com.jujutsuaddon.addon.capability.AddonCurseBaselineData;
import com.jujutsuaddon.addon.capability.CurseListMonitor;
import com.jujutsuaddon.addon.client.util.UIScaleHelper;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.SummonAbsorbedCurseC2SPacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.util.*;

public class CurseManagementScreen extends Screen {

    // ==================== 分组咒灵数据结构 ====================

    /**
     * 分组键 - 基于 EntityType + 完整 data
     */
    private static class CurseGroupKey {
        final EntityType<?> entityType;
        final int dataHash;

        CurseGroupKey(EntityType<?> type, @Nullable CompoundTag data) {
            this.entityType = type;
            this.dataHash = (data != null && !data.isEmpty())
                    ? data.toString().hashCode()
                    : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CurseGroupKey that = (CurseGroupKey) o;
            return dataHash == that.dataHash &&
                    Objects.equals(entityType, that.entityType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityType, dataHash);
        }
    }

    /**
     * 分组后的咒灵条目
     */
    private static class GroupedCurse {
        final EntityType<?> entityType;
        final Component displayName;
        final List<Integer> originalIndices;
        final int dataHash;      // 该分组的 data hash
        boolean isVariant;       // ★ 是否是变体（非基准）- 非 final
        GroupedCurse(EntityType<?> type, Component name, int dataHash) {
            this.entityType = type;
            this.displayName = name;
            this.originalIndices = new ArrayList<>();
            this.dataHash = dataHash;
            this.isVariant = false;
        }
        int getCount() {
            return originalIndices.size();
        }
        int getFirstIndex() {
            return originalIndices.isEmpty() ? -1 : originalIndices.get(0);
        }
    }

    // 布局参数
    private int columns, rows, itemsPerPage;
    private int cellSize, cellPadding;
    private int gridStartX, gridStartY;
    private int gridWidth, gridHeight;

    // 状态
    private int currentPage = 0;
    private int totalPages = 1;
    private List<AbsorbedCurse> rawCurses = new ArrayList<>();
    private List<GroupedCurse> groupedCurses = new ArrayList<>();
    private int hoveredIndex = -1;
    private int uniqueTypeCount = 0;

    // 实体缓存
    private final Map<EntityType<?>, Entity> entityCache = new HashMap<>();

    public CurseManagementScreen() {
        super(Component.translatable("gui.jujutsu_addon.curse_management.title"));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        refreshCurseList();
        createButtons();
    }

    private void calculateLayout() {
        int[] gridConfig = UIScaleHelper.calculateCurseGridLayout(this.width, this.height);
        columns = gridConfig[0];
        rows = gridConfig[1];
        cellSize = gridConfig[2];
        cellPadding = gridConfig[3];
        itemsPerPage = columns * rows;

        gridWidth = columns * cellSize + (columns - 1) * cellPadding;
        gridHeight = rows * cellSize + (rows - 1) * cellPadding;

        gridStartX = (this.width - gridWidth) / 2;
        gridStartY = 45;
    }

    private void createButtons() {
        int contentBottom = gridStartY + gridHeight;
        int[] btnLayout = UIScaleHelper.calculateButtonLayout(
                this.width, this.height, contentBottom, 8);

        int buttonY = btnLayout[0];
        int btnHeight = btnLayout[1];
        int btnSmall = btnLayout[2];
        int btnMed = btnLayout[3];
        int btnLarge = btnLayout[4];

        int pageNavWidth = btnSmall * 2 + 50;
        int pageNavX = (this.width - pageNavWidth) / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("◀"), btn -> prevPage()
        ).bounds(pageNavX, buttonY, btnSmall, btnHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("▶"), btn -> nextPage()
        ).bounds(pageNavX + pageNavWidth - btnSmall, buttonY, btnSmall, btnHeight).build());

        int closeY = buttonY + btnHeight + 2;
        if (closeY + btnHeight > this.height - 3) {
            closeY = this.height - btnHeight - 3;
        }
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), btn -> this.onClose()
        ).bounds((this.width - btnMed) / 2, closeY, btnMed, btnHeight).build());

        int summonY = closeY + btnHeight + 2;
        if (summonY + btnHeight <= this.height - 2) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.jujutsu_addon.curse_management.summon_all"),
                    btn -> summonAll()
            ).bounds((this.width - btnLarge) / 2, summonY, btnLarge, btnHeight).build());
        }
    }

    private void refreshCurseList() {
        rawCurses.clear();
        groupedCurses.clear();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return;

        AddonCurseBaselineData baselineData = player.getCapability(AddonCurseBaselineData.CAPABILITY).orElse(null);
        rawCurses.addAll(sorcererData.getCurses());

        // 按 EntityType + data 分组
        Map<CurseGroupKey, GroupedCurse> groupMap = new LinkedHashMap<>();

        for (int i = 0; i < rawCurses.size(); i++) {
            AbsorbedCurse curse = rawCurses.get(i);
            EntityType<?> type = curse.getType();
            CompoundTag nbt = curse.getData();
            int dataHash = CurseListMonitor.computeDataHash(nbt);

            CurseGroupKey key = new CurseGroupKey(type, nbt);
            GroupedCurse group = groupMap.computeIfAbsent(key,
                    k -> new GroupedCurse(type, curse.getName(), dataHash));
            group.originalIndices.add(i);

            if (baselineData != null && baselineData.isVariant(type, dataHash)) {
                group.isVariant = true;
            }
        }

        groupedCurses.addAll(groupMap.values());

        // ★ 计算不同 EntityType 的数量
        uniqueTypeCount = (int) groupedCurses.stream()
                .map(g -> g.entityType)
                .distinct()
                .count();

        // 排序：按类型 -> 基准优先 -> 数量
        groupedCurses.sort((a, b) -> {
            String typeA = EntityType.getKey(a.entityType).toString();
            String typeB = EntityType.getKey(b.entityType).toString();
            int typeCompare = typeA.compareTo(typeB);
            if (typeCompare != 0) return typeCompare;

            if (a.isVariant != b.isVariant) {
                return a.isVariant ? 1 : -1;
            }

            return Integer.compare(b.getCount(), a.getCount());
        });

        totalPages = Math.max(1, (groupedCurses.size() + itemsPerPage - 1) / itemsPerPage);
        currentPage = Math.min(currentPage, totalPages - 1);
    }

    private void nextPage() {
        if (currentPage < totalPages - 1) currentPage++;
    }

    private void prevPage() {
        if (currentPage > 0) currentPage--;
    }

    private void summonAll() {
        AddonNetwork.sendToServer(new SummonAbsorbedCurseC2SPacket(-1));
        this.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (Minecraft.getInstance().player != null &&
                Minecraft.getInstance().player.tickCount % 20 == 0) {
            refreshCurseList();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        // ★ 使用 uniqueTypeCount 而不是 groupedCurses.size()
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.jujutsu_addon.curse_management.count_grouped",
                        rawCurses.size(), uniqueTypeCount),
                this.width / 2, 25, 0xAAAAAA);

        int bgPadding = Math.max(3, cellPadding);
        graphics.fill(gridStartX - bgPadding, gridStartY - bgPadding,
                gridStartX + gridWidth + bgPadding, gridStartY + gridHeight + bgPadding, 0xCC222222);
        graphics.renderOutline(gridStartX - bgPadding, gridStartY - bgPadding,
                gridWidth + bgPadding * 2, gridHeight + bgPadding * 2, 0xFF555555);

        hoveredIndex = -1;
        int startIndex = currentPage * itemsPerPage;

        for (int i = 0; i < itemsPerPage; i++) {
            int groupIndex = startIndex + i;
            int col = i % columns;
            int row = i / columns;
            int cellX = gridStartX + col * (cellSize + cellPadding);
            int cellY = gridStartY + row * (cellSize + cellPadding);

            boolean isHovered = mouseX >= cellX && mouseX < cellX + cellSize &&
                    mouseY >= cellY && mouseY < cellY + cellSize;

            if (isHovered) hoveredIndex = groupIndex;

            if (groupIndex < groupedCurses.size()) {
                renderGroupedCurseCell(graphics, groupedCurses.get(groupIndex), cellX, cellY, isHovered);
            } else {
                graphics.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, 0x40333333);
                graphics.renderOutline(cellX, cellY, cellSize, cellSize, 0xFF444444);
            }
        }

        int pageY = gridStartY + gridHeight + 6;
        graphics.drawCenteredString(this.font,
                (currentPage + 1) + " / " + totalPages, this.width / 2, pageY, 0xFFFF00);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (hoveredIndex >= 0 && hoveredIndex < groupedCurses.size()) {
            renderGroupedCurseTooltip(graphics, groupedCurses.get(hoveredIndex), mouseX, mouseY);
        }
    }

    private void renderGroupedCurseCell(GuiGraphics graphics, GroupedCurse group,
                                        int x, int y, boolean isHovered) {
        int count = group.getCount();
        boolean hasData = group.isVariant;

        // 背景颜色
        int bgColor;
        if (isHovered) {
            bgColor = 0xDD445566;
        } else if (hasData) {
            bgColor = 0xDD3A4455;  // 有数据的用青色调
        } else if (count >= 5) {
            bgColor = 0xDD443366;
        } else if (count >= 3) {
            bgColor = 0xDD3A3A5A;
        } else {
            bgColor = 0xDD333344;
        }
        graphics.fill(x, y, x + cellSize, y + cellSize, bgColor);

        // 边框颜色
        int borderColor;
        if (isHovered) {
            borderColor = 0xFF66AAFF;
        } else if (hasData) {
            borderColor = 0xFF55AAAA;  // 有数据的用青色边框
        } else if (count >= 5) {
            borderColor = 0xFFAA66FF;
        } else if (count >= 3) {
            borderColor = 0xFF8888CC;
        } else {
            borderColor = 0xFF666699;
        }
        graphics.renderOutline(x, y, cellSize, cellSize, borderColor);

        // 渲染实体
        renderEntityInCell(graphics, group.entityType, x, y, cellSize, isHovered);

        // 名称
        String entityName = group.displayName.getString();
        int maxNameWidth = cellSize - 4;
        if (this.font.width(entityName) > maxNameWidth) {
            entityName = this.font.plainSubstrByWidth(entityName, maxNameWidth - 6) + "..";
        }
        graphics.drawCenteredString(this.font, entityName, x + cellSize / 2, y + cellSize - 10, 0xCCCCCC);

        // 数量徽章
        if (count > 1) {
            renderCountBadge(graphics, x, y, cellSize, count);
        }

        // ★★★ 有数据就显示星号（和基准不同）★★★
        if (hasData) {
            graphics.drawString(this.font, "★", x + 2, y + 2, 0x55FFFF, true);
        }
    }

    private void renderCountBadge(GuiGraphics graphics, int cellX, int cellY, int cellSize, int count) {
        String countText = count >= 99 ? "99+" : String.valueOf(count);
        int textWidth = this.font.width(countText);

        int badgePadding = 2;
        int badgeWidth = textWidth + badgePadding * 2;
        int badgeHeight = 10;
        int badgeX = cellX + cellSize - badgeWidth - 2;
        int badgeY = cellY + 2;

        int badgeBgColor;
        int badgeTextColor;
        if (count >= 10) {
            badgeBgColor = 0xEEFF4444;
            badgeTextColor = 0xFFFFFF;
        } else if (count >= 5) {
            badgeBgColor = 0xEEFFAA00;
            badgeTextColor = 0x000000;
        } else {
            badgeBgColor = 0xEE44AA44;
            badgeTextColor = 0xFFFFFF;
        }

        graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, badgeBgColor);
        graphics.renderOutline(badgeX, badgeY, badgeWidth, badgeHeight, 0xFF000000);
        graphics.drawString(this.font, countText, badgeX + badgePadding, badgeY + 1, badgeTextColor, false);
    }

    private void renderEntityInCell(GuiGraphics graphics, EntityType<?> entityType,
                                    int x, int y, int size, boolean isHovered) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        Entity entity = entityCache.computeIfAbsent(entityType, type -> {
            try {
                return type.create(this.minecraft.level);
            } catch (Exception e) {
                return null;
            }
        });

        if (entity == null) {
            String name = EntityType.getKey(entityType).getPath();
            String abbrev = getAbbreviation(name, 3);
            graphics.fill(x + 4, y + 4, x + size - 4, y + size - 14, 0xFF222233);
            graphics.drawCenteredString(this.font, abbrev, x + size / 2, y + size / 2 - 8, 0xAA66FF);
            return;
        }

        float entityHeight = entity.getBbHeight();
        float entityWidth = entity.getBbWidth();
        float maxDim = Math.max(entityHeight, entityWidth);

        int entityScale = (int) Math.max(2, Math.min(size / 5.0f, (size * 0.35f) / maxDim));
        int centerX = x + size / 2;
        int centerY = y + size / 2 + (int)(entityHeight * entityScale / 3);

        float angleX = isHovered ? 0.3f : 0.0f;
        renderEntityInInventory(graphics.pose(), centerX, centerY, entityScale, angleX, -0.2f, entity);
    }

    private static void renderEntityInInventory(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                int x, int y, int scale,
                                                float angleX, float angleY, Entity entity) {
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float) Math.PI);
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(angleY * 20.0F * ((float) Math.PI / 180.0F));
        quaternionf.mul(quaternionf1);

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = 180.0F + angleX * 20.0F;
        }
        entity.setYRot(180.0F + angleX * 40.0F);
        entity.setXRot(-angleY * 20.0F);

        if (entity instanceof LivingEntity living) {
            living.yHeadRot = entity.getYRot();
            living.yHeadRotO = entity.getYRot();
        }

        com.mojang.blaze3d.vertex.PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(0.0D, 0.0D, 1000.0D);
        RenderSystem.applyModelViewMatrix();

        poseStack.pushPose();
        poseStack.translate(x, y, -950.0D);
        poseStack.mulPoseMatrix((new Matrix4f()).scaling((float) scale, (float) scale, (float) (-scale)));
        poseStack.mulPose(quaternionf);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternionf1.conjugate();
        dispatcher.overrideCameraOrientation(quaternionf1);
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.runAsFancy(() ->
                dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, buffer, 15728880));

        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        poseStack.popPose();
        Lighting.setupFor3DItems();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private String getAbbreviation(String name, int maxLength) {
        if (name == null || name.isEmpty()) return "?";
        name = name.replace("cursed_", "").replace("curse_", "");
        String[] parts = name.split("_");
        StringBuilder abbrev = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && abbrev.length() < maxLength) {
                abbrev.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return abbrev.length() > 0 ? abbrev.toString() :
                name.substring(0, Math.min(maxLength, name.length())).toUpperCase();
    }

    private void renderGroupedCurseTooltip(GuiGraphics graphics, GroupedCurse group, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        // 名称
        tooltip.add(group.displayName.copy().withStyle(s -> s.withColor(0xAA66FF)));
        // 类型
        tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.type",
                EntityType.getKey(group.entityType).toString()).withStyle(s -> s.withColor(0x888888)));
        // 数量
        int count = group.getCount();
        if (count > 1) {
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.count_same", count)
                    .withStyle(s -> s.withColor(0xFFAA00)));
        }
        // ★★★ 显示经验和召唤消耗 ★★★
        int firstIndex = group.getFirstIndex();
        if (!rawCurses.isEmpty() && firstIndex >= 0 && firstIndex < rawCurses.size()) {
            AbsorbedCurse curse = rawCurses.get(firstIndex);
            // 经验值
            float experience = getCurseExperience(curse);
            if (experience > 0) {
                tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.experience",
                        String.format("%.0f", experience)).withStyle(s -> s.withColor(0x55FF55)));
            }
            // 单个召唤消耗
            float cost = JJKAbilities.getCurseCost(curse);
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.summon_cost",
                    String.format("%.1f", cost)).withStyle(s -> s.withColor(0x55AAFF)));
        }
        // 变体标记
        if (group.isVariant) {
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.has_data")
                    .withStyle(s -> s.withColor(0x55FFFF)));
        }
        tooltip.add(Component.empty());
        // 操作提示
        tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.click_to_summon_one")
                .withStyle(s -> s.withColor(0x55FF55)));
        if (count > 1) {
            // 计算该组所有咒灵的总消耗
            float totalCost = 0;
            for (int idx : group.originalIndices) {
                if (idx >= 0 && idx < rawCurses.size()) {
                    totalCost += JJKAbilities.getCurseCost(rawCurses.get(idx));
                }
            }
            tooltip.add(Component.translatable("gui.jujutsu_addon.curse_management.shift_click_to_summon_all_cost",
                    count, String.format("%.1f", totalCost)).withStyle(s -> s.withColor(0x55AAFF)));
        }
        graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < groupedCurses.size()) {
            GroupedCurse group = groupedCurses.get(hoveredIndex);

            if (hasShiftDown() && group.getCount() > 1) {
                List<Integer> indices = new ArrayList<>(group.originalIndices);
                Collections.reverse(indices);
                for (int index : indices) {
                    AddonNetwork.sendToServer(new SummonAbsorbedCurseC2SPacket(index));
                }
            } else {
                int index = group.getFirstIndex();
                if (index >= 0) {
                    AddonNetwork.sendToServer(new SummonAbsorbedCurseC2SPacket(index));
                }
            }

            refreshCurseList();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) prevPage();
        else if (delta < 0) nextPage();
        return true;
    }

    @Override
    public void onClose() {
        entityCache.clear();
        super.onClose();
    }

    /**
     * 从 AbsorbedCurse 的 NBT 数据中读取经验值
     */
    private float getCurseExperience(AbsorbedCurse curse) {
        CompoundTag data = curse.getData();
        if (data == null || data.isEmpty()) {
            return 0;
        }
        // 原版 sorcerer data 中经验的 key
        if (data.contains("experience")) {
            return data.getFloat("experience");
        }
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
