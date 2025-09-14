package com.yuushya.modelling.blockentity.transformData;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

public enum ItemTransformType {
    POS_X(0), POS_Y(1), POS_Z(2),
    ROT_X(3), ROT_Y(4), ROT_Z(5),
    SCALE_X(6), SCALE_Y(7), SCALE_Z(8),
    ITEM_STACK(9),
    SHOWN(10),
    LIT(11),
    REMOVE(12),
    SUCCESS(13), FAIL(14),
    SHAPE(15);

    public final int type;

    ItemTransformType(int i) {
        type = i;
    }

    public static ItemTransformType from(int i) {
        return switch (i) {
            case 0 -> POS_X;
            case 1 -> POS_Y;
            case 2 -> POS_Z;
            case 3 -> ROT_X;
            case 4 -> ROT_Y;
            case 5 -> ROT_Z;
            case 6 -> SCALE_X;
            case 7 -> SCALE_Y;
            case 8 -> SCALE_Z;
            case 9 -> ITEM_STACK;
            case 10 -> SHOWN;
            case 11 -> LIT;
            case 12 -> REMOVE;
            case 13 -> SUCCESS;
            case 15 -> SHAPE;
            default -> FAIL;
        };
    }

    public double extract(TransformItemData transformData){
        return switch (this){
            case POS_X -> transformData.pos.x();
            case POS_Y -> transformData.pos.y();
            case POS_Z -> transformData.pos.z();
            case ROT_X -> transformData.rot.x();
            case ROT_Y -> transformData.rot.y();
            case ROT_Z -> transformData.rot.z();
            case SCALE_X -> transformData.scales.x();
            case SCALE_Y -> transformData.scales.y();
            case SCALE_Z -> transformData.scales.z();
            case ITEM_STACK -> Item.getId(transformData.itemStack.getItem());
            case SHOWN -> transformData.isShown ? 1 : 0;
            case LIT, REMOVE, SUCCESS, SHAPE, FAIL -> 0;
        };
    }

    public void modify(TransformItemData transformData, Double number){
        switch (this){
            case POS_X -> transformData.pos.set(number, transformData.pos.y(), transformData.pos.z());
            case POS_Y -> transformData.pos.set(transformData.pos.x(), number, transformData.pos.z());
            case POS_Z -> transformData.pos.set(transformData.pos.x(), transformData.pos.y(), number);
            case ROT_X -> transformData.rot.set(number.floatValue(), transformData.rot.y(), transformData.rot.z());
            case ROT_Y -> transformData.rot.set(transformData.rot.x(), number.floatValue(), transformData.rot.z());
            case ROT_Z -> transformData.rot.set(transformData.rot.x(), transformData.rot.y(), number.floatValue());
            case SCALE_X -> transformData.scales.set(number.floatValue(), transformData.scales.y(), transformData.scales.z());
            case SCALE_Y -> transformData.scales.set(transformData.scales.x(), number.floatValue(), transformData.scales.z());
            case SCALE_Z -> transformData.scales.set(transformData.scales.x(), transformData.scales.y(), number.floatValue());
            case ITEM_STACK -> transformData.itemStack = new ItemStack(Item.byId((int) Math.round(number)));
            case SHOWN -> transformData.isShown = number != 0;
            case FAIL -> {
            }
        }
    }

    public double extract(ItemBlockEntity itemBlockEntity, int slot){
        if(this == LIT){
            return itemBlockEntity.getBlockState().getValue(YuushyaBlockStates.LIT);
        }
        return extract(itemBlockEntity.getTransformData(slot));
    }

    public BlockShape extractShape(ItemBlockEntity itemBlockEntity) {
        return itemBlockEntity.getBlockState().getValue(SHAPES);
    }

    public void modify(ItemBlockEntity itemBlockEntity, int slot, double number){
        if (itemBlockEntity.getLevel() == null) return;
        if(this == SUCCESS){
            itemBlockEntity.setChanged();
            return;
        }
        if(this == LIT){
            Level level = itemBlockEntity.getLevel();
            level.setBlock(itemBlockEntity.getBlockPos(), itemBlockEntity.getBlockState().setValue(YuushyaBlockStates.LIT,(int) Math.round(number)), 18);
            return;
        }
        if(this == REMOVE){
            itemBlockEntity.removeTransformData(slot);
            return;
        }
        if (this == SHAPE) {
            Level level = itemBlockEntity.getLevel();
            level.setBlock(itemBlockEntity.getBlockPos(), itemBlockEntity.getBlockState().setValue(SHAPES, BlockShape.values()[(int) number]), 18);
            return;
        }
        itemBlockEntity.setSlot(slot);
        modify(itemBlockEntity.getTransformData(slot), number);
    }
}