package com.yuushya.modelling.gui.modeling.collision;

import net.minecraft.network.chat.Component;

/**
 * 笔刷形状：点一下要覆盖多大的体积。
 * <p>
 * 三个「方块比例」模式的边长按方块算（1/8 方块 = 2 格，1/4 = 4 格，1/2 = 8 格，居中展开）；
 * 条状沿当前平面 / 命中面的法线方向贯通整条；板状铺满除法线以外的两个方向；
 * 方框则是按住拖动划出范围，松开时才一次性填充 / 清除。
 */
public enum CollisionBrush {
    /** 单格（丁状）。 */
    PIN(1),
    /** 1/8 方块 = 2 格见方。 */
    EIGHTH(2),
    /** 1/4 方块 = 4 格见方。 */
    QUARTER(4),
    /** 1/2 方块 = 8 格见方。 */
    HALF(8),
    /** 沿主轴贯通的一根。 */
    BAR(1),
    /** 垂直于主轴铺满的一层。 */
    PLATE(0),
    /** 按住拖动划出方框，松开后填充 / 清除整个方框。 */
    RECT(1);

    private final int side;

    CollisionBrush(int side) {
        this.side = side;
    }

    public Component label() {
        return Component.translatable("gui.yuushya.collision.brush." + name().toLowerCase());
    }

    /**
     * 各轴的格数。
     *
     * @param primaryAxis 当前平面 / 命中面的法线轴
     */
    public int[] sizes(int primaryAxis) {
        int[] sizes = {this.side, this.side, this.side};
        switch (this) {
            case BAR -> sizes[primaryAxis] = CollisionVoxelGrid.SIZE;
            case PLATE -> {
                sizes[0] = CollisionVoxelGrid.SIZE;
                sizes[1] = CollisionVoxelGrid.SIZE;
                sizes[2] = CollisionVoxelGrid.SIZE;
                sizes[primaryAxis] = 1;
            }
            default -> {
            }
        }
        return sizes;
    }

    /**
     * 从锚点出发、按当前笔刷算出的作用范围。
     *
     * @param anchor      锚点体素（画板格为贴平面那一格，第 4 个元素是法线轴）
     * @param primaryAxis 主轴（画板法线 / 命中面法线），条状沿它贯通、板状垂直于它铺开
     * @return {@code {x0, y0, z0, x1, y1, z1}}（闭区间）
     */
    public int[] range(int[] anchor, int primaryAxis) {
        int[] sizes = sizes(primaryAxis);
        int[] min = new int[3];
        int[] max = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            int size = Math.clamp(sizes[axis], 1, CollisionVoxelGrid.SIZE);
            int start;
            if (size >= CollisionVoxelGrid.SIZE) {
                start = 0;
            } else if (size == 1) {
                start = anchor[axis];
            } else {
                // 边长大于 1 时让锚点落在中间，点哪儿就在哪儿展开
                start = anchor[axis] - (size - 1) / 2;
            }
            start = Math.clamp(start, 0, CollisionVoxelGrid.SIZE - size);
            min[axis] = start;
            max[axis] = start + size - 1;
        }
        return new int[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }
}