package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.item.AbstractYuushyaItem;
import com.yuushya.modelling.item.TooltipBlockItem;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.item.showblocktool.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Yuushya.MOD_ID);

    // Tools & Utils
    public static final DeferredItem<Item> GET_BLOCKSTATE_ITEM = ITEMS.registerItem("get_blockstate_item", (properties) -> new GetBlockStateItem(properties.stacksTo(1), 3));
    public static final DeferredItem<Item> POS_TRANS_ITEM = ITEMS.registerItem("pos_trans_item", (properties) -> new PosTransItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> MICRO_POS_TRANS_ITEM = ITEMS.registerItem("micro_pos_trans_item", (properties) -> new MicroPosTransItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> ROT_TRANS_ITEM = ITEMS.registerItem("rot_trans_item", (properties) -> new RotTransItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> SCALE_TRANS_ITEM = ITEMS.registerItem("scale_trans_item", (properties) -> new ScaleTransItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> SLOT_TRANS_ITEM = ITEMS.registerItem("slot_trans_item", (properties) -> new SlotTransItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> GET_SHOWBLOCK_ITEM = ITEMS.registerItem("get_showblock_item", (properties) -> new GetShowBlockEntityItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> MOVE_TRANSFORMDATA_ITEM = ITEMS.registerItem("move_transformdata_item", (properties) -> new MoveTransformDataItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> DEBUG_STICK_ITEM = ITEMS.registerItem("debug_stick_item", (properties) -> new YuushyaDebugStickItem(properties.stacksTo(1), 4));
    public static final DeferredItem<Item> GET_LIT_ITEM = ITEMS.registerItem("get_lit_item", (properties) -> new GetLitItem(properties.stacksTo(1), 2));
    public static final DeferredItem<Item> DESTROY_ITEM = ITEMS.registerItem("destroy_item", (properties) -> new DestroyItem(properties.stacksTo(1).durability(384), 2));
    public static final DeferredItem<Item> GUI_ITEM = ITEMS.registerItem("gui_item", (properties) -> new GuiItem(properties.stacksTo(1), 2));
    public static final DeferredItem<Item> ENGRAVE_ITEM = ITEMS.registerItem("engrave_item", (properties) -> new EngraveItem(properties.stacksTo(1), 3));
    public static final DeferredItem<Item> COLOR_PICKER_ITEM = ITEMS.registerItem("color_picker_item", (properties) -> new ColorPickerItem(properties.stacksTo(1), 2));
    public static final DeferredItem<Item> HISTORY_ITEM = ITEMS.registerItem("history_item", (properties) -> new HistoryItem(properties.stacksTo(1), 2));
    public static final DeferredItem<Item> SHAPE_ITEM = ITEMS.registerItem("shape_item", (properties) -> new ShapeItem(properties.stacksTo(1), 3));

    // Special Items
    public static final DeferredItem<Item> THE_ENCYCLOPEDIA = ITEMS.registerItem("the_encyclopedia", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 1));
    public static final DeferredItem<Item> SHIMMERING_PEARL = ITEMS.registerItem("shimmering_pearl", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 1));
    public static final DeferredItem<Item> EVERLASTING_WOOD = ITEMS.registerItem("everlasting_wood", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 1));
    public static final DeferredItem<Item> SPROUTING_DIRT = ITEMS.registerItem("sprouting_dirt", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 1));
    public static final DeferredItem<Item> FLOATING_BLOOM = ITEMS.registerItem("floating_bloom", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 1));
    public static final DeferredItem<Item> SPARKING_FLAME = ITEMS.registerItem("sparking_flame", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 1));

    // Chibi
    public static final DeferredItem<Item> CHIBI_0 = ITEMS.registerItem("chibi_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_0_ALT = ITEMS.registerItem("chibi_0_alt", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_1 = ITEMS.registerItem("chibi_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_2 = ITEMS.registerItem("chibi_2", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_3 = ITEMS.registerItem("chibi_3", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_4 = ITEMS.registerItem("chibi_4", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_5 = ITEMS.registerItem("chibi_5", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CHIBI_6 = ITEMS.registerItem("chibi_6", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));

    // Anatomy
    public static final DeferredItem<Item> ANATOMY_0 = ITEMS.registerItem("anatomy_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> ANATOMY_0_ALT = ITEMS.registerItem("anatomy_0_alt", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> ANATOMY_1 = ITEMS.registerItem("anatomy_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> ANATOMY_2 = ITEMS.registerItem("anatomy_2", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));

    // Cube
    public static final DeferredItem<Item> CUBE_0 = ITEMS.registerItem("cube_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_1 = ITEMS.registerItem("cube_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_2 = ITEMS.registerItem("cube_2", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_3 = ITEMS.registerItem("cube_3", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_4 = ITEMS.registerItem("cube_4", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_5 = ITEMS.registerItem("cube_5", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_6 = ITEMS.registerItem("cube_6", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_7 = ITEMS.registerItem("cube_7", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_8 = ITEMS.registerItem("cube_8", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_9 = ITEMS.registerItem("cube_9", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_10 = ITEMS.registerItem("cube_10", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CUBE_11 = ITEMS.registerItem("cube_11", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));

    // Triangle
    public static final DeferredItem<Item> TRIANGLE_0 = ITEMS.registerItem("triangle_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> TRIANGLE_1 = ITEMS.registerItem("triangle_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));

    // Cylinder
    public static final DeferredItem<Item> CYLINDER_0 = ITEMS.registerItem("cylinder_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CYLINDER_1 = ITEMS.registerItem("cylinder_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));

    // Circle
    public static final DeferredItem<Item> CIRCLE_0 = ITEMS.registerItem("circle_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CIRCLE_1 = ITEMS.registerItem("circle_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CIRCLE_HOLLOW_0 = ITEMS.registerItem("circle_hollow_0", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));
    public static final DeferredItem<Item> CIRCLE_HOLLOW_1 = ITEMS.registerItem("circle_hollow_1", (properties) -> new AbstractYuushyaItem(properties.stacksTo(16).rarity(Rarity.RARE), 0));

    // BlockItems
    public static final DeferredItem<Item> SHOW_BLOCK = ITEMS.registerItem("showblock", (properties) -> new TooltipBlockItem(BlockRegistry.SHOW_BLOCK.get(), properties, 1));
    public static final DeferredItem<Item> ITEM_BLOCK = ITEMS.registerItem("itemblock", (properties) -> new TooltipBlockItem(BlockRegistry.ITEM_BLOCK.get(), properties, 1));
    public static final DeferredItem<Item> TEXT_BLOCK = ITEMS.registerItem("textblock", (properties) -> new TooltipBlockItem(BlockRegistry.TEXT_BLOCK.get(), properties, 1));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}