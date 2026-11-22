package com.yuushya.modelling.utils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 有上限的撤销 / 重做记录。
 * <p>
 * 只保存快照本身，怎么生成、怎么恢复由调用方决定；撤销时把「当前状态」交给 {@link #undo}
 * 压进重做栈，因此调用方不需要自己维护两份栈。
 *
 * @param <T> 快照类型
 */
public final class UndoHistory<T> {

    private final int limit;
    private final Deque<T> undoStack = new ArrayDeque<>();
    private final Deque<T> redoStack = new ArrayDeque<>();

    public UndoHistory(int limit) {
        this.limit = Math.max(1, limit);
    }

    /** 记一步：超出上限时丢掉最旧的一步，并清空重做栈。 */
    public void push(T snapshot) {
        if (this.undoStack.size() >= this.limit) {
            this.undoStack.removeLast();
        }
        this.undoStack.push(snapshot);
        this.redoStack.clear();
    }

    public boolean canUndo() {
        return !this.undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoStack.isEmpty();
    }

    /**
     * 撤销一步。
     *
     * @param current 当前状态，会被压进重做栈
     * @return 要恢复的状态；没有可撤销的记录时返回 {@code null}
     */
    public T undo(T current) {
        if (this.undoStack.isEmpty()) {
            return null;
        }
        this.redoStack.push(current);
        return this.undoStack.pop();
    }

    /**
     * 重做一步。
     *
     * @param current 当前状态，会被压回撤销栈
     * @return 要恢复的状态；没有可重做的记录时返回 {@code null}
     */
    public T redo(T current) {
        if (this.redoStack.isEmpty()) {
            return null;
        }
        this.undoStack.push(current);
        return this.redoStack.pop();
    }
}