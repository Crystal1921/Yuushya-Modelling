package com.yuushya.modelling.gui.modeling.target;

import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * 建模界面对"正在编辑的东西"的唯一抽象。
 * <p>
 * 界面只跟这个接口打交道：读写变换、增删元素、同步、序列化、视口预览、面数统计。
 * 具体是 ShowBlock 还是 ItemBlock 由实现决定，界面里不存在任何 {@code instanceof}。
 * <p>
 * 所有写入都先落到客户端数据上并进入待发送队列，由 {@link #flush()} 统一发出去 —— 这与
 * 旧界面的 {@code storage} 机制一致，避免拖动数字框时每帧发包。
 */
public interface ModelingTarget {

    // ---- 身份 ----

    BlockEntity entity();

    Level level();

    BlockPos pos();

    /** 顶栏与元素属性区显示的标题，例如"石头"或"钻石剑"。 */
    Component displayName();

    // ---- 元素容器 ----

    int size();

    int slot();

    void setSlot(int slot);

    List<? extends ITransformDataProvider> elements();

    ITransformDataProvider element(int slot);

    /** 在末尾追加一个空元素并返回其下标。 */
    int appendEmpty();

    /** 真正从列表中删除（后续元素前移）。 */
    void removeAt(int slot);

    boolean isEmptyElement(int slot);

    /** 该物品能否作为新元素加入（ShowBlock 只接受方块物品）。 */
    boolean accepts(ItemStack stack);

    /** 用物品栈设置元素内容（方块状态 / 物品栈）。 */
    void setContent(int slot, ItemStack stack);

    // ---- 通道读写 ----

    double get(int slot, ModelingChannel channel);

    /** 改本地数据 + 入队，等待 {@link #flush()}。 */
    void set(int slot, ModelingChannel channel, double value);

    /** 只改本地数据，不发包（用于交换 / 粘贴后的整体重发）。 */
    void setLocal(int slot, ModelingChannel channel, double value);

    /** 该通道在此目标上是否可用（ItemBlock 独有通道对 ShowBlock 返回 false）。 */
    default boolean supports(ModelingChannel channel) {
        return true;
    }

    // ---- 同步 ----

    /** 冲刷待发送的改动。 */
    void flush();

    /** 通知服务端本轮编辑结束（SUCCESS 包）。 */
    void commit();

    /** 触发方块重渲染（几何变了以后调用）。 */
    void resync();

    /** 把一个元素的全部字段重新下发到指定槽位（后续元素前移时用）。 */
    void resend(int slot, ITransformDataProvider data);

    /** 清空从 {@code from} 到 {@code oldSize-1} 的多余槽位。 */
    void clearTail(int from, int oldSize);

        /** 交换两个槽位的整个元素（内容 + 变换），用于上移 / 下移。 */
        void swap(int a, int b);

    // ---- 视口 / 预览 ----

    /**
     * 视口渲染用的元素快照：每个元素的内容 + 变换，视口据此自己套矩阵绘制。
     * <p>
     * 不再把整份数据塞进一个 {@code ItemStack} 走物品模型通道 —— 那条路要求
     * {@code BakedModel} 在 {@code getRenderPasses} 里重建，且缓存以 {@code ItemStack}
     * 为弱键，既慢又容易失效。
     *
     * @param transform 位移 / 旋转 / 缩放 / 显示状态
     * @param state     方块内容；物品形态（或物品没有对应方块状态）为 {@code null}
     * @param stack     原始物品栈；方块形态下只给自定义渲染器兜底，单方块元素为 {@link ItemStack#EMPTY}
     * @param color     元素染色（ARGB），{@code -1} 表示不染色
     */
    record ElementView(ITransformDataProvider transform, @Nullable BlockState state,
                       ItemStack stack, int color) {

        public boolean renderable() {
            return this.transform.isShown() && (this.state != null || !this.stack.isEmpty());
        }
    }

    /** 当前全部元素，供 3D 视口逐元素绘制。 */
    List<ElementView> views();

    /** 当前模型的面数（开销较大，只在点统计时算）。 */
    long faceCount();

    // ---- 序列化 ----

    String serialize();

    /** 粘贴：完全替换当前全部元素。返回是否成功。 */
    boolean paste(String json);

    void saveToFile(String name) throws IOException;

    // ---- 右上「元素属性」区 ----

    /** 元素内容对应的方块状态；ItemBlock 且为物品形态时返回 {@code null}。 */
    @Nullable
    BlockState contentState(int slot);

    /** 方块状态属性行（◀ 属性名 ▶ / ◀ 值 ▶），无属性时返回空列表。 */
    List<PropertyRow> properties(int slot);

    /** 循环切换方块形状，返回新的显示文本。 */
    Component cycleShape();

    /** 当前形状的显示文本。 */
    Component shapeLabel();

    /** 右下「变换」面板的行定义（位移 / 旋转 / 缩放 / 亮度 / 颜色 …）。 */
    List<TransformRow> rows();

        /**
         * 「元素属性」区下方的开关行（例如 ItemBlock 的物品 / 方块切换）。
         * 标签左对齐，右侧是当前值；没有则返回空列表。
         */
        default List<PropertyRow> toggles(int slot) {
            return List.of();
        }

    /** 属性区的一行：标签 + 左/右循环。 */
    record PropertyRow(Component label, Component value, Runnable previous, Runnable next) {
    }
}