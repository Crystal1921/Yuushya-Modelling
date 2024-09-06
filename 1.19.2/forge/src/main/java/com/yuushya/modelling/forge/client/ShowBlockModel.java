package com.yuushya.modelling.forge.client;


import com.yuushya.modelling.blockentity.ITransformDataInventory;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShowBlockModel extends com.yuushya.modelling.blockentity.showblock.ShowBlockModel implements IForgeBakedModel, BakedModel {
    public static ModelProperty<ShowBlockEntity> BASE_BLOCK_ENTITY = new ModelProperty<>();

    public ShowBlockModel(Direction facing) {
        super(facing);
    }

    public ShowBlockModel(Direction facing,BakedModel backup) {
        super(facing,backup);
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
        ShowBlockEntity blockEntity=data.get(BASE_BLOCK_ENTITY);
        if (blockEntity==null) return Collections.emptyList();
        return super.getQuads(state,side,rand,blockEntity.getTransformDatas());
    }

    private static final Map<ItemStack,ShowBlockModel> itemModelCache = new HashMap<>();
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    @Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        CompoundTag data = itemStack.getTagElement(BLOCK_ENTITY_TAG);
        if(data == null){
            return List.of(backup);
        }
        return List.of(itemModelCache.computeIfAbsent(itemStack,(_stack)-> new ShowBlockModel(Direction.SOUTH){
            private final List<TransformData> transformDatas;
            {
                this.transformDatas = new ArrayList<>();
                ITransformDataInventory.load(data,transformDatas);
            }
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                return super.getQuads(blockState,side,rand,transformDatas);
            }
        }));
    }
}
