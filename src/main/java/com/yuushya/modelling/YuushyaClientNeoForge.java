package com.yuushya.modelling;

import com.yuushya.modelling.client.NeoItemBlockModel;
import com.yuushya.modelling.client.NeoShowBlockModel;
import com.yuushya.modelling.gui.widget.ColorTexture;
import com.yuushya.modelling.registries.BlockRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.YuushyaDataTags;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = Yuushya.MOD_ID_USED)
public class YuushyaClientNeoForge {

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ColorTexture colorTexture = new ColorTexture();
        });
    }

    @SubscribeEvent
    public static void onModelBaked(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation inventory = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "showblock"), "inventory");
        event.getModels().put(inventory, new NeoShowBlockModel(Direction.SOUTH, event.getModels().get(inventory)));
        for (BlockState blockState : BlockRegistry.SHOW_BLOCK.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation stateResourceLocation = BlockModelShaper.stateToModelLocation(blockState);
            event.getModels().put(stateResourceLocation, new NeoShowBlockModel(blockState.getValue(HORIZONTAL_FACING), event.getModels().get(stateResourceLocation)));
        }

        ModelResourceLocation inventory2 = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "itemblock"), "inventory");
        event.getModels().put(inventory2, new NeoItemBlockModel(Direction.SOUTH, event.getModels().get(inventory2)));
        for (BlockState blockState : BlockRegistry.ITEM_BLOCK.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation stateResourceLocation = BlockModelShaper.stateToModelLocation(blockState);
            event.getModels().put(stateResourceLocation, new NeoItemBlockModel(blockState.getValue(HORIZONTAL_FACING), event.getModels().get(stateResourceLocation)));
        }
    }

    /**
     * getColor是对面片执行的，所以只需要知道这个面片事实上来自哪个方块就能知道颜色
     * 而且原版方块的tintIndex的值除了-1之外似乎设为多少都无所谓
     * 那么可以在生成面片时存储其来自的blockState
     * Block类刚好可以将方块状态和id互相转换
     * 前24位为原方块的blockState，后8位为原方块的tint（若其为正）
     */
    @SubscribeEvent
    public void handleBlockColor(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, view, pos, tintIndex) -> {
                    if (tintIndex > -1) {
                        // decodeTintWithState
                        // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
                        BlockState trueState = Block.stateById(tintIndex >> 8);
                        int trueTint = tintIndex & 0xFF;
                        return event.getBlockColors().getColor(trueState, view, pos, trueTint);
                    } else {
                        return 0xFFFFFFFF;
                    }
                },
                BlockRegistry.SHOW_BLOCK.get()
        );
    }

    @SubscribeEvent
    public void handleItemColor(RegisterColorHandlersEvent.Item event) {
        event.register(
                (itemStack, i) -> {
                    BlockState blockState = YuushyaDataTags.getBlockState(itemStack);
                    if (blockState == null) {
                        blockState = Blocks.AIR.defaultBlockState();
                    }
                    return event.getBlockColors().getColor(blockState, null, null, i);
                }, ItemRegistry.GET_BLOCKSTATE_ITEM.get()
        );
        event.register(
                (arg, tintIndex) -> {
                    if (tintIndex > -1) {
                        // decodeTintWithState
                        // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
                        BlockState trueState = Block.stateById(tintIndex >> 8);
                        int trueTint = tintIndex & 0xFF;
                        return event.getBlockColors().getColor(trueState, null, null, trueTint);
                    } else {
                        return 0xFFFFFFFF;
                    }
                }, ItemRegistry.SHOW_BLOCK.get()
        );
    }

}
