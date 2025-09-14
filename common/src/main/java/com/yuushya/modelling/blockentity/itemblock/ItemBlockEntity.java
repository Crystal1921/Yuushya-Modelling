package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.blockentity.ITransformDataInventory;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.registries.YuushyaRegistries;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ItemBlockEntity extends BlockEntity implements ITransformDataInventory {
    @Getter
    private final List<TransformData> transformData;
    @NotNull
    public TransformData getTransFormDataNow(){return getTransformData(slot);}
    public void removeTransFormDataNow(){removeTransformData(slot);}
    public void setTransformDataNow(TransformData transformData){setTransformData(slot,transformData);}
    public void setSlotBlockStateNow(BlockState blockState){setSlotBlockState(slot,blockState);}


    private Integer slot;
    public int getSlot(){return slot;}
    public void setSlot(int slot){
        if (slot>= transformData.size()){
            for (int i = slot- transformData.size()+1; i>0; i--)
                transformData.add(new TransformData());
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
    public void setShowRotAxis(){showRotAxis =5;  }

    //显示平移的坐标轴
    private Integer showPosAxis =0;
    public boolean showPosAxis(){return showPosAxis >0;}
    public void setShowPosAxis(){showPosAxis =5;  }

    private Integer showText =0;
    public boolean showText(){return showText>0;}
    public void setShowText(){showText =5;}

    public ItemBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(YuushyaRegistries.ITEM_BLOCK_ENTITY.get(), blockPos, blockState);
        transformData = new ArrayList<>();
        transformData.add(new TransformData());
        slot = 0;
    }
}
