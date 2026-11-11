package com.yuushya.modelling.gui.modeling.target;

/**
 * 建模界面能改的"量"。与具体方块类型无关，由 {@link ModelingTarget} 映射到底层的
 * {@code TransformType} / {@code ItemTransformType}。
 * <p>
 * 前 9 项与两个底层枚举的序号完全一致（POS_X=0 … SCALE_Z=8），因此映射是一对一的
 * switch，不需要靠 ordinal 做算术。
 */
public enum ModelingChannel {

    POS_X, POS_Y, POS_Z,
    ROT_X, ROT_Y, ROT_Z,
    SCALE_X, SCALE_Y, SCALE_Z,
    /** 发光等级，写的是方块状态而不是元素数据。 */
    LIT,
        /** 元素染色（仅 ItemBlock），按通道拆开以便复用三轴数字框。 */
        COLOR_R, COLOR_G, COLOR_B,
    /** 物品 / 方块归属（仅 ItemBlock）。 */
    ENABLE_BLOCK,
    /** 是否显示。 */
    SHOWN;

    public boolean isPosition() {
        return this == POS_X || this == POS_Y || this == POS_Z;
    }

    public boolean isRotation() {
        return this == ROT_X || this == ROT_Y || this == ROT_Z;
    }

    public boolean isScale() {
        return this == SCALE_X || this == SCALE_Y || this == SCALE_Z;
    }

        public boolean isColor() {
            return this == COLOR_R || this == COLOR_G || this == COLOR_B;
        }

        /** 是否为"每个元素一份"的数据（LIT 写的是方块状态，不算）。 */
        public boolean isElementData() {
            return this != LIT;
        }

    /** 拖动数字框时每像素对应的增量。 */
    public double step() {
        if (isPosition()) {
            return 0.5;
        }
        if (isRotation()) {
            return 1.0;
        }
        if (isScale()) {
            return 0.02;
        }
        return 1.0;
    }

    /** 数字框显示的小数位。 */
    public int decimals() {
        return isPosition() || isScale() ? 1 : 0;
    }
}