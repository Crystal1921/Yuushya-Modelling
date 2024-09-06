package com.yuushya.modelling.fabric;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.YuushyaClient;
import com.yuushya.modelling.fabric.client.ShowBlockModel;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ExtraModelProvider;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

import static com.yuushya.modelling.registries.YuushyaRegistries.BLOCKS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class YuushyaClientFabric  implements ClientModInitializer {
    private static final ModelResourceLocation SHOWBLOCK_ITEM_MODEL_RESOURCE_LOCATION = new ModelResourceLocation(new ResourceLocation(Yuushya.MOD_ID,"showblock"),"inventory");
    private static final ResourceLocation SHOWBLOCK_ITEM_BACKUP = new ResourceLocation(Yuushya.MOD_ID,"item/showblock");
    @Override
    public void onInitializeClient() {
        YuushyaClient.onInitializeClient();
        ModelLoadingRegistry.INSTANCE.registerModelProvider(new ExtraModelProvider() {
            @Override
            public void provideExtraModels(ResourceManager manager, Consumer<ResourceLocation> out) {
                out.accept(SHOWBLOCK_ITEM_BACKUP);
            }
        });
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(
                (resourceManager) -> (modelResourceLocation, modelProviderContext) -> {
                    if(modelResourceLocation.equals(SHOWBLOCK_ITEM_MODEL_RESOURCE_LOCATION)){
                        return new ShowBlockModel(Direction.SOUTH,SHOWBLOCK_ITEM_BACKUP);
                    }
                    for(BlockState blockState: BLOCKS.get("showblock").get().getStateDefinition().getPossibleStates()){
                        if (modelResourceLocation.equals(BlockModelShaper.stateToModelLocation(blockState))) {
                            return new ShowBlockModel(blockState.getValue(HORIZONTAL_FACING));
                        }
                    }
                    return null;
                }
        );
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
            CompoundTag compoundTag = itemStack.getOrCreateTag();
            BlockState blockState = NbtUtils.readBlockState(compoundTag.getCompound("BlockState"));
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
