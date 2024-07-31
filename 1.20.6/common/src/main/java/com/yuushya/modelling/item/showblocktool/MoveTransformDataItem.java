package com.yuushya.modelling.item.showblocktool;

import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.item.AbstractToolItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MoveTransformDataItem extends AbstractToolItem {
    private final TransformData transformData=new TransformData();
    public MoveTransformDataItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockStateTarget, Level level, BlockPos blockPos, ItemStack handItemStack){
        //右手右键复制内容，以及清空展示方块内的东西//with main hand right-click can read
        getTag(level,handItemStack);
        if(blockStateTarget.getBlock() instanceof ShowBlock){
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            BlockState blockStateShowBlock =showBlockEntity.getTransFormDataNow().blockState;
            if (!(blockStateShowBlock.getBlock() instanceof AirBlock)){
                transformData.set(showBlockEntity.getTransFormDataNow());
                showBlockEntity.removeTransFormDataNow();
                showBlockEntity.saveChanged();
            }
            else {
                player.displayClientMessage(Component.translatable(this.getDescriptionId()+".mainhand.pass"), true);
                return InteractionResult.PASS;
            }
        }
        else {
            transformData.set();
            transformData.blockState= blockStateTarget;
        }
        setTag(handItemStack);
        player.displayClientMessage(Component.translatable(this.getDescriptionId()+".mainhand.success"),true);
        return InteractionResult.SUCCESS;
    }
    @Override
    public InteractionResult inOffHandRightClickOnBlock(Player player, BlockState blockStateTarget, Level level, BlockPos blockPos, ItemStack handItemStack){
        //左手右键放置状态到展示方块里//with off hand right-click can put all state to showblock
        getTag(level,handItemStack);
        if(transformData.blockState.getBlock() instanceof AirBlock){
            player.displayClientMessage(Component.translatable(this.getDescriptionId()+".offhand.fail"), true);
            return InteractionResult.SUCCESS;
        }
        if(blockStateTarget.getBlock() instanceof ShowBlock){
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            showBlockEntity.getTransFormDataNow().set(transformData);
            showBlockEntity.saveChanged();
            player.displayClientMessage(Component.translatable(this.getDescriptionId()+".offhand.success"), true);
            return InteractionResult.SUCCESS;
        } else {return InteractionResult.PASS;}
    }

    //method for readNbt and writeNbt
    public void getTag(Level level,ItemStack itemStack){
        CustomData customData = itemStack.getOrDefault((DataComponentType<CustomData>) YuushyaRegistries.TRANSFORM_DATA.get(), CustomData.EMPTY);
        CompoundTag compoundTag = customData.copyTag();
        if(compoundTag.contains("TransformData")){
            transformData.load(compoundTag.getCompound("TransformData"));
        }
        else{ // to load the 1.20.4 below data //TODO: will remove in next version
            customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            compoundTag = customData.copyTag();
            if(compoundTag.contains("TransformData")){
                transformData.load(compoundTag.getCompound("TransformData"));
            }
        }
    }
    public void setTag(ItemStack itemStack){
        CompoundTag transformDataTag=new CompoundTag();
        transformData.saveAdditional(transformDataTag);

        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put("TransformData",transformDataTag);
        itemStack.set((DataComponentType<CustomData>) YuushyaRegistries.TRANSFORM_DATA.get(), CustomData.of(compoundTag));
    }

}