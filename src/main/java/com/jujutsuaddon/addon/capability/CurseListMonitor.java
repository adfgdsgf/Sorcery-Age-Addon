package com.jujutsuaddon.addon.capability;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.CurseBaselineSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.*;

/**
 * 咒灵列表监听器
 *
 * 定期检查玩家咒灵列表，为新类型记录基准 data hash
 */
@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseListMonitor {

    private static final Map<UUID, Set<Integer>> playerCurseSnapshots = new HashMap<>();
    private static final int CHECK_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.tickCount % CHECK_INTERVAL != 0) return;

        checkPlayerCurses(serverPlayer);
    }

    private static void checkPlayerCurses(ServerPlayer player) {
        ISorcererData sorcererData = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return;

        List<AbsorbedCurse> curses = sorcererData.getCurses();
        if (curses.isEmpty()) {
            playerCurseSnapshots.remove(player.getUUID());
            return;
        }

        AddonCurseBaselineData baselineData = player.getCapability(AddonCurseBaselineData.CAPABILITY).orElse(null);
        if (baselineData == null) return;

        Set<Integer> currentHashes = new HashSet<>();
        List<AbsorbedCurse> newCurses = new ArrayList<>();
        Set<Integer> previousSnapshot = playerCurseSnapshots.getOrDefault(player.getUUID(), Collections.emptySet());

        for (AbsorbedCurse curse : curses) {
            int curseHash = computeCurseIdentityHash(curse);
            currentHashes.add(curseHash);

            if (!previousSnapshot.contains(curseHash)) {
                newCurses.add(curse);
            }
        }

        playerCurseSnapshots.put(player.getUUID(), currentHashes);

        if (!newCurses.isEmpty()) {
            boolean baselineChanged = processNewCurses(baselineData, newCurses);
            if (baselineChanged) {
                syncToClient(player, baselineData);
            }
        }
    }

    private static boolean processNewCurses(AddonCurseBaselineData baselineData, List<AbsorbedCurse> newCurses) {
        boolean changed = false;
        for (AbsorbedCurse curse : newCurses) {
            EntityType<?> type = curse.getType();
            int dataHash = computeDataHash(curse.getData());
            if (baselineData.recordBaselineIfAbsent(type, dataHash)) {
                changed = true;
            }
        }
        return changed;
    }

    private static void syncToClient(ServerPlayer player, AddonCurseBaselineData data) {
        AddonNetwork.sendToPlayer(new CurseBaselineSyncS2CPacket(data.save()), player);
    }

    private static int computeCurseIdentityHash(AbsorbedCurse curse) {
        return Objects.hash(
                EntityType.getKey(curse.getType()).toString(),
                curse.getName().getString(),
                computeDataHash(curse.getData())
        );
    }

    public static int computeDataHash(CompoundTag data) {
        return (data != null && !data.isEmpty()) ? data.toString().hashCode() : 0;
    }

    public static void onPlayerLogout(UUID playerId) {
        playerCurseSnapshots.remove(playerId);
    }
}
