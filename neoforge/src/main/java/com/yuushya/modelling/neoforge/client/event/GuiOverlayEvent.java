package com.yuushya.modelling.neoforge.client.event;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.neoforge.client.gui.PickColorOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import static net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR;

@EventBusSubscriber
public class GuiOverlayEvent {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(CROSSHAIR, ResourceLocation.fromNamespaceAndPath("pick_color", Yuushya.MOD_ID_USED), new PickColorOverlay());
    }
}
