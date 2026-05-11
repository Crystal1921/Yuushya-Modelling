package com.yuushya.modelling.blockentity.itemblock;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformItemDataInventory;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.gui.engrave.EngraveItemResult;
import com.yuushya.modelling.registries.BlockEntityRegistry;
import com.yuushya.modelling.utils.CustomRenderInstance;
import com.yuushya.modelling.utils.ShareUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.LIT;
import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_AO;
import static com.yuushya.modelling.blockentity.AbstractTransformBlock.ENABLE_SPECIAL_RENDER;
import static com.yuushya.modelling.item.showblocktool.HistoryItem.HISTORY_ITEMBLOCK_ITEM_MAP;

public class ItemBlockEntity extends AbstractTransformBlockEntity implements ITransformItemDataInventory {
    @Getter
    private final List<TransformItemData> transformData;

    public ItemBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityRegistry.ITEM_BLOCK_ENTITY.get(), blockPos, blockState);
        transformData = new ArrayList<>();
        transformData.add(new TransformItemData());
        slot = 0;
    }

    @NotNull
    public TransformItemData getTransFormDataNow() {
        return getTransformData(slot);
    }

    @Override
    public void setSlot(int slot) {
        if (slot >= transformData.size()) {
            for (int i = slot - transformData.size() + 1; i > 0; i--)
                transformData.add(new TransformItemData());
        }
        this.slot = slot;
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ITransformItemDataInventory.load(input, transformData, this.getLevel().registryAccess());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ITransformItemDataInventory.saveAdditional(output, transformData, this.getLevel().registryAccess());
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public void writeBlockState(ItemStack itemStack, BlockState blockState) {
        BlockItemStateProperties blockItemStateProperties = BlockItemStateProperties.EMPTY;
        itemStack.set(DataComponents.BLOCK_STATE, blockItemStateProperties
                .with(LIT, blockState.getValue(LIT))
                .with(SHAPES, blockState.getValue(SHAPES))
                .with(ENABLE_AO, blockState.getValue(ENABLE_AO))
                .with(ENABLE_SPECIAL_RENDER, blockState.getValue(ENABLE_SPECIAL_RENDER)));
    }

    public void setRemoved() {
        if (this.level != null && this.level.isClientSide()) {
            CustomRenderInstance.getINSTANCE().dirty = true;
            if (!this.isEmpty()) {
                String res = ShareUtils.transferItems(this.getTransformData());
                ShareUtils.ShareItemInformation information = ShareUtils.fromItems(res);
                if (this.level != null) {
                    String name = this.level.dimension().identifier() + "/" + this.getBlockPos().toShortString();
                    HISTORY_ITEMBLOCK_ITEM_MAP.put(name, new EngraveItemResult(name, information));
                }
            }
        }
        super.setRemoved();
    }
}
