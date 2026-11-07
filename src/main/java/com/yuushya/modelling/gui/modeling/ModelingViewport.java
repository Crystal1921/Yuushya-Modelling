package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 建模界面的 3D 视口：实心背景 + 可自由环绕的相机。
 * <p>
 * 用「物品形态 + {@code ItemRenderer.renderStatic}」绘制：{@code NeoShowBlockModel.getRenderPasses}
 * 会从 {@code BLOCK_ENTITY_DATA} 重建模型，几何、渲染类型与光照都由物品渲染器处理。
 * 世界管线的 {@code renderSingleBlock} 在 GUI 阶段没有配套状态，画不出来。
 */
public final class ModelingViewport {

    private static final int BACKGROUND = 0xFF151515;

    private final ModelingCamera camera;

    private ItemStack stack;
    private long cachedSignature = Long.MIN_VALUE;

    public ModelingViewport(ModelingCamera camera) {
        this.camera = camera;
    }

    /** 数据变化时丢弃物品形态缓存，让几何随之刷新。 */
    public void invalidate() {
        this.cachedSignature = Long.MIN_VALUE;
    }

    /** 最近一次构建的物品形态，供 gizmo 用同一份模型取显示变换。 */
    public ItemStack currentStack() {
        return this.stack == null ? ItemStack.EMPTY : this.stack;
    }

    public void render(GuiGraphics guiGraphics, ModelingLayout.Rect rect, ShowBlockEntity blockEntity, List<TransformBlockData> data) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), BACKGROUND);

        ItemStack itemStack = stackFor(blockEntity, data);
        if (itemStack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = blockEntity.getLevel() != null ? blockEntity.getLevel() : minecraft.level;

        guiGraphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
        try {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            // 与 ModelingCamera 共用同一份矩阵，gizmo 才能严丝合缝地对齐
            poseStack.mulPose(this.camera.baseMatrix(rect));

            minecraft.getItemRenderer().renderStatic(itemStack, ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    poseStack, guiGraphics.bufferSource(), level, 0);
            poseStack.popPose();
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /** 当前方块实体的物品形态；变换数据未变时复用同一实例以命中模型缓存。 */
    private ItemStack stackFor(ShowBlockEntity blockEntity, List<TransformBlockData> data) {
        long signature = signature(blockEntity, data);
        if (this.stack != null && signature == this.cachedSignature) {
            return this.stack;
        }
        Level level = blockEntity.getLevel() != null ? blockEntity.getLevel() : Minecraft.getInstance().level;
        if (level == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = new CompoundTag();
        ITransformDataInventory.saveAdditional(tag, data, level.registryAccess());

        ItemStack itemStack = new ItemStack(ItemRegistry.SHOW_BLOCK.get());
        itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        this.stack = itemStack;
        this.cachedSignature = signature;
        return itemStack;
    }

    /** 变换数据的轻量指纹，用于判断是否需要重建物品形态。 */
    private static long signature(ShowBlockEntity blockEntity, List<TransformBlockData> data) {
        long hash = blockEntity.getSlot() * 31L;
        for (TransformBlockData entry : data) {
            hash = hash * 31L + entry.blockState.hashCode();
            hash = hash * 31L + Double.hashCode(entry.pos.x + entry.pos.y * 7 + entry.pos.z * 13);
            hash = hash * 31L + Float.hashCode(entry.rot.x() + entry.rot.y() * 7 + entry.rot.z() * 13);
            hash = hash * 31L + Float.hashCode(entry.scales.x() + entry.scales.y() * 7 + entry.scales.z() * 13);
            hash = hash * 31L + (entry.isShown ? 1 : 0);
        }
        return hash;
    }
}
