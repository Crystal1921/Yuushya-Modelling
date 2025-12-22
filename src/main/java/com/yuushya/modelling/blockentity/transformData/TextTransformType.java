package com.yuushya.modelling.blockentity.transformData;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

public enum TextTransformType {
    POS_X(0), POS_Y(1), POS_Z(2),
    ROT_X(3), ROT_Y(4), ROT_Z(5),
    SCALE_X(6), SCALE_Y(7), SCALE_Z(8),
    TEXT_LINES(9),
    SHOWN(10),
    LIT(11),
    REMOVE(12),
    SUCCESS(13), FAIL(14),
    SHAPE(15);

    public final int type;

    TextTransformType(int i) {
        type = i;
    }

    public double extract(TransformTextData transformData) {
        return switch (this) {
            case POS_X -> transformData.pos.x();
            case POS_Y -> transformData.pos.y();
            case POS_Z -> transformData.pos.z();
            case ROT_X -> transformData.rot.x();
            case ROT_Y -> transformData.rot.y();
            case ROT_Z -> transformData.rot.z();
            case SCALE_X -> transformData.scales.x();
            case SCALE_Y -> transformData.scales.y();
            case SCALE_Z -> transformData.scales.z();
            case TEXT_LINES -> transformData.textLines.size();
            case SHOWN -> transformData.isShown ? 1 : 0;
            case LIT, REMOVE, SUCCESS, SHAPE, FAIL -> 0;
        };
    }

    public void modify(TransformTextData transformData, Double number) {
        switch (this) {
            case POS_X -> transformData.pos.set(number, transformData.pos.y(), transformData.pos.z());
            case POS_Y -> transformData.pos.set(transformData.pos.x(), number, transformData.pos.z());
            case POS_Z -> transformData.pos.set(transformData.pos.x(), transformData.pos.y(), number);
            case ROT_X -> transformData.rot.set(number.floatValue(), transformData.rot.y(), transformData.rot.z());
            case ROT_Y -> transformData.rot.set(transformData.rot.x(), number.floatValue(), transformData.rot.z());
            case ROT_Z -> transformData.rot.set(transformData.rot.x(), transformData.rot.y(), number.floatValue());
            case SCALE_X -> transformData.scales.set(number.floatValue(), transformData.scales.y(), transformData.scales.z());
            case SCALE_Y -> transformData.scales.set(transformData.scales.x(), number.floatValue(), transformData.scales.z());
            case SCALE_Z -> transformData.scales.set(transformData.scales.x(), transformData.scales.y(), number.floatValue());
            case SHOWN -> transformData.isShown = number != 0;
            case FAIL -> {
            }
        }
    }

    public void modifyTextLines(TransformTextData transformData, List<String> textLines) {
        transformData.textLines.clear();
        transformData.textLines.addAll(textLines);
    }

    public double extract(TextBlockEntity textBlockEntity, int slot) {
        if (this == LIT) {
            return textBlockEntity.getBlockState().getValue(YuushyaBlockStates.LIT);
        }
        return extract(textBlockEntity.getTransformData(slot));
    }

    public BlockShape extractShape(TextBlockEntity textBlockEntity) {
        return textBlockEntity.getBlockState().getValue(SHAPES);
    }

    public void modify(TextBlockEntity textBlockEntity, int slot, double number) {
        if (textBlockEntity.getLevel() == null) return;
        if (this == SUCCESS) {
            textBlockEntity.saveChanged();
            return;
        }
        if (this == LIT) {
            Level level = textBlockEntity.getLevel();
            level.setBlock(textBlockEntity.getBlockPos(), textBlockEntity.getBlockState().setValue(YuushyaBlockStates.LIT, (int) Math.round(number)), 18);
            return;
        }
        if (this == REMOVE) {
            textBlockEntity.removeTransformData(slot);
            return;
        }
        if (this == SHAPE) {
            Level level = textBlockEntity.getLevel();
            level.setBlock(textBlockEntity.getBlockPos(), textBlockEntity.getBlockState().setValue(SHAPES, BlockShape.values()[(int) number]), 18);
            return;
        }
        textBlockEntity.setSlot(slot);
        modify(textBlockEntity.getTransformData(slot), number);
    }

    public void modify(TextBlockEntity textBlockEntity, int slot, List<String> textLines) {
        if (textBlockEntity.getLevel() == null) return;
        textBlockEntity.setSlot(slot);
        modifyTextLines(textBlockEntity.getTransformData(slot), textLines);
    }
}
