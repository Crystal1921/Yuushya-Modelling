package com.yuushya.modelling.gui.modeling.collision;

import com.yuushya.modelling.gui.modeling.ModelingCamera;
import com.yuushya.modelling.gui.modeling.ModelingLayout;

/**
 * 碰撞箱雕刻的拾取状态：光标压住哪个体素 / 哪块画板，以及左键放置、右键挖除的目标。
 * <p>
 * 三块「画板」就是方块局部空间的三个坐标平面 —— {@code x = 0}（YZ 面）、{@code y = 0}（XZ 面，地板）、
 * {@code z = 0}（XY 面，背墙）。拾取以「光标射线实际穿过的笔刷体积」为准：
 * 优先已有实心格，其次上一帧仍压在光标下的预览框，最后才落到三张零坐标画板，
 * 保证画出来的框就是点下去会改的格子。
 */
public final class CollisionPicker {

    /** 三块画板：0 = x=0（YZ），1 = y=0（XZ），2 = z=0（XY）。 */
    private static final int WALL_YZ = 0;
    private static final int WALL_XZ = 1;
    private static final int WALL_XY = 2;
    private static final int WALL_COUNT = 3;

    private final CollisionVoxelGrid grid;

    // 体素命中
    private boolean voxelHover;
    private int hoverX;
    private int hoverY;
    private int hoverZ;
    /** 命中面的法线轴（0 = X，1 = Y，2 = Z），条状 / 板状靠它定向。 */
    private int hoverFaceAxis;
    /** 命中面外侧的格子（放置目标）；不放时 placeValid 为 false。 */
    private boolean placeValid;
    private int placeX;
    private int placeY;
    private int placeZ;
    /** 射线在三个轴上的步进方向（±1）：给命中格找相邻空格时用来判断「往里」是哪边。 */
    private int hoverStepX = 1;
    private int hoverStepY = 1;
    private int hoverStepZ = 1;

    // 画板命中
    private int wallPlane = -1;
    private int wallCellU;
    private int wallCellV;

    /** 当前帧解析出的放置 / 挖除锚点 {@code {x, y, z, 法线轴}}。 */
    private int[] placeTarget;
    private int[] eraseTarget;

    public CollisionPicker(CollisionVoxelGrid grid) {
        this.grid = grid;
    }

    public int[] placeTarget() {
        return this.placeTarget;
    }

    public int[] eraseTarget() {
        return this.eraseTarget;
    }

    /**
     * 刷新一帧的悬停结果。
     *
     * @param sticky 上一帧的预览框是否还能作为候选（拖方框时为 {@code false}，让预览紧跟光标）
     */
    public void update(double mouseX, double mouseY, CollisionBrush brush, boolean sticky,
                       ModelingCamera camera, ModelingLayout.Rect viewport) {
        int[] previousPlace = sticky ? this.placeTarget : null;
        int[] previousErase = sticky ? this.eraseTarget : null;
        this.placeTarget = null;
        this.eraseTarget = null;
        CollisionRay ray = CollisionRay.cast(mouseX, mouseY, camera, viewport);
        if (ray == null) {
            return;
        }
        trace(ray);
        this.placeTarget = resolve(ray, true, brush, previousPlace);
        this.eraseTarget = resolve(ray, false, brush, previousErase);
    }

    /**
     * 在指定像素位置做一次完整拾取，返回该点的新锚点（不做「粘滞」）。
     * <p>
     * 拖动画笔时逐点采样都走这里，因此笔迹始终贴着当前光标下的表面。
     *
     * @return 锚点 {@code {x, y, z, 法线轴}}；没有可落笔的地方时返回 {@code null}
     */
    public int[] pick(double mouseX, double mouseY, CollisionBrush brush, boolean place,
                      ModelingCamera camera, ModelingLayout.Rect viewport) {
        CollisionRay ray = CollisionRay.cast(mouseX, mouseY, camera, viewport);
        if (ray == null) {
            return null;
        }
        trace(ray);
        return resolve(ray, place, brush, null);
    }

