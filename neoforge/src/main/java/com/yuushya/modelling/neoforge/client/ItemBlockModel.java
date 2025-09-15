package com.yuushya.modelling.neoforge.client;


import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformItemDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
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

public class ItemBlockModel extends com.yuushya.modelling.blockentity.itemblock.ItemBlockModel implements IBakedModelExtension, BakedModel {
    private static final Map<ItemStack, ItemBlockModel> itemModelCache = new HashMap<>();
    public static ModelProperty<ItemBlockEntity> BASE_BLOCK_ENTITY = new ModelProperty<>();

    public ItemBlockModel(Direction facing) {
        super(facing);
    }

    public ItemBlockModel(Direction facing, BakedModel backup) {
        super(facing, backup);
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (level.getBlockEntity(pos) == null) {
            return ModelData.builder().build();
        } else {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ItemBlockEntity blockEntity1)
                return ModelData.builder().with(BASE_BLOCK_ENTITY, blockEntity1).build();
            else
                return ModelData.builder().build();
        }
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        ItemBlockEntity blockEntity = data.get(BASE_BLOCK_ENTITY);
        if (blockEntity == null) return Collections.emptyList();
        return super.getQuads(side, rand, blockEntity.getTransformData());
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        CustomData data = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Collections.emptyList();
        }
        RegistryAccess registryAccess = level.registryAccess();
        if (data == CustomData.EMPTY) {
            return List.of(backup);
        }
        return List.of(itemModelCache.computeIfAbsent(itemStack, (_stack) -> new ItemBlockModel(Direction.SOUTH) {
            private final List<TransformItemData> transformDatas;

            {
                this.transformDatas = new ArrayList<>();
                ITransformItemDataInventory.load(data.copyTag(), transformDatas, registryAccess);
            }

            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
                return super.getQuads(side, rand, transformDatas);
            }
        }));
    }
}
