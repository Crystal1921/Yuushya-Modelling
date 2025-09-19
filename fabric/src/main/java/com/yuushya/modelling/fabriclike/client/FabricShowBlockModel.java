package com.yuushya.modelling.fabriclike.client;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockModel;
import com.yuushya.modelling.blockentity.transformData.ITransformDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FabricShowBlockModel extends ShowBlockModel implements UnbakedModel, BakedModel, FabricBakedModel {
    private static final Map<ItemStack, FabricShowBlockModel> itemModelCache = new HashMap<>();

    public FabricShowBlockModel(Direction facing) {
        super(facing);
    }

    public FabricShowBlockModel(Direction facing, BakedModel backup) {
        super(facing, backup);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    //释放blockQuads的是每次区块构建的时候生成的，所以直接修改自己，不用new新的
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        ShowBlockEntity blockEntity = (ShowBlockEntity) blockView.getBlockEntity(pos);
        if (blockEntity == null) return;
        VanillaModelEncoder.emitBlockQuads(new FabricShowBlockModel(facing) {
            @Override
            public boolean isVanillaAdapter() {
                return true;
            }

            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                return super.getQuads(blockState, side, rand, blockEntity.getTransformData());
            }
        }, state, randomSupplier, context);
    }

    //释放itemQuads的只有一个showModel单例，这个单例会拿到各种stack，所以这里得用new
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data == CustomData.EMPTY) {
            VanillaModelEncoder.emitItemQuads(backup, null, randomSupplier, context);
        } else {
            List<TransformBlockData> transformDatas = new ArrayList<>();
            ITransformDataInventory.load(data.copyTag(), transformDatas);
            VanillaModelEncoder.emitItemQuads(itemModelCache.computeIfAbsent(stack, (_stack) -> new FabricShowBlockModel(Direction.SOUTH) {
                @Override
                public boolean isVanillaAdapter() {
                    return true;
                }

                @Override
                public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                    return super.getQuads(blockState, side, rand, transformDatas);
                }

            }), null, randomSupplier, context);
        }
    }
}
