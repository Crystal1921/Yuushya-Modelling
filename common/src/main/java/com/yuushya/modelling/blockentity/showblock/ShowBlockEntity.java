package com.yuushya.modelling.blockentity.showblock;


import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.ITransformDataInventory;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShowBlockEntity extends BlockEntity implements ITransformDataInventory {

    private final List<TransformData> transformDatas;
    @Override
    public List<TransformData> getTransformDatas() {return transformDatas;}
    @NotNull
    public TransformData getTransFormDataNow(){return getTransformData(slot);}
    public void removeTransFormDataNow(){removeTransformData(slot);}
    public void setTransformDataNow(TransformData transformData){setTransformData(slot,transformData);}
    public void setSlotBlockStateNow(BlockState blockState){setSlotBlockState(slot,blockState);}


    private Integer slot;
    public int getSlot(){return slot;}
    public void setSlot(int slot){
        if (slot>=transformDatas.size()){
            for (int i=slot-transformDatas.size()+1;i>0;i--)
                transformDatas.add(new TransformData());
        }
        this.slot=slot;
    }
    private Integer showFrame =0;
    public boolean showFrame(){return showFrame >0;}
    public void setShowFrame(){showFrame =5;}
    public void consumeShowFrame(){
        showFrame = showFrame< 0? 0: showFrame -1;
    }
    //显示旋转的坐标轴
    private Integer showRotAxis =0;
    public boolean showRotAxis(){return showRotAxis >0;}
    public void setShowRotAixs(){showRotAxis =5;  }

    //显示平移的坐标轴
    private Integer showPosAxis =0;
    public boolean showPosAxis(){return showPosAxis >0;}
    public void setShowPosAixs(){showPosAxis =5;  }

    private Integer showText =0;
    public boolean showText(){return showText>0;}
    public void setShowText(){showText =5;}

    private Direction.Axis showAxis = null;
    public Direction.Axis getShowAxis(){ return showAxis; }
    public void setShowAxis(Direction.Axis axis){ showAxis = axis; }
    public void consumeShowAxis(){
        if (showRotAxis <= 0 && showPosAxis <= 0) showAxis = null;
    }

    public void consumeShow(){
        showRotAxis = showRotAxis< 0? 0: showRotAxis -1;
        showPosAxis = showPosAxis< 0? 0: showPosAxis -1;
        showText = showText <0? 0: showText -1;
    }

    public ShowBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(YuushyaRegistries.SHOW_BLOCK_ENTITY.get(), blockPos, blockState);
        transformDatas = new ArrayList<>();
        transformDatas.add(new TransformData());
        slot=0;
    }
    @Override
    //readNbt
    public void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        super.loadAdditional(compoundTag,registries);
        ITransformDataInventory.load(compoundTag,transformDatas);
        slot= (int) compoundTag.getByte("ControlSlot");
        //client chunk update
        if (this.getLevel() != null && this.getLevel().isClientSide){
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
    @Override
    //writeNbt
    protected void saveAdditional(CompoundTag compoundTag,HolderLookup.Provider registries) {
        super.saveAdditional(compoundTag,registries);
        ITransformDataInventory.saveAdditional(compoundTag,transformDatas);
        compoundTag.putByte("ControlSlot",slot.byteValue());
    }

    @Override
    //toInitialChunkDataNbt //When you first load world it writeNbt firstly
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        //saveChanged();
        CompoundTag compoundTag =  super.getUpdateTag(registries);
        ITransformDataInventory.saveAdditional(compoundTag,transformDatas);
        return compoundTag;
    }

    public void saveChanged() {
        this.setChanged();

        if (this.getLevel() != null && !this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this,(blockEntity,access)->{
            CompoundTag compoundTag=getUpdateTag(access);
            saveAdditional(compoundTag,access);
            return compoundTag;});
    }


}





