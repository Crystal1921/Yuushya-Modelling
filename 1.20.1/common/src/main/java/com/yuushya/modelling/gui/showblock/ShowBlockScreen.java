package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.blockentity.TransformType;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.SliderButton;
import com.yuushya.modelling.gui.validate.DividedDoubleRange;
import com.yuushya.modelling.gui.validate.DoubleRange;
import com.yuushya.modelling.gui.validate.LazyDoubleRange;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yuushya.modelling.blockentity.TransformType.*;
import static com.yuushya.modelling.item.showblocktool.PosTransItem.getMaxPos;

public class ShowBlockScreen extends Screen {
    private final ShowBlockEntity blockEntity;
    private final BlockState newBlockState;
    private int slot;
    public void setSlot(int slot){
        for(TransformType key:this.storage.keySet()){
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),this.slot, key,storage.get(key));
        }
        this.storage.clear();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for(TransformType type:this.sliderButtons.keySet()){
            this.sliderButtons.get(type).setValidatedValue(type.extract(this.blockEntity,slot));
        }
        updateStateButtonVisible();
    }
    private final Map<TransformType,Double> storage = new HashMap<>();
    private final Map<TransformType,Double> standardStep = new HashMap<>();
    private final Map<TransformType, SliderButton<Double>> sliderButtons = new HashMap<>();
    private final Map<TransformType, Button> minusButtons = new HashMap<>();
    private final Map<TransformType, Button> addButtons = new HashMap<>();
    private void addSmallButton(TransformType type,boolean increase,int x){
        Map<TransformType, Button> buttons = increase ? addButtons : minusButtons;
        Component text = increase ? Component.literal("+") : Component.literal("-");
        buttons.put(type,Button.builder(text,
                        (btn)->{
                            step(sliderButtons.get(type),increase);
                        })
                .bounds(x,sliderButtons.get(type).getY(),10,20)
                .build());
    }
    private CycleButton<Mode> modeButton;
    private Button addStateButton;
    private Button removeStateButton;
    private CycleButton<Boolean> shownStateButton;
    private final Map<TransformType, EditBox> editBoxes = new HashMap<>();
    private BlockStateIconList blockStateList;
    private Button leftPropertyButton;
    private Property<?> property;
    private Button rightPropertyButton;
    private Button leftStateButton;
    private Button rightStateButton;

    public boolean updateStateButtonVisible(){
        Collection<Property<?>> collection = blockStateList.updateRenderProperties(getBlockState());
        boolean stateButtonVisible = !collection.isEmpty();
        if(stateButtonVisible && property==null) property = collection.iterator().next();
        leftStateButton.visible = stateButtonVisible;
        rightStateButton.visible = stateButtonVisible;
        leftPropertyButton.visible = stateButtonVisible;
        rightPropertyButton.visible = stateButtonVisible;
        return stateButtonVisible;
    }

    public BlockState getBlockState(){
        return blockEntity.getTransformData(slot).blockState;
    }

    public ShowBlockScreen(ShowBlockEntity blockEntity, BlockState newBlockState) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
        this.newBlockState = newBlockState;
        if(blockEntity.getSlot() < blockEntity.getTransformDatas().size()){
            this.slot = blockEntity.getSlot();
        }
    }

    private int leftColumnX(){ return this.width/4*3 + 10; }
    private int leftColumnWidth(){ return this.width/4 - 20; }
    private static final int TOP = 10;
    private static final int PER_HEIGHT = 20;
    // i \in [1,...]
    private static int top(int i, int offset){ return TOP+PER_HEIGHT + 10 + PER_HEIGHT*i+offset; }

    @Override
    protected void init() {
        addStateButton = Button.builder(Component.literal("+"),
                        (btn)->{
                            if(this.newBlockState!=null){
                                blockStateList.addSlot();
                                updateTransformData(BLOCK_STATE,(double) Block.getId(this.newBlockState));
                                updateTransformData(SHOWN,1.0);
                            }
                        })
                .bounds(2,TOP,20,PER_HEIGHT).build();
        removeStateButton = Button.builder(Component.literal("X"),
                        (btn)->{
                            updateTransformData(REMOVE,0.0);
                        }
                )
                .bounds(2+20,TOP,20,PER_HEIGHT).build();

        shownStateButton = CycleButton.<Boolean>booleanBuilder(Component.literal("On"),Component.literal("Off"))
                .displayOnlyValue()
                .withInitialValue(true)
                .create(2+20+20,TOP,20,PER_HEIGHT,Component.empty(),
                        (btn,bl)->{
                            updateTransformData(SHOWN,bl?1.0:0.0);
                        }
                );

        blockStateList =  new BlockStateIconList(this.minecraft,40 ,this.height,2,TOP+20, this.height-TOP, 40,45,this.blockEntity.getTransformDatas(),this);

        leftPropertyButton = Button.builder(Component.literal("<"),
                        (btn)->{
                            property = YuushyaBlockStates.getRelative(blockStateList.updateRenderProperties(getBlockState()), property, true);
                        })
                .bounds(45,TOP + 110,10,PER_HEIGHT)
                .build();
        rightPropertyButton = Button.builder(Component.literal(">"),
                        (btn)->{
                            property = YuushyaBlockStates.getRelative(blockStateList.updateRenderProperties(getBlockState()), property, false);
                        })
                .bounds(95,TOP + 110,10,PER_HEIGHT)
                .build();
        leftStateButton = Button.builder(Component.literal("<"),
                        (btn)->{
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, true);
                            updateTransformData(BLOCK_STATE,(double)Block.getId(nextBlockState));
                        })
                .bounds(45,TOP + 130,10,PER_HEIGHT)
                .build();
        rightStateButton = Button.builder(Component.literal(">"),
                        (btn)->{
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, true);
                            updateTransformData(BLOCK_STATE,(double)Block.getId(nextBlockState));
                        })
                .bounds(95,TOP + 130,10,PER_HEIGHT)
                .build();

        modeButton = CycleButton.builder(Mode::getSymbol)
                        .withValues(Mode.values())
                        .withInitialValue(Mode.SLIDER)
                        .create(leftColumnX(),TOP,leftColumnWidth(),PER_HEIGHT,Component.literal("MODE"),
                                (btn,mode)->{
                                    switch (mode){
                                        case SLIDER -> sliderButtons.forEach((type,btn1)->btn1.setStep(standardStep.get(type)));
                                        case FINE_TUNE -> sliderButtons.forEach((type,btn1)->btn1.setStep(0.001));
                                        case EDIT -> {}
                                    }
                                }
                        );

        sliderButtons.put(POS_X,
                LazyDoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.pos_text"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.x)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.x)-1,
                                (number)->{updateTransformData(POS_X,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x",String.format("%05.1f",number))))
                        .step(standardStep.computeIfAbsent(POS_X,(type)->1.0))
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(0,0) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(POS_X,false,leftColumnX()-10);
        addSmallButton(POS_X,true,leftColumnX()+leftColumnWidth());
        //editBoxs.put(POS_X, new EditBox(this.font,leftColumnX() + leftColumnWidth() ,leftTop(0) , 27, PER_HEIGHT,Component.literal("test")));

        sliderButtons.put(POS_Y,
                LazyDoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.pos_text"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.y)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.y)-1,
                                (number)->{updateTransformData(POS_Y,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y",String.format("%05.1f",number))))
                        .step(standardStep.computeIfAbsent(POS_Y,(type)->1.0))
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_Y.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(1,0) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(POS_Y,false,leftColumnX()-10);
        addSmallButton(POS_Y,true,leftColumnX()+leftColumnWidth());

        sliderButtons.put(POS_Z,
                LazyDoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.pos_text"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.z)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.z)-1,
                                (number)->{updateTransformData(POS_Z,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z",String.format("%05.1f",number))))
                        .step(standardStep.computeIfAbsent(POS_Z,(type)->1.0))
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_Z.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(2,0) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(POS_Z,false,leftColumnX()-10);
        addSmallButton(POS_Z,true,leftColumnX()+leftColumnWidth());

        sliderButtons.put(ROT_X,
                DoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.rot_text"),0.0,360.0,
                            (number)->{updateTransformData(ROT_X,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x",String.format("%05.1f",number))))
                        .step(standardStep.computeIfAbsent(ROT_X,(type)->22.5))
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(3,10) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(ROT_X,false,leftColumnX()-10);
        addSmallButton(ROT_X,true,leftColumnX()+leftColumnWidth());

        sliderButtons.put(ROT_Y,
                DoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.rot_text"),0.0,360.0,
                                (number)->{updateTransformData(ROT_Y,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y",String.format("%05.1f",number))))
                        .step(standardStep.computeIfAbsent(ROT_Y,(type)->22.5))
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_Y.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(4,10) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(ROT_Y,false,leftColumnX()-10);
        addSmallButton(ROT_Y,true,leftColumnX()+leftColumnWidth());

        sliderButtons.put(ROT_Z,
                DoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.rot_text"),0.0,360.0,
                                (number)->{updateTransformData(ROT_Z,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z",String.format("%05.1f",number))))
                        .step(standardStep.computeIfAbsent(ROT_Z,(type)->22.5))
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_Z.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(5,10) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(ROT_Z,false,leftColumnX()-10);
        addSmallButton(ROT_Z,true,leftColumnX()+leftColumnWidth());

        sliderButtons.put(SCALE_X,
                DividedDoubleRange.buttonBuilder(Component.empty(),0.0,1.0,10.0,
                                (number)->{
                                    updateTransformData(SCALE_X,number);
                                    updateTransformData(SCALE_Y,number);
                                    updateTransformData(SCALE_Z,number);
                                })
                        .text((caption,number)->Component.translatable("block.yuushya.showblock.scale_text",String.format("%05.1f",number)))
                        .step(standardStep.computeIfAbsent(SCALE_X,(type)->0.1))
                        .initial(SCALE_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(6,20) , leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(SCALE_X,false,leftColumnX()-10);
        addSmallButton(SCALE_X,true,leftColumnX()+leftColumnWidth());

        sliderButtons.put(LIT,
                DoubleRange.buttonBuilder(Component.literal("LIT"),0.0,15.0,
                        (number)->{
                            updateTransformData(LIT,number);
                        })
                        .text(LazyDoubleRange::captionToString)
                        .step(standardStep.computeIfAbsent(LIT,(type)->1.0))
                        .initial(LIT.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(7,30),leftColumnWidth(),PER_HEIGHT).build());
        addSmallButton(LIT,false,leftColumnX()-10);
        addSmallButton(LIT,true,leftColumnX()+leftColumnWidth());

        for(SliderButton<Double> widget: sliderButtons.values()){
            this.addRenderableWidget(widget);
        }
        for(EditBox box: editBoxes.values()){
            this.addRenderableWidget(box);
        }
        for(Button button: minusButtons.values()){
            this.addRenderableWidget(button);
        }
        for(Button button: addButtons.values()){
            this.addRenderableWidget(button);
        }
        this.addRenderableWidget(modeButton);
        this.addWidget(this.blockStateList);
        this.addRenderableWidget(addStateButton);
        this.addRenderableWidget(removeStateButton);
        this.addRenderableWidget(shownStateButton);
        this.addRenderableWidget(leftPropertyButton);
        this.addRenderableWidget(rightPropertyButton);
        this.addRenderableWidget(leftStateButton);
        this.addRenderableWidget(rightStateButton);

        blockStateList.setSelectedSlot(slot);//updateStateButtonVisible();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.blockStateList.render(guiGraphics,mouseX,mouseY,partialTick);
        BlockState blockState = getBlockState();
        guiGraphics.drawString(this.font,this.blockStateList.updateRenderDisplayName(blockState), 45, TOP +6+20, 0xFFFFFFFF, false);
        List<String> properties = this.blockStateList.updateRenderBlockStateProperties(blockState);
        for(int i=0;i<properties.size();i++){
            MutableComponent displayBlockState = Component.literal(properties.get(i));
            guiGraphics.drawString(this.font, displayBlockState, 45, TOP +6+20 + this.font.lineHeight*(i+1)+1, 0xFFEBC6, false);
        }
        if(updateStateButtonVisible()){
            guiGraphics.drawString(this.font, property.getName(), 65,TOP + 120, 0xFFFFFFFF, false);
            guiGraphics.drawString(this.font, YuushyaDebugStickItem.getNameHelper(blockState,property), 65,TOP + 140, 0xFFFFFFFF, false);
        }
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
        this.storage.clear();
        TransformDataNetwork.sendToServerSideSuccess(blockEntity.getBlockPos());
    }

    private void updateTransformData(TransformType type, Double number){
        this.storage.put(type,number);
        type.modify(blockEntity,slot,number);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    public void step(SliderButton<Double> sliderButton, boolean increase){
        if(increase) sliderButton.setValidatedValue(sliderButton.getValidatedValue()+sliderButton.getStep());
        else sliderButton.setValidatedValue(sliderButton.getValidatedValue()-sliderButton.getStep());
    }

    public enum Mode implements StringRepresentable {
        SLIDER("slider"),FINE_TUNE("fine_tune"),EDIT("edit");

        private final String name;
        private final Component symbol;
        Mode(String name){
            this.name = name;
            this.symbol = Component.translatable("gui.showBlockScreen.mode."+name);
        }
        @Override
        public String getSerializedName() { return name; }
        public Component getSymbol(){ return symbol; }
    }
}


/*
this.minecraft.getToasts().addToast(
        SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.NARRATOR_TOGGLE,Component.literal("Hello World!"), Component.literal("This is a toast."))
);
 */