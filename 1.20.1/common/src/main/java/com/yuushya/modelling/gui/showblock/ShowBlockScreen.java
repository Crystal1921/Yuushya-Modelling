package com.yuushya.modelling.gui.showblock;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.blockentity.TransformType;
import com.yuushya.modelling.blockentity.iTransformDataInventory;
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
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

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
                    .bounds(sliderButton.getX()-SMALL_BUTTON_WIDTH,sliderButton.getY(),SMALL_BUTTON_WIDTH,PER_HEIGHT).build();
            addButton = Button.builder(Component.literal("+"), (btn)-> step(true))
                    .bounds(sliderButton.getX()+sliderButton.getWidth(),sliderButton.getY(),SMALL_BUTTON_WIDTH,PER_HEIGHT).build();
            editBox = new EditBox(font,sliderButton.getX() ,sliderButton.getY() , sliderButton.getWidth(), PER_HEIGHT, sliderButton.getCaption());
            editBox.setMaxLength(15);
            setEditBoxInitial();

            cancelButton = Button.builder(Component.literal("x"), (btn)-> setEditBoxInitial())
                    .bounds(sliderButton.getX()-SMALL_BUTTON_WIDTH,sliderButton.getY(),SMALL_BUTTON_WIDTH,PER_HEIGHT).build();
            finishButton = Button.builder(Component.literal("v"), (btn)-> saveEditBoxValue())
                    .bounds(sliderButton.getX()+sliderButton.getWidth(),sliderButton.getY(),SMALL_BUTTON_WIDTH,PER_HEIGHT).build();
            triggerVisible(true);
        }
    }

    private CycleButton<Mode> modeButton;
    private Button addStateButton;
    private Button removeStateButton;
    private Button copyButton;
    private Button parseButton;
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

    private static final int RIGHT_COLUMN_X = 2;
    private static final int RIGHT_BAR_WIDTH = PER_HEIGHT;
    private static final int SMALL_BUTTON_WIDTH = 10;
    private static final int RIGHT_LIST_WIDTH = 40;
    private static final int RIGHT_LIST_PER_HEIGHT = 45;
    private static final int RIGHT_LIST_TOP = TOP+PER_HEIGHT+5;
    private static final int RIGHT_LIST_HEIGHT = 3* RIGHT_LIST_PER_HEIGHT+2;
    private static final int RIGHT_LIST_BOTTOM = RIGHT_LIST_TOP + RIGHT_LIST_HEIGHT;
    private static final int RIGHT_STATE_INFORM_X = RIGHT_COLUMN_X+RIGHT_LIST_WIDTH +3;
    private static final int RIGHT_STATE_PANEL_Y = RIGHT_LIST_BOTTOM + 5;

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
                .bounds(RIGHT_COLUMN_X,TOP,RIGHT_BAR_WIDTH,PER_HEIGHT).build();
        removeStateButton = Button.builder(Component.literal("X"),
                        (btn)->{
                            updateTransformData(REMOVE,0.0);
                        }
                )
                .bounds(RIGHT_COLUMN_X+RIGHT_BAR_WIDTH,TOP,RIGHT_BAR_WIDTH,PER_HEIGHT).build();

        shownStateButton = CycleButton.<Boolean>booleanBuilder(Component.literal("On"),Component.literal("Off"))
                .displayOnlyValue()
                .withInitialValue(true)
                .create(RIGHT_COLUMN_X+RIGHT_BAR_WIDTH+RIGHT_BAR_WIDTH,TOP,RIGHT_BAR_WIDTH,PER_HEIGHT,Component.empty(),
                        (btn,bl)->{
                            updateTransformData(SHOWN,bl?1.0:0.0);
                        }
                );

        copyButton = Button.builder(Component.literal("Copy"),
                        (btn)->{
                            CompoundTag compoundTag = new CompoundTag();
                            iTransformDataInventory.saveAdditional(compoundTag, blockEntity.getTransformDatas());
                            String res = NbtUtils.structureToSnbt(compoundTag);
                            setClipboard(res);
                        }
                )
                .bounds(RIGHT_COLUMN_X+RIGHT_BAR_WIDTH*2+40,TOP,40,PER_HEIGHT).build();

        parseButton = Button.builder(Component.literal("paste"),
                        (btn)->{
                            String string = getClipboard();
                            try {
                                CompoundTag compoundTag = NbtUtils.snbtToStructure(string);
                                updateAllTransformData(compoundTag);
                            } catch (CommandSyntaxException e) {
                                this.minecraft.getToasts().addToast(
                                        SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.PACK_LOAD_FAILURE,Component.literal("Parsing Error"), Component.literal(e.getMessage()))
                                );
                            }
                        }
                )
                .bounds(RIGHT_COLUMN_X+RIGHT_BAR_WIDTH*2+80,TOP,40,PER_HEIGHT).build();

        blockStateList =  new BlockStateIconList(this.minecraft,RIGHT_LIST_WIDTH ,RIGHT_LIST_HEIGHT ,RIGHT_COLUMN_X,RIGHT_LIST_TOP,RIGHT_LIST_BOTTOM , RIGHT_LIST_WIDTH,RIGHT_LIST_PER_HEIGHT,this.blockEntity.getTransformDatas(),this);

        leftPropertyButton = Button.builder(Component.literal("<"),
                        (btn)->{
                            property = YuushyaBlockStates.getRelative(blockStateList.updateRenderProperties(getBlockState()), property, true);
                        })
                .bounds(RIGHT_COLUMN_X,RIGHT_STATE_PANEL_Y,SMALL_BUTTON_WIDTH,PER_HEIGHT)
                .build();
        rightPropertyButton = Button.builder(Component.literal(">"),
                        (btn)->{
                            property = YuushyaBlockStates.getRelative(blockStateList.updateRenderProperties(getBlockState()), property, false);
                        })
                .bounds(RIGHT_COLUMN_X+RIGHT_LIST_WIDTH/2*3,RIGHT_STATE_PANEL_Y,SMALL_BUTTON_WIDTH,PER_HEIGHT)
                .build();
        leftStateButton = Button.builder(Component.literal("<"),
                        (btn)->{
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, true);
                            updateTransformData(BLOCK_STATE,(double)Block.getId(nextBlockState));
                        })
                .bounds(RIGHT_COLUMN_X,RIGHT_STATE_PANEL_Y+PER_HEIGHT,SMALL_BUTTON_WIDTH,PER_HEIGHT)
                .build();
        rightStateButton = Button.builder(Component.literal(">"),
                        (btn)->{
                            BlockState nextBlockState = YuushyaBlockStates.cycleState(getBlockState(), property, true);
                            updateTransformData(BLOCK_STATE,(double)Block.getId(nextBlockState));
                        })
                .bounds(RIGHT_COLUMN_X+RIGHT_LIST_WIDTH/2*3,RIGHT_STATE_PANEL_Y+PER_HEIGHT,SMALL_BUTTON_WIDTH,PER_HEIGHT)
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
        this.addRenderableWidget(copyButton);
        this.addRenderableWidget(parseButton);

        blockStateList.setSelectedSlot(slot);//updateStateButtonVisible();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.blockStateList.render(guiGraphics,mouseX,mouseY,partialTick);
        BlockState blockState = getBlockState();
        guiGraphics.drawString(this.font,this.blockStateList.updateRenderDisplayName(blockState), RIGHT_STATE_INFORM_X, TOP +6 + PER_HEIGHT, 0xFFFFFFFF, false);
        List<String> properties = this.blockStateList.updateRenderBlockStateProperties(blockState);
        for(int i=0;i<properties.size();i++){
            MutableComponent displayBlockState = Component.literal(properties.get(i));
            guiGraphics.drawString(this.font, displayBlockState, RIGHT_STATE_INFORM_X, TOP + 6 + PER_HEIGHT + this.font.lineHeight*(i+1)+1, 0xFFEBC6, false);
        }
        if(updateStateButtonVisible()){
            guiGraphics.drawString(this.font, property.getName(), RIGHT_COLUMN_X+RIGHT_LIST_WIDTH/2,RIGHT_STATE_PANEL_Y+5, 0xFFFFFFFF, false);
            guiGraphics.drawString(this.font, YuushyaDebugStickItem.getNameHelper(blockState,property), RIGHT_COLUMN_X+RIGHT_LIST_WIDTH/2,RIGHT_STATE_PANEL_Y+5+PER_HEIGHT, 0xFFFFFFFF, false);
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

    private void updateAllTransformData(CompoundTag compoundTag){
        List<TransformData> dataList = blockEntity.getTransformDatas();
        iTransformDataInventory.load(compoundTag,dataList);
        this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        this.storage.clear();
        for(int slot=0;slot<dataList.size();slot++){
            TransformData data = dataList.get(slot);
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, POS_X,data.pos.x);
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, POS_Y,data.pos.y);
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, POS_Z,data.pos.z);

            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, ROT_X,data.rot.x);
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, ROT_Y,data.rot.y);
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, ROT_Z,data.rot.z);

            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, SCALE_X,data.scales.x);
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, BLOCK_STATE,Block.getId(data.blockState) );
            TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),slot, SHOWN,data.isShown?1:0 );
        }
        TransformDataNetwork.sendToServerSideSuccess(blockEntity.getBlockPos());
        this.blockStateList.updateRenderList();
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

    private void setClipboard(String clipboardValue) {
        if (this.minecraft != null) {
            TextFieldHelper.setClipboardContents(this.minecraft, clipboardValue);
        }
    }

    private String getClipboard() {
        return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
    }
}
