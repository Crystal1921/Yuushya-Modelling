package com.yuushya.modelling.gui.modeling;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuushya.modelling.client.TintedBufferSource;
import com.yuushya.modelling.gui.modeling.target.ModelingTarget;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

/**
 * 建模界面的 3D 视口：实心背景 + 可自由环绕的相机。
 * <p>
 * 直接读取方块实体里的元素数据，<b>逐元素</b>套变换矩阵后绘制内容，不再把整份数据
 * 塞进 {@code ItemStack} 走物品模型通道。好处是每帧都是最新数据、不需要
 * {@code BakedModel} 缓存、也不会再掉进「3 参数 {@code getQuads} 只返回备份方块」的坑。
 * <p>
 * 变换链与 {@code ShowBlockModel} / {@code NeoItemBlockModel} 完全一致：
 * <pre>
 *   YuushyaUtils.scale   → T(.5)·S·T(-.5)
 *   YuushyaUtils.translate → T(pos/16)
 *   YuushyaUtils.rotate  → T(.5)·R·T(-.5)
 * </pre>
 * 顶点仍是 0..1 的方块局部坐标，因此相机矩阵末端的 {@code translate(-0.5)} 负责居中，
 * 与 gizmo 共用同一份矩阵。
 * <p>
 * GUI 阶段是正交投影，矩阵变换后的坐标直接就是像素。
 */
public final class ModelingViewport {

    public static final int BACKGROUND = 0xFF151515;

    /** 与 {@code ItemRenderer.renderModelLists} 一致的固定种子，避免面片在帧间抖动。 */
    private static final long QUAD_SEED = 42L;

    private final ModelingCamera camera;

    public ModelingViewport(ModelingCamera camera) {
        this.camera = camera;
    }

