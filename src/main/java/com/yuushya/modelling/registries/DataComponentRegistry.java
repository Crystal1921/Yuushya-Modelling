package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;

public class DataComponentRegistry {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Yuushya.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TRANS_DIRECTION = DATA_COMPONENT_TYPES.registerComponentType(
            "trans",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockState>> BLOCKSTATE = DATA_COMPONENT_TYPES.registerComponentType(
            "blockstate",
            builder -> builder.persistent(BlockState.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> TRANSFORM_DATA = DATA_COMPONENT_TYPES.registerComponentType(
            "transfrom_data", // 保留了原代码中的拼写 "transfrom_data"
            builder -> builder.persistent(CustomData.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COLOR_DATA = DATA_COMPONENT_TYPES.registerComponentType(
            "color_data",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static void register(IEventBus bus) {
        DATA_COMPONENT_TYPES.register(bus);
    }
}