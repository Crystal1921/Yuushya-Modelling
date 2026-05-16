package com.yuushya.modelling.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.yuushya.modelling.registries.DataComponentRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record PosTransConditional() implements SelectItemModelProperty<String> {

    public static final MapCodec<PosTransConditional> MAP_CODEC = MapCodec.unit(new PosTransConditional());
    public static final Type<PosTransConditional, String> TYPE = Type.create(
            MAP_CODEC,
            Codec.STRING
    );

    @Override
    public @NonNull String get(@NonNull ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext) {
        Integer orDefault = itemStack.getOrDefault(DataComponentRegistry.TRANS_DIRECTION, -1);
        return String.valueOf(orDefault);
    }

    @Override
    public @NonNull Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public @NonNull Type<? extends SelectItemModelProperty<String>, String> type() {
        return TYPE;
    }
}
