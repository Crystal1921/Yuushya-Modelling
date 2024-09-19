package com.yuushya.modelling.gui.showblock;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class EditScreen extends Screen {

    private Button selectButton;

    private EditBox editBox;
    private final Component editBoxLabel;
    private final Consumer<String> callback;
    private final Predicate<String> isValidText;
    private final Screen lastScreen;

    public EditScreen(Screen lastScreen, Component title, Component editBoxLabel, Consumer<String> callback, Predicate<String> isValidText) {
        super(title);
        this.lastScreen = lastScreen;
        this.editBoxLabel = editBoxLabel;
        this.isValidText = isValidText;
        this.callback = callback;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.selectButton.active && this.getFocused() == this.editBox && (keyCode == 257 || keyCode == 335)) {
            this.onSelect();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        this.editBox = new EditBox(this.font, this.width / 2 - 100, 116, 200, 20, this.editBoxLabel);
        this.editBox.setMaxLength(128);
        this.editBox.setResponder(string -> this.updateSelectButtonStatus());
        this.addWidget(this.editBox);
        this.selectButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onSelect()).bounds(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.callback.accept(null)).bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20).build());
        this.updateSelectButtonStatus();
        this.setInitialFocus(this.editBox);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String string = this.editBox.getValue();
        this.init(minecraft, width, height);
        this.editBox.setValue(string);
    }

    private void onSelect() {
        this.callback.accept(this.editBox.getValue());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void removed() {

    }

    private void updateSelectButtonStatus() {
        this.selectButton.active = isValidText.test(this.editBox.getValue());
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack,this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawString(poseStack,this.font, this.editBoxLabel, this.width / 2 - 100 + 1, 100, 0xA0A0A0);
        this.editBox.render(poseStack, mouseX, mouseY, partialTick);
    }
}
