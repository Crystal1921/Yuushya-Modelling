package com.yuushya.modelling.gui.modeling.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;

/**
 * 碰撞箱雕刻用的 16³ 体素掩码（每格 1/16 方块，与 Minecraft 的碰撞精度一致）。
 * <p>
 * 用位运算存 4096 个格子：编辑时只改内部的 {@code long[]}，应用时再用
 * {@link #toShape()} 合并成 {@link VoxelShape} 存回方块实体。
 */
public final class CollisionVoxelGrid {

    /** 每轴的格数。 */
    public static final int SIZE = 16;
        /** 总格数。 */
        public static final int CELLS = SIZE * SIZE * SIZE;
    /** 一格对应的方块长度。 */
    public static final double CELL = 1.0 / SIZE;

        private static final int WORDS = CELLS / Long.SIZE;

    private final long[] bits;

    public CollisionVoxelGrid() {
        this.bits = new long[WORDS];
    }

    /** 格子坐标是否在 0..15 范围内。 */
    public static boolean inside(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }

    /** 世界坐标（0..1）落在哪一格；区间外夹到边界。 */
    public static int cellOf(double coordinate) {
        int cell = (int) Math.floor(coordinate * SIZE);
        return Math.clamp(cell, 0, SIZE - 1);
    }

    /**
     * 两个锚点围成的闭区间范围。
     *
     * @param first  一个角 {@code {x, y, z, ...}}（多余元素会被忽略）
     * @param second 另一个角，同上
     * @return {@code {x0, y0, z0, x1, y1, z1}}（闭区间）
     */
    public static int[] rangeBetween(int[] first, int[] second) {
        int[] min = new int[3];
        int[] max = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            min[axis] = Math.min(first[axis], second[axis]);
            max[axis] = Math.max(first[axis], second[axis]);
        }
        return new int[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }

    private static int index(int x, int y, int z) {
        return (x * SIZE + y) * SIZE + z;
    }

    public boolean get(int x, int y, int z) {
        if (!inside(x, y, z)) {
            return false;
        }
        int index = index(x, y, z);
        return (this.bits[index / Long.SIZE] & (1L << (index % Long.SIZE))) != 0;
    }

    /** @return 该格之前是否有内容 */
    public boolean set(int x, int y, int z, boolean value) {
        if (!inside(x, y, z)) {
            return false;
        }
        int index = index(x, y, z);
        int word = index / Long.SIZE;
        long mask = 1L << (index % Long.SIZE);
        boolean previous = (this.bits[word] & mask) != 0;
        if (value) {
            this.bits[word] |= mask;
        } else {
            this.bits[word] &= ~mask;
        }
        return previous;
    }

    public void clear() {
        Arrays.fill(this.bits, 0L);
    }

    public void fillAll() {
        Arrays.fill(this.bits, -1L);
    }

    public boolean isEmpty() {
        for (long word : this.bits) {
            if (word != 0L) {
                return false;
            }
        }
        return true;
    }

    public int count() {
        int total = 0;
        for (long word : this.bits) {
            total += Long.bitCount(word);
        }
        return total;
    }

