package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.item.AbstractYuushyaItem;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.item.showblocktool.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Yuushya.MOD_ID);

    // Tools & Utils
    public static final RegistryObject<Item> GET_BLOCKSTATE_ITEM = ITEMS.register("get_blockstate_item", () -> new GetBlockStateItem(new Item.Properties().stacksTo(1), 3));
    public static final RegistryObject<Item> POS_TRANS_ITEM = ITEMS.register("pos_trans_item", () -> new PosTransItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> MICRO_POS_TRANS_ITEM = ITEMS.register("micro_pos_trans_item", () -> new MicroPosTransItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> ROT_TRANS_ITEM = ITEMS.register("rot_trans_item", () -> new RotTransItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> SCALE_TRANS_ITEM = ITEMS.register("scale_trans_item", () -> new ScaleTransItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> SLOT_TRANS_ITEM = ITEMS.register("slot_trans_item", () -> new SlotTransItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> GET_SHOWBLOCK_ITEM = ITEMS.register("get_showblock_item", () -> new GetShowBlockEntityItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> MOVE_TRANSFORMDATA_ITEM = ITEMS.register("move_transformdata_item", () -> new MoveTransformDataItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> DEBUG_STICK_ITEM = ITEMS.register("debug_stick_item", () -> new YuushyaDebugStickItem(new Item.Properties().stacksTo(1), 4));
    public static final RegistryObject<Item> GET_LIT_ITEM = ITEMS.register("get_lit_item", () -> new GetLitItem(new Item.Properties().stacksTo(1), 2));
    public static final RegistryObject<Item> DESTROY_ITEM = ITEMS.register("destroy_item", () -> new DestroyItem(new Item.Properties().stacksTo(1).durability(384), 2));
    public static final RegistryObject<Item> GUI_ITEM = ITEMS.register("gui_item", () -> new GuiItem(new Item.Properties().stacksTo(1), 2));
    public static final RegistryObject<Item> ENGRAVE_ITEM = ITEMS.register("engrave_item", () -> new EngraveItem(new Item.Properties().stacksTo(1), 3));
    public static final RegistryObject<Item> COLOR_PICKER_ITEM = ITEMS.register("color_picker_item", () -> new ColorPickerItem(new Item.Properties().stacksTo(1), 2));
    public static final RegistryObject<Item> HISTORY_ITEM = ITEMS.register("history_item", () -> new HistoryItem(new Item.Properties().stacksTo(1), 2));

    // Special Items
    public static final RegistryObject<Item> THE_ENCYCLOPEDIA = ITEMS.register("the_encyclopedia", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1));
    public static final RegistryObject<Item> SHIMMERING_PEARL = ITEMS.register("shimmering_pearl", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1));
    public static final RegistryObject<Item> EVERLASTING_WOOD = ITEMS.register("everlasting_wood", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1));
    public static final RegistryObject<Item> SPROUTING_DIRT = ITEMS.register("sprouting_dirt", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1));
    public static final RegistryObject<Item> FLOATING_BLOOM = ITEMS.register("floating_bloom", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1));
    public static final RegistryObject<Item> SPARKING_FLAME = ITEMS.register("sparking_flame", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 1));

    // Chibi
    public static final RegistryObject<Item> CHIBI_0 = ITEMS.register("chibi_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_0_ALT = ITEMS.register("chibi_0_alt", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_1 = ITEMS.register("chibi_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_2 = ITEMS.register("chibi_2", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_3 = ITEMS.register("chibi_3", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_4 = ITEMS.register("chibi_4", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_5 = ITEMS.register("chibi_5", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CHIBI_6 = ITEMS.register("chibi_6", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));

    // Anatomy
    public static final RegistryObject<Item> ANATOMY_0 = ITEMS.register("anatomy_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> ANATOMY_0_ALT = ITEMS.register("anatomy_0_alt", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> ANATOMY_1 = ITEMS.register("anatomy_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> ANATOMY_2 = ITEMS.register("anatomy_2", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));

    // Cube
    public static final RegistryObject<Item> CUBE_0 = ITEMS.register("cube_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_1 = ITEMS.register("cube_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_2 = ITEMS.register("cube_2", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_3 = ITEMS.register("cube_3", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_4 = ITEMS.register("cube_4", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_5 = ITEMS.register("cube_5", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_6 = ITEMS.register("cube_6", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_7 = ITEMS.register("cube_7", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_8 = ITEMS.register("cube_8", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_9 = ITEMS.register("cube_9", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_10 = ITEMS.register("cube_10", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CUBE_11 = ITEMS.register("cube_11", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));

    // Triangle
    public static final RegistryObject<Item> TRIANGLE_0 = ITEMS.register("triangle_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> TRIANGLE_1 = ITEMS.register("triangle_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));

    // Cylinder
    public static final RegistryObject<Item> CYLINDER_0 = ITEMS.register("cylinder_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CYLINDER_1 = ITEMS.register("cylinder_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));

    // Circle
    public static final RegistryObject<Item> CIRCLE_0 = ITEMS.register("circle_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CIRCLE_1 = ITEMS.register("circle_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CIRCLE_HOLLOW_0 = ITEMS.register("circle_hollow_0", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> CIRCLE_HOLLOW_1 = ITEMS.register("circle_hollow_1", () -> new AbstractYuushyaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 0));

    // BlockItems
    public static final RegistryObject<Item> SHOW_BLOCK = ITEMS.register("showblock", () -> new BlockItem(BlockRegistry.SHOW_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ITEM_BLOCK = ITEMS.register("itemblock", () -> new BlockItem(BlockRegistry.ITEM_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> TEXT_BLOCK = ITEMS.register("textblock", () -> new BlockItem(BlockRegistry.TEXT_BLOCK.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}