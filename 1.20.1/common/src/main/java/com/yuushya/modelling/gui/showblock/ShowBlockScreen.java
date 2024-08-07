package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.blockentity.TransformDataNetwork;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.validate.UnitDoubleRange;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class ShowBlockScreen extends Screen {
    private final ShowBlockEntity blockEntity;

    public ShowBlockScreen(ShowBlockEntity blockEntity) {
        super(GameNarrator.NO_TITLE);
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        Button buttonWidget = Button.builder(Component.literal("Hello World"), (btn) -> {
            // When the button is clicked, we can display a toast to the screen.
            this.minecraft.getToasts().addToast(
                    SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.NARRATOR_TOGGLE,Component.literal("Hello World!"), Component.literal("This is a toast."))
            );
        }).bounds(40, 40, 120, 20).build();

        AbstractWidget testWidget = UnitDoubleRange.createButton(Component.literal("Hello2"),
                40,60,120,20,(number)->{
                    TransformDataNetwork.updateTransformData(blockEntity,0,TransformDataNetwork.TransformType.ROT_X,number*360f);
                    this.blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                    TransformDataNetwork.sendToServerSide(blockEntity.getBlockPos(),0, TransformDataNetwork.TransformType.ROT_X,number*360f);
                });
        // x, y, width, height
        // It's recommended to use the fixed height of 20 to prevent rendering issues with the button
        // textures.

        // Register the button widget.

        this.addRenderableWidget(buttonWidget);
        this.addRenderableWidget(testWidget);

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
        TransformDataNetwork.sendToServerSideSuccess(blockEntity.getBlockPos());
    }
}
