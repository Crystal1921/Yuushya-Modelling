package com.yuushya.modelling.gui.modeling.target;

import net.minecraft.network.chat.Component;

/**
 * 右下「变换」面板里的一行。
 * <p>
 * 由 {@link ModelingTarget} 给出，界面照着摆数字框，不再对具体方块类型做分支：
 * ShowBlock 是 位移 / 旋转 / 缩放 / 亮度，ItemBlock 再多一行颜色（R/G/B）。
 */
public sealed interface TransformRow {

    Component label();

    /**
     * 三轴并排的一行。
     *
     * @param decimals 小数位
     */
    record Vector(Component label, ModelingChannel x, ModelingChannel y, ModelingChannel z,
                  double min, double max, double step, int decimals) implements TransformRow {

        /** 不夹取区间（位移 / 旋转 / 缩放）。 */
        public static Vector unbounded(Component label, ModelingChannel x, ModelingChannel y,
                                       ModelingChannel z, double step, int decimals) {
            return new Vector(label, x, y, z, Double.NaN, Double.NaN, step, decimals);
        }
    }

    /** 单个数字框。{@code width} 为 0 时使用行内可用宽度。 */
    record Single(Component label, ModelingChannel channel,
                  double min, double max, double step, int decimals, int width) implements TransformRow {
    }
}