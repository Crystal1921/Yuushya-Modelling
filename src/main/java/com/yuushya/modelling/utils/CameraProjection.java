package com.yuushya.modelling.utils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 世界坐标 → NDC 的投影，供变换编辑界面把鼠标位移换算成沿轴位移。
 * <p>
 * 投影矩阵只能在世界渲染阶段取（{@code RenderLevelStageEvent}）：GUI 渲染时 {@code RenderSystem} 里的
 * 那份已经换成正交的界面投影。视图矩阵则按调用时刻的相机位置与朝向即时构建，即
 * {@code MVP = P · R_view · T(-camPos)}，与 PostProcess 的写法一致。
 * <p>
 * 这里刻意不取方块实体 PoseStack 上的矩阵——方块实体可能被缓存渲染管线以另一套姿势重放，
 * 取到的那份未必就是画在屏幕上的那份；相机位置与朝向才是唯一真值来源。
 */
public final class CameraProjection {
    /** 超过这个时长没有刷新就认为投影矩阵过期（世界没有在渲染）。 */
    private static final long MAX_AGE_NANOS = 200_000_000L;

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static long projectionStamp = 0L;

    private CameraProjection() {
    }

    public static void captureProjection(Matrix4f projection) {
        PROJECTION.set(projection);
        projectionStamp = System.nanoTime();
    }

    /** 投影矩阵是否在有效期内。 */
    public static boolean isUsable() {
        return System.nanoTime() - projectionStamp < MAX_AGE_NANOS;
    }

    /** 投影矩阵距上次刷新的毫秒数（调试用）。 */
    public static long projectionAgeMillis() {
        return (System.nanoTime() - projectionStamp) / 1_000_000L;
    }

    /**
     * 世界坐标 → NDC。世界坐标中 1 单位 = 1 个方块。
     *
     * @return NDC 坐标；点位于相机后方时返回 {@code null}
     */
    public static Vector3f toNdc(double x, double y, double z) {
        Vector4f clip = new Vector4f((float) x, (float) y, (float) z, 1.0F);
        mvp().transform(clip);
        if (clip.w <= 1.0E-4F) {
            return null;
        }
        return new Vector3f(clip.x / clip.w, clip.y / clip.w, clip.z / clip.w);
    }

    /** {@code MVP = P · (R_view · T(-camPos))}。 */
    private static Matrix4f mvp() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Matrix4f modelView = new Matrix4f()
                .rotation(camera.rotation().conjugate(new Quaternionf()))
                .translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
        return new Matrix4f(PROJECTION).mul(modelView);
    }
}
