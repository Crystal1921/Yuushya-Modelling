package com.yuushya.modelling.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.renderstate.GetBlockStateItemRenderState;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class GetBlockStateItemSpecialRenderer implements SpecialModelRenderer<GetBlockStateItemRenderState> {
    public static final Identifier GET_BLOCK_STATEITEM_MODEL_RENDERER = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "get_blockstate_item");
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    @Override
    public void submit(@Nullable GetBlockStateItemRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (renderState != null) {
            if (renderState.isEmpty) {
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.5D, 0.5D);
                renderState.defaultItemRenderState.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
                poseStack.popPose();
            } else {
                renderState.carriedBlock.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
            }
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {

    }

    @Override
    public GetBlockStateItemRenderState extractArgument(ItemStack itemStack) {
        Minecraft mc = Minecraft.getInstance();
        BlockModelResolver blockModelResolver = mc.getBlockModelResolver();
        ItemModelResolver itemModelResolver = mc.getItemModelResolver();
        ClientLevel level = mc.level;
        BlockState blockState = itemStack.getOrDefault(DataComponentRegistry.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
        GetBlockStateItemRenderState renderState = new GetBlockStateItemRenderState();
        blockModelResolver.update(renderState.carriedBlock, blockState, BLOCK_DISPLAY_CONTEXT);
        boolean isEmpty = blockState.isEmpty();
        if (isEmpty) {
            itemModelResolver.updateForTopItem(renderState.defaultItemRenderState, ItemRegistry.GET_SHOWBLOCK_ITEM.toStack(), ItemDisplayContext.NONE, level, null, 943);
        }
        renderState.isEmpty = isEmpty;
        return renderState;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<GetBlockStateItemRenderState> {
        public static final MapCodec<GetBlockStateItemSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new GetBlockStateItemSpecialRenderer.Unbaked());

        @Override
        public @NonNull SpecialModelRenderer<GetBlockStateItemRenderState> bake(@NonNull BakingContext bakingContext) {
            return new GetBlockStateItemSpecialRenderer();
        }

        @Override
        public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<GetBlockStateItemRenderState>> type() {
            return MAP_CODEC;
        }
    }
}