    /** 重新计算体素 / 画板的命中格。 */
    private void trace(CollisionRay ray) {
        this.voxelHover = false;
        this.placeValid = false;
        this.wallPlane = -1;
        traceVoxel(ray);
        traceWall(ray);
        if (this.voxelHover) {
            // 画板只是「画布」：射线碰到实体格时一律编辑实体格
            this.wallPlane = -1;
        } else {
            this.placeValid = false;
        }
    }

    /**
     * 解析一帧的落笔锚点。
     * <p>
     * 优先级：已有实心格 → 上一帧仍压在光标下的预览框 → 零坐标画板。
     * 「上一帧的预览框」让大笔刷伸出去的那部分也能点中，同时不会把画板上的目标顶掉；
     * 没有候选时返回 {@code null}，界面就不画框，点击也不生效。
     */
    private int[] resolve(CollisionRay ray, boolean place, CollisionBrush brush, int[] previous) {
        int[] voxel = place ? voxelPlaceTarget(ray, brush) : voxelEraseTarget(ray, brush);
        if (voxel != null) {
            return voxel;
        }
        // 画板格必须跟着光标走；只有光标已经离开画板/体素时，才允许沿用上一帧的框
        boolean overWall = !this.voxelHover && this.wallPlane >= 0;
        if (!overWall && previous != null && rangeHit(ray, brush, previous)) {
            return previous;
        }
        return wallTarget(ray, brush);
    }

    /**
     * 左键（放置）候选：命中面外侧那格优先，边界处退回往里一格，最后退回命中格本身。
     * <p>
     * 每个候选都要通过「笔刷体积被射线穿过」的检查，保证预览框始终压在光标下；
     * 大笔刷伸出碰撞箱轮廓的部分也因此可以继续点击。
     */
    private int[] voxelPlaceTarget(CollisionRay ray, CollisionBrush brush) {
        if (!this.voxelHover) {
            return null;
        }
        if (this.placeValid) {
            int[] outward = {this.placeX, this.placeY, this.placeZ, this.hoverFaceAxis};
            if (rangeHit(ray, brush, outward)) {
                return outward;
            }
        }
        int[] inward = inwardFreeCell();
        if (inward != null && rangeHit(ray, brush, inward)) {
            return inward;
        }
        int[] hit = {this.hoverX, this.hoverY, this.hoverZ, this.hoverFaceAxis};
        return rangeHit(ray, brush, hit) ? hit : null;
    }

    /**
     * 命中格「往里」那一格（沿射线穿过该面的方向），仅在它是空格时返回。
     * <p>
     * 命中面正好贴在 16³ 边界上时，外侧那一格在体积之外、无处可放；这时退一步填到方块内部，
     * 至少能让左键在贴着边界的表面上继续加料，而不是完全没反应。
     */
    private int[] inwardFreeCell() {
        int axis = this.hoverFaceAxis;
        int step = axis == 0 ? this.hoverStepX : axis == 1 ? this.hoverStepY : this.hoverStepZ;
        int x = this.hoverX + (axis == 0 ? step : 0);
        int y = this.hoverY + (axis == 1 ? step : 0);
        int z = this.hoverZ + (axis == 2 ? step : 0);
        if (CollisionVoxelGrid.inside(x, y, z) && !this.grid.get(x, y, z)) {
            return new int[]{x, y, z, axis};
        }
        return null;
    }

    /** 右键（挖除）候选：命中格本身。 */
    private int[] voxelEraseTarget(CollisionRay ray, CollisionBrush brush) {
        if (!this.voxelHover) {
            return null;
        }
        int[] hit = {this.hoverX, this.hoverY, this.hoverZ, this.hoverFaceAxis};
        return rangeHit(ray, brush, hit) ? hit : null;
    }

