package com.yuushya.modelling.event;

import com.yuushya.modelling.blockentity.showblock.ShowBlockModel;
import com.yuushya.modelling.blockentity.textblock.TextModelSpecialRenderer;
import com.yuushya.modelling.client.renderer.GetBlockStateItemSpecialRenderer;
import com.yuushya.modelling.registries.BlockRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT)
public class ModelEvent {
    @SubscribeEvent
    public static void onModelBaked(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> blockStateBlockStateModelMap = event.getBakingResult().blockStateModels();
        for (BlockState possibleState : BlockRegistry.SHOW_BLOCK.get().getStateDefinition().getPossibleStates()) {
            blockStateBlockStateModelMap.put(possibleState, new ShowBlockModel());
        }
    }

    @SubscribeEvent
    public static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(TextModelSpecialRenderer.TEXT_MODEL_RENDERER, TextModelSpecialRenderer.Unbaked.MAP_CODEC);
        event.register(GetBlockStateItemSpecialRenderer.GET_BLOCK_STATEITEM_MODEL_RENDERER, GetBlockStateItemSpecialRenderer.Unbaked.MAP_CODEC);
    }

    /**
     * getColor是对面片执行的，所以只需要知道这个面片事实上来自哪个方块就能知道颜色
     * 而且原版方块的tintIndex的值除了-1之外似乎设为多少都无所谓
     * 那么可以在生成面片时存储其来自的blockState
     * Block类刚好可以将方块状态和id互相转换
     * 前24位为原方块的blockState，后8位为原方块的tint（若其为正）
     */
    @SubscribeEvent
    public static void handleBlockColor(RegisterColorHandlersEvent.BlockTintSources event) {
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

    @SubscribeEvent
    public static void handleItemColor(RegisterColorHandlersEvent.ItemTintSources event) {
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
