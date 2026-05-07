//package com.yuushya.modelling.client;
//
//import com.mojang.blaze3d.font.GlyphInfo;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import com.yuushya.modelling.mixinInterface.GlyphRenderTypesExt;
//import net.minecraft.client.gui.Font;
//import net.minecraft.client.gui.font.FontSet;
//import net.minecraft.client.gui.font.glyphs.BakedGlyph;
//import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.network.chat.Style;
//import net.minecraft.network.chat.TextColor;
//import net.minecraft.resources.Identifier;
//import net.minecraft.util.FormattedCharSequence;
//import net.minecraft.util.FormattedCharSink;
//import org.joml.Matrix4f;
//import org.joml.Vector3f;
//
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.List;
//
//public final class FontRenderUtil {
//    private static final Vector3f SHADOW_OFFSET = new Vector3f(0.0F, 0.0F, 0.03F);
//
//    private FontRenderUtil() {
//    }
//
//    private static int adjustColor(int color) {
//        return (color & -67108864) == 0 ? color | 0xFF000000 : color;
//    }
//
//    public static int drawStringUnified(
//            Font font,
//            FormattedCharSequence text,
//            float x,
//            float y,
//            int color,
//            boolean dropShadow,
//            Matrix4f pose,
//            MultiBufferSource buffer,
//            int backgroundColor,
//            int packedLight
//    ) {
//        // 若 adjustColor 是 private，请放开访问，或直接在此复制实现
//        color = adjustColor(color);
//
//        Matrix4f mainPose = new Matrix4f(pose);
//        if (dropShadow) {
//            renderTextOnce(font, text, x, y, color, true, pose, buffer, backgroundColor, packedLight);
//            // SHADOW_OFFSET: 原版为 (0.0F, 0.0F, 0.03F)
//            mainPose.translate(SHADOW_OFFSET);
//        }
//
//        float endX = renderTextOnce(font, text, x, y, color, false, mainPose, buffer, backgroundColor, packedLight);
//        return (int) endX + (dropShadow ? 1 : 0);
//    }
//
//    private static float renderTextOnce(
//            Font font,
//            FormattedCharSequence text,
//            float x,
//            float y,
//            int color,
//            boolean dropShadow,
//            Matrix4f pose,
//            MultiBufferSource buffer,
//            int backgroundColor,
//            int packedLight
//    ) {
//        StringRenderOutputUnified out = new StringRenderOutputUnified(
//                font, buffer, x, y, color, dropShadow, pose, packedLight
//        );
//        text.accept(out);
//        return out.finish(backgroundColor, x);
//    }
//
//    private static class StringRenderOutputUnified implements FormattedCharSink {
//        private final Font font;
//        private final MultiBufferSource bufferSource;
//        private final boolean dropShadow;
//        private final float dimFactor;
//        private final float r, g, b, a;
//        private final Matrix4f pose;
//        private final int packedLight;
//        float x, y;
//        @Nullable
//        private List<BakedGlyph.Effect> effects;
//
//        StringRenderOutputUnified(
//                Font font,
//                MultiBufferSource bufferSource,
//                float x,
//                float y,
//                int color,
//                boolean dropShadow,
//                Matrix4f pose,
//                int packedLight
//        ) {
//            this.font = font;
//            this.bufferSource = bufferSource;
//            this.x = x;
//            this.y = y;
//            this.dropShadow = dropShadow;
//            this.dimFactor = dropShadow ? 0.25F : 1.0F;
//            this.r = (float) (color >> 16 & 0xFF) / 255.0F * this.dimFactor;
//            this.g = (float) (color >> 8 & 0xFF) / 255.0F * this.dimFactor;
//            this.b = (float) (color & 0xFF) / 255.0F * this.dimFactor;
//            this.a = (float) (color >> 24 & 0xFF) / 255.0F;
//            this.pose = pose;
//            this.packedLight = packedLight;
//        }
//
//        private void addEffect(BakedGlyph.Effect effect) {
//            if (this.effects == null) this.effects = new ArrayList<>();
//            this.effects.add(effect);
//        }
//
//        @Override
//        public boolean accept(int positionInCurrentSequence, Style style, int codePoint) {
//            FontSet fontset = font.getFontSet(style.getFont());
//            GlyphInfo glyphinfo = fontset.getGlyphInfo(codePoint, font.filterFishyGlyphs);
//            BakedGlyph bakedglyph = style.isObfuscated() && codePoint != 32
//                    ? fontset.getRandomGlyph(glyphinfo)
//                    : fontset.getGlyph(codePoint);
//
//            Identifier Identifier = ((GlyphRenderTypesExt)(Object) bakedglyph.renderTypes).yuushya_Modelling$getId();
//
//            boolean bold = style.isBold();
//            float alpha = this.a;
//
//            TextColor tc = style.getColor();
//            float cr, cg, cb;
//            if (tc != null) {
//                int v = tc.getValue();
//                cr = (float) (v >> 16 & 0xFF) / 255.0F * this.dimFactor;
//                cg = (float) (v >> 8 & 0xFF) / 255.0F * this.dimFactor;
//                cb = (float) (v & 0xFF) / 255.0F * this.dimFactor;
//            } else {
//                cr = this.r;
//                cg = this.g;
//                cb = this.b;
//            }
//
//            if (!(bakedglyph instanceof EmptyGlyph)) {
//                float boldOffset = bold ? glyphinfo.getBoldOffset() : 0.0F;
//                float shadowOffset = this.dropShadow ? glyphinfo.getShadowOffset() : 0.0F;
//                VertexConsumer vc = this.bufferSource.getBuffer(TextShader.TEXT.apply(Identifier));
//                font.renderChar(
//                        bakedglyph, bold, style.isItalic(), boldOffset,
//                        this.x + shadowOffset, this.y + shadowOffset,
//                        this.pose, vc, cr, cg, cb, alpha, this.packedLight
//                );
//            }
//
//            float advance = glyphinfo.getAdvance(bold);
//            float shadowPad = this.dropShadow ? 1.0F : 0.0F;
//
//            if (style.isStrikethrough()) {
//                this.addEffect(new BakedGlyph.Effect(
//                        this.x + shadowPad - 1.0F, this.y + shadowPad + 4.5F,
//                        this.x + shadowPad + advance, this.y + shadowPad + 4.5F - 1.0F,
//                        0.01F, cr, cg, cb, alpha
//                ));
//            }
//
//            if (style.isUnderlined()) {
//                this.addEffect(new BakedGlyph.Effect(
//                        this.x + shadowPad - 1.0F, this.y + shadowPad + 9.0F,
//                        this.x + shadowPad + advance, this.y + shadowPad + 9.0F - 1.0F,
//                        0.01F, cr, cg, cb, alpha
//                ));
//            }
//
//            this.x += advance;
//            return true;
//        }
//
//        public float finish(int backgroundColor, float startX) {
//            if (backgroundColor != 0) {
//                float a = (float) (backgroundColor >> 24 & 0xFF) / 255.0F;
//                float r = (float) (backgroundColor >> 16 & 0xFF) / 255.0F;
//                float g = (float) (backgroundColor >> 8 & 0xFF) / 255.0F;
//                float b = (float) (backgroundColor & 0xFF) / 255.0F;
//                this.addEffect(new BakedGlyph.Effect(
//                        startX - 1.0F, this.y + 9.0F,
//                        this.x + 1.0F, this.y - 1.0F,
//                        0.01F, r, g, b, a
//                ));
//            }
//
//            if (this.effects != null) {
//                BakedGlyph white = font.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
//                Identifier Identifier = ((GlyphRenderTypesExt)(Object) white.renderTypes).yuushya_Modelling$getId();
//                VertexConsumer vc = this.bufferSource.getBuffer(TextShader.TEXT.apply(Identifier));
//                for (BakedGlyph.Effect e : this.effects) {
//                    white.renderEffect(e, this.pose, vc, this.packedLight);
//                }
//            }
//
//            return this.x;
//        }
//    }
//}