    public void render(GuiGraphics guiGraphics, ModelingLayout.Rect rect, ModelingTarget target) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), -100, BACKGROUND);

        List<ModelingTarget.ElementView> views = target.views();
        if (views.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();

        guiGraphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
        try {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            // 与 ModelingCamera 共用同一份矩阵，gizmo 才能严丝合缝地对齐
            poseStack.mulPose(this.camera.baseMatrix(rect));

            for (ModelingTarget.ElementView view : views) {
                if (!view.renderable()) {
                    continue;
                }
                poseStack.pushPose();
                applyElementTransform(poseStack, view);
                if (view.state() != null) {
                    renderBlock(minecraft, guiGraphics, poseStack, view.state(), view.stack(), view.color());
                } else {
                    renderItem(minecraft, guiGraphics, poseStack, view.stack(), view.color());
                }
                poseStack.popPose();
            }

            poseStack.popPose();
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /** 元素自身的变换，顺序与 {@code ShowBlockModel#getQuads} 一致。 */
    private static void applyElementTransform(PoseStack poseStack, ModelingTarget.ElementView view) {
        YuushyaUtils.scale(poseStack, view.transform().getScale());
        YuushyaUtils.translate(poseStack, view.transform().getPosition());
        YuushyaUtils.rotate(poseStack, view.transform().getRotation());
    }

    // ---- 方块内容 ----

    /**
     * 画方块。方块模型的面片按方向分组，{@code side == null} 只返回"无剔除"那一部分，
     * 所以必须把六个方向和 {@code null} 都取一遍。
     * <p>
     * 声明了自定义渲染的方块（{@code isCustomRenderer()}）没有面片，几何由
     * {@code BlockEntityRenderer} 生成，这时退回物品渲染器，让它走
     * {@code renderByItem}；ItemBlock 元素能给出原始物品栈，就优先用它，保住物品上的组件。
     *
     * @param stack 元素的原始物品栈，只在上面的自定义渲染回退里用到
     * @param color 元素染色（ARGB），{@code -1} 表示不染色
     */
    private static void renderBlock(Minecraft minecraft, GuiGraphics guiGraphics,
                                    PoseStack poseStack, BlockState state, ItemStack stack, int color) {
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        if (model.isCustomRenderer()) {
            ItemStack fallback = stack.isEmpty() ? blockStack(state) : stack;
            if (!fallback.isEmpty()) {
                renderItem(minecraft, guiGraphics, poseStack, fallback, color);
            }
            return;
        }

        RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create(QUAD_SEED);
        float mulR = FastColor.ARGB32.red(color) / 255.0F;
        float mulG = FastColor.ARGB32.green(color) / 255.0F;
        float mulB = FastColor.ARGB32.blue(color) / 255.0F;

        // 直接用手上的真实方块状态问色，不需要 ShowBlockModel 那套"编码进 tintIndex"的手法
        for (Direction side : Direction.values()) {
            submit(minecraft, consumer, pose, state, model.getQuads(state, side, random), mulR, mulG, mulB);
        }
        submit(minecraft, consumer, pose, state, model.getQuads(state, null, random), mulR, mulG, mulB);
    }

    /** 方块状态 → 携带该状态的物品栈（自定义渲染器只认物品形态）。 */
    private static ItemStack blockStack(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (!state.getProperties().isEmpty()) {
            BlockItemStateProperties props = BlockItemStateProperties.EMPTY;
            for (Property<?> property : state.getProperties()) {
                props = props.with(property, state);
            }
            stack.set(DataComponents.BLOCK_STATE, props);
        }
        return stack;
    }

    private static void submit(Minecraft minecraft, VertexConsumer consumer, PoseStack.Pose pose,
                               BlockState state, List<BakedQuad> quads,
                               float mulR, float mulG, float mulB) {
        for (BakedQuad quad : quads) {
            int argb = -1;
            if (quad.isTinted()) {
                argb = minecraft.getBlockColors().getColor(state, null, null, quad.getTintIndex());
            }
            put(consumer, pose, quad, argb, mulR, mulG, mulB);
        }
    }

    // ---- 物品内容 ----

    /**
     * 画物品。物品模型可能是多通道的（{@code getRenderPasses}），每个通道自己声明
     * 需要哪些 {@code RenderType}，逐个通道提交即可。
     * <p>
     * 声明了自定义渲染的物品（箱子、潜影盒、旗帜、三叉戟 …）没有面片，几何由
     * {@code BlockEntityWithoutLevelRenderer} 生成，必须交给
     * {@link net.minecraft.client.renderer.entity.ItemRenderer#render}。
     */
    private static void renderItem(Minecraft minecraft, GuiGraphics guiGraphics, PoseStack poseStack,
                                   ItemStack stack, int color) {
        BakedModel model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
        if (model.isCustomRenderer()) {
            renderByItemRenderer(minecraft, guiGraphics, poseStack, stack, model, color);
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create(QUAD_SEED);

        float mulR = FastColor.ARGB32.red(color) / 255.0F;
        float mulG = FastColor.ARGB32.green(color) / 255.0F;
        float mulB = FastColor.ARGB32.blue(color) / 255.0F;

        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(renderType);
                random.setSeed(QUAD_SEED);
                for (Direction side : Direction.values()) {
                    submitItem(minecraft, consumer, pose, stack,
                            pass.getQuads(null, side, random), mulR, mulG, mulB);
                }
                random.setSeed(QUAD_SEED);
                submitItem(minecraft, consumer, pose, stack,
                        pass.getQuads(null, null, random), mulR, mulG, mulB);
            }
        }
    }

    /**
     * 交给原版物品渲染器处理自定义模型（无面片，几何由 {@code BEWLR} 生成）。
     * <p>
     * {@code ItemRenderer.render} 内部会执行 {@code translate(-0.5)}，而我们的元素变换
     * 与相机矩阵已经让内容占据 0..1 的方块空间，所以这里先 {@code translate(+0.5)}
     * 抵消掉，避免整体偏移半个方块。
     * <p>
     * 用 {@link ItemDisplayContext#NONE}：它对应恒等显示变换，位置完全由我们的矩阵决定。
     * <p>
     * 顶点颜色由 {@code BEWLR} 自己写，没有「传一个颜色参数」的入口，所以元素染色靠
     * {@link com.yuushya.modelling.client.TintedBufferSource} 包一层缓冲区，
     * 把每个顶点颜色乘上元素颜色。
     */
    private static void renderByItemRenderer(Minecraft minecraft, GuiGraphics guiGraphics,
                                             PoseStack poseStack, ItemStack stack, BakedModel model,
                                             int color) {
        MultiBufferSource buffers = TintedBufferSource.of(guiGraphics.bufferSource(), color);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poseStack,
                buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        poseStack.popPose();
    }

    private static void submitItem(Minecraft minecraft, VertexConsumer consumer, PoseStack.Pose pose,
                                   ItemStack stack, List<BakedQuad> quads,
                                   float mulR, float mulG, float mulB) {
        for (BakedQuad quad : quads) {
            int argb = -1;
            if (quad.isTinted()) {
                argb = minecraft.getItemColors().getColor(stack, quad.getTintIndex());
            }
            put(consumer, pose, quad, argb, mulR, mulG, mulB);
        }
    }

    // ---- 提交 ----

    /**
     * 提交单个面片。
     *
     * @param argb 已有颜色（{@code -1} 视作不透明白色），一般来自方块 / 物品染色器
     * @param mulR 额外的颜色乘数（元素染色），无染色时传 1
     */
    private static void put(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                            int argb, float mulR, float mulG, float mulB) {
        float alpha = FastColor.ARGB32.alpha(argb) / 255.0F;
        float red = FastColor.ARGB32.red(argb) / 255.0F * mulR;
        float green = FastColor.ARGB32.green(argb) / 255.0F * mulG;
        float blue = FastColor.ARGB32.blue(argb) / 255.0F * mulB;
        consumer.putBulkData(pose, quad, red, green, blue, alpha,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
