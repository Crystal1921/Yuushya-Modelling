package com.yuushya.modelling.gui.modeling.target;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.gui.engrave.EngraveBlockResultLoader;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.network.TransformDataOncePacket;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ModelingTarget} 的 ShowBlock 实现。
 * <p>
 * 元素内容是"方块状态"，同步走 {@link TransformDataOncePacket}，序列化走
 * {@link ShareUtils#transfer(List)}，存盘进 {@code modellings/blocks}。
 */
public final class ShowBlockTarget implements ModelingTarget {

    /** 与 {@code YuushyaBlockStates.LIT} 的定义一致（0..15）。 */
    private static final double LIT_MIN = 0.0;
    private static final double LIT_MAX = 15.0;

    private final ShowBlockEntity entity;
    private final List<TransformType> pendingTypes = new ArrayList<>();
    private final List<Double> pendingValues = new ArrayList<>();


    public ShowBlockTarget(ShowBlockEntity entity) {
        this.entity = entity;
    }

    public ShowBlockEntity showBlock() {
        return this.entity;
    }

    // ---- 身份 ----

    @Override
    public BlockEntity entity() {
        return this.entity;
    }

    @Override
    public Level level() {
        return this.entity.getLevel();
    }

    @Override
    public BlockPos pos() {
        return this.entity.getBlockPos();
    }

    @Override
    public Component displayName() {
        BlockState state = contentState(this.slot());
        return state == null
                ? Component.translatable("gui.yuushya.modeling.empty")
                : state.getBlock().getName();
    }

    // ---- 元素容器 ----

    @Override
    public int size() {
        return this.entity.size();
    }

    @Override
    public int slot() {
        return this.entity.getSlot();
    }

    @Override
    public void setSlot(int slot) {
        this.entity.setSlot(slot);
    }

    @Override
    public List<? extends ITransformDataProvider> elements() {
        return this.entity.getTransformData();
    }

    @Override
    public TransformBlockData element(int slot) {
        return this.entity.getTransformData(slot);
    }

    @Override
    public int appendEmpty() {
        this.entity.addTransformData(new TransformBlockData());
        return this.entity.size() - 1;
    }

    @Override
    public void removeAt(int slot) {
        this.entity.removeTransformDataAt(slot);
    }

    @Override
    public boolean isEmptyElement(int slot) {
        return slot < 0 || slot >= size() || element(slot).empty();
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stateOf(stack) != null;
    }

    @Override
    public void setContent(int slot, ItemStack stack) {
        BlockState state = stateOf(stack);
        if (state != null) {
            element(slot).blockState = state;
        }
    }

    // ---- 通道读写 ----

    @Override
    public double get(int slot, ModelingChannel channel) {
        if (channel == ModelingChannel.LIT) {
            return this.entity.getBlockState().getValue(YuushyaBlockStates.LIT);
        }
        TransformBlockData data = element(slot);
        return switch (channel) {
            case POS_X -> data.pos.x;
            case POS_Y -> data.pos.y;
            case POS_Z -> data.pos.z;
            case ROT_X -> data.rot.x();
            case ROT_Y -> data.rot.y();
            case ROT_Z -> data.rot.z();
            case SCALE_X -> data.scales.x();
            case SCALE_Y -> data.scales.y();
            case SCALE_Z -> data.scales.z();
            case SHOWN -> data.isShown ? 1.0 : 0.0;
            case LIT, COLOR_R, COLOR_G, COLOR_B, ENABLE_BLOCK -> 0.0;
        };
    }

    @Override
    public void set(int slot, ModelingChannel channel, double value) {
        setLocal(slot, channel, value);
        TransformType type = toType(channel);
        if (type != null) {
            queue(type, value);
        }
    }

    @Override
    public void setLocal(int slot, ModelingChannel channel, double value) {
        TransformType type = toType(channel);
        if (type == null) {
            return;
        }
            if (type == TransformType.LIT) {
                // 发光写的是方块状态，本地也要立刻改，否则要等服务端回调才有反馈
                type.modify(this.entity, slot, value);
                return;
            }
            this.entity.setSlot(slot);
            type.modify(this.entity.getTransformData(slot), value);
        }

    @Override
    public boolean supports(ModelingChannel channel) {
        return channel != ModelingChannel.COLOR_R && channel != ModelingChannel.COLOR_G
                && channel != ModelingChannel.COLOR_B && channel != ModelingChannel.ENABLE_BLOCK;
    }

    private static @Nullable TransformType toType(ModelingChannel channel) {
        return switch (channel) {
            case POS_X -> TransformType.POS_X;
            case POS_Y -> TransformType.POS_Y;
            case POS_Z -> TransformType.POS_Z;
            case ROT_X -> TransformType.ROT_X;
            case ROT_Y -> TransformType.ROT_Y;
            case ROT_Z -> TransformType.ROT_Z;
            case SCALE_X -> TransformType.SCALE_X;
            case SCALE_Y -> TransformType.SCALE_Y;
            case SCALE_Z -> TransformType.SCALE_Z;
            case LIT -> TransformType.LIT;
            case SHOWN -> TransformType.SHOWN;
            case COLOR_R, COLOR_G, COLOR_B, ENABLE_BLOCK -> null;
        };
    }

    // ---- 同步 ----

    private void queue(TransformType type, double value) {
        int index = this.pendingTypes.indexOf(type);
        if (index >= 0) {
            this.pendingValues.set(index, value);
            return;
        }
        this.pendingTypes.add(type);
        this.pendingValues.add(value);
    }

    @Override
    public void flush() {
        if (this.pendingTypes.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.pendingTypes.size(); i++) {
            TransformDataOncePacket.sendToServerSide(pos(), slot(), this.pendingTypes.get(i),
                    this.pendingValues.get(i));
        }
        this.pendingTypes.clear();
        this.pendingValues.clear();
    }

    @Override
    public void commit() {
        TransformDataOncePacket.sendToServerSideSuccess(pos());
    }

    @Override
    public void resync() {
        Level level = level();
        if (level != null) {
            level.sendBlockUpdated(pos(), this.entity.getBlockState(), this.entity.getBlockState(),
                    Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public void resend(int slot, ITransformDataProvider data) {
        if (!(data instanceof TransformBlockData block)) {
            return;
        }
        BlockPos pos = pos();
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.POS_X, block.pos.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.POS_Y, block.pos.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.POS_Z, block.pos.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.ROT_X, block.rot.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.ROT_Y, block.rot.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.ROT_Z, block.rot.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SCALE_X, block.scales.x);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SCALE_Y, block.scales.y);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SCALE_Z, block.scales.z);
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.BLOCK_STATE, Block.getId(block.blockState));
        TransformDataOncePacket.sendToServerSide(pos, slot, TransformType.SHOWN, block.isShown ? 1 : 0);
    }

    @Override
    public void clearTail(int from, int oldSize) {
        for (int i = from; i < oldSize; i++) {
            TransformDataOncePacket.sendToServerSide(pos(), i, TransformType.REMOVE, 0.0);
        }
    }

        /**
         * 交换两个槽位的整个元素。
         * <p>
         * 内容（方块状态）也一起交换 —— 上移 / 下移的语义就是"这一行整体挪位"，
         * 只换几何会把内容留在原地。
         */
        @Override
        public void swap(int a, int b) {
            List<TransformBlockData> data = this.entity.getTransformData();
            TransformBlockData first = data.get(a);
            TransformBlockData second = data.get(b);
            TransformBlockData copy = new TransformBlockData(first.pos, first.rot, first.scales,
                    first.blockState, first.isShown);
            first.set(second);
            second.set(copy);
            resend(a, first);
            resend(b, second);
        }

    // ---- 视口 / 统计 ----

    @Override
    public List<ElementView> views() {
        List<TransformBlockData> data = this.entity.getTransformData();
        List<ElementView> views = new ArrayList<>(data.size());
        for (TransformBlockData entry : data) {
            BlockState state = entry.blockState.isAir() ? null : entry.blockState;
            views.add(new ElementView(entry, state, ItemStack.EMPTY, -1));
        }
        return views;
    }

    @Override
    public long faceCount() {
        return ShowBlockScreen.calculateFaces(this.entity);
    }

    // ---- 序列化 ----

    @Override
    public String serialize() {
        return ShareUtils.transfer(this.entity.getTransformData());
    }

    @Override
    public boolean paste(String json) {
        ShareUtils.ShareBlockInformation info;
        try {
            info = ShareUtils.from(json);
        } catch (Exception e) {
            return false;
        }
        if (info == null || info.blocks().isEmpty()) {
            return false;
        }
        int oldSize = size();
        List<TransformBlockData> data = this.entity.getTransformData();
        info.transfer(data);
        if (data.isEmpty()) {
            data.add(new TransformBlockData());
        }
        for (int i = 0; i < data.size(); i++) {
            resend(i, data.get(i));
        }
        clearTail(data.size(), oldSize);
        if (slot() >= size()) {
            setSlot(Math.max(0, size() - 1));
        }
        return true;
    }

    @Override
    public void saveToFile(String name) throws IOException {
        EngraveBlockResultLoader.saveBlock(serialize(), name);
    }

    // ---- 元素属性区 ----

    @Override
    public @Nullable BlockState contentState(int slot) {
        return isEmptyElement(slot) ? null : element(slot).blockState;
    }

    @Override
    public List<PropertyRow> properties(int slot) {
        BlockState state = contentState(slot);
        if (state == null) {
            return List.of();
        }
        List<PropertyRow> rows = new ArrayList<>();
        for (Property<?> property : state.getBlock().getStateDefinition().getProperties()) {
            // 发光与形状由独立控件负责，不在通用属性列表里重复出现
            if (property == YuushyaBlockStates.LIT || property == YuushyaBlockStates.SHAPES) {
                continue;
            }
            rows.add(new PropertyRow(
                    Component.literal(property.getName()),
                    Component.literal(nameOf(state, property)),
                    () -> cycleContent(slot, property, true),
                    () -> cycleContent(slot, property, false)));
        }
        return rows;
    }

    private static <T extends Comparable<T>> String nameOf(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private <T extends Comparable<T>> void cycleContent(int slot, Property<T> property, boolean previous) {
        BlockState state = contentState(slot);
        if (state == null) {
            return;
        }
        applyContent(slot, state.setValue(property, YuushyaBlockStates.getRelative(
                property.getPossibleValues(), state.getValue(property), previous)));
    }

    private void applyContent(int slot, BlockState state) {
        element(slot).blockState = state;
        TransformDataOncePacket.sendToServerSide(pos(), slot, TransformType.BLOCK_STATE, Block.getId(state));
        resync();
    }

    @Override
    public Component cycleShape() {
        BlockState state = this.entity.getBlockState();
        BlockState next = state.setValue(YuushyaBlockStates.SHAPES, YuushyaBlockStates.getRelative(
                List.of(BlockShape.values()), state.getValue(YuushyaBlockStates.SHAPES), true));
        Level level = level();
        if (level != null) {
            level.setBlock(pos(), next, 18);
        }
        return shapeLabel();
    }

    @Override
    public Component shapeLabel() {
        return this.entity.getBlockState().getValue(YuushyaBlockStates.SHAPES).getSymbol();
    }

    @Override
    public List<TransformRow> rows() {
        return List.of(
                TransformRow.Vector.unbounded(Component.translatable("gui.yuushya.modeling.pos"),
                        ModelingChannel.POS_X, ModelingChannel.POS_Y, ModelingChannel.POS_Z, 0.5, 1),
                TransformRow.Vector.unbounded(Component.translatable("gui.yuushya.modeling.rot"),
                        ModelingChannel.ROT_X, ModelingChannel.ROT_Y, ModelingChannel.ROT_Z, 1.0, 0),
                TransformRow.Vector.unbounded(Component.translatable("gui.yuushya.modeling.scale"),
                        ModelingChannel.SCALE_X, ModelingChannel.SCALE_Y, ModelingChannel.SCALE_Z, 0.02, 1),
                new TransformRow.Single(Component.translatable("gui.yuushya.modeling.lit"),
                        ModelingChannel.LIT, LIT_MIN, LIT_MAX, 1.0, 0, 46));
    }

    // ---- 物品 → 方块状态 ----

    /** 方块物品取其状态（含 BLOCK_STATE 组件），否则看模组自带的 BLOCKSTATE 组件。 */
    public static @Nullable BlockState stateOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        BlockState state;
        if (stack.getItem() instanceof BlockItem blockItem) {
            state = blockItem.getBlock().defaultBlockState();
            BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
            if (props != null) {
                state = props.apply(state);
            }
        } else {
            var holder = DataComponentRegistry.BLOCKSTATE;
            state = holder.isBound() ? stack.get(holder.get()) : null;
        }
        return state == null || state.isAir() ? null : state;
            }
        }
