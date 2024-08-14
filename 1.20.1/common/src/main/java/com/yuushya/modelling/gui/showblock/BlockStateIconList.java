package com.yuushya.modelling.gui.showblock;

import com.mojang.logging.LogUtils;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStateIconList extends ObjectSelectionList<BlockStateIconList.Entry> {

    private static final Logger LOGGER = LogUtils.getLogger();
    protected final List<TransformData> transformDataList;
    protected final ShowBlockScreen screen;
    private int itemWidth;
    public BlockStateIconList(Minecraft minecraft, int width, int height,int x , int y0, int y1,int itemWidth, int itemHeight,
                                List<TransformData> transformDataList, ShowBlockScreen showBlockScreen
    ) {
        super(minecraft, width, height, y0, y1, itemHeight);
        this.setLeftPos(x);
        this.transformDataList = transformDataList;
        this.screen = showBlockScreen;
        this.centerListVertically = false;
        this.setRenderTopAndBottom(false);
        this.setRenderBackground(false);
        this.itemWidth = itemWidth;
        this.updateRenderList();
    }

    @Override
    public int getRowWidth() { return itemWidth; }

    public void updateRenderList(){
        this.clearEntries();
        for(int i=0;i<transformDataList.size();i++){
            this.addEntry(new Entry(this,i));
        }
        this.notifyListUpdated();
    }
    private void notifyListUpdated() {
    }

    @Override
    public void setSelected(@Nullable BlockStateIconList.Entry selected) {
        super.setSelected(selected);
        if (selected != null) {
            this.screen.setSlot(selected.slot);
        }
    }

    public static final class Entry extends ObjectSelectionList.Entry<Entry>{

        private final BlockStateIconList parent;
        private final int slot;
        private final Minecraft minecraft;
        private Map<Integer,ItemStack> rememberItemStack = new HashMap<>();
        public BlockState updateRenderState(){
            return (parent.transformDataList.size() > slot)? parent.transformDataList.get(slot).blockState : Blocks.AIR.defaultBlockState();
        }
        public ItemStack updateRenderItemstack(BlockState blockState){
            return rememberItemStack.computeIfAbsent(Block.getId(blockState),(id)->{
                ItemStack itemStack = YuushyaRegistries.ITEMS.get("get_blockstate_item").get().getDefaultInstance();
                CompoundTag compoundTag = itemStack.getOrCreateTag();
                compoundTag.put("BlockState",NbtUtils.writeBlockState(Block.stateById(id)));
                itemStack.setTag(compoundTag);
                return itemStack;
            });
        }

        public Entry(BlockStateIconList parent,int slot){
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        @Override
        public Component getNarration() {
            return Component.literal("test");
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.parent.setSelected(this);
            //LOGGER.info("select "+this.slot);
            return true;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            BlockState blockState = updateRenderState();
            Item item = blockState.getBlock().asItem();
            MutableComponent displayName = (item== Items.AIR) ? blockState.getBlock().getName() : (MutableComponent)item.getName(item.getDefaultInstance());
            MutableComponent displayBlockState = Component.literal(YuushyaUtils.getBlockStateProperties(blockState));
            guiGraphics.drawString(this.minecraft.font, displayName, left + 32 + 3, top + 1, 0xFFFFFF, false);
            guiGraphics.drawString(this.minecraft.font, displayBlockState, left + 32 + 3, top + this.minecraft.font.lineHeight+1, 0xFFEBC6, false);
            guiGraphics.fill(left, top, left + 32, top + 32, -1601138544);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + 16, top + 16, top);
            guiGraphics.pose().scale(32.0f, 32.0f, 32.0f);
            //
            //BlockRenderDispatcher blockRenderDispatcher = this.minecraft.getBlockRenderer();
            //BakedModel model = blockRenderDispatcher.getBlockModel(blockState);
            ItemStack itemStack = updateRenderItemstack(blockState);
            BakedModel model = this.minecraft.getItemRenderer().getModel(itemStack, this.minecraft.level, null, this.minecraft.player.getId());
            this.minecraft.getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
            guiGraphics.pose().popPose();
        }
    }
}
