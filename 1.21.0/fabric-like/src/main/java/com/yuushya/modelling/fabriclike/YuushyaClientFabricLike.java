package com.yuushya.modelling.fabriclike;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.YuushyaClient;
import com.yuushya.modelling.fabriclike.client.ShowBlockModel;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.yuushya.modelling.registries.YuushyaRegistries.BLOCKS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class YuushyaClientFabricLike {
    private static final ModelResourceLocation SHOWBLOCK_ITEM_MODEL_RESOURCE_LOCATION = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"showblock"),"inventory");
    public static void onInitializeClient() {
        YuushyaClient.onInitializeClient();
        ModelLoadingPlugin.register((context)->{
            context.modifyModelAfterBake().register((model,modelBakeAfterContext)->{
                if(modelBakeAfterContext.topLevelId()!=null){
                    if(modelBakeAfterContext.topLevelId().equals(SHOWBLOCK_ITEM_MODEL_RESOURCE_LOCATION)){
                        return new ShowBlockModel(Direction.SOUTH,model);
                    }
                    for(BlockState blockState: YuushyaRegistries.BLOCKS.get("showblock").get().getStateDefinition().getPossibleStates())
                        if (modelBakeAfterContext.topLevelId().equals(BlockModelShaper.stateToModelLocation(blockState))) {
                            return new ShowBlockModel(blockState.getValue(HORIZONTAL_FACING),model);
                        }
                }
                return model;
            });
        });

        //BuiltinItemRendererRegistry.INSTANCE.register();
        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
                    if (tintIndex > -1) {
                        // decodeTintWithState
                        // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
                        BlockState trueState = Block.stateById(tintIndex >> 8);
                        int trueTint = tintIndex & 0xFF;
                        BlockColor blockColor = ColorProviderRegistry.BLOCK.get(trueState.getBlock());
                        if (blockColor == null) return 0xFFFFFFFF;
                        return blockColor.getColor(trueState, view, pos, trueTint);
                    } else {
                        return 0xFFFFFFFF;
                    }
                },
                YuushyaRegistries.SHOW_BLOCK.get());
        ColorProviderRegistry.ITEM.register((itemStack, i) -> {
            BlockState blockState = itemStack.getOrDefault((DataComponentType<BlockState>) YuushyaRegistries.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
            BlockColor blockColor = ColorProviderRegistry.BLOCK.get(blockState.getBlock());
            if (blockColor == null) return 0xFFFFFFFF;
            return blockColor.getColor(blockState, null, null, i);
        },YuushyaRegistries.ITEMS.get("get_blockstate_item").get());
        ColorProviderRegistry.ITEM.register((itemStack, tintIndex) -> {
            if (tintIndex > -1) {
                // decodeTintWithState
                // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
                BlockState trueState = Block.stateById(tintIndex >> 8);
                int trueTint = tintIndex & 0xFF;
                BlockColor blockColor = ColorProviderRegistry.BLOCK.get(trueState.getBlock());
                if (blockColor == null) return 0xFFFFFFFF;
                return blockColor.getColor(trueState, null, null, trueTint);
            } else {
                return 0xFFFFFFFF;
            }
        },YuushyaRegistries.ITEMS.get("showblock").get());


        BuiltinItemRendererRegistry.INSTANCE.register(YuushyaRegistries.ITEMS.get("get_blockstate_item").get(), GetBlockStateItem::renderByItem);

    }
}
