package com.yuushya.modelling.fabriclike.client;

import com.yuushya.modelling.blockentity.ITransformDataInventory;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ShowBlockModel extends com.yuushya.modelling.blockentity.showblock.ShowBlockModel implements UnbakedModel,BakedModel, FabricBakedModel {
    public ShowBlockModel(Direction facing) {
        super(facing);
    }

    public ShowBlockModel(Direction facing,BakedModel backup) {
        super(facing,backup);
    }

    private static final Map<ItemStack,ShowBlockModel> itemModelCache = new HashMap<>();
    protected boolean vanillaAdapter = false;
    protected List<TransformData> transformDatas = null;

    @Override
    public boolean isVanillaAdapter() {
        return this.vanillaAdapter;
    }

    //释放blockQuads的是每次区块构建的时候生成的，所以直接修改自己，不用new新的
    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        ShowBlockEntity blockEntity=(ShowBlockEntity) blockView.getBlockEntity(pos);
        if (blockEntity==null) return;
        this.vanillaAdapter = true;
        this.transformDatas = blockEntity.getTransformDatas();
        VanillaModelEncoder.emitBlockQuads(this, state, randomSupplier, context, context.getEmitter());
    }
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction side, RandomSource rand) {
        if(transformDatas==null) return super.getQuads(blockState,side,rand);
        return super.getQuads(blockState,side,rand,transformDatas);
    }

    //释放itemQuads的只有一个showModel单例，这个单例会拿到各种stack，所以这里得用new
    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        CompoundTag data = stack.getTag();
        if(data == null){
            VanillaModelEncoder.emitItemQuads(backup, null, randomSupplier, context);
        }
        else{
            VanillaModelEncoder.emitItemQuads(itemModelCache.computeIfAbsent(stack,(_stack)->new ShowBlockModel(Direction.SOUTH) {
                {
                    this.transformDatas = new ArrayList<>();
                    ITransformDataInventory.load(data, this.transformDatas);
                    this.vanillaAdapter = true;
                }
            }), null, randomSupplier, context);
        }
    }
}
