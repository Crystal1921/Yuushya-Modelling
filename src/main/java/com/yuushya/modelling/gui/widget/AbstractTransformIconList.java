package com.yuushya.modelling.gui.widget;

import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 右侧槽位列表的通用基类。
 * <p>
 * ItemStackIconList / BlockStateIconList / TextIconList 共用同一套列表行为：刷新列表、
 * 新增槽位、选中回调、行宽与滚动条位置，以及条目的多选逻辑。各界面只负责提供条目类型
 * 与自己特有的绘制内容。
 *
 * @param <D> 槽位数据类型
 * @param <E> 条目类型
 */
public abstract class AbstractTransformIconList<D extends ITransformDataProvider, E extends AbstractTransformIconList.Entry<D, E>>
        extends ObjectSelectionList<E> {

    protected final List<D> transformDataList;
    protected final List<E> chosen = new ArrayList<>();
    protected int itemWidth;
    protected int itemHeight;

    protected AbstractTransformIconList(Minecraft minecraft, int width, int height, int x, int y0,
                                        int itemWidth, int itemHeight, List<D> transformDataList) {
        super(minecraft, width, height, y0, itemHeight);
        this.setX(x);
        this.transformDataList = transformDataList;
        this.centerListVertically = false;
        this.setRenderHeader(false, 0);
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.updateRenderList();
    }

    protected abstract E createEntry(int slot);

    /** 请求超出列表范围的槽位时返回的缺省数据。 */
    protected abstract D defaultData();

    /** 该槽位当前是否为空（为空时才允许新增槽位）。 */
    protected abstract boolean isSlotEmpty(int slot);

    /** 条目被选中时切换界面槽位。 */
    protected abstract void selectSlot(int slot);

    public void updateRenderList() {
        this.clearEntries();
        for (int i = 0; i < transformDataList.size(); i++) {
            this.addEntry(createEntry(i));
        }
    }

    public void addSlot() {
        E selected = this.getSelected();
        if (selected != null && isSlotEmpty(selected.getSlot())) {
            return;
        }
        if (children().size() == transformDataList.size()) {
            E entry = createEntry(transformDataList.size());
            this.addEntry(entry);
            this.setSelected(entry);
        }
    }

    /** 最近一次选中的条目所在槽位，没有则返回 -1。 */
    public int getChosenOne() {
        if (!this.chosen.isEmpty()) {
            return this.chosen.getLast().getSlot();
        }
        return -1;
    }

    @Override
    public void setSelected(@Nullable E selected) {
        super.setSelected(selected);
        if (selected != null) {
            selectSlot(selected.getSlot());
        }
    }

    public void setSelectedSlot(int slot) {
        this.setSelected(this.children().get(slot));
    }

    @Override
    public int getRowWidth() {
        return itemWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.getWidth() - 4;
    }

    /** 条目的公共部分：槽位编号、多选标记与点击切换逻辑。 */
    public abstract static class Entry<D extends ITransformDataProvider, E extends Entry<D, E>> extends ObjectSelectionList.Entry<E> {

        protected final AbstractTransformIconList<D, E> parent;
        protected final Minecraft minecraft;
        @Getter
        protected final int slot;
        protected boolean chosen = false;

        protected Entry(AbstractTransformIconList<D, E> parent, int slot) {
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        public D getTransformData() {
            return parent.transformDataList.size() > slot ? parent.transformDataList.get(slot) : parent.defaultData();
        }

        public boolean updateRenderShown() {
            return parent.transformDataList.size() <= slot || parent.transformDataList.get(slot).isShown();
        }

        /** 条目首次被选中时的额外处理，默认无。 */
        protected void onFirstSelected() {
        }

        @SuppressWarnings("unchecked")
        protected final E self() {
            return (E) this;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            E previousSelected = this.parent.getSelected();
            if (previousSelected != self()) {
                onFirstSelected();
            }
            this.parent.setSelected(self());

            if (previousSelected == self()) {
                this.chosen = !this.chosen;
                if (this.chosen) {
                    this.parent.chosen.add(self());
                } else {
                    this.parent.chosen.remove(self());
                }
            }
            return true;
        }
    }
}
