package com.yuushya.modelling.gui.modeling.collision;

import com.yuushya.modelling.gui.modeling.ModelingCamera;
import com.yuushya.modelling.gui.modeling.ModelingLayout;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * 屏幕像素反投影出来的方块局部空间射线（方向已归一化）。
 * <p>
 * 相机矩阵是仿射的，像素 {@code (mx, my)} 对应的模型空间射线就是
 * {@code M⁻¹(mx, my, +DEPTH)} → {@code M⁻¹(mx, my, -DEPTH)} 两点连线（正交投影下与投影矩阵无关）。
 * GUI 的 z 越大越靠近观察者，所以起点在观察者一侧。
 * <p>
 * 深度必须取得足够大：GUI 的 z 跨度换算到模型空间要除以缩放系数，放大到极限时
 * 1 像素只对应千分之几个方块，取小了整段「射线」都会落在方块内部。
 */
public record CollisionRay(double originX, double originY, double originZ,
                           double dirX, double dirY, double dirZ) {

    /** 反投影在屏幕 z 方向上的半长。 */
    private static final float DEPTH = 20000.0F;
    private static final double EPSILON = 1.0E-9;

    /**
     * 把视口里的一个像素反投影成射线。
     *
     * @return 射线；光标不在视口内、或相机矩阵不可逆时返回 {@code null}
     */
    public static CollisionRay cast(double mouseX, double mouseY,
                                    ModelingCamera camera, ModelingLayout.Rect viewport) {
        if (viewport == null || !viewport.contains(mouseX, mouseY)) {
            return null;
        }
        Matrix4f inverse = new Matrix4f(camera.baseMatrix(viewport));
        if (Math.abs(inverse.determinant()) < 1.0E-9F) {
            return null;
        }
        inverse.invert();
        Vector4f near = inverse.transform(
                new Vector4f((float) mouseX, (float) mouseY, DEPTH, 1.0F), new Vector4f());
        Vector4f far = inverse.transform(
                new Vector4f((float) mouseX, (float) mouseY, -DEPTH, 1.0F), new Vector4f());
        double dirX = far.x() - near.x();
        double dirY = far.y() - near.y();
        double dirZ = far.z() - near.z();
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < EPSILON) {
            return null;
        }
        return new CollisionRay(near.x(), near.y(), near.z(),
                dirX / length, dirY / length, dirZ / length);
    }

    /**
     * 与 {@code [0,1]³} 的相交区间。
     *
     * @return {@code {enter, exit}}（enter 已夹到 0）；不相交时返回 {@code null}
     */
    public double[] clipToUnitCube() {
        return clipToBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }

    /**
     * 与任意轴对齐盒的进入参数。
     *
     * @return 射线进入盒子处的 {@code t}；不相交时返回 {@code -1}
     */
    public double entryToBox(double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ) {
        double[] interval = clipToBox(minX, minY, minZ, maxX, maxY, maxZ);
        return interval == null ? -1.0 : interval[0];
    }

    /**
     * 与轴对齐盒求交（slab 法）。
     *
     * @return {@code {enter, exit}}，enter 已夹到 0；不相交时返回 {@code null}
     */
    private double[] clipToBox(double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ) {
        double tMin = -Double.MAX_VALUE;
        double tMax = Double.MAX_VALUE;
        double[] origins = {this.originX, this.originY, this.originZ};
        double[] dirs = {this.dirX, this.dirY, this.dirZ};
        double[] mins = {minX, minY, minZ};
        double[] maxs = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            double origin = origins[axis];
            double dir = dirs[axis];
            if (Math.abs(dir) < EPSILON) {
                if (origin < mins[axis] || origin > maxs[axis]) {
                    return null;
                }
                continue;
            }
            double t1 = (mins[axis] - origin) / dir;
            double t2 = (maxs[axis] - origin) / dir;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return null;
            }
        }
        if (tMax < 0.0) {
            return null;
        }
        return new double[]{Math.max(tMin, 0.0), tMax};
    }
}