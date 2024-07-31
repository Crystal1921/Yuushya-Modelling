package com.yuushya.modelling;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntityRender;
import com.yuushya.modelling.registries.YuushyaRegistries;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class YuushyaClient {
    public static void onInitializeClient(){
        RenderTypeRegistry.register(RenderType.cutout(), YuushyaRegistries.SHOW_BLOCK.get());
        BlockEntityRendererRegistry.register((BlockEntityType<ShowBlockEntity>) YuushyaRegistries.SHOW_BLOCK_ENTITY.get(), ShowBlockEntityRender::new);

        for (String s: List.of("rot_trans_item","pos_trans_item","micro_pos_trans_item","get_showblock_item"))
            ItemPropertiesRegistry.register(YuushyaRegistries.ITEMS.get(s).get(),ResourceLocation.parse("direction"),(itemStack, clientWorld, livingEntity, i) -> ((Integer)(itemStack.getOrDefault(YuushyaRegistries.TRANS_DIRECTION.get(),0)))*0.1F );
        ItemPropertiesRegistry.register(YuushyaRegistries.ITEMS.get("get_blockstate_item").get(),ResourceLocation.parse("direction"),(itemStack, clientWorld, livingEntity,i) -> {
            BlockState blockState = itemStack.getOrDefault((DataComponentType<BlockState>) YuushyaRegistries.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
            if(!blockState.equals(Blocks.AIR.defaultBlockState())) return 1;
            return 0;
        });


    }

}
