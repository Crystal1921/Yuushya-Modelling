package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.world.level.block.Block;

public enum TransformType {
    POS_X(0), POS_Y(1), POS_Z(2),
    ROT_X(3), ROT_Y(4), ROT_Z(5),
    SCALA_X(6), SCALA_Y(7), SCALA_Z(8),
    BLOCK_STATE(9),
    SHOWN(10),
    SUCCESS(11), FAIL(12);

    public final int type;

    TransformType(int i) {
        type = i;
    }

    public static TransformType from(int i) {
        return switch (i) {
            case 0 -> POS_X;
            case 1 -> POS_Y;
            case 2 -> POS_Z;
            case 3 -> ROT_X;
            case 4 -> ROT_Y;
            case 5 -> ROT_Z;
            case 6 -> SCALA_X;
            case 7 -> SCALA_Y;
            case 8 -> SCALA_Z;
            case 9 -> BLOCK_STATE;
            case 10 -> SHOWN;
            case 11 -> SUCCESS;
            default -> FAIL;
        };
    }


    public double extract(TransformData transformData){
        return switch (this){
            case POS_X -> transformData.pos.x();
            case POS_Y -> transformData.pos.y();
            case POS_Z -> transformData.pos.z();
            case ROT_X -> transformData.rot.x();
            case ROT_Y -> transformData.rot.y();
            case ROT_Z -> transformData.rot.z();
            case SCALA_X -> transformData.scales.x();
            case SCALA_Y -> transformData.scales.y();
            case SCALA_Z -> transformData.scales.z();
            case BLOCK_STATE -> Block.getId(transformData.blockState);
            case SHOWN -> transformData.isShown ? 1 : 0;
            case SUCCESS -> 0;
            case FAIL -> 0;
        };
    }

    public void modify(TransformData transformData, Double number){
        switch (this){
            case POS_X -> transformData.pos.set(number,transformData.pos.y(),transformData.pos.z());
            case POS_Y -> transformData.pos.set(transformData.pos.x(),number,transformData.pos.z());
            case POS_Z -> transformData.pos.set(transformData.pos.x(),transformData.pos.y(),number);
            case ROT_X -> transformData.rot.set(number,transformData.rot.y(),transformData.rot.z());
            case ROT_Y -> transformData.rot.set(transformData.rot.x(),number,transformData.rot.z());
            case ROT_Z -> transformData.rot.set(transformData.rot.x(),transformData.rot.y(),number);
            case SCALA_X -> transformData.scales.set(number,transformData.scales.y(),transformData.scales.z());
            case SCALA_Y -> transformData.scales.set(transformData.scales.x(),number,transformData.scales.z());
            case SCALA_Z -> transformData.scales.set(transformData.scales.x(),transformData.scales.y(),number);
            case BLOCK_STATE -> transformData.blockState = Block.stateById((int) Math.round(number));
            case SHOWN -> transformData.isShown = number != 0;
            //case SUCCESS -> showBlockEntity.saveChanged();
            case FAIL -> {
            }
        }
    }


    public double extract(ShowBlockEntity showBlockEntity, int slot){
        return extract(showBlockEntity.getTransformData(slot));
    }

    public void modify(ShowBlockEntity showBlockEntity, int slot, double number){
        if(this == TransformType.SUCCESS){
            showBlockEntity.saveChanged();
            return;
        }
        showBlockEntity.setSlot(slot);
        modify(showBlockEntity.getTransformData(slot),number);
    }
}
