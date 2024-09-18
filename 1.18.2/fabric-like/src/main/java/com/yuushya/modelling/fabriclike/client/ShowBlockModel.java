package com.yuushya.modelling.fabriclike.client;

import com.yuushya.modelling.blockentity.ITransformDataInventory;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.yuushya.modelling.utils.YuushyaUtils.BLOCK_ENTITY_TAG;

public class ShowBlockModel extends com.yuushya.modelling.blockentity.showblock.ShowBlockModel implements UnbakedModel,BakedModel, FabricBakedModel {
    public ShowBlockModel(Direction facing) {
        super(facing);
    }
    public ShowBlockModel(Direction facing,BakedModel backup) {
        super(facing,backup);
    }
    public ResourceLocation backupResourceLocation;
    public ShowBlockModel(Direction facing, ResourceLocation backupResourceLocation ) {
        super(facing);
        this.backupResourceLocation = backupResourceLocation;
    }

    @Override
    public @Nullable BakedModel bake(ModelBakery modelBakery, Function<Material, TextureAtlasSprite> function, ModelState modelState, ResourceLocation resourceLocation) {
        if(backupResourceLocation !=null){
            backup = modelBakery.getModel(backupResourceLocation).bake(modelBakery, function, modelState, resourceLocation);
        }
        return super.bake(modelBakery, function, modelState, resourceLocation);
    }

    private static final Map<ItemStack,ShowBlockModel> itemModelCache = new HashMap<>();

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    //释放blockQuads的是每次区块构建的时候生成的，所以直接修改自己，不用new新的
    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        ShowBlockEntity blockEntity=(ShowBlockEntity) blockView.getBlockEntity(pos);
        if (blockEntity==null) return;
        context.fallbackConsumer().accept(new ShowBlockModel(facing) {
            @Override
            public boolean isVanillaAdapter() {
                return true;
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, Random rand) {
                return super.getQuads(blockState,side,rand,blockEntity.getTransformDatas());
            }
        });
    }

    //释放itemQuads的只有一个showModel单例，这个单例会拿到各种stack，所以这里得用new
    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        CompoundTag data = stack.getTagElement(BLOCK_ENTITY_TAG);
        if(data == null){
            context.fallbackConsumer().accept(backup);
        }
        else{
            List<TransformData> transformDatas = new ArrayList<>();
            ITransformDataInventory.load(data,transformDatas);
            context.fallbackConsumer().accept(itemModelCache.computeIfAbsent(stack,(_stack)->new ShowBlockModel(Direction.SOUTH) {
                @Override
                public boolean isVanillaAdapter() {
                    return true;
                }

                @Override
                public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, Random rand) {
                    return super.getQuads(blockState,side,rand,transformDatas);
                }
            }));
        }
    }
}
