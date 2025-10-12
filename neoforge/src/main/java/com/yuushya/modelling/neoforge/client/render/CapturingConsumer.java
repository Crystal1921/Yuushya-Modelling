//package com.yuushya.modelling.neoforge.client.render;
//
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.util.FastColor;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class CapturingConsumer implements VertexConsumer {
//    private final RenderType renderType;
//    private final List<Vertex> out = new ArrayList<>();
//    private float x, y, z, u, v;
//    private int colorARGB = 0xFFFFFFFF, lightPacked, overlayPacked;
//    private float nx = 0, ny = 1, nz = 0;
//
//    public CapturingConsumer(RenderType rt) {
//        this.renderType = rt;
//    }
//
//    public RenderType renderType() {
//        return renderType;
//    }
//
//    public List<Vertex> vertices() {
//        return out;
//    }
//
//    @Override
//    public VertexConsumer addVertex(float x, float y, float z) {
//        this.x = x;
//        this.y = y;
//        this.z = z;
//        return this;
//    }
//
//    @Override
//    public VertexConsumer setColor(int r, int g, int b, int a) {
//        this.colorARGB = FastColor.ARGB32.color(a, r, g, b);
//        return this;
//    }
//
//    @Override
//    public VertexConsumer setUv(float u, float v) {
//        this.u = u;
//        this.v = v;
//        return this;
//    }
//
//    @Override
//    public VertexConsumer setUv1(int u, int v) {
//        this.overlayPacked = (v << 16) | (u & 0xFFFF);
//        return this;
//    }
//
//    @Override
//    public VertexConsumer setUv2(int u, int v) {
//        this.lightPacked = (v << 16) | (u & 0xFFFF);
//        return this;
//    }
//
//    @Override
//    public VertexConsumer setNormal(float nx, float ny, float nz) {
//        this.nx = nx;
//        this.ny = ny;
//        this.nz = nz;
//        return this;
//    }
//
//    public void addVertex(float f, float g, float h, int i, float j, float k, int l, int m, float n, float o, float p) {
//        this.addVertex(f, g, h);
//        this.setColor(i);
//        this.setUv(j, k);
//        this.setOverlay(l);
//        this.setLight(m);
//        this.setNormal(n, o, p);
//        out.add(new Vertex(x, y, z, u, v, colorARGB, lightPacked, overlayPacked, nx, ny, nz));
//    }
//}
