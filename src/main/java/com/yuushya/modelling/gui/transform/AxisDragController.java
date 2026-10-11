package com.yuushya.modelling.gui.transform;

import com.yuushya.modelling.utils.CameraProjection;
import org.joml.Vector3f;

/**
 * 世界区域拖拽改值的会话状态机。
 * <p>
 * 左键拖动 = 沿当前轴平移，右键拖动 = 绕当前轴旋转。像素位移会投影到该轴在屏幕上的方向，
 * 再除以缩放系数换算成数据单位，因此手感与视角、方块缩放无关。
 * <p>
 * 轴向的屏幕投影在按下瞬间冻结（见 {@link #begin}），拖拽期间不再随视距变化；轴与视线过于接近平行为
 * 不可拖拽状态，会在 {@link #begin} 处直接拒绝。
 * <p>
 * 本类只维护会话状态与换算，实际读写由 {@link Host} 提供；几何全部在世界坐标里做，
 * 由界面负责把方块局部坐标换算过去。
 */
public final class AxisDragController {

    /** 由界面提供的读写与高亮回调。 */
    public interface Host {
        /** 指定轴、通道（true = 旋转，false = 平移）的当前值。 */
        double value(int axis, boolean rotation);

        /** 写入新值；界面内部负责范围夹取与控件同步。 */
        void apply(int axis, boolean rotation, double value);

        /** 高亮该轴。 */
        void highlight(int axis);

        /** 该轴上的缩放系数，用于把像素换算成数据单位。 */
        double axisScale(int axis);

        /** 手柄原点的世界坐标（1 单位 = 1 方块）。无法给出时返回 null。 */
        float[] axisOriginWorld();

        /** 该轴在世界坐标中的单位方向。 */
        float[] axisDirection(int axis);
    }

    /** 拖拽整体灵敏度倍率。 */
    public static final double SENSITIVITY = 1.0;
    /** 按住 Shift 时的倍率。 */
    public static final double FINE_FACTOR = 0.1;

    private static final double EPSILON = 1.0E-6;
    /** 轴手柄的半长（方块），与渲染器画出的线段一致。 */
    private static final float HANDLE_HALF_LENGTH = 1.5F;
    /**
     * 开始拖拽所需的最小投影长度（像素/方块）。
     * <p>
     * 低于此值时该轴几乎与视线平行，1 像素位移会对应巨大的数值跳变，换算已失去量纲，直接拒绝开始。
     */
    private static final double MIN_PIXELS_PER_BLOCK = 12.0;

    private final Host host;
    private boolean active;
    private int axis;
    private boolean rotation;
    private double startValue;
    private double startMouseX;
    private double startMouseY;
    /** 按下瞬间冻结的轴投影（像素/方块）与其模平方，整个会话不再变化。 */
    private double screenAxisX;
    private double screenAxisY;
    private double screenAxisLengthSq;

    public AxisDragController(Host host) {
        this.host = host;
    }

    public boolean isActive() {
        return active;
    }

    public int axis() {
        return axis;
    }

    public boolean rotation() {
        return rotation;
    }

    /**
     * 开始一次拖拽。
     * <p>
     * 轴的屏幕投影在这里冻结，整个会话不再随视距变化：否则"值变→手柄移动→投影长度变→同样的鼠标位移算出的值又变"
     * 会形成正反馈，手柄移向屏幕边缘（轴逐渐与视线平行）时表现为抖动与跳变。
     *
     * @return 是否成功开始；轴与视线过于接近平行时返回 {@code false}
     */
    public boolean begin(int axis, boolean rotation, double mouseX, double mouseY, int screenWidth, int screenHeight) {
        float[][] segments = usableSegments(screenWidth, screenHeight);
        if (segments == null) {
            return false;
        }
        // 绕 axis 旋转时，屏幕上跟手的是与之垂直的切线轴（右手系循环）
        double[] screenAxis = axisScreenScale(segments[rotation ? (axis + 2) % 3 : axis]);
        double lengthSq = scalarLengthSq(screenAxis);
        if (lengthSq < MIN_PIXELS_PER_BLOCK * MIN_PIXELS_PER_BLOCK) {
            return false;
        }
        this.axis = axis;
        this.rotation = rotation;
        this.startValue = host.value(axis, rotation);
        this.startMouseX = mouseX;
        this.startMouseY = mouseY;
        this.screenAxisX = screenAxis[0];
        this.screenAxisY = screenAxis[1];
        this.screenAxisLengthSq = lengthSq;
        this.active = true;
        host.highlight(axis);
        return true;
    }

    /** 结束会话（当前值已经写入数据，无需额外提交）。 */
    public void end() {
        this.active = false;
    }

    /** 取消会话并回滚到按下瞬间的值。 */
    public void cancel() {
        if (!active) {
            return;
        }
        this.active = false;
        host.apply(axis, rotation, startValue);
    }

    /**
     * 按当前鼠标位置刷新取值。全部换算都基于 {@link #begin} 时冻结的那份轴投影。
     *
     * @param fine 是否按住 Shift
     */
    public void update(double mouseX, double mouseY, boolean fine) {
        if (!active || screenAxisLengthSq < EPSILON) {
            return;
        }
        host.highlight(axis);
        double factor = SENSITIVITY * (fine ? FINE_FACTOR : 1.0);
        // 鼠标位移在冻结轴向上的分量，单位是"方块"
        double blocks = ((mouseX - startMouseX) * screenAxisX + (mouseY - startMouseY) * screenAxisY) / screenAxisLengthSq;

        if (rotation) {
            // 参考点距轴 1 个模型单位，旋转 δ 后位移 scales * δ 个方块
            double scale = host.axisScale((axis + 2) % 3);
            if (scale == 0.0) {
                return;
            }
            host.apply(axis, true, startValue + Math.toDegrees(blocks / scale) * factor);
        } else {
            // 1 个数据单位 = scales / 16 个方块
            double scale = host.axisScale(axis);
            if (scale == 0.0) {
                return;
            }
            host.apply(axis, false, startValue + blocks * 16.0 / scale * factor);
        }
    }