    /** 指定范围（闭区间 {@code {x0,y0,z0,x1,y1,z1}}）里是否有格子与 {@code filled} 不同。 */
    public boolean needsFill(int[] range, boolean filled) {
        for (int x = range[0]; x <= range[3]; x++) {
            for (int y = range[1]; y <= range[4]; y++) {
                for (int z = range[2]; z <= range[5]; z++) {
                    if (get(x, y, z) != filled) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 把指定范围（闭区间）整体写成 {@code filled}。
     *
     * @return 是否有格子真的被改动
     */
    public boolean fill(int[] range, boolean filled) {
        boolean changed = false;
        for (int x = range[0]; x <= range[3]; x++) {
            for (int y = range[1]; y <= range[4]; y++) {
                for (int z = range[2]; z <= range[5]; z++) {
                    if (set(x, y, z, filled) != filled) {
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    /** 镜像后的坐标（以方块中心为镜面）。 */
    private static int mirror(int coordinate) {
        return SIZE - 1 - coordinate;
    }

    private static void setBits(long[] bits, int x, int y, int z) {
        int index = index(x, y, z);
        bits[index / Long.SIZE] |= 1L << (index % Long.SIZE);
    }

    /**
     * 沿某轴整体翻转：只保留镜像后的形状。
     *
     * @param axis 0 = X，1 = Y，2 = Z
     * @return 是否有变化
     */
    public boolean flip(int axis) {
        long[] before = this.bits.clone();
        long[] mirrored = new long[WORDS];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (!get(x, y, z)) {
                        continue;
                    }
                    setBits(mirrored,
                            axis == 0 ? mirror(x) : x,
                            axis == 1 ? mirror(y) : y,
                            axis == 2 ? mirror(z) : z);
                }
            }
        }
        System.arraycopy(mirrored, 0, this.bits, 0, WORDS);
        return !Arrays.equals(before, this.bits);
    }

    /**
     * 沿某轴镜像并与原形状取并集：原形状保留，另一侧补出镜像。
     *
     * @param axis 0 = X，1 = Y，2 = Z
     * @return 是否有变化
     */
    public boolean mirrorUnion(int axis) {
        long[] before = this.bits.clone();
        long[] extra = new long[WORDS];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (!get(x, y, z)) {
                        continue;
                    }
                    setBits(extra,
                            axis == 0 ? mirror(x) : x,
                            axis == 1 ? mirror(y) : y,
                            axis == 2 ? mirror(z) : z);
                }
            }
        }
        for (int word = 0; word < WORDS; word++) {
            this.bits[word] |= extra[word];
        }
        return !Arrays.equals(before, this.bits);
    }

    /**
     * 沿某轴整体平移一格，移出方块范围的格子会被丢弃。
     *
     * @param axis  0 = X，1 = Y，2 = Z
     * @param delta 位移（格），负数为反方向
     * @return 是否有变化
     */
    public boolean translate(int axis, int delta) {
        if (delta == 0) {
            return false;
        }
        long[] before = this.bits.clone();
        long[] moved = new long[WORDS];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (!get(x, y, z)) {
                        continue;
                    }
                    int nx = axis == 0 ? x + delta : x;
                    int ny = axis == 1 ? y + delta : y;
                    int nz = axis == 2 ? z + delta : z;
                    if (inside(nx, ny, nz)) {
                        setBits(moved, nx, ny, nz);
                    }
                }
            }
        }
        System.arraycopy(moved, 0, this.bits, 0, WORDS);
        return !Arrays.equals(before, this.bits);
    }
    /** 撤销用的快照。 */
    public long[] snapshot() {
        return this.bits.clone();
    }

    /** 恢复快照；长度不匹配时忽略。 */
    public void restore(long[] snapshot) {
        if (snapshot.length == this.bits.length) {
            System.arraycopy(snapshot, 0, this.bits, 0, this.bits.length);
        }
    }

    /**
     * 合并成一个 VoxelShape。
     * <p>
     * 先沿 X 合并同一行里连续的格子（满块每层最多 256 个盒子），
     * 再交给 {@link Shapes#joinUnoptimized} 拼起来并 {@code optimize()} 合并相邻面。
     */
    public VoxelShape toShape() {
        VoxelShape result = Shapes.empty();
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                int x = 0;
                while (x < SIZE) {
                    if (!get(x, y, z)) {
                        x++;
                        continue;
                    }
                    int start = x;
                    while (x < SIZE && get(x, y, z)) {
                        x++;
                    }
                    result = Shapes.joinUnoptimized(result, Shapes.box(
                            start * CELL, y * CELL, z * CELL,
                            x * CELL, (y + 1) * CELL, (z + 1) * CELL), BooleanOp.OR);
                }
            }
        }
        return result.optimize();
    }

    /** 把已有形状栅格化回来，便于在旧碰撞箱基础上继续雕。 */
    public void fromShape(VoxelShape shape) {
        clear();
        if (shape == null || shape.isEmpty()) {
            return;
        }
        for (AABB box : shape.toAabbs()) {
            int minX = startCell(box.minX);
            int minY = startCell(box.minY);
            int minZ = startCell(box.minZ);
            int maxX = endCell(box.maxX);
            int maxY = endCell(box.maxY);
            int maxZ = endCell(box.maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        set(x, y, z, true);
                    }
                }
            }
        }
    }

    /** 盒子的起点格：向下取整。 */
    private static int startCell(double coordinate) {
        return Math.clamp((int) Math.floor(coordinate * SIZE + 1.0E-4), 0, SIZE - 1);
    }

    /** 盒子的终点格：向上取整后减一（盒子右边界是开区间）。 */
    private static int endCell(double coordinate) {
        return Math.clamp((int) Math.ceil(coordinate * SIZE - 1.0E-4) - 1, 0, SIZE - 1);
    }
}