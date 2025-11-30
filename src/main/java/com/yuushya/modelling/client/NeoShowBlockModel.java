package com.yuushya.modelling.client;


import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockModel;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
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
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NeoShowBlockModel extends ShowBlockModel implements IBakedModelExtension, BakedModel {
    private static final Map<ItemStack, NeoShowBlockModel> itemModelCache = new HashMap<>();
    public static ModelProperty<ShowBlockEntity> BASE_BLOCK_ENTITY = new ModelProperty<>();

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
        return List.of(itemModelCache.computeIfAbsent(itemStack, (_stack) -> new NeoShowBlockModel(Direction.SOUTH) {
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
    }
}
