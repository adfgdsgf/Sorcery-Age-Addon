package com.jujutsuaddon.addon.capability.core;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class GenericCapabilityProvider<T extends INBTSerializable<CompoundTag>> implements ICapabilitySerializable<CompoundTag> {
    private final T instance;
    private final Capability<T> capability;
    private final LazyOptional<T> optional;

    public GenericCapabilityProvider(Capability<T> capability, Supplier<T> factory) {
        this.capability = capability;
        this.instance = factory.get();
        this.optional = LazyOptional.of(() -> instance);
    }

    @NotNull
    @Override
    public <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
        return capability.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
