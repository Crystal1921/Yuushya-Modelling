//package com.yuushya.modelling.neoforge.client.render;
//
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//public class MeshBufferSource implements MultiBufferSource {
//    private final Map<RenderType, CapturingConsumer> map = new LinkedHashMap<>();
//
//    @Override
//    public VertexConsumer getBuffer(RenderType type) {
//        return map.computeIfAbsent(type, CapturingConsumer::new);
//    }
//
//    public Map<RenderType, List<Vertex>> freeze() {
//        Map<RenderType, List<Vertex>> out = new LinkedHashMap<>();
//        map.forEach((rt, cc) -> out.put(rt, List.copyOf(cc.vertices())));
//        return out;
//    }
//}
