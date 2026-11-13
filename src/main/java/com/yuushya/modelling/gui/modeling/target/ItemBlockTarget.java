package com.yuushya.modelling.gui.modeling.target;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.gui.engrave.EngraveItemResultLoader;
import com.yuushya.modelling.network.ItemStackPacket;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.utils.ShareUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ModelingTarget} 的 ItemBlock 实现。
 * <p>
 * 元素内容是"物品栈"，同步走 {@link ItemTransformDataOncePacket} 加独立的
 * {@link ItemStackPacket}（内容变更无法用 double 表达），序列化走
 * {@link ShareUtils#transferItems(List)}，存盘进 {@code modellings/items}。
 * <p>
 * 比 ShowBlock 多三样东西：元素颜色（RGB 三个数字框）、"物品/方块"归属开关，
 * 以及内容为方块时才可用的方块状态属性。
 */
public final class ItemBlockTarget implements ModelingTarget {

    private static final double LIT_MIN = 0.0;
    private static final double LIT_MAX = 15.0;

    private final ItemBlockEntity entity;
    private final List<ItemTransformType> pendingTypes = new ArrayList<>();
    private final List<Double> pendingValues = new ArrayList<>();
    /** 待发送的内容变更（每个槽位最多一条）。 */
    private final List<Integer> pendingStacks = new ArrayList<>();
    private final List<ItemStack> pendingStackValues = new ArrayList<>();


    public ItemBlockTarget(ItemBlockEntity entity) {
        this.entity = entity;
    }

    public ItemBlockEntity itemBlock() {
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
        ItemStack stack = contentStack(this.slot());
        return stack.isEmpty()
                ? Component.translatable("gui.yuushya.modeling.empty")
                : stack.getHoverName();
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
    public TransformItemData element(int slot) {
        return this.entity.getTransformData(slot);
    }

    @Override
    public int appendEmpty() {
        this.entity.addTransformData(new TransformItemData());
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
        return stack != null && !stack.isEmpty();
    }

    @Override
    public void setContent(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        element(slot).itemStack = stack.copy();
        queueStack(slot, stack.copy());
    }

    /** 元素当前的物品栈；空元素返回 {@link ItemStack#EMPTY}。 */
    public ItemStack contentStack(int slot) {
        return isEmptyElement(slot) ? ItemStack.EMPTY : element(slot).itemStack;
    }

    // ---- 通道读写 ----

    @Override
    public double get(int slot, ModelingChannel channel) {
        if (channel == ModelingChannel.LIT) {
            return this.entity.getBlockState().getValue(YuushyaBlockStates.LIT);
        }
        TransformItemData data = element(slot);
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
            case ENABLE_BLOCK -> data.enableBlock ? 1.0 : 0.0;
            case COLOR_R -> FastColor.ARGB32.red(data.color);
            case COLOR_G -> FastColor.ARGB32.green(data.color);
            case COLOR_B -> FastColor.ARGB32.blue(data.color);
            case LIT -> 0.0;
        };
    }

    @Override
    public void set(int slot, ModelingChannel channel, double value) {
        setLocal(slot, channel, value);
        ItemTransformType type = toType(channel);
        if (type != null) {
            queue(type, channel.isColor() ? element(slot).color : value);
        }
    }

    @Override
    public void setLocal(int slot, ModelingChannel channel, double value) {
        ItemTransformType type = toType(channel);
        if (type == null) {
            return;
        }
        if (type == ItemTransformType.LIT) {
            type.modify(this.entity, slot, value);
            return;
        }
        if (type == ItemTransformType.COLOR) {
            applyColorComponent(slot, channel, value);
            return;
        }
        this.entity.setSlot(slot);
        type.modify(this.entity.getTransformData(slot), value);
    }

    /** RGB 三个通道合成一个 int 写回元素。 */
    private void applyColorComponent(int slot, ModelingChannel channel, double value) {
        TransformItemData data = element(slot);
        int r = FastColor.ARGB32.red(data.color);
        int g = FastColor.ARGB32.green(data.color);
        int b = FastColor.ARGB32.blue(data.color);
        int v = (int) Math.round(value);
        switch (channel) {
            case COLOR_R -> r = v;
            case COLOR_G -> g = v;
            case COLOR_B -> b = v;
            default -> {
                return;
            }
        }
        data.color = FastColor.ARGB32.color(255, r, g, b);
    }

    @Override
    public boolean supports(ModelingChannel channel) {
        return true;
    }

    private static @Nullable ItemTransformType toType(ModelingChannel channel) {
        return switch (channel) {
            case POS_X -> ItemTransformType.POS_X;
            case POS_Y -> ItemTransformType.POS_Y;
            case POS_Z -> ItemTransformType.POS_Z;
            case ROT_X -> ItemTransformType.ROT_X;
            case ROT_Y -> ItemTransformType.ROT_Y;
            case ROT_Z -> ItemTransformType.ROT_Z;
            case SCALE_X -> ItemTransformType.SCALE_X;
            case SCALE_Y -> ItemTransformType.SCALE_Y;
            case SCALE_Z -> ItemTransformType.SCALE_Z;
            case LIT -> ItemTransformType.LIT;
            case SHOWN -> ItemTransformType.SHOWN;
            case ENABLE_BLOCK -> ItemTransformType.ENABLE_BLOCK;
            case COLOR_R, COLOR_G, COLOR_B -> ItemTransformType.COLOR;
        };
    }

    // ---- 同步 ----

    private void queue(ItemTransformType type, double value) {
        int index = this.pendingTypes.indexOf(type);
        if (index >= 0) {
            this.pendingValues.set(index, value);
            return;
        }
        this.pendingTypes.add(type);
        this.pendingValues.add(value);
    }

    private void queueStack(int slot, ItemStack stack) {
        int index = this.pendingStacks.indexOf(slot);
        if (index >= 0) {
            this.pendingStackValues.set(index, stack);
            return;
        }
        this.pendingStacks.add(slot);
        this.pendingStackValues.add(stack);
    }

    @Override
    public void flush() {
        BlockPos pos = pos();
        for (int i = 0; i < this.pendingTypes.size(); i++) {
            ItemTransformDataOncePacket.sendToServerSide(pos, slot(), this.pendingTypes.get(i),
                    this.pendingValues.get(i));
        }
        this.pendingTypes.clear();
        this.pendingValues.clear();

        for (int i = 0; i < this.pendingStacks.size(); i++) {
            PacketDistributor.sendToServer(new ItemStackPacket(pos, this.pendingStacks.get(i),
                    this.pendingStackValues.get(i)));
        }
        this.pendingStacks.clear();
        this.pendingStackValues.clear();
    }

    @Override
    public void commit() {
        ItemTransformDataOncePacket.sendToServerSideSuccess(pos());
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
        if (!(data instanceof TransformItemData item)) {
            return;
        }
        BlockPos pos = pos();
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.POS_X, item.pos.x);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.POS_Y, item.pos.y);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.POS_Z, item.pos.z);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.ROT_X, item.rot.x);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.ROT_Y, item.rot.y);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.ROT_Z, item.rot.z);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.SCALE_X, item.scales.x);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.SCALE_Y, item.scales.y);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.SCALE_Z, item.scales.z);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.SHOWN, item.isShown ? 1 : 0);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.ENABLE_BLOCK, item.enableBlock ? 1 : 0);
        ItemTransformDataOncePacket.sendToServerSide(pos, slot, ItemTransformType.COLOR, item.color);
        PacketDistributor.sendToServer(new ItemStackPacket(pos, slot, item.itemStack));
    }

    @Override
    public void clearTail(int from, int oldSize) {
        for (int i = from; i < oldSize; i++) {
            ItemTransformDataOncePacket.sendToServerSide(pos(), i, ItemTransformType.REMOVE, 0.0);
        }
    }

        @Override
        public void swap(int a, int b) {
            List<TransformItemData> data = this.entity.getTransformData();
            TransformItemData first = data.get(a);
            TransformItemData second = data.get(b);
            TransformItemData copy = new TransformItemData(first.pos, first.rot, first.scales,
                    first.itemStack, first.color, first.isShown, first.enableBlock);
            first.set(second);
            second.set(copy);
            resend(a, first);
            resend(b, second);
        }

    // ---- 视口 / 统计 ----

    @Override
    public List<ElementView> views() {
        List<TransformItemData> data = this.entity.getTransformData();
        List<ElementView> views = new ArrayList<>(data.size());
        for (TransformItemData entry : data) {
            views.add(new ElementView(entry, null, entry.itemStack, entry.color));
        }
        return views;
    }

    @Override
    public long faceCount() {
        Minecraft minecraft = Minecraft.getInstance();
            RandomSource random = RandomSource.create(42L);
            long total = 0;
            for (TransformItemData data : this.entity.getTransformData()) {
                if (!data.isShown || data.itemStack.isEmpty()) {
                    continue;
                }
                BakedModel model = minecraft.getItemRenderer().getModel(data.itemStack, null, null, 0);
                for (Direction side : Direction.values()) {
                    total += model.getQuads(null, side, random).size();
                }
                total += model.getQuads(null, null, random).size();
            }
            return total;
        }

    // ---- 序列化 ----

    @Override
    public String serialize() {
        return ShareUtils.transferItems(this.entity.getTransformData());
    }

    @Override
    public boolean paste(String json) {
        ShareUtils.ShareItemInformation info;
        try {
            info = ShareUtils.fromItems(json);
        } catch (Exception e) {
            return false;
        }
        if (info == null || info.items().isEmpty()) {
            return false;
        }
        int oldSize = size();
        List<TransformItemData> data = this.entity.getTransformData();
        info.transferItems(data);
        if (data.isEmpty()) {
            data.add(new TransformItemData());
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
        EngraveItemResultLoader.saveItem(serialize(), name);
    }

    // ---- 元素属性区 ----

    /** 只有元素是方块且开启了方块渲染时才有方块状态可编辑。 */
    @Override
    public @Nullable BlockState contentState(int slot) {
        if (isEmptyElement(slot)) {
            return null;
        }
        TransformItemData data = element(slot);
        if (!data.enableBlock) {
            return null;
        }
        return ShowBlockTarget.stateOf(data.itemStack);
    }

    @Override
    public List<PropertyRow> properties(int slot) {
        BlockState state = contentState(slot);
        if (state == null) {
            return List.of();
        }
        List<PropertyRow> rows = new ArrayList<>();
        for (Property<?> property : state.getBlock().getStateDefinition().getProperties()) {
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
        BlockState next = state.setValue(property, YuushyaBlockStates.getRelative(
                property.getPossibleValues(), state.getValue(property), previous));

            // 保留全部属性，只把这一项换成新值，否则来回切换会丢掉其它属性
            BlockItemStateProperties props = BlockItemStateProperties.EMPTY;
            for (Property<?> each : next.getProperties()) {
                props = withProperty(props, next, each);
        }
            ItemStack stack = new ItemStack(next.getBlock().asItem(), element(slot).itemStack.getCount());
            if (!stack.isEmpty()) {
                stack.set(DataComponents.BLOCK_STATE, props);
                setContent(slot, stack);
            }
            resync();
        }

        private static BlockItemStateProperties withProperty(
                BlockItemStateProperties props, BlockState state, Property<?> property) {
            return props.with(property, state);
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
                        ModelingChannel.LIT, LIT_MIN, LIT_MAX, 1.0, 0, 46),
                new TransformRow.Vector(Component.translatable("gui.yuushya.modeling.color"),
                        ModelingChannel.COLOR_R, ModelingChannel.COLOR_G, ModelingChannel.COLOR_B,
                        0.0, 255.0, 1.0, 0));
    }

        /** 物品 / 方块归属开关：物品形态下没有方块状态可改。 */
        @Override
        public List<PropertyRow> toggles(int slot) {
            if (isEmptyElement(slot)) {
                return List.of();
            }
            boolean enabled = element(slot).enableBlock;
            return List.of(new PropertyRow(
                    Component.translatable("gui.yuushya.modeling.enableBlock"),
                    Component.translatable(enabled ? "gui.yuushya.modeling.enableBlock.on"
                            : "gui.yuushya.modeling.enableBlock.off"),
                    () -> set(slot, ModelingChannel.ENABLE_BLOCK, 0.0),
                    () -> set(slot, ModelingChannel.ENABLE_BLOCK, 1.0)));
        }
    }