    /** 画板格 → 贴平面那一格；没有命中画板时返回 {@code null}。 */
    private int[] wallAnchor() {
        if (this.wallPlane < 0) {
            return null;
        }
        int[] cell = wallCellToVoxel(this.wallPlane, this.wallCellU, this.wallCellV);
        return new int[]{cell[0], cell[1], cell[2], wallAxis(this.wallPlane)};
    }

    /** 画板候选：只有落在光标下的画板格才接受。 */
    private int[] wallTarget(CollisionRay ray, CollisionBrush brush) {
        int[] wall = wallAnchor();
        if (wall == null) {
            return null;
        }
        return rangeHit(ray, brush, wall) ? wall : null;
    }

    /** 从该锚点展开的笔刷体积是否被射线穿过。 */
    private boolean rangeHit(CollisionRay ray, CollisionBrush brush, int[] anchor) {
        int[] range = brush.range(anchor, anchor[3]);
        double cell = CollisionVoxelGrid.CELL;
        return ray.entryToBox(range[0] * cell, range[1] * cell, range[2] * cell,
                (range[3] + 1) * cell, (range[4] + 1) * cell, (range[5] + 1) * cell) >= 0.0;
    }

    /**
     * 在 {@code [0,1]³} 里步进找第一个实心格。
     */
    private void traceVoxel(CollisionRay ray) {
        double[] interval = ray.clipToUnitCube();
        if (interval == null) {
            return;
        }
        double enterT = interval[0];
        double exitT = interval[1];
        int cellX = CollisionVoxelGrid.cellOf(ray.originX() + ray.dirX() * enterT);
        int cellY = CollisionVoxelGrid.cellOf(ray.originY() + ray.dirY() * enterT);
        int cellZ = CollisionVoxelGrid.cellOf(ray.originZ() + ray.dirZ() * enterT);

        int stepX = ray.dirX() > 0 ? 1 : -1;
        int stepY = ray.dirY() > 0 ? 1 : -1;
        int stepZ = ray.dirZ() > 0 ? 1 : -1;
        this.hoverStepX = stepX;
        this.hoverStepY = stepY;
        this.hoverStepZ = stepZ;
        double cellSize = CollisionVoxelGrid.CELL;

        double maxX = Math.abs(ray.dirX()) < 1.0E-9 ? Double.MAX_VALUE
                : ((cellX + (stepX > 0 ? 1 : 0)) * cellSize - ray.originX()) / ray.dirX();
        double maxY = Math.abs(ray.dirY()) < 1.0E-9 ? Double.MAX_VALUE
                : ((cellY + (stepY > 0 ? 1 : 0)) * cellSize - ray.originY()) / ray.dirY();
        double maxZ = Math.abs(ray.dirZ()) < 1.0E-9 ? Double.MAX_VALUE
                : ((cellZ + (stepZ > 0 ? 1 : 0)) * cellSize - ray.originZ()) / ray.dirZ();
        double deltaX = Math.abs(ray.dirX()) < 1.0E-9 ? Double.MAX_VALUE : cellSize / Math.abs(ray.dirX());
        double deltaY = Math.abs(ray.dirY()) < 1.0E-9 ? Double.MAX_VALUE : cellSize / Math.abs(ray.dirY());
        double deltaZ = Math.abs(ray.dirZ()) < 1.0E-9 ? Double.MAX_VALUE : cellSize / Math.abs(ray.dirZ());

        int previousX = -1;
        int previousY = -1;
        int previousZ = -1;
        int entryAxis = 0;

        for (int step = 0; step < CollisionVoxelGrid.SIZE * 3 + 3; step++) {
            if (this.grid.get(cellX, cellY, cellZ)) {
                this.voxelHover = true;
                this.hoverX = cellX;
                this.hoverY = cellY;
                this.hoverZ = cellZ;
                this.hoverFaceAxis = entryAxis;
                this.placeValid = CollisionVoxelGrid.inside(previousX, previousY, previousZ)
                        && !this.grid.get(previousX, previousY, previousZ);
                this.placeX = previousX;
                this.placeY = previousY;
                this.placeZ = previousZ;
                return;
            }
            previousX = cellX;
            previousY = cellY;
            previousZ = cellZ;

            double next = Math.min(maxX, Math.min(maxY, maxZ));
            if (next > exitT) {
                return;
            }
            if (maxX <= maxY && maxX <= maxZ) {
                cellX += stepX;
                maxX += deltaX;
                entryAxis = 0;
            } else if (maxY <= maxZ) {
                cellY += stepY;
                maxY += deltaY;
                entryAxis = 1;
            } else {
                cellZ += stepZ;
                maxZ += deltaZ;
                entryAxis = 2;
            }
            if (!CollisionVoxelGrid.inside(cellX, cellY, cellZ)) {
                return;
            }
        }
    }

