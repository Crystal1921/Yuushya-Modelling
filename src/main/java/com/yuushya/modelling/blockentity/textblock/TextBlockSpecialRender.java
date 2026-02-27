package com.yuushya.modelling.blockentity.textblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.transformData.ITransformTextDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.client.SimpleGeneratedModel;
import com.yuushya.modelling.registries.BlockRegistry;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.rotate;
import static com.yuushya.modelling.blockentity.textblock.TextBlockEntityRender.scale;
import static com.yuushya.modelling.client.FontRenderUtil.drawStringUnified;

public class TextBlockSpecialRender extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    protected final BakedModel backup = new SimpleGeneratedModel(getTexture(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "item/abandon_textblock")));;
    protected final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public TextBlockSpecialRender(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
        this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void renderByItem(@NotNull ItemStack itemStack, @NotNull ItemDisplayContext transformType, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource multiBufferSource, int combinedLightIn, int combinedOverlayIn) {
        CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel clientLevel = mc.level;
        int light = LightTexture.FULL_BRIGHT;
        Font font = mc.font;
        if (clientLevel == null) {
            return;
        }
        if (data == CustomData.EMPTY) {
            mc.getBlockRenderer().getModelRenderer().renderModel(matrixStack.last(), multiBufferSource.getBuffer(RenderType.CUTOUT), BlockRegistry.TEXT_BLOCK.get().defaultBlockState(), backup, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            return;
        }

        List<TransformTextData> transformDatas = new ArrayList<>();
        ITransformTextDataInventory.load(data.copyTag(), transformDatas);

        for (TransformTextData transformData : transformDatas) {

            List<String> textLines = transformData.textLines;
            MutableComponent mutableComponent = Component.empty();

            textLines.forEach(line -> {
                MutableComponent lineComponent = Component.Serializer.fromJson(line, clientLevel.registryAccess());
                if (lineComponent != null) {
                    mutableComponent.append(lineComponent);
                }
            });
            matrixStack.pushPose();

            matrixStack.translate(0.0D, 1.0D, 0.0D);

            scale(matrixStack, transformData.scales);
            YuushyaUtils.translate(matrixStack, transformData.pos);
            rotate(matrixStack, transformData.rot);

            matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            matrixStack.scale(0.1F, 0.1F, 0.1F);
            Matrix4f matrix4f = matrixStack.last().pose();

            boolean isCulled = transformData.isCulled;
            boolean isMirror = transformData.isMirror;

            if (!isCulled && !isMirror) {
                drawStringUnified(font, mutableComponent.getVisualOrderText(), 0, 0, -1, false, matrix4f, multiBufferSource, 0, light);
            }

            if (!isCulled && isMirror) {
                font.drawInBatch(mutableComponent, 0, 0, -1, false, matrix4f, multiBufferSource, Font.DisplayMode.NORMAL, 0, light);
                matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                matrixStack.translate(-font.width(mutableComponent), 0.0D, 0.0D);
                Matrix4f matrix4fMirror = matrixStack.last().pose();
                font.drawInBatch(mutableComponent, 0, 0, -1, false, matrix4fMirror, multiBufferSource, Font.DisplayMode.NORMAL, 0, light);
            }

            if (isCulled) {
                font.drawInBatch(mutableComponent, 0, 0, -1, false, matrix4f, multiBufferSource, Font.DisplayMode.NORMAL, 0, light);
            }


            matrixStack.popPose();
        }
    }

    public static TextureAtlasSprite getTexture(ResourceLocation resource) {
        return Minecraft.getInstance().getTextureAtlas(BLOCK_ATLAS).apply(resource);
    }
}
