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
import net.minecraft.client.gui.Font;
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
            TransformDataNetwork.sendToServerSide(this.blockEntity.getBlockPos(),this.slot, key,this.storage.get(key));
        }
        this.storage.clear();
        this.slot = slot;
        this.blockEntity.setSlot(slot);
        for(TransformComponent component: this.panel.values()){
            component.setSliderInitial(this.blockEntity,this.slot);
        }
        updateStateButtonVisible();
    }
    private final Map<TransformType,Double> storage = new HashMap<>();

    private final Map<TransformType, TransformComponent> panel = new HashMap<>();
    private TransformComponent choose(TransformType type){
        return panel.computeIfAbsent(type, TransformComponent::new);
    }
    public static final class TransformComponent {
        TransformType type;
        double standardStep;
        double fine_tuneStep = 0.001;
        SliderButton<Double> sliderButton;
        Button minusButton;
        Button addButton;
        EditBox editBox;
        Button cancelButton;
        Button finishButton;
        TransformComponent(TransformType type){
            this.type = type;
        }

        double setStandardStep(double step){
            this.standardStep = step;
            return step;
        }
        void setSliderInitial(ShowBlockEntity blockEntity,int slot ){ sliderButton.setValidatedValue(type.extract(blockEntity,slot));}
        void setSliderStep(){ sliderButton.setStep(standardStep); }
        void setSliderFineTune(){ sliderButton.setStep(fine_tuneStep); }
        void step( boolean increase){
            if(increase) sliderButton.setValidatedValue(sliderButton.getValidatedValue()+sliderButton.getStep());
            else sliderButton.setValidatedValue(sliderButton.getValidatedValue()-sliderButton.getStep());
        }
        void setEditBoxInitial(){ editBox.setValue(String.valueOf(sliderButton.getValidatedValue()));}
        void saveEditBoxValue(){
            double number;
            try{
                number =  Double.parseDouble(editBox.getValue());
            } catch (NumberFormatException ignored){
                number = sliderButton.getValidatedValue();
            }
            sliderButton.setValidatedValue(number);
            setEditBoxInitial();
        }
        void triggerVisible(boolean sliderVisible){
            sliderButton.visible = sliderVisible;
            addButton.visible = sliderVisible;
            minusButton.visible = sliderVisible;
            editBox.setVisible(!sliderVisible);
            finishButton.visible = !sliderVisible;
            cancelButton.visible = !sliderVisible;
        }

        void initWidget(Font font){
            minusButton = Button.builder(Component.literal("-"), (btn)-> step(false))
                    .bounds(sliderButton.getX()-10,sliderButton.getY(),10,20).build();
            addButton = Button.builder(Component.literal("+"), (btn)-> step(true))
                    .bounds(sliderButton.getX()+sliderButton.getWidth(),sliderButton.getY(),10,20).build();
            editBox = new EditBox(font,sliderButton.getX() ,sliderButton.getY() , sliderButton.getWidth(), PER_HEIGHT, sliderButton.getCaption());
            editBox.setMaxLength(15);
            setEditBoxInitial();

            cancelButton = Button.builder(Component.literal("x"), (btn)-> setEditBoxInitial())
                    .bounds(sliderButton.getX()-10,sliderButton.getY(),10,20).build();
            finishButton = Button.builder(Component.literal("v"), (btn)-> saveEditBoxValue())
                    .bounds(sliderButton.getX()+sliderButton.getWidth(),sliderButton.getY(),10,20).build();
            triggerVisible(true);
        }
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
                                        case SLIDER -> panel.values().forEach(TransformComponent::setSliderStep);
                                        case EDIT,FINE_TUNE -> panel.values().forEach(TransformComponent::setSliderFineTune);
                                    }
                                    switch (mode){
                                        case SLIDER,FINE_TUNE -> panel.values().forEach((it)->it.triggerVisible(true));
                                        case EDIT -> panel.values().forEach((it)->it.triggerVisible(false));
                                    }
                                }
                        );

        choose(POS_X).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.pos_text"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.x)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.x)-1,
                                (number)->{updateTransformData(POS_X,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x",String.format("%05.1f",number))))
                        .step(choose(POS_X).setStandardStep(1.0))
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(0,0) , leftColumnWidth(),PER_HEIGHT).build();

        choose(POS_Y).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.pos_text"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.y)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.y)-1,
                                (number)->{updateTransformData(POS_Y,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y",String.format("%05.1f",number))))
                        .step(choose(POS_Y).setStandardStep(1.0))
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_Y.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(1,0) , leftColumnWidth(),PER_HEIGHT).build();

        choose(POS_Z).sliderButton =
                LazyDoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.pos_text"),
                                ()-> (double) -getMaxPos(blockEntity.getTransformData(slot).scales.z)+1,
                                ()-> (double) getMaxPos(blockEntity.getTransformData(slot).scales.z)-1,
                                (number)->{updateTransformData(POS_Z,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z",String.format("%05.1f",number))))
                        .step(choose(POS_Z).setStandardStep(1.0))
                        .onMouseOver((btn)->{blockEntity.setShowPosAixs();})
                        .initial(POS_Z.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(2,0) , leftColumnWidth(),PER_HEIGHT).build();

        choose(ROT_X).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.rot_text"),0.0,360.0,
                            (number)->{updateTransformData(ROT_X,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.x",String.format("%05.1f",number))))
                        .step(choose(ROT_X).setStandardStep(22.5))
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(3,10) , leftColumnWidth(),PER_HEIGHT).build();

        choose(ROT_Y).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.rot_text"),0.0,360.0,
                                (number)->{updateTransformData(ROT_Y,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.y",String.format("%05.1f",number))))
                        .step(choose(ROT_Y).setStandardStep(22.5))
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_Y.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(4,10) , leftColumnWidth(),PER_HEIGHT).build();

        choose(ROT_Z).sliderButton =
                DoubleRange.buttonBuilder(Component.translatable("block.yuushya.showblock.rot_text"),0.0,360.0,
                                (number)->{updateTransformData(ROT_Z,number);})
                        .text((caption,number)->Component.empty().append(caption).append(Component.translatable("block.yuushya.showblock.z",String.format("%05.1f",number))))
                        .step(choose(ROT_Z).setStandardStep(22.5))
                        .onMouseOver((btn)->{blockEntity.setShowRotAixs();})
                        .initial(ROT_Z.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(5,10) , leftColumnWidth(),PER_HEIGHT).build();

        choose(SCALE_X).sliderButton =
                DividedDoubleRange.buttonBuilder(Component.empty(),0.0,1.0,10.0,
                                (number)->{
                                    updateTransformData(SCALE_X,number);
                                    updateTransformData(SCALE_Y,number);
                                    updateTransformData(SCALE_Z,number);
                                })
                        .text((caption,number)->Component.translatable("block.yuushya.showblock.scale_text",String.format("%05.1f",number)))
                        .step(choose(SCALE_X).setStandardStep(0.1))
                        .initial(SCALE_X.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(6,20) , leftColumnWidth(),PER_HEIGHT).build();

        choose(LIT).sliderButton =
                DoubleRange.buttonBuilder(Component.literal("LIT"),0.0,15.0,
                        (number)->{
                            updateTransformData(LIT,number);
                        })
                        .text(LazyDoubleRange::captionToString)
                        .step(choose(LIT).setStandardStep(1))
                        .initial(LIT.extract(blockEntity,slot))
                        .bounds(leftColumnX(),top(7,30),leftColumnWidth(),PER_HEIGHT).build();

        for(TransformComponent component:this.panel.values()){
            component.initWidget(this.font);
            this.addRenderableWidget(component.sliderButton);
            this.addRenderableWidget(component.minusButton);
            this.addRenderableWidget(component.addButton);
            this.addRenderableWidget(component.editBox);
            this.addRenderableWidget(component.cancelButton);
            this.addRenderableWidget(component.finishButton);
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
        this.blockStateList.render(guiGraphics,mouseX,mouseY,partialTick);
        BlockState blockState = getBlockState();
        guiGraphics.drawString(this.font,this.blockStateList.updateRenderDisplayName(blockState), 45, TOP +6+20, 0xFFFFFFFF, false);
        List<String> properties = this.blockStateList.updateRenderBlockStateProperties(blockState);
        for(int i=0;i<properties.size();i++){
            MutableComponent displayBlockState = Component.literal(properties.get(i));
            guiGraphics.drawString(this.font, displayBlockState, 45, TOP +6+20 + this.font.lineHeight*(i+1)+1, 0xFFEBC6, false);
        }
        if(updateStateButtonVisible()){
            guiGraphics.drawString(this.font, property.getName(), 62,TOP + 115, 0xFFFFFFFF, false);
            guiGraphics.drawString(this.font, YuushyaDebugStickItem.getNameHelper(blockState,property), 62,TOP + 135, 0xFFFFFFFF, false);
        }
        if(modeButton.getValue() == Mode.EDIT){
            for(TransformComponent component:this.panel.values()){
                guiGraphics.drawString(this.font,component.editBox.getMessage(),component.editBox.getX()+component.editBox.getWidth()/2,component.editBox.getY()+component.editBox.getHeight()/3,0x707070);
                component.editBox.render(guiGraphics,mouseX,mouseY,partialTick);
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }


    @Override
    public void tick() {
        for(TransformComponent component:this.panel.values()){
            component.editBox.tick();
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