    /**
     * 与三块画板求交，取最近的一块并把命中的格子记下来。
     */
    private void traceWall(CollisionRay ray) {
        double best = -1.0;
        int bestPlane = -1;
        int bestU = 0;
        int bestV = 0;
        for (int plane = 0; plane < WALL_COUNT; plane++) {
            double dir = switch (plane) {
                case WALL_YZ -> ray.dirX();
                case WALL_XZ -> ray.dirY();
                default -> ray.dirZ();
            };
            double origin = switch (plane) {
                case WALL_YZ -> ray.originX();
                case WALL_XZ -> ray.originY();
                default -> ray.originZ();
            };
            if (Math.abs(dir) < 1.0E-9) {
                continue;
            }
            // 画板位于方块内侧的坐标平面（x=0 / y=0 / z=0）
            double t = -origin / dir;
            if (t < 0.0 || (best >= 0.0 && t >= best)) {
                continue;
            }
            double hitX = ray.originX() + ray.dirX() * t;
            double hitY = ray.originY() + ray.dirY() * t;
            double hitZ = ray.originZ() + ray.dirZ() * t;
            if (hitX < -1.0E-4 || hitX > 1.0 + 1.0E-4
                    || hitY < -1.0E-4 || hitY > 1.0 + 1.0E-4
                    || hitZ < -1.0E-4 || hitZ > 1.0 + 1.0E-4) {
                continue;
            }
            int u;
            int v;
            switch (plane) {
                case WALL_YZ -> {
                    u = CollisionVoxelGrid.cellOf(hitZ);
                    v = CollisionVoxelGrid.SIZE - 1 - CollisionVoxelGrid.cellOf(hitY);
                }
                case WALL_XZ -> {
                    u = CollisionVoxelGrid.cellOf(hitX);
                    v = CollisionVoxelGrid.cellOf(hitZ);
                }
                default -> {
                    u = CollisionVoxelGrid.cellOf(hitX);
                    v = CollisionVoxelGrid.SIZE - 1 - CollisionVoxelGrid.cellOf(hitY);
                }
            }
            best = t;
            bestPlane = plane;
            bestU = u;
            bestV = v;
        }
        if (bestPlane >= 0) {
            this.wallPlane = bestPlane;
            this.wallCellU = bestU;
            this.wallCellV = bestV;
        }
    }

    /** 画板格 → 该列起点体素坐标（贴平面那一格，垂直轴从 0 开始）。 */
    private static int[] wallCellToVoxel(int plane, int u, int v) {
        int top = CollisionVoxelGrid.SIZE - 1;
        return switch (plane) {
            case WALL_YZ -> new int[]{0, top - v, u};
            case WALL_XZ -> new int[]{u, 0, v};
            default -> new int[]{u, top - v, 0};
        };
    }

    /** 画板对应的「垂直轴」。 */
    private static int wallAxis(int plane) {
        return switch (plane) {
            case WALL_YZ -> 0;
            case WALL_XZ -> 1;
            default -> 2;
        };
    }
}