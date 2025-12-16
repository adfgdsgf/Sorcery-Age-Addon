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
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SkillBarManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JujutsuAddon.MODID);

    private static final SkillBarData data = new SkillBarData();
    private static Path saveFile;
    private static String currentWorldId;

    private static final Map<Ability, Integer> initialCooldowns = new HashMap<>();

    // ★★★ 领域粘性机制 ★★★
    private static final Map<Integer, Long> domainActiveUntil = new HashMap<>();
    private static final long DOMAIN_STICKY_DURATION = 1000; // 1秒粘性

    public static void init() {
    }

    public static void onPlayerLogin(LocalPlayer player) {
        if (player == null) return;
        cleanup();
        Minecraft.getInstance().execute(() -> {
            currentWorldId = generateWorldId(player);
            saveFile = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config")
                    .resolve("jujutsu_addon")
                    .resolve("skillbar_" + currentWorldId + ".txt");
            load();
        });
    }

    private static String generateWorldId(LocalPlayer player) {
        UUID uuid = player.getUUID();
        String serverPart;
        Minecraft mc = Minecraft.getInstance();
        ServerData serverData = mc.getCurrentServer();

        if (serverData != null) {
            serverPart = "mp_" + hashString(serverData.ip);
        } else if (mc.getSingleplayerServer() != null) {
            try {
                Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                String folderName = worldPath.getFileName().toString();
                if (folderName.isEmpty() || folderName.equals(".")) {
                    folderName = worldPath.toAbsolutePath().toString();
                }
                serverPart = "sp_" + hashString(folderName);
            } catch (Exception e) {
                long seed = mc.getSingleplayerServer().getWorldData().worldGenOptions().seed();
                serverPart = "sp_seed_" + Long.toHexString(seed);
            }
        } else {
            String dimKey = player.level().dimension().location().toString();
            serverPart = "dim_" + hashString(dimKey + System.currentTimeMillis());
        }
        return uuid.toString().substring(0, 8) + "_" + serverPart;
    }

    private static String hashString(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
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

    private static void cleanup() {
        currentWorldId = null;
        saveFile = null;
        data.clear();
        initialCooldowns.clear();
        ClientEvents.resetState();
        domainActiveUntil.clear();
    }

    public static boolean isInitialized() {
        return currentWorldId != null && saveFile != null;
    }

    public static SkillBarData getData() {
        return data;
    }

    // ===== 预设管理 =====

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

    public static boolean hasCurseManipulation() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        CursedTechnique nativeTech = sorcererData.getTechnique();
        if (nativeTech == CursedTechnique.CURSE_MANIPULATION) return true;
        CursedTechnique currentStolen = sorcererData.getCurrentStolen();
        if (currentStolen == CursedTechnique.CURSE_MANIPULATION) return true;
        CursedTechnique currentCopied = sorcererData.getCurrentCopied();
        if (currentCopied == CursedTechnique.CURSE_MANIPULATION) return true;
        if (currentStolen != null && TechniqueHelper.isCopyTechnique(currentStolen)) {
            if (currentCopied == CursedTechnique.CURSE_MANIPULATION) return true;
        }
        if (currentCopied != null && TechniqueHelper.isStealTechnique(currentCopied)) {
            if (currentStolen == CursedTechnique.CURSE_MANIPULATION) return true;
        }
        return false;
    }

    public static boolean ownsCurseManipulation() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        CursedTechnique nativeTech = sorcererData.getTechnique();
        if (nativeTech == CursedTechnique.CURSE_MANIPULATION) return true;
        if (sorcererData.hasStolen(CursedTechnique.CURSE_MANIPULATION)) return true;
        if (sorcererData.getCopied().contains(CursedTechnique.CURSE_MANIPULATION)) return true;
        return false;
    }

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

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ 激活状态检测（带粘性保护）★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

    public static boolean isSlotActive(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;

        Ability ability = data.getSlot(slot);
        if (ability == null) return false;

        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        // 1. 按键按住
        if (ClientEvents.isSlotHeld(slot)) {
            return true;
        }

        // 2. 领域展开（带粘性保护）
        if (ability instanceof DomainExpansion) {
            boolean hasToggled = sorcererData.hasToggled(ability);

            long now = System.currentTimeMillis();

            if (hasToggled) {
                // 激活中，持续刷新粘性时间
                domainActiveUntil.put(slot, now + DOMAIN_STICKY_DURATION);
                return true;
            }

            // hasToggled = false，检查粘性保护
            Long activeUntil = domainActiveUntil.get(slot);
            if (activeUntil != null) {
                if (now < activeUntil) {
                    return true; // 还在粘性期内
                }
                domainActiveUntil.remove(slot);
            }
            return false;
        }

        // 3. 普通 toggled 技能
        if (sorcererData.hasToggled(ability)) {
            return true;
        }

        // 4. 召唤物
        if (ability instanceof Summon<?> summon) {
            return sorcererData.hasSummonOfClass(summon.getClazz());
        }

        return false;
    }

    public static boolean isSummonAbility(int slot) {
        Ability ability = data.getSlot(slot);
        return ability instanceof Summon<?>;
    }

    // ===== 存档相关 =====

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
