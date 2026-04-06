package com.yuushya.modelling.client;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockModel;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class NeoShowBlockModel extends ShowBlockModel implements IBakedModelExtension, BakedModel {
    private static final Cache<ItemStack, NeoShowBlockModel> itemModelCache = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .maximumSize(1000)
            .build();
    public static ModelProperty<ShowBlockEntity> BASE_BLOCK_ENTITY = new ModelProperty<>();
    private static final net.neoforged.neoforge.client.ChunkRenderTypeSet CUTOUT_MIPPED = net.neoforged.neoforge.client.ChunkRenderTypeSet.of(RenderType.cutoutMipped());

    public NeoShowBlockModel(Direction facing) {
        super(facing);
    }

    public NeoShowBlockModel(Direction facing, BakedModel backup) {
        super(facing, backup);
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (level.getBlockEntity(pos) == null) {
            return ModelData.builder().build();
        } else {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShowBlockEntity blockEntity1)
                return ModelData.builder().with(BASE_BLOCK_ENTITY, blockEntity1).build();
            else
                return ModelData.builder().build();
        }
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        ShowBlockEntity blockEntity = data.get(BASE_BLOCK_ENTITY);
        if (blockEntity == null) return Collections.emptyList();
        return super.getQuads(state, side, rand, blockEntity.getTransformData());
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data == CustomData.EMPTY) {
            return List.of(backup);
        }
        try {
            return List.of(itemModelCache.get(itemStack, () -> new NeoShowBlockModel(Direction.SOUTH) {
                private final List<TransformBlockData> transformDatas;

                {
                    this.transformDatas = new ArrayList<>();
                    ITransformDataInventory.load(data.copyTag(), transformDatas);
                }

                @Override
                public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                    return super.getQuads(blockState, side, rand, transformDatas);
                }
            }));
        } catch (ExecutionException e) {
            Yuushya.LOGGER.error(e.getCause());
            return List.of();
        }
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return CUTOUT_MIPPED;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return List.of(RenderType.cutoutMipped());
    }
}
