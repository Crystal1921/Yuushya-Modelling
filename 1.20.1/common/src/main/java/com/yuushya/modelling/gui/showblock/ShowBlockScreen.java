package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.blockentity.TransformType;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.SliderButton;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

import static com.yuushya.modelling.blockentity.TransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;

public class ShowBlockScreen extends Screen {
    private final ShowBlockEntity blockEntity;
    private int slot;
    public void setSlot(int slot){
        for(TransformType key:this.storage.keySet()){
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),this.slot, key,storage.get(key));
        }
        this.storage.clear();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for(TransformType type:this.widgets.keySet()){
            this.widgets.get(type).setValidatedValue(type.extract(this.blockEntity,slot));
        }

    }
    private final Map<TransformType,Double> storage = new HashMap<>();
    private final Map<TransformType, SliderButton<Double>> widgets = new HashMap<>();
    private BlockStateIconList rightList;

    public ShowBlockScreen(ShowBlockEntity blockEntity) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
        this.slot = blockEntity.getSlot();
    }

    private int leftColumnX(){ return this.width/4*3; }
    private int leftColumnWidth(){ return this.width/4; }
    private static final int TOP = 10;
    private static final int PER_HEIGHT = 20;
    // i \in [1,...]
    private static int top(int i, int offset){ return TOP+PER_HEIGHT*i+offset; }
    private int leftColumnIndex = 0;
    private int leftTop(int offset){ return top(leftColumnIndex++,offset); }

    @Override
    protected void init() {
        this.rightList =  new BlockStateIconList(this.minecraft,this.width/4 ,this.height,10,TOP, this.height-TOP, this.width/4,36,this.blockEntity.getTransformDatas(),this);
        this.rightList.setSelected(this.rightList.children().get(this.slot));

        widgets.put(POS_X,
                LazyDoubleRange.buttonBuilder(Component.literal("POS X"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.x)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.x)-1,
                                (number)->{updateTransformData(POS_X,number);})
                        .step(1.0)
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(0) , leftColumnWidth(),PER_HEIGHT).build());
        widgets.put(POS_Y,
                LazyDoubleRange.buttonBuilder(Component.literal("POS Y"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.y)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.y)-1,
                                (number)->{updateTransformData(POS_Y,number);})
                        .step(1.0)
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_Y.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(0) , leftColumnWidth(),PER_HEIGHT).build());
        widgets.put(POS_Z,
                LazyDoubleRange.buttonBuilder(Component.literal("POS Z"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.z)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.z)-1,
                                (number)->{updateTransformData(POS_Z,number);})
                        .step(1.0)
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_Z.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(0) , leftColumnWidth(),PER_HEIGHT).build());

        widgets.put(ROT_X,
                DoubleRange.buttonBuilder(Component.literal("ROT X"),0.0,360.0,
                            (number)->{updateTransformData(ROT_X,number);})
                        .step(22.5)
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(10) , leftColumnWidth(),PER_HEIGHT).build());
        widgets.put(ROT_Y,
                DoubleRange.buttonBuilder(Component.literal("ROT Y"),0.0,360.0,
                                (number)->{updateTransformData(ROT_Y,number);})
                        .step(22.5)
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_Y.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(10) , leftColumnWidth(),PER_HEIGHT).build());
        widgets.put(ROT_Z,
                DoubleRange.buttonBuilder(Component.literal("ROT Z"),0.0,360.0,
                                (number)->{updateTransformData(ROT_Z,number);})
                        .step(22.5)
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_Z.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(10) , leftColumnWidth(),PER_HEIGHT).build());
        widgets.put(SCALA_X,
                DividedDoubleRange.buttonBuilder(Component.literal("SCALA"),0.0,1.0,10.0,
                                (number)->{
                                    updateTransformData(SCALA_X,number);
                                    updateTransformData(SCALA_Y,number);
                                    updateTransformData(SCALA_Z,number);
                                })
                        .step(0.1)
                        .initial(SCALA_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),leftTop(20) , leftColumnWidth(),PER_HEIGHT).build());
        for(AbstractWidget widget:widgets.values()){
            this.addRenderableWidget(widget);
        }
        this.addWidget(this.rightList);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.rightList.render(guiGraphics,mouseX,mouseY,partialTick);

//       guiGraphics.drawString(this.font,"Special Button", 40, 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }



    @Override
    public void removed() {
        for(TransformType key:storage.keySet()){
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, key,storage.get(key));
        }
        TransformDataNetwork.sendToServerSideSuccess(blockEntity.getBlockPos());
    }

    private void updateTransformData(TransformType type, Double number){
        this.storage.put(type,number);
        type.modify(blockEntity,slot,number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }
}


/*
this.minecraft.getToasts().addToast(
        SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.NARRATOR_TOGGLE,Component.literal("Hello World!"), Component.literal("This is a toast."))
);
 */