    /**
     * 返回屏幕上距离给定点最近的轴（0 = X，1 = Y，2 = Z），用于“点哪条线拖哪条”。
     * <p>
     * 判定基于与渲染器画出的线段（原点 ± 1.5 方块）的距离，含透视，因此与看到的轴线一致。
     *
     * @return 轴编号；投影不可用或无法判定时返回 -1
     */
    public int nearestAxis(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        float[][] segments = usableSegments(screenWidth, screenHeight);
        if (segments == null) {
            return -1;
        }
        int nearest = -1;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            double distance = distanceToSegment(mouseX, mouseY, segments[i][0], segments[i][1], segments[i][2], segments[i][3]);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }

    /**
     * 调试快照：把当前换算出的几何原样导出，便于在 GUI 上直接绘制、与世界里的轴线比对。
     *
     * @param projectionUsable 投影缓存是否可用
     * @param originPixels     手柄原点的屏幕坐标，不可得时为 {@code null}
     * @param segments         三条轴线段 {@code {x0, y0, x1, y1}}，不可得时为 {@code null}
     * @param distances        鼠标到三条线段的像素距离，不可得时为 {@code null}
     */
    public record DebugSnapshot(boolean projectionUsable, float[] originPixels, float[][] segments, double[] distances) {
    }

    /** 调试用：导出三条轴线段、手柄原点与鼠标距离。 */
    public DebugSnapshot debugSnapshot(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!CameraProjection.isUsable()) {
            return new DebugSnapshot(false, null, null, null);
        }
        float[] worldOrigin = host.axisOriginWorld();
        float[] originPixels = worldOrigin == null ? null : toScreen(worldOrigin, null, 0.0F, screenWidth, screenHeight);
        float[][] segments = gizmoSegments(screenWidth, screenHeight);
        if (segments == null) {
            return new DebugSnapshot(true, originPixels, null, null);
        }
        double[] distances = new double[3];
        for (int i = 0; i < 3; i++) {
            distances[i] = distanceToSegment(mouseX, mouseY, segments[i][0], segments[i][1], segments[i][2], segments[i][3]);
        }
        return new DebugSnapshot(true, originPixels, segments, distances);
    }

    /** 投影可用时返回三条轴线段的端点，否则返回 {@code null}。 */
    private float[][] usableSegments(int screenWidth, int screenHeight) {
        return CameraProjection.isUsable() ? gizmoSegments(screenWidth, screenHeight) : null;
    }

    /**
     * 三条轴线在世界坐标中的端点投影到 GUI 后得到的线段，即渲染器实际画出的那三段线
     * （原点沿轴 ± {@link #HANDLE_HALF_LENGTH} 方块）。
     * <p>
     * 拾取与拖拽换算都基于同一组线段，保证“点中哪条就沿哪条走”。
     *
     * @return {@code segments[axis] = {x0, y0, x1, y1}}；任一端点无法投影时返回 {@code null}
     */
    private float[][] gizmoSegments(int screenWidth, int screenHeight) {
        float[] origin = host.axisOriginWorld();
        if (origin == null) {
            return null;
        }
        float[][] segments = new float[3][4];
        for (int i = 0; i < 3; i++) {
            float[] direction = host.axisDirection(i);
            if (direction == null) {
                return null;
            }
            float[] from = toScreen(origin, direction, -HANDLE_HALF_LENGTH, screenWidth, screenHeight);
            float[] to = toScreen(origin, direction, HANDLE_HALF_LENGTH, screenWidth, screenHeight);
            if (from == null || to == null) {
                return null;
            }
            segments[i][0] = from[0];
            segments[i][1] = from[1];
            segments[i][2] = to[0];
            segments[i][3] = to[1];
        }
        return segments;
    }

    /** 轴线上“1 个方块”对应的屏幕位移（像素），由线段端点求得（含透视）。 */
    private static double[] axisScreenScale(float[] segment) {
        double length = 2.0 * HANDLE_HALF_LENGTH;
        return new double[]{(segment[2] - segment[0]) / length, (segment[3] - segment[1]) / length};
    }

    /** 世界坐标沿 {@code direction} 偏移 {@code offset} 个方块后的屏幕坐标；{@code direction} 为 null 时取原点本身。 */
    private static float[] toScreen(float[] origin, float[] direction, float offset, int screenWidth, int screenHeight) {
        float x = origin[0];
        float y = origin[1];
        float z = origin[2];
        if (direction != null) {
            x += direction[0] * offset;
            y += direction[1] * offset;
            z += direction[2] * offset;
        }
        Vector3f ndc = CameraProjection.toNdc(x, y, z);
        if (ndc == null) {
            return null;
        }
        return new float[]{
                (ndc.x * 0.5F + 0.5F) * screenWidth,
                (1.0F - (ndc.y * 0.5F + 0.5F)) * screenHeight};
    }

    private static double distanceToSegment(double px, double py, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lengthSq = dx * dx + dy * dy;
        double t = lengthSq < 1.0E-6 ? 0.0 : ((px - x0) * dx + (py - y0) * dy) / lengthSq;
        t = t < 0.0 ? 0.0 : Math.min(t, 1.0);
        return Math.hypot(px - (x0 + t * dx), py - (y0 + t * dy));
    }

    private static double scalarLengthSq(double[] d) {
        return d[0] * d[0] + d[1] * d[1];
    }
}
