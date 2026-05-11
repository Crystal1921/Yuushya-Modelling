package com.yuushya.modelling;

import com.yuushya.modelling.blockentity.showblock.ShowBlockModel;
import com.yuushya.modelling.registries.BlockRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.Map;

@Mod(value = Yuushya.MOD_ID_USED, dist = Dist.CLIENT)
public class YuushyaClientNeoForge {
    public YuushyaClientNeoForge(IEventBus modBus) {
        modBus.addListener(this::onInitializeClient);
        modBus.addListener(this::onModelBaked);
        modBus.addListener(this::handleBlockColor);
        modBus.addListener(this::handleItemColor);
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        //TODO 这里要改
//        event.enqueueWork(() -> {
//            ColorTexture colorTexture = new ColorTexture();
//        });
    }

    public void onModelBaked(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> blockStateBlockStateModelMap = event.getBakingResult().blockStateModels();
        for (BlockState possibleState : BlockRegistry.SHOW_BLOCK.get().getStateDefinition().getPossibleStates()) {
            blockStateBlockStateModelMap.put(possibleState, new ShowBlockModel());
        }

//        ModelIdentifier inventory = new ModelIdentifier(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "showblock"), "inventory");
//        event.getModels().put(inventory, new NeoShowBlockModel(Direction.SOUTH, event.getModels().get(inventory)));
//        for (BlockState blockState : BlockRegistry.SHOW_BLOCK.get().getStateDefinition().getPossibleStates()) {
//            ModelIdentifier stateIdentifier = BlockModelShaper.stateToModelLocation(blockState);
//            event.getModels().put(stateIdentifier, new NeoShowBlockModel(blockState.getValue(HORIZONTAL_FACING), event.getModels().get(stateIdentifier)));
//        }
//
//        ModelIdentifier inventory2 = new ModelIdentifier(Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "itemblock"), "inventory");
//        event.getModels().put(inventory2, new NeoItemBlockModel(Direction.SOUTH, event.getModels().get(inventory2)));
//        for (BlockState blockState : BlockRegistry.ITEM_BLOCK.get().getStateDefinition().getPossibleStates()) {
//            ModelIdentifier stateIdentifier = BlockModelShaper.stateToModelLocation(blockState);
//            event.getModels().put(stateIdentifier, new NeoItemBlockModel(blockState.getValue(HORIZONTAL_FACING), event.getModels().get(stateIdentifier)));
//        }
    }

    /**
     * getColor是对面片执行的，所以只需要知道这个面片事实上来自哪个方块就能知道颜色
     * 而且原版方块的tintIndex的值除了-1之外似乎设为多少都无所谓
     * 那么可以在生成面片时存储其来自的blockState
     * Block类刚好可以将方块状态和id互相转换
     * 前24位为原方块的blockState，后8位为原方块的tint（若其为正）
     */
    public void handleBlockColor(RegisterColorHandlersEvent.BlockTintSources event) {
        //TODO 这里不知道新版tintColor是怎么实现的，先注释掉了
//        event.register(
//                (state, view, pos, tintIndex) -> {
//                    if (tintIndex > -1) {
//                        // decodeTintWithState
//                        // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
//                        BlockState trueState = Block.stateById(tintIndex >> 8);
//                        int trueTint = tintIndex & 0xFF;
//                        return event.getBlockColors().getColor(trueState, view, pos, trueTint);
//                    } else {
//                        return 0xFFFFFFFF;
//                    }
//                },
//                BlockRegistry.SHOW_BLOCK.get()
//        );
    }

    public void handleItemColor(RegisterColorHandlersEvent.ItemTintSources event) {
        //TODO 这里不知道新版tintColor是怎么实现的，先注释掉了
//        event.register(
//                (itemStack, i) -> {
//                    BlockState blockState = itemStack.getOrDefault(DataComponentRegistry.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
//                    return event.getBlockColors().getColor(blockState, null, null, i);
//                }, ItemRegistry.GET_BLOCKSTATE_ITEM.get()
//        );
//        event.register(
//                (arg, tintIndex) -> {
//                    if (tintIndex > -1) {
//                        // decodeTintWithState
//                        // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
//                        BlockState trueState = Block.stateById(tintIndex >> 8);
//                        int trueTint = tintIndex & 0xFF;
//                        return event.getBlockColors().getColor(trueState, null, null, trueTint);
//                    } else {
//                        return 0xFFFFFFFF;
//                    }
//                }, ItemRegistry.SHOW_BLOCK.get()
//        );
    }

}
