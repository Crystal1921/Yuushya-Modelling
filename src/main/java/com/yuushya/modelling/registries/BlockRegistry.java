package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Yuushya.MOD_ID);

    public static final DeferredBlock<Block> SHOW_BLOCK = BLOCKS.register("showblock", () -> new ShowBlock(BlockBehaviour.Properties.of().noOcclusion().forceSolidOn().strength(4.0f).lightLevel(blockState -> blockState.getValue(YuushyaBlockStates.LIT)), 0));
    public static final DeferredBlock<Block> ITEM_BLOCK = BLOCKS.register("itemblock", () -> new ItemBlock(BlockBehaviour.Properties.of().noOcclusion().forceSolidOn().strength(4.0f).lightLevel(blockState -> blockState.getValue(YuushyaBlockStates.LIT)), 1));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}