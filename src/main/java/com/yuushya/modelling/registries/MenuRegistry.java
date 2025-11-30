package com.yuushya.modelling.registries;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.gui.engrave.EngraveMenu;
import com.yuushya.modelling.gui.history.HistoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPE = DeferredRegister.create(Registries.MENU, Yuushya.MOD_ID);

    public static final Supplier<MenuType<EngraveMenu>> EngraveMenu = MENU_TYPE.register("engrave", () -> IMenuTypeExtension.create(EngraveMenu::new));
    public static final Supplier<MenuType<HistoryMenu>> HistoryMenu = MENU_TYPE.register("history", () -> IMenuTypeExtension.create(HistoryMenu::new));

    public static void register(IEventBus bus) {
        MENU_TYPE.register(bus);
    }
}