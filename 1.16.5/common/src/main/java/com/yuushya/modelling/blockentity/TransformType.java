package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public enum TransformType {
    POS_X(0), POS_Y(1), POS_Z(2),
    ROT_X(3), ROT_Y(4), ROT_Z(5),
    SCALE_X(6), SCALE_Y(7), SCALE_Z(8),
    BLOCK_STATE(9),
    SHOWN(10),
    LIT(11),
    REMOVE(12),
    SUCCESS(13), FAIL(14);

    public final int type;

    TransformType(int i) {
        type = i;
    }

    public static TransformType from(int i) {
        switch (i) {
            case 0:
                return POS_X;
            case 1:
                return POS_Y;
            case 2:
                return POS_Z;
            case 3:
                return ROT_X;
            case 4:
                return ROT_Y;
            case 5:
                return ROT_Z;
            case 6:
                return SCALE_X;
            case 7:
                return SCALE_Y;
            case 8:
                return SCALE_Z;
            case 9:
                return BLOCK_STATE;
            case 10:
                return SHOWN;
            case 11:
                return LIT;
            case 12:
                return REMOVE;
            case 13:
                return SUCCESS;
            default:
                return FAIL;
        }
    }


    public double extract(TransformData transformData){
        switch (this) {
            case POS_X:
                return transformData.pos.x;
            case POS_Y:
                return transformData.pos.y;
            case POS_Z:
                return transformData.pos.z;
            case ROT_X:
                return transformData.rot.x();
            case ROT_Y:
                return transformData.rot.y();
            case ROT_Z:
                return transformData.rot.z();
            case SCALE_X:
                return transformData.scales.x();
            case SCALE_Y:
                return transformData.scales.y();
            case SCALE_Z:
                return transformData.scales.z();
            case BLOCK_STATE:
                return Block.getId(transformData.blockState);
            case SHOWN:
                return transformData.isShown ? 1 : 0;
            case LIT:
            case REMOVE:
            case SUCCESS:
            case FAIL:
                return 0;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void modify(TransformData transformData, double number){
        switch (this) {
            case POS_X:
                transformData.pos = new Vec3(number, transformData.pos.y, transformData.pos.z);
                break;
            case POS_Y:
                transformData.pos = new Vec3(transformData.pos.x, number, transformData.pos.z);
                break;
            case POS_Z:
                transformData.pos = new Vec3(transformData.pos.x, transformData.pos.y, number);
                break;
            case ROT_X:
                transformData.rot.set((float) number, transformData.rot.y(), transformData.rot.z());
                break;
            case ROT_Y:
                transformData.rot.set(transformData.rot.x(), (float) number, transformData.rot.z());
                break;
            case ROT_Z:
                transformData.rot.set(transformData.rot.x(), transformData.rot.y(), (float) number);
                break;
            case SCALE_X:
                transformData.scales.set((float) number, transformData.scales.y(), transformData.scales.z());
                break;
            case SCALE_Y:
                transformData.scales.set(transformData.scales.x(), (float) number, transformData.scales.z());
                break;
            case SCALE_Z:
                transformData.scales.set(transformData.scales.x(), transformData.scales.y(), (float) number);
                break;
            case BLOCK_STATE:
                transformData.blockState = Block.stateById((int) Math.round(number));
                break;
            case SHOWN:
                transformData.isShown = number != 0;
                break;
            //case SUCCESS -> showBlockEntity.saveChanged();
            case FAIL:
                break;
        }
    }


    public double extract(ShowBlockEntity showBlockEntity, int slot){
        if(this == LIT){
            return showBlockEntity.getBlockState().getValue(YuushyaBlockStates.LIT);
        }
        return extract(showBlockEntity.getTransformData(slot));
    }

    public void modify(ShowBlockEntity showBlockEntity, int slot, double number){
        if(this == SUCCESS){
            showBlockEntity.saveChanged();
            return;
        }
        if(this == LIT){
            Level level = showBlockEntity.getLevel();
            level.setBlock(showBlockEntity.getBlockPos(), showBlockEntity.getBlockState().setValue(YuushyaBlockStates.LIT,(int) Math.round(number)), 18);
            return;
        }
        if(this == REMOVE){
            showBlockEntity.removeTransformData(slot);
            return;
        }
        showBlockEntity.setSlot(slot);
        modify(showBlockEntity.getTransformData(slot),number);
    }
}
