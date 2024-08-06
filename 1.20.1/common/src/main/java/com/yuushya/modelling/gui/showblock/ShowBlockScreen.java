package com.yuushya.modelling.gui.showblock;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.validate.UnitDoubleRange;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

                    this.minecraft.getToasts().addToast(
                            SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.NARRATOR_TOGGLE,Component.literal(number.toString()), Component.literal(number.toString()))
                    );
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
        BlockState blockState = this.blockEntity.getTransFormDataNow().blockState;
        BlockRenderDispatcher blockRenderDispatcher = this.minecraft.getBlockRenderer();
        BakedModel model = blockRenderDispatcher.getBlockModel(blockState);
        //blockRenderDispatcher.getModelRenderer().renderModel(guiGraphics.pose().last(),guiGraphics.bufferSource().getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)),blockState,model,0,0,0,0xF000F0, OverlayTexture.NO_OVERLAY);
        this.minecraft.getItemRenderer().render(blockState.getBlock().asItem().getDefaultInstance(), ItemDisplayContext.GUI, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(this.font,"Special Button", 40, 40 - this.font.lineHeight - 10, 0xFFFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
