package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.item.AbstractYuushyaItem;
import com.yuushya.modelling.item.YuushyaDebugStickItem;
import com.yuushya.modelling.item.showblocktool.*;
import dev.architectury.registry.CreativeTabOutput;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class YuushyaRegistries {
    public static final YuushyaDeferredRegister<Block> BLOCKS = new YuushyaDeferredRegister<>(Registries.BLOCK);
    public static final YuushyaDeferredRegister<Item> ITEMS = new YuushyaDeferredRegister<>(Registries.ITEM);
    public static final YuushyaDeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = new YuushyaDeferredRegister<>(Registries.BLOCK_ENTITY_TYPE);

    public static final CreativeTabRegistry.TabSupplier YUUSHYA_MODELLING = CreativeTabRegistry.create(new ResourceLocation(Yuushya.MOD_ID,"modelling"),()->new ItemStack(ITEMS.getInstanceOrDefault("pos_trans_item", Items.APPLE)));
    public static void registerAll(){
        ITEMS.register("pos_trans_item",()->new PosTransItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("micro_pos_trans_item",()->new MicroPosTransItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("rot_trans_item",()->new RotTransItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("scale_trans_item",()->new ScaleTransItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("slot_trans_item",()->new SlotTransItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("get_showblock_item",()->new GetShowBlockEntityItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("move_transformdata_item",()->new MoveTransformDataItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("debug_stick_item",()->new YuushyaDebugStickItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),4));
        ITEMS.register("get_lit_item",()->new GetLitItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo(1),2));
        ITEMS.register("the_encyclopedia",   ()->new AbstractYuushyaItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo( 16).rarity(Rarity.RARE),1));
        ITEMS.register("shimmering_pearl",   ()->new AbstractYuushyaItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo( 16).rarity(Rarity.RARE),1));
        ITEMS.register("everlasting_wood",   ()->new AbstractYuushyaItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo( 16).rarity(Rarity.RARE),1));
        ITEMS.register("sprouting_dirt",   ()->new AbstractYuushyaItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo( 16).rarity(Rarity.RARE),1));
        ITEMS.register("floating_bloom",   ()->new AbstractYuushyaItem(new Item.Properties().arch$tab(YUUSHYA_MODELLING).stacksTo( 16).rarity(Rarity.RARE),1));

        CreativeTabRegistry.appendStack(YUUSHYA_MODELLING,()->new ItemStack(ITEMS.get("get_blockstate_item").get()));

        SHOW_BLOCK= BLOCKS.register("showblock",()->new ShowBlock(BlockBehaviour.Properties.of(Material.METAL).noCollission().noOcclusion().strength(4.0f).lightLevel(blockState ->blockState.getValue(YuushyaBlockStates.LIT)),1));
        ITEMS.register("showblock",()->new BlockItem(BLOCKS.get("showblock").get(),new Item.Properties().arch$tab(YUUSHYA_MODELLING)));
        SHOW_BLOCK_ENTITY= BLOCK_ENTITIES.register("showblockentity",()->BlockEntityType.Builder.of(ShowBlockEntity::new,BLOCKS.get("showblock").get()).build(null));//Util.fetchChoiceType(References.BLOCK_ENTITY,"yuushya:showblockentity")

    }
    public static RegistrySupplier<Item> GET_BLOCKSTATE_ITEM = null;
    public static RegistrySupplier<Block> SHOW_BLOCK = null;
    public static RegistrySupplier<BlockEntityType<?>> SHOW_BLOCK_ENTITY = null;

}
