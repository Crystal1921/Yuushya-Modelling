package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GroupRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Yuushya.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> YUUSHYA_MODELLING = TABS.register("modelling", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.yuushya.modelling"))
            .icon(() -> new ItemStack(ItemRegistry.POS_TRANS_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(ItemRegistry.GET_BLOCKSTATE_ITEM.get());
                output.accept(ItemRegistry.POS_TRANS_ITEM.get());
                output.accept(ItemRegistry.MICRO_POS_TRANS_ITEM.get());
                output.accept(ItemRegistry.ROT_TRANS_ITEM.get());
                output.accept(ItemRegistry.SCALE_TRANS_ITEM.get());
                output.accept(ItemRegistry.SLOT_TRANS_ITEM.get());
                output.accept(ItemRegistry.GET_SHOWBLOCK_ITEM.get());
                output.accept(ItemRegistry.MOVE_TRANSFORMDATA_ITEM.get());
                output.accept(ItemRegistry.DEBUG_STICK_ITEM.get());
                output.accept(ItemRegistry.GET_LIT_ITEM.get());
                output.accept(ItemRegistry.DESTROY_ITEM.get());
                output.accept(ItemRegistry.GUI_ITEM.get());
                output.accept(ItemRegistry.ENGRAVE_ITEM.get());
                output.accept(ItemRegistry.COLOR_PICKER_ITEM.get());
                output.accept(ItemRegistry.HISTORY_ITEM.get());
                output.accept(ItemRegistry.SHOW_BLOCK.get());
                output.accept(ItemRegistry.ITEM_BLOCK.get());
                output.accept(ItemRegistry.TEXT_BLOCK.get());
                output.accept(ItemRegistry.THE_ENCYCLOPEDIA.get());
                output.accept(ItemRegistry.SHIMMERING_PEARL.get());
                output.accept(ItemRegistry.EVERLASTING_WOOD.get());
                output.accept(ItemRegistry.SPROUTING_DIRT.get());
                output.accept(ItemRegistry.FLOATING_BLOOM.get());
                output.accept(ItemRegistry.SPARKING_FLAME.get());
            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> YUUSHYA_PRIMITIVE = TABS.register("primitive", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.yuushya.primitive"))
            .icon(() -> new ItemStack(ItemRegistry.CHIBI_0.get()))
            .displayItems((parameters, output) -> {
                output.accept(ItemRegistry.CHIBI_0.get());
                output.accept(ItemRegistry.CHIBI_0_ALT.get());
                output.accept(ItemRegistry.CHIBI_1.get());
                output.accept(ItemRegistry.CHIBI_2.get());
                output.accept(ItemRegistry.CHIBI_3.get());
                output.accept(ItemRegistry.CHIBI_4.get());
                output.accept(ItemRegistry.CHIBI_5.get());
                output.accept(ItemRegistry.CHIBI_6.get());

                output.accept(ItemRegistry.ANATOMY_0.get());
                output.accept(ItemRegistry.ANATOMY_0_ALT.get());
                output.accept(ItemRegistry.ANATOMY_1.get());
                output.accept(ItemRegistry.ANATOMY_2.get());

                output.accept(ItemRegistry.CUBE_0.get());
                output.accept(ItemRegistry.CUBE_1.get());
                output.accept(ItemRegistry.CUBE_2.get());
                output.accept(ItemRegistry.CUBE_3.get());
                output.accept(ItemRegistry.CUBE_4.get());
                output.accept(ItemRegistry.CUBE_5.get());
                output.accept(ItemRegistry.CUBE_6.get());
                output.accept(ItemRegistry.CUBE_7.get());
                output.accept(ItemRegistry.CUBE_8.get());
                output.accept(ItemRegistry.CUBE_9.get());
                output.accept(ItemRegistry.CUBE_10.get());
                output.accept(ItemRegistry.CUBE_11.get());

                output.accept(ItemRegistry.TRIANGLE_0.get());
                output.accept(ItemRegistry.TRIANGLE_1.get());

                output.accept(ItemRegistry.CYLINDER_0.get());
                output.accept(ItemRegistry.CYLINDER_1.get());

                output.accept(ItemRegistry.CIRCLE_0.get());
                output.accept(ItemRegistry.CIRCLE_1.get());
                output.accept(ItemRegistry.CIRCLE_HOLLOW_0.get());
                output.accept(ItemRegistry.CIRCLE_HOLLOW_1.get());
            }).build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}