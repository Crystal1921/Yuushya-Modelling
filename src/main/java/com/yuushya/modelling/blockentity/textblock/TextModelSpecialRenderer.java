package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.renderstate.TextBlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class TextModelSpecialRenderer implements SpecialModelRenderer<TextBlockEntityRenderState> {
    public static final Identifier TEXT_MODEL_RENDERER = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textblock");
    public static final Identifier TEXT_MODEL_TEXTURE = Identifier.fromNamespaceAndPath(Yuushya.MOD_ID, "textures/block/text_block.png");

    @Override
    public void submit(@Nullable TextBlockEntityRenderState textBlockEntityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int i1, boolean b, int i2) {
        if (textBlockEntityRenderState != null) {

        } else {
            submitNodeCollector.submitCustomGeometry(poseStack,
                    RenderTypes.entityCutoutCull(TEXT_MODEL_TEXTURE),
                    ((pose, vertexConsumer) -> {
                        translate(pose);
                        renderCube(pose, vertexConsumer);
                    }));
        }
    }

    private void translate(PoseStack.Pose pose) {
//        pose.scale(-1.0F, -1.0F, -1.0F);
//        pose.rotate(Axis.ZP.rotationDegrees(180F));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {

    }

    @Override
    public @Nullable TextBlockEntityRenderState extractArgument(ItemStack itemStack) {
        TypedEntityData<BlockEntityType<?>> blockEntityTypeTypedEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityTypeTypedEntityData != null) {
            CompoundTag compoundTag = blockEntityTypeTypedEntityData.copyTagWithoutId();
        }
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
    /**
     * 绘制一个四边形面（逆时针顺序）
     *
     * @param x0,     y0, z0 第一个顶点坐标
     * @param x1,     y1, z1 第二个顶点坐标
     * @param x2,     y2, z2 第三个顶点坐标
     * @param x3,     y3, z3 第四个顶点坐标
     * @param nx,     ny, nz 法线方向
     * @param pose    变换矩阵
     * @param builder 顶点消费者
     */
    private static void renderQuad(
            PoseStack.Pose pose,
            VertexConsumer builder,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz
    ) {
        addVertex(pose, builder, x0, y0, z0, nx, ny, nz, (float) 0.0, (float) 0.0);
        addVertex(pose, builder, x1, y1, z1, nx, ny, nz, (float) 1.0, (float) 0.0);
        addVertex(pose, builder, x2, y2, z2, nx, ny, nz, (float) 1.0, (float) 1.0);
        addVertex(pose, builder, x3, y3, z3, nx, ny, nz, (float) 0.0, (float) 1.0);
    }

    /**
     * 绘制一个完整的正方体（边长为1格）
     * @param pose 变换矩阵
     * @param builder 顶点消费者
     */
    public static void renderCube(PoseStack.Pose pose, VertexConsumer builder) {
        float x0 = 0.0F, y0 = 0.0F, z0 = 0.0F;
        float x1 = 1.0F, y1 = 1.0F, z1 = 1.0F;

        // 底面 (y=0, 法线向下)
        renderQuad(pose, builder, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0.0F, -1.0F, 0.0F);
        // 北面 (z=0, 法线向北)
        renderQuad(pose, builder, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0.0F, 0.0F, -1.0F);
        // 南面 (z=1, 法线向南)
        renderQuad(pose, builder, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0.0F, 0.0F, 1.0F);
        // 西面 (x=0, 法线向西)
        renderQuad(pose, builder, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1.0F, 0.0F, 0.0F);
        // 东面 (x=1, 法线向东)
        renderQuad(pose, builder, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1.0F, 0.0F, 0.0F);
        // 顶面 (y=1, 法线向上)
        renderQuad(pose, builder, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0.0F, 1.0F, 0.0F);
    }

    /**
     * 添加一个顶点
     * @param pose 变换矩阵
     * @param builder 顶点消费者
     * @param x, y, z 顶点坐标
     * @param nx, ny, nz 法线方向
     * @param u, v UV坐标
     */
    private static void addVertex(PoseStack.Pose pose, VertexConsumer builder, float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        builder.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Brightness.FULL_BRIGHT.pack())
                .setNormal(pose, nx, ny, nz);
    }
}
