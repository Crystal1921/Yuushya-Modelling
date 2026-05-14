package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.renderstate.TextBlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class TextModelSpecialRenderer implements SpecialModelRenderer<TextBlockEntityRenderState> {
    public static final Identifier TEXT_MODEL_RENDERER = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textblock");

    @Override
    public void submit(@Nullable TextBlockEntityRenderState textBlockEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int i1, boolean b, int i2) {

    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {

    }

    @Override
    public @Nullable TextBlockEntityRenderState extractArgument(ItemStack itemStack) {
//        TypedEntityData<BlockEntityType<?>> blockEntityTypeTypedEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
//        CompoundTag compoundTag = blockEntityTypeTypedEntityData.copyTagWithoutId();
        return null;
    }

    public static record Unbaked() implements SpecialModelRenderer.Unbaked<TextBlockEntityRenderState> {
        public static final MapCodec<TextModelSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new TextModelSpecialRenderer.Unbaked());

        @Override
        public @NonNull SpecialModelRenderer<TextBlockEntityRenderState> bake(@NonNull BakingContext bakingContext) {
            return new TextModelSpecialRenderer();
        }

        @Override
        public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<TextBlockEntityRenderState>> type() {
            return MAP_CODEC;
        }
    }
}
