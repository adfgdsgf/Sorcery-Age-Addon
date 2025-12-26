package com.jujutsuaddon.addon.capability.vow;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 誓约数据 Capability Provider
 */
public class VowDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final ResourceLocation IDENTIFIER =
            new ResourceLocation("jujutsu_addon", "vow_data");

    public static final Capability<IVowData> VOW_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private VowData data = null;
    private final LazyOptional<IVowData> optional = LazyOptional.of(this::createData);

    private VowData createData() {
        if (data == null) {
            data = new VowData();
        }
        return data;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == VOW_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createData().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createData().deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }

    // ==================== 事件注册 ====================

    @Mod.EventBusSubscriber(modid = "jujutsu_addon")
    public static class EventHandler {

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof LivingEntity) {
                // 只给玩家附加，还是给所有LivingEntity？
                // 目前给所有LivingEntity，因为NPC也可能有誓约
                event.addCapability(IDENTIFIER, new VowDataProvider());
            }
        }
    }

    // ==================== Capability 注册 ====================

    @Mod.EventBusSubscriber(modid = "jujutsu_addon", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IVowData.class);
        }
    }
}
