package com.yuushya.modelling.event;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.gui.PickColorOverlay;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import static net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR;

@EventBusSubscriber(value = Dist.CLIENT)
public class GuiOverlayEvent {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(CROSSHAIR, Identifier.fromNamespaceAndPath("pick_color", Yuushya.MOD_ID_USED), new PickColorOverlay());
    }
}
