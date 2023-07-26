package com.yuushya.modelling.blockentity.showblock;


import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.iTransformDataInventory;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShowBlockEntity extends BlockEntity implements iTransformDataInventory {

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

    //显示旋转的坐标轴
    private Integer showRotAxis =0;
    public boolean showRotAxis(){return showRotAxis >0;}
    public void setShowRotAixs(){
        setShowText();
        showRotAxis =5;  }

    //显示平移的坐标轴
    private Integer showPosAxis =0;
    public boolean showPosAxis(){return showPosAxis >0;}
    public void setShowPosAixs(){
        setShowText();
        showPosAxis =5;  }

    private Integer showText =0;
    public boolean showText(){return showText>0;}
    public void setShowText(){showText =5;}
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
    public void load(CompoundTag compoundTag){
        super.load(compoundTag);
        iTransformDataInventory.load(compoundTag,transformDatas);
        slot= (int) compoundTag.getByte("ControlSlot");
    }
    @Override
    //writeNbt
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        iTransformDataInventory.saveAdditional(compoundTag,transformDatas);
        compoundTag.putByte("ControlSlot",slot.byteValue());
    }

    @Override
    //toInitialChunkDataNbt //When you first load world it writeNbt firstly
    public CompoundTag getUpdateTag() {
        saveChanged();
        CompoundTag compoundTag =  super.getUpdateTag();
        //saveAdditional(compoundTag);
        return compoundTag;
    }

    public void saveChanged() {
        this.setChanged();
        if (this.getLevel()!=null){
            BlockState blockState =this.getLevel().getBlockState(this.getBlockPos());
            this.getLevel().sendBlockUpdated(this.getBlockPos(),blockState,blockState,3);
            //this.getLevel().setBlocksDirty(this.getBlockPos(), this.getLevel().getBlockState(this.getBlockPos()), Blocks.AIR.defaultBlockState());
        }
    }
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag compoundTag=getUpdateTag();
        return ClientboundBlockEntityDataPacket.create(this,(blockEntity)->{saveAdditional(compoundTag);return compoundTag;});}


}





