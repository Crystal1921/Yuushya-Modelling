package com.yuushya.modelling.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AbstractToolItem extends AbstractYuushyaItem{
    public AbstractToolItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 119.5F;
    }

    @Override
    public boolean canDestroyBlock(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            InteractionResult result = inMainHandLeftClickOnBlock(player, blockState, level, blockPos, player.getItemInHand(InteractionHand.MAIN_HAND));
            if (!level.isClientSide()&&result.consumesAction()) {
                level.playSound(null, blockPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1f, 0.2f);
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        BlockPos blockPos = player.blockPosition();
        ItemStack handItemStack = player.getItemInHand(hand);

        InteractionResult resultHolder;
        if (hand == InteractionHand.OFF_HAND)
            resultHolder = inOffHandRightClickInAir(player,level.getBlockState(blockPos),level,blockPos,handItemStack);
        else
            resultHolder = inMainHandRightClickInAir(player,level.getBlockState(blockPos),level,blockPos,handItemStack);

        if (!level.isClientSide() && resultHolder.consumesAction()){
            level.playSound(null,blockPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS,1f,0.2f);
        }
        return resultHolder;
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        Level level = useOnContext.getLevel();
        InteractionHand hand= useOnContext.getHand();
        BlockPos blockPos = useOnContext.getClickedPos();

        InteractionResult result;
        if (hand == InteractionHand.OFF_HAND)
            result = inOffHandRightClickOnBlock(player, level.getBlockState(blockPos), level, blockPos, useOnContext.getItemInHand());
        else
            result = inMainHandRightClickOnBlock(player, level.getBlockState(blockPos), level, blockPos, useOnContext.getItemInHand());

        if (!level.isClientSide() && result.consumesAction()){
            level.playSound(null,blockPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS,1f,0.2f);
        }
        return result;
    }

    //对方块主手右键
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        return InteractionResult.PASS;
    }
    //对方块主手左键
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        return InteractionResult.PASS;
    }
    //对方块副手右键
    public InteractionResult inOffHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        return InteractionResult.PASS;
    }
    //对空气主手右键
    public InteractionResult inMainHandRightClickInAir(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        return InteractionResult.PASS;
    }
    //对空气副手右键
    public InteractionResult inOffHandRightClickInAir(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        return InteractionResult.PASS;
    }

}

