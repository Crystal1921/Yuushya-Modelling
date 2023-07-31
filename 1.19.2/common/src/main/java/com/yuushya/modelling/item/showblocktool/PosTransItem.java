package com.yuushya.modelling.item.showblocktool;

import com.mojang.math.Vector3d;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.item.AbstractMultiPurposeToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class PosTransItem extends AbstractMultiPurposeToolItem {
    private static final int MAX_POS_1 =17;
    public PosTransItem(Properties properties, Integer tipLines) {
        super(properties, tipLines);
        MAX_FORMS=3;//x:0,y:1,z:2
    }

    @Override
    public InteractionResult inMainHandRightClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        //右手右键 向后位移
        getTag(handItemStack);
        return translateData(player,blockState,level,blockPos,handItemStack,(transformData)->{
            Vector3d pos = transformData.pos;
            switch (getForm()){
                case 0-> pos.x=(pos.x-1)% MAX_POS_1;
                case 1-> pos.y=(pos.y-1)% MAX_POS_1;
                case 2-> pos.z=(pos.z-1)% MAX_POS_1;
            }
            player.displayClientMessage(Component.translatable(this.getDescriptionId()+".switch",pos.x,pos.y,pos.z),true);
        });
    }
    @Override
    public InteractionResult inMainHandLeftClickOnBlock(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack){
        //右手左键 向前位移
        getTag(handItemStack);
        return translateData(player,blockState,level,blockPos,handItemStack,(transformData)->{
            Vector3d pos = transformData.pos;
            switch (getForm()){
                case 0-> pos.x=(pos.x+1)% MAX_POS_1;
                case 1-> pos.y=(pos.y+1)% MAX_POS_1;
                case 2-> pos.z=(pos.z+1)% MAX_POS_1;
            }
            player.displayClientMessage(Component.translatable(this.getDescriptionId()+".switch",pos.x,pos.y,pos.z),true);
        });
    }

    protected static InteractionResult translateData(Player player, BlockState blockState, Level level, BlockPos blockPos, ItemStack handItemStack, Consumer<TransformData> consumer){
        if(blockState.getBlock() instanceof ShowBlock) {
            ShowBlockEntity showBlockEntity = (ShowBlockEntity) level.getBlockEntity(blockPos);
            TransformData transformData=showBlockEntity.getTransFormDataNow();
            consumer.accept(transformData);
            showBlockEntity.saveChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
