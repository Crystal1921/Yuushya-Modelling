package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.itemblock.ItemBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.textblock.TextBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Yuushya.MOD_ID);

    public static final RegistryObject<Block> SHOW_BLOCK = BLOCKS.register("showblock", () -> new ShowBlock(BlockBehaviour.Properties.of().noOcclusion().forceSolidOn().strength(4.0f).lightLevel(blockState -> blockState.getValue(YuushyaBlockStates.LIT)), 0));
    public static final RegistryObject<Block> ITEM_BLOCK = BLOCKS.register("itemblock", () -> new ItemBlock(BlockBehaviour.Properties.of().noOcclusion().forceSolidOn().strength(4.0f).lightLevel(blockState -> blockState.getValue(YuushyaBlockStates.LIT)), 1));
    public static final RegistryObject<Block> TEXT_BLOCK = BLOCKS.register("textblock", () -> new TextBlock(BlockBehaviour.Properties.of().noOcclusion().forceSolidOn().strength(4.0f).lightLevel(blockState -> blockState.getValue(YuushyaBlockStates.LIT)), 1));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}