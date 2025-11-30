package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Yuushya.MOD_ID);

    public static final Supplier<BlockEntityType<ShowBlockEntity>> SHOW_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("showblockentity", () -> BlockEntityType.Builder.of(ShowBlockEntity::new, BlockRegistry.SHOW_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<ItemBlockEntity>> ITEM_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("itemblockentity", () -> BlockEntityType.Builder.of(ItemBlockEntity::new, BlockRegistry.ITEM_BLOCK.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}