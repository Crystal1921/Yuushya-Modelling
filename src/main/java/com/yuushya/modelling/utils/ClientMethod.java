package com.yuushya.modelling.utils;

import com.mojang.blaze3d.platform.Window;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.network.PickColorPacket;
import com.yuushya.modelling.registries.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClientMethod {
    public static final Path ITEM_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("modellings").resolve("items");
    public static final Path BLOCK_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("modellings").resolve("blocks");
    public static final Path TEXT_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("modellings").resolve("texts");

    public static void openGuiScreen(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        ItemStack newItemStack = player.getItemInHand(InteractionHand.OFF_HAND);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        BlockState newBlockState = null;

        if (newItemStack.getItem() instanceof GetBlockStateItem) {
            newBlockState = newItemStack.getOrDefault(DataComponentRegistry.BLOCKSTATE.get(), Blocks.AIR.defaultBlockState());
        } else if (newItemStack.getItem() instanceof BlockItem item) {
            newBlockState = item.getBlock().defaultBlockState();
        }
        if (!newItemStack.isEmpty()) {
            newItemStack = newItemStack.copy();
        }

        if (blockEntity instanceof ShowBlockEntity showBlockEntity) {
            Minecraft.getInstance().setScreen(
                    new ShowBlockScreen(showBlockEntity, newBlockState)
            );
        } else if (blockEntity instanceof ItemBlockEntity itemBlockEntity) {
            Minecraft.getInstance().setScreen(
                    new ItemBlockScreen(itemBlockEntity, newItemStack)
            );
        } else if (blockEntity instanceof TextBlockEntity textBlockEntity) {
            // For TextBlockEntity, we can optionally pass initial text lines
            List<String> newTextLines = new ArrayList<>();
            Minecraft.getInstance().setScreen(
                    new TextBlockScreen(textBlockEntity, newTextLines)
            );
        }
    }

    public static String getClipboard() {
        Minecraft mc = Minecraft.getInstance();
        return TextFieldHelper.getClipboardContents(mc);
    }

    public static void setClipboard(String clipboardValue) {
        Minecraft mc = Minecraft.getInstance();
        TextFieldHelper.setClipboardContents(mc, clipboardValue);
    }

    public static void pickColor(Level level, ItemStack handItemStack) {
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

        PacketDistributor.sendToServer(new PickColorPacket(color));

        setClipboard(String.format("#%06X", color));
    }
}
