package com.yuushya.modelling.item.showblocktool;

import com.mojang.blaze3d.platform.Window;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.registries.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;

import static com.yuushya.modelling.gui.itemblock.ItemBlockScreen.setClipboard;

public class ColorPickerItem extends AbstractToolItem {
    public ColorPickerItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    private static void pickColor(Level level, ItemStack handItemStack) {
        if (level instanceof ClientLevel) {
            Window window = Minecraft.getInstance().getWindow();
            int width = window.getScreenWidth();
            int height = window.getScreenHeight();
            // 计算屏幕中心坐标
            int centerX = width / 2;
            int centerY = height / 2;

            // 创建一个缓冲区用来存放像素数据（RGBA 各1字节）
            ByteBuffer buffer = BufferUtils.createByteBuffer(4);

            // 从当前帧缓冲读取中心像素颜色
            GL11.glReadPixels(centerX, centerY, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            // 取出像素值（注意 glReadPixels 的原点在左下角）
            int r = (255 - buffer.get(0)) & 0xFF;
            int g = (255 - buffer.get(1)) & 0xFF;
            int b = (255 - buffer.get(2)) & 0xFF;
            int color = (r << 16) | (g << 8) | b;

            handItemStack.set(DataComponentRegistry.COLOR_DATA, color);

            setClipboard(String.format("#%06X", color));
        }
    }

    public InteractionResult inMainHandRightClickInAir(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        pickColor(level, handItemStack);
        return InteractionResult.SUCCESS;
    }

    //对方块主手右键
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        pickColor(level, handItemStack);
        return InteractionResult.SUCCESS;
    }

    //对方块主手左键
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        Integer i = handItemStack.get(DataComponentRegistry.COLOR_DATA);
        if (i != null) {
            setClipboard(String.format("#%06X", i));
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, Item.TooltipContext context, @NotNull List<Component> tooltips, @NotNull TooltipFlag tooltipFlag) {
        Integer i = itemStack.get(DataComponentRegistry.COLOR_DATA);
        if (i != null) {
            String hex = String.format("#%08X", i);
            tooltips.add(Component.literal(hex).withColor(i));
        } else {
            tooltips.add(Component.translatable("item.yuushya.color_picker.none").withColor(Color.LIGHT_GRAY.getRGB()));
        }

        super.appendHoverText(itemStack, context, tooltips, tooltipFlag);
    }
}
