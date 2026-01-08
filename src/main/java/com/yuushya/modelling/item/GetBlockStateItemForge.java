package com.yuushya.modelling.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.textblock.TextBlockSpecialRender;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class GetBlockStateItemForge extends GetBlockStateItem {
    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){

                @Override
                public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
                    GetBlockStateItem.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
                }

            };
        }
    };
    public GetBlockStateItemForge(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }
    @SuppressWarnings("removal")
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ITEM_EXTENSIONS);
    }
}
