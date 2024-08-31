package com.yuushya.modelling.gui.showblock;

import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.TransformData;
import com.yuushya.modelling.registries.YuushyaRegistries;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.MultiBufferSource;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yuushya.modelling.utils.YuushyaUtils.PROPERTY_ENTRY_TO_STRING_FUNCTION;

public class BlockStateIconList extends ObjectSelectionList<BlockStateIconList.Entry> {

    //private static final Logger LOGGER = LogUtils.getLogger();
    protected final List<TransformData> transformDataList;
    protected final ShowBlockScreen screen;
    private int itemHeight;
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
        this.itemHeight = itemHeight;
        this.updateRenderList();
    }


    private final Map<Integer,MutableComponent> rememberDisplayName = new HashMap<>();
    public MutableComponent updateRenderDisplayName(BlockState blockState){
        return rememberDisplayName.computeIfAbsent(Block.getId(blockState),
                (id)->{
                    BlockState blockState1 = Block.stateById(id);
                    Item item = blockState1.getBlock().asItem();
                    return (item== Items.AIR) ? blockState1.getBlock().getName() : (MutableComponent)item.getName(item.getDefaultInstance());
                });
    }
    private final Map<Integer,List<String>> rememberBlockStateProperties = new HashMap<>();
    public List<String> updateRenderBlockStateProperties(BlockState blockState){
        return rememberBlockStateProperties.computeIfAbsent(Block.getId(blockState),
                (id)->Block.stateById(id).getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).toList());
    }
    private final Map<Integer,ItemStack> rememberItemStack = new HashMap<>();
    public ItemStack updateRenderItemstack(BlockState blockState){
        return rememberItemStack.computeIfAbsent(Block.getId(blockState),(id)->{
            ItemStack itemStack = YuushyaRegistries.ITEMS.get("get_blockstate_item").get().getDefaultInstance();
            CompoundTag compoundTag = itemStack.getOrCreateTag();
            compoundTag.put("BlockState",NbtUtils.writeBlockState(Block.stateById(id)));
            itemStack.setTag(compoundTag);
            return itemStack;
        });
    }
    private final Map<Integer, Collection<Property<?>>> rememberProperties = new HashMap<>();
    public Collection<Property<?>> updateRenderProperties(BlockState blockState){
        return rememberProperties.computeIfAbsent(Block.getId(blockState),(id)->
                Block.stateById(id).getBlock().getStateDefinition().getProperties());
    }

    @Override
    public int getRowWidth() { return itemWidth; }

    @Override
    protected int getScrollbarPosition() {
        return this.x1 - 4;
    }

    public void updateRenderList(){
        this.clearEntries();
        for(int i=0;i<transformDataList.size();i++){
            this.addEntry(new Entry(this,i));
        }
        this.notifyListUpdated();
    }

    public void addSlot(){
        if( this.getSelected()!= null && transformDataList.get(this.getSelected().slot).blockState.getBlock() == Blocks.AIR){
            return;
        }
        if(children().size() == transformDataList.size()){
            Entry entry = new Entry(this,transformDataList.size());
            this.addEntry(entry);
            this.setSelected(entry);
        }
    }

    private void notifyListUpdated() {
    }

    public void setSelectedSlot(int slot){
        this.setSelected(this.children().get(slot));
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
        public BlockState updateRenderState(){
            return (parent.transformDataList.size() > slot)? parent.transformDataList.get(slot).blockState : Blocks.AIR.defaultBlockState();
        }
        public Entry(BlockStateIconList parent,int slot){
            this.parent = parent;
            this.minecraft = parent.minecraft;
            this.slot = slot;
        }

        @Override
        public Component getNarration() {
            BlockState blockState = updateRenderState();
            Item item = blockState.getBlock().asItem();
            MutableComponent displayName = (item== Items.AIR) ? blockState.getBlock().getName() : (MutableComponent)item.getName(item.getDefaultInstance());
            return displayName;
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
            MutableComponent displayName = this.parent.updateRenderDisplayName(blockState);
            Font font = this.minecraft.font;
            int fontHeight = font.lineHeight;
            guiGraphics.drawString(font, displayName, left + 3, top + 32, 0xFFFFFF, false);
            //guiGraphics.drawString(this.minecraft.font, displayName, left + 32 + 3, top + 1, 0xFFFFFF, false);
            //MutableComponent displayBlockState = Component.literal(YuushyaUtils.getBlockStateProperties(blockState));
            //guiGraphics.drawString(this.minecraft.font, displayBlockState, left + 32 + 3, top + this.minecraft.font.lineHeight+1, 0xFFEBC6, false);
//            List<String> properties = this.parent.updateRenderBlockStateProperties(blockState);
//            for(int i=0;i<properties.size();i++){
//                if(this.parent.itemHeight < this.minecraft.font.lineHeight*(i+2)) break;
//                MutableComponent displayBlockState = Component.literal(properties.get(i));
//                guiGraphics.drawString(this.minecraft.font, displayBlockState, left + 32 + 3, top + this.minecraft.font.lineHeight*(i+1)+1, 0xFFEBC6, false);
//            }
            guiGraphics.fill(left, top, left + 32 + 4, top + fontHeight + 32, -1601138544);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + 16, top  + 16, 32);
            guiGraphics.pose().scale(32.0f, 32.0f, 32.0f);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
            //
            //BlockRenderDispatcher blockRenderDispatcher = this.minecraft.getBlockRenderer();
            //BakedModel model = blockRenderDispatcher.getBlockModel(blockState);
            ItemStack itemStack = this.parent.updateRenderItemstack(blockState);
            BakedModel model = this.minecraft.getItemRenderer().getModel(itemStack, this.minecraft.level, null, this.minecraft.player.getId());
            this.minecraft.getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
            guiGraphics.pose().popPose();


        }

    }
}
