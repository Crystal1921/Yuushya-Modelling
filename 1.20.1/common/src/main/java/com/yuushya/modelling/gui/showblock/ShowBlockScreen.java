package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.blockentity.TransformType;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.SliderButton;
import com.yuushya.modelling.gui.validate.UnitDoubleRange;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

import static com.yuushya.modelling.blockentity.TransformType.*;

public class ShowBlockScreen extends Screen {
    private final ShowBlockEntity blockEntity;
    private int slot = 0;
    private final Map<TransformType,Double> storage = new HashMap<>();
    private final Map<TransformType, SliderButton<Double>> widgets = new HashMap<>();

    public ShowBlockScreen(ShowBlockEntity blockEntity) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
    }

    private static final int LEFT_COLUMN_X = 10;
    private static final int LEFT_COLUMN_WIDTH = 120;
    private static final int TOP = 10;
    private static final int PER_HEIGHT = 20;

    @Override
    protected void init() {
        Button button = Button.builder(Component.literal("test"),(btn)->{
            widgets.get(ROT_X).setValue(0.5);
        }).bounds(LEFT_COLUMN_X,TOP,LEFT_COLUMN_WIDTH,PER_HEIGHT).build();

        widgets.put(ROT_X,
            UnitDoubleRange.buttonBuilder(Component.literal("Hello2"),
                        (number)->{
                            blockEntity.setShowRotAixs();
                            updateTransformData(ROT_X,360*number);
                        })
                    .initial(ROT_X.extract(blockEntity,slot)/360)
                    .bounds(LEFT_COLUMN_X,TOP+PER_HEIGHT,LEFT_COLUMN_WIDTH,PER_HEIGHT).build());


        this.addRenderableWidget(button);
        for(AbstractWidget widget:widgets.values()){
            this.addRenderableWidget(widget);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)this.width / 2.0f, 90.0f, 50.0f);
        guiGraphics.pose().scale(16.0f, 16.0f, 16.0f);
//        BlockState blockState = this.blockEntity.getTransFormDataNow().blockState;
//        BlockRenderDispatcher blockRenderDispatcher = this.minecraft.getBlockRenderer();
//        BakedModel model = blockRenderDispatcher.getBlockModel(blockState);
//        this.minecraft.getItemRenderer().render(Blocks.SAND.asItem().getDefaultInstance(), ItemDisplayContext.GUI, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(this.font,"Special Button", 40, 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);
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