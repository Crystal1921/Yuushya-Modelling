package com.yuushya.modelling.gui.modeling;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 建模 / 碰撞箱两个编辑界面的公共骨架：布局、相机、导航轴，以及视口里的相机手势。
 * <p>
 * 两个界面共用同一套「三栏 + 中置 3D 视口」布局（{@link ModelingLayout}）与同一台相机
 * （{@link ModelingCamera}），视口里的手势也一致：
 * <ul>
 *   <li>左键 / 中键拖动环绕，右键拖动平移，滚轮缩放</li>
 *   <li>按下后位移超过阈值才算真正的拖动，避免单击时画面抖一下</li>
 * </ul>
 * 子类只负责在 {@link #initScreen()} 里搭自己的控件，并在需要时调用这里的相机手势方法；
 * 具体「按下时做什么」由子类决定（建模界面命中 gizmo 时不会走到相机）。
 */
public abstract class AbstractModelingScreen extends Screen {

    /** 面板标题行高度，两个界面共用。 */
    protected static final int TITLE_HEIGHT = ModelingLayout.PANEL_TITLE_HEIGHT;
    /** 左右面板里按钮 / 输入框的标准行高。 */
    protected static final int ROW = 20;

    /** 相机拖动的默认起始阈值（像素）。 */
    private static final double DEFAULT_CAMERA_DRAG_THRESHOLD = 4.0;

    private static final int CAMERA_NONE = 0;
    private static final int CAMERA_ORBIT = 1;
    private static final int CAMERA_PAN = 2;

    protected final ModelingCamera camera = new ModelingCamera();
    protected final ModelingAxisIndicator axisIndicator = new ModelingAxisIndicator(this.camera);
    /** 当前窗口尺寸下的区域划分，{@link #init()} 里按窗口大小重建。 */
    protected ModelingLayout layout;

    private int cameraGesture = CAMERA_NONE;
    private double cameraPressX;
    private double cameraPressY;
    private boolean cameraDragged;

    protected AbstractModelingScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        this.layout = new ModelingLayout(this.width, this.height);
        initScreen();
    }

    /** 布局就绪后搭建子界面自己的控件。 */
    protected abstract void initScreen();

    /** 渲染前的兜底：从未初始化过布局时补一份。 */
    protected void ensureLayout() {
        if (this.layout == null) {
            this.layout = new ModelingLayout(this.width, this.height);
        }
    }

    /**
     * 加入一个需要整批重建的控件：既交给界面渲染，也记进分组，便于之后统一移除。
     */
    protected void addManagedWidget(List<AbstractWidget> group, AbstractWidget widget) {
        this.addRenderableWidget(widget);
        group.add(widget);
    }

    /** 移除分组里的全部控件并清空分组。 */
    protected void clearManagedWidgets(List<AbstractWidget> group) {
        for (AbstractWidget widget : group) {
            this.removeWidget(widget);
        }
        group.clear();
    }

    /** 光标是否落在 3D 视口内。 */
    protected boolean isOverViewport(double mouseX, double mouseY) {
        return this.layout != null && this.layout.viewport.contains(mouseX, mouseY);
    }

    /**
     * 开始相机手势：左键 / 中键环绕、右键平移。
     *
     * @return 该按钮是否被当作相机手势
     */
    protected boolean beginCameraGesture(double mouseX, double mouseY, int button) {
        this.cameraPressX = mouseX;
        this.cameraPressY = mouseY;
        this.cameraDragged = false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            this.cameraGesture = CAMERA_ORBIT;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.cameraGesture = CAMERA_PAN;
            return true;
        }
        this.cameraGesture = CAMERA_NONE;
        return false;
    }


    /** 本次相机手势是否已经真正拖动过（超过阈值）。 */
    protected boolean cameraMoved() {
        return this.cameraDragged;
    }

    /** 相机拖动阈值（像素）；返回 0 表示一拖动就生效。 */
    protected double cameraDragThreshold() {
        return DEFAULT_CAMERA_DRAG_THRESHOLD;
    }

    /**
     * 处理相机拖动。
     *
     * @return 是否消费了这次拖动（没有相机手势时返回 {@code false}）
     */
    protected boolean dragCamera(double mouseX, double mouseY, double dragX, double dragY) {
        if (this.cameraGesture == CAMERA_NONE) {
            return false;
        }
        if (!this.cameraDragged) {
            double threshold = cameraDragThreshold();
            this.cameraDragged = threshold <= 0.0
                    || Math.abs(mouseX - this.cameraPressX) > threshold
                    || Math.abs(mouseY - this.cameraPressY) > threshold;
        }
        if (this.cameraDragged) {
            if (this.cameraGesture == CAMERA_ORBIT) {
                this.camera.orbit(dragX, dragY);
            } else {
                this.camera.pan(dragX, dragY);
            }
        }
        return true;
    }

    /**
     * 结束相机手势。
     *
     * @return 之前是否有手势在进行
     */
    protected boolean endCameraGesture() {
        if (this.cameraGesture == CAMERA_NONE) {
            return false;
        }
        this.cameraGesture = CAMERA_NONE;
        this.cameraDragged = false;
        return true;
    }

    /**
     * 视口内滚轮缩放。
     *
     * @return 是否消费了这次滚动
     */
    protected boolean zoomCamera(double mouseX, double mouseY, double scrollY) {
        if (scrollY == 0.0 || !isOverViewport(mouseX, mouseY)) {
            return false;
        }
        this.camera.zoom(scrollY);
        return true;
    }

    protected void renderAxisIndicator(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.axisIndicator.render(guiGraphics, this.layout.viewport, mouseX, mouseY);
    }

    /**
     * 底栏两段提示：左侧常态显示，右侧只在放得下时才画。
     * <p>
     * 窄窗口或长文本时两侧会撞在一起，所以优先保证左边那段（放 / 挖 / 状态）可读。
     */
    protected void renderFooterHints(GuiGraphics guiGraphics, Component left, Component right) {
        ModelingLayout.Rect footer = this.layout.footer;
        int leftX = footer.x() + 5;
        int y = footer.y() + 4;
        guiGraphics.drawString(this.font, left, leftX, y, ModelingChrome.TEXT, false);
        if (right == null) {
            return;
        }
        int rightX = footer.right() - 5 - this.font.width(right);
        if (rightX > leftX + this.font.width(left) + 8) {
            guiGraphics.drawString(this.font, right, rightX, y, ModelingChrome.PANEL_TITLE, false);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, -943, ModelingChrome.CHROME_BG);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}