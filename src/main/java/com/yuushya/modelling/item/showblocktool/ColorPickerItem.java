package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.ClientMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.*;
import java.util.function.Consumer;

import static com.yuushya.modelling.utils.ClientMethod.setClipboard;

public class ColorPickerItem extends AbstractToolItem {
    public ColorPickerItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    public InteractionResult inMainHandRightClickInAir(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (level.isClientSide()) {
            ClientMethod.pickColor(level, handItemStack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    //对方块主手右键
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (level.isClientSide()) {
            ClientMethod.pickColor(level, handItemStack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    //对方块主手左键
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack) {
        if (level.isClientSide()) {
            Integer i = handItemStack.get(DataComponentRegistry.COLOR_DATA);
            if (i != null) {
                setClipboard(String.format("#%06X", i));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        Integer i = itemStack.get(DataComponentRegistry.COLOR_DATA);
        if (i != null) {
            String hex = String.format("#%08X", i);
            builder.accept(Component.literal(hex).withColor(i));
        } else {
            builder.accept(Component.translatable("item.yuushya.color_picker.none").withColor(Color.LIGHT_GRAY.getRGB()));
        }

        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }
}
