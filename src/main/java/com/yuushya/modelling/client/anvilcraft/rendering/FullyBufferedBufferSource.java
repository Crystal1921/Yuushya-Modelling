package com.yuushya.modelling.client.anvilcraft.rendering;

import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.client.renderer.RenderStateShard.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FullyBufferedBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {
    private final Map<RenderType, BufferBuilder> bufferBuilders = new LinkedHashMap<>();
    @Getter
    private final Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    @Getter
    private final Map<RenderType, BufferBuilder.SortState> meshSorts = new HashMap<>();
    private final Map<RenderType, RenderType> cachedRenderTypeConvertions = new HashMap<>();

    public FullyBufferedBufferSource() {
        super(null, null);
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        RenderType actualType = forceUseBlockRenderTypes(renderType);
        return bufferBuilders.computeIfAbsent(
                actualType,
                it -> {
                    BufferBuilder bufferBuilder = new BufferBuilder(786432);
                    bufferBuilder.begin(it.mode, it.format());
                    return bufferBuilder;
                });
    }

    private RenderType forceUseBlockRenderTypes(RenderType renderType) {
        if (renderType.format() == DefaultVertexFormat.NEW_ENTITY && renderType instanceof RenderType.CompositeRenderType compositeRenderType) {
            return cachedRenderTypeConvertions.computeIfAbsent(
                    renderType,
                    it -> createBlockRenderType(compositeRenderType)
            );
        }
        return renderType;
    }

    private RenderType createBlockRenderType(RenderType.CompositeRenderType renderType) {
        RenderType.CompositeState state = renderType.state();
        return RenderType.create(
                "yuushya_modelling:generated",
                DefaultVertexFormat.BLOCK,
                renderType.mode,
                786432,
                renderType.affectsCrumbling,
                renderType.sortOnUpload,
                RenderType.CompositeState.builder()
                        .setCullState(state.cullState)
                        .setOutputState(MAIN_TARGET)
                        .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                        .setTextureState(state.textureState)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(state.transparencyState)
                        .createCompositeState(false)
        );
    }

    public boolean isEmpty() {
        if (bufferBuilders.isEmpty()) return true;
        return bufferBuilders.values().stream().noneMatch(it -> it.vertices > 0);
    }

    @Override public void endBatch(RenderType renderType) {}
    @Override public void endLastBatch() {}
    @Override public void endBatch() {}

    public void upload(
            Function<RenderType, VertexBuffer> vertexBufferGetter,
            Consumer<Runnable> runner
    ) {
        List<RenderType> renderTypes = new ArrayList<>(bufferBuilders.keySet());
        for (RenderType renderType : renderTypes) {
            BufferBuilder bufferBuilder = bufferBuilders.get(renderType);
            if (bufferBuilder == null) continue;

            // ★ 在 end() 之前读取 vertices
            int vertexCount = bufferBuilder.vertices;
            int indexCount = renderType.mode.indexCount(vertexCount);

            runner.accept(() -> {
                if (vertexCount <= 0) {
                    bufferBuilder.end().release();
                    bufferBuilders.remove(renderType);
                    return;
                }
                BufferBuilder.RenderedBuffer mesh = bufferBuilder.end();
                indexCountMap.put(renderType, indexCount);

                VertexBuffer vertexBuffer = vertexBufferGetter.apply(renderType);
                vertexBuffer.bind();
                vertexBuffer.upload(mesh);
                VertexBuffer.unbind();

                bufferBuilders.remove(renderType);
            });
        }
    }

    @Override
    public void close() {
        bufferBuilders.clear();
    }
}