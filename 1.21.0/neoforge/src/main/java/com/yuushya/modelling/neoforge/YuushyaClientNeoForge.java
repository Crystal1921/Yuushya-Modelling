package com.yuushya.modelling.neoforge;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.YuushyaClient;
import com.yuushya.modelling.neoforge.client.ShowBlockModel;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;


@Mod(value = Yuushya.MOD_ID_USED, dist = Dist.CLIENT)
public class YuushyaClientNeoForge {
    public YuushyaClientNeoForge(IEventBus modBus){
        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(this::onInitializeClient);
            modBus.addListener(this::onModelBaked);
            modBus.addListener(this::handleBlockColor);
            modBus.addListener(this::handleItemColor);
        }
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        YuushyaClient.onInitializeClient();
    }

    public void onModelBaked(ModelEvent.ModifyBakingResult event){
        ModelResourceLocation inventory =new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID,"showblock"),"inventory");
        event.getModels().put(inventory, new ShowBlockModel(Direction.SOUTH,event.getModels().get(inventory)));
        for(BlockState blockState: YuushyaRegistries.BLOCKS.get("showblock").get().getStateDefinition().getPossibleStates()){
            ModelResourceLocation stateResourceLocation = BlockModelShaper.stateToModelLocation(blockState);
            event.getModels().put(stateResourceLocation,new ShowBlockModel(blockState.getValue(HORIZONTAL_FACING),event.getModels().get(stateResourceLocation)));
        }
    }
    /**
     * getColor是对面片执行的，所以只需要知道这个面片事实上来自哪个方块就能知道颜色
     * 而且原版方块的tintIndex的值除了-1之外似乎设为多少都无所谓
     * 那么可以在生成面片时存储其来自的blockState
     * Block类刚好可以将方块状态和id互相转换
     * 前24位为原方块的blockState，后8位为原方块的tint（若其为正）
     */
    public void handleBlockColor(RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register(
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
                YuushyaRegistries.SHOW_BLOCK.get()
        );
    }

    public void handleItemColor(RegisterColorHandlersEvent.Item event) {
        event.getItemColors().register(
                (itemStack, i) -> {
                    BlockState blockState = itemStack.getOrDefault((DataComponentType<BlockState>) YuushyaRegistries.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
                    return event.getBlockColors().getColor(blockState, null, null, i);
                },YuushyaRegistries.ITEMS.get("get_blockstate_item").get()
        );
        event.getItemColors().register(
                new ItemColor() {
                    @Override
                    public int getColor(ItemStack arg, int tintIndex) {
                        if (tintIndex > -1) {
                            // decodeTintWithState
                            // 假设原tint为负数，则最高位为1，通常可以返回空气（因为不太可能出现上千万的方块状态），那么空气也不会被染色
                            BlockState trueState = Block.stateById(tintIndex >> 8);
                            int trueTint = tintIndex & 0xFF;
                            return event.getBlockColors().getColor(trueState, null, null, trueTint);
                        } else {
                            return 0xFFFFFFFF;
                        }
                    }
                },YuushyaRegistries.ITEMS.get("showblock").get()
        );
    }

}
