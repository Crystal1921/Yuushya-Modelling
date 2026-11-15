package com.yuushya.modelling.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;

/**
 * 给「没有颜色参数可传」的渲染输出乘上元素染色。
 * <p>
 * {@code ItemRenderer#render}（自定义渲染器 / BEWLR）这类入口只收一个
 * {@link MultiBufferSource}，颜色由渲染器自己写进顶点，外部无从干预。这里包一层缓冲区，
 * 在 {@link VertexConsumer#setColor(int, int, int, int)} 处乘上染色，其余调用原样转发。
 * <p>
 * 默认实现里 {@code putBulkData} 最终也会走到 {@code setColor}，所以整条路径都能着色。
 */
public final class TintedBufferSource implements MultiBufferSource {

    private final MultiBufferSource delegate;
    private final float mulR;
    private final float mulG;
    private final float mulB;

    private TintedBufferSource(MultiBufferSource delegate, float mulR, float mulG, float mulB) {
        this.delegate = delegate;
        this.mulR = mulR;
        this.mulG = mulG;
        this.mulB = mulB;
    }

    /**
     * @param argb 元素染色（ARGB）；纯白（含 {@code -1}）表示不染色，此时直接返回原缓冲区
     */
    public static MultiBufferSource of(MultiBufferSource delegate, int argb) {
        int red = FastColor.ARGB32.red(argb);
        int green = FastColor.ARGB32.green(argb);
        int blue = FastColor.ARGB32.blue(argb);
        if (red == 255 && green == 255 && blue == 255) {
            return delegate;
        }
        return new TintedBufferSource(delegate, red / 255.0F, green / 255.0F, blue / 255.0F);
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new TintedVertexConsumer(this.delegate.getBuffer(renderType), this.mulR, this.mulG, this.mulB);
    }

    /** 只改顶点颜色，其余原样转发。 */
    private record TintedVertexConsumer(VertexConsumer delegate, float mulR, float mulG,
                                        float mulB) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor((int) (red * this.mulR), (int) (green * this.mulG),
                    (int) (blue * this.mulB), alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
    }
}
