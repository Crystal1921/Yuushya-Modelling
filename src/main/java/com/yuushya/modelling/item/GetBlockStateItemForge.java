package com.yuushya.modelling.item;

import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;

public class GetBlockStateItemForge extends GetBlockStateItem {
//    public static final IClientItemExtensions ITEM_EXTENSIONS = FMLEnvironment.getDist() == Dist.CLIENT ? new IClientItemExtensions() {
//        @Override
//        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
//            return new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
//
//                @Override
//                public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
//                    GetBlockStateItem.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
//                }
//
//            };
//        }
//    } : null;


    //TODO : 这里要新model，getCustomRenderer已被移除
    public GetBlockStateItemForge(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }
}
