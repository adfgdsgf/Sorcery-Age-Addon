package com.jujutsuaddon.addon.client.skillbar;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.ClientEvents;
import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import com.jujutsuaddon.addon.util.helper.TenShadowsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public class SkillBarManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JujutsuAddon.MODID);

    private static final SkillBarData data = new SkillBarData();
    private static Path saveFile;
    private static String currentWorldId;

    private static final Map<Ability, Integer> initialCooldowns = new HashMap<>();

    public static void init() {
    }

    public static void onPlayerLogin(LocalPlayer player) {
        if (player == null) return;

        // ★★★ 关键：先清理旧数据 ★★★
        cleanup();

        // 延迟执行，确保服务器数据已加载
        Minecraft.getInstance().execute(() -> {
            currentWorldId = generateWorldId(player);

            saveFile = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config")
                    .resolve("jujutsu_addon")
                    .resolve("skillbar_" + currentWorldId + ".txt");


            load();
        });
    }

    /**
     * ★★★ 生成世界唯一标识（使用文件夹路径而非显示名称）★★★
     */
    private static String generateWorldId(LocalPlayer player) {
        UUID uuid = player.getUUID();
        String serverPart;

        Minecraft mc = Minecraft.getInstance();
        ServerData serverData = mc.getCurrentServer();

        if (serverData != null) {
            // 多人服务器：使用服务器 IP
            serverPart = "mp_" + hashString(serverData.ip);
        } else if (mc.getSingleplayerServer() != null) {
            // ★★★ 单人存档：使用存档文件夹路径 ★★★
            try {
                Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                String folderName = worldPath.getFileName().toString();
                // 如果文件夹名也有问题，用完整路径
                if (folderName.isEmpty() || folderName.equals(".")) {
                    folderName = worldPath.toAbsolutePath().toString();
                }
                serverPart = "sp_" + hashString(folderName);
            } catch (Exception e) {
                // 回退：使用种子
                long seed = mc.getSingleplayerServer().getWorldData().worldGenOptions().seed();
                serverPart = "sp_seed_" + Long.toHexString(seed);
            }
        } else {
            // 回退：使用维度 + 时间戳
            String dimKey = player.level().dimension().location().toString();
            serverPart = "dim_" + hashString(dimKey + System.currentTimeMillis());
        }

        String worldId = uuid.toString().substring(0, 8) + "_" + serverPart;
        return worldId;
    }

    /**
     * 简单哈希（取前8位）
     */
    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));  // ★★★ 指定编码 ★★★
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            // 回退：简单字符求和
            int hash = 0;
            for (char c : input.toCharArray()) {
                hash = 31 * hash + c;
            }
            return Integer.toHexString(Math.abs(hash));
        }
    }

    public static void onPlayerLogout() {
        if (currentWorldId != null) {
            save();
        }
        cleanup();
    }

    /**
     * ★★★ 清理所有状态 ★★★
     */
    private static void cleanup() {
        currentWorldId = null;
        saveFile = null;
        data.clear();
        initialCooldowns.clear();
        ClientEvents.resetState();
    }

    public static boolean isInitialized() {
        return currentWorldId != null && saveFile != null;
    }

    public static SkillBarData getData() {
        return data;
    }

    // ===== 以下方法不变 =====

    public static int getCurrentPresetIndex() {
        return data.getCurrentPresetIndex();
    }

    public static void nextPreset() {
        data.nextPreset();
        save();
    }

    public static void prevPreset() {
        data.prevPreset();
        save();
    }

    public static void setPreset(int index) {
        data.setCurrentPreset(index);
        save();
    }

    public static void setSlot(int slot, @Nullable Ability ability) {
        data.setSlot(slot, ability);
        save();
    }

    @Nullable
    public static Ability getSlot(int slot) {
        return data.getSlot(slot);
    }

    public static void clearSlot(int slot) {
        data.clearSlot(slot);
        save();
    }

    public static void setSlotTechnique(int slot, @Nullable CursedTechnique technique) {
        data.setSlotTechnique(slot, technique);
        save();
    }

    public static boolean isSlotTechnique(int slot) {
        return data.isSlotTechnique(slot);
    }

    @Nullable
    public static CursedTechnique getSlotTechnique(int slot) {
        return data.getSlotTechnique(slot);
    }

    public static boolean playerHasTechnique(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;
        CursedTechnique technique = data.getSlotTechnique(slot);
        if (technique == null) return false;
        // ★★★ 使用工具类检查所有额外术式 ★★★
        return TechniqueHelper.hasExtraTechnique(player, technique);
    }

    public static boolean isTechniqueActive(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;

        CursedTechnique technique = data.getSlotTechnique(slot);
        if (technique == null) return false;

        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        return sorcererData.getCurrentCopied() == technique;
    }

    // ===== 咒灵管理槽位 =====
    public static void setSlotCurseManagement(int slot) {
        data.setSlotCurseManagement(slot);
        save();
    }
    public static boolean isSlotCurseManagement(int slot) {
        return data.isSlotCurseManagement(slot);
    }

    /**
     * 检查玩家是否拥有咒灵操术（原生/偷取/复制任一）
     */
    public static boolean hasCurseManipulation() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        CursedTechnique nativeTech = sorcererData.getTechnique();
        // ★★★ 情况1：原生咒灵操术 - 始终可用 ★★★
        if (nativeTech == CursedTechnique.CURSE_MANIPULATION) {
            return true;
        }
        // ★★★ 情况2：偷取的咒灵操术 - 必须是当前激活的 ★★★
        CursedTechnique currentStolen = sorcererData.getCurrentStolen();
        if (currentStolen == CursedTechnique.CURSE_MANIPULATION) {
            return true;
        }
        // ★★★ 情况3：复制的咒灵操术 - 必须是当前激活的 ★★★
        CursedTechnique currentCopied = sorcererData.getCurrentCopied();
        if (currentCopied == CursedTechnique.CURSE_MANIPULATION) {
            return true;
        }
        // ★★★ 情况4：通过偷取的复制能力获得的咒灵操术 ★★★
        // 如果偷取了复制术式，且当前激活的复制术式是咒灵操术
        if (currentStolen != null && TechniqueHelper.isCopyTechnique(currentStolen)) {
            if (currentCopied == CursedTechnique.CURSE_MANIPULATION) {
                return true;
            }
        }
        // ★★★ 情况5：通过复制的偷取能力获得的咒灵操术 ★★★
        // 如果复制了偷取术式，且当前激活的偷取术式是咒灵操术
        if (currentCopied != null && TechniqueHelper.isStealTechnique(currentCopied)) {
            if (currentStolen == CursedTechnique.CURSE_MANIPULATION) {
                return true;
            }
        }
        return false;
    }

    /**
     * ★★★ 新增：检查玩家是否拥有咒灵操术（不管是否激活）★★★
     * 用于在列表中显示入口（即使不可用也显示，但会标记为灰色）
     */
    public static boolean ownsCurseManipulation() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        CursedTechnique nativeTech = sorcererData.getTechnique();
        // 原生咒灵操术
        if (nativeTech == CursedTechnique.CURSE_MANIPULATION) return true;
        // 偷取的咒灵操术
        if (sorcererData.hasStolen(CursedTechnique.CURSE_MANIPULATION)) return true;
        // 复制的咒灵操术
        if (sorcererData.getCopied().contains(CursedTechnique.CURSE_MANIPULATION)) return true;
        return false;
    }
    /**
     * 检查咒灵管理槽位是否可用
     */
    public static boolean isCurseManagementAvailable(int slot) {
        if (!data.isSlotCurseManagement(slot)) return false;
        return hasCurseManipulation();
    }

    public static boolean playerHasAbility(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;

        Ability ability = data.getSlot(slot);
        if (ability == null) return false;

        List<Ability> playerAbilities = JJKAbilities.getAbilities(player);
        if (playerAbilities.contains(ability)) return true;

        if (TenShadowsHelper.isEnabled() && TenShadowsHelper.hasTenShadows(player)) {
            List<Ability> tenShadowsAbilities = TenShadowsHelper.getAllAvailableTenShadowsAbilities(player);
            if (tenShadowsAbilities.contains(ability)) return true;
        }

        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData != null) {
            for (CursedTechnique technique : sorcererData.getCopied()) {
                if (technique != null) {
                    for (Ability copiedAbility : technique.getAbilities()) {
                        if (copiedAbility == ability) return true;
                    }
                }
            }
        }

        return false;
    }

    public static int getCooldown(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return 0;

        Ability ability = data.getSlot(slot);
        if (ability == null) return 0;

        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return 0;

        int remaining = sorcererData.getRemainingCooldown(ability);

        if (remaining > 0) {
            int current = initialCooldowns.getOrDefault(ability, 0);
            if (remaining > current) {
                initialCooldowns.put(ability, remaining);
            }
        } else {
            initialCooldowns.remove(ability);
        }

        return remaining;
    }

    public static int getTotalCooldown(int slot) {
        Ability ability = data.getSlot(slot);
        if (ability == null) return 1;

        Integer initial = initialCooldowns.get(ability);
        if (initial != null && initial > 0) {
            return initial;
        }

        return 100;
    }

    public static boolean isSlotActive(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;
        Ability ability = data.getSlot(slot);
        if (ability == null) return false;
        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;
        if (ClientEvents.isSlotHeld(slot)) return true;
        if (sorcererData.hasToggled(ability)) return true;
        if (ability instanceof Summon<?> summon) {
            return sorcererData.hasSummonOfClass(summon.getClazz());
        }
        return false;
    }

    public static boolean isSummonAbility(int slot) {
        Ability ability = data.getSlot(slot);
        return ability instanceof Summon<?>;
    }

    public static void save() {
        if (saveFile == null) return;

        try {
            Files.createDirectories(saveFile.getParent());
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(saveFile))) {
                String[] serialized = data.serialize();
                for (String line : serialized) {
                    writer.println(line);
                }
            }

        } catch (IOException e) {

        }
    }

    public static void load() {
        if (saveFile == null || !Files.exists(saveFile)) {

            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(saveFile)) {
            String[] lines = reader.lines().toArray(String[]::new);
            data.deserialize(lines);

        } catch (IOException e) {

        }
    }
}
