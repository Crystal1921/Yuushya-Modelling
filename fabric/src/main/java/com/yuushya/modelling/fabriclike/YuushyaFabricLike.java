package com.yuushya.modelling.fabriclike;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.gui.widget.ColorTexture;
import com.yuushya.modelling.item.showblocktool.GetBlockStateItem;
import com.yuushya.modelling.registries.YuushyaRegistries;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

public class YuushyaFabricLike {
    private static boolean isInitialed = false;
    public static void init() {
        YuushyaRegistries.ITEMS.register("get_blockstate_item", () -> new GetBlockStateItem(new Item.Properties(), 3));
        Yuushya.init();
    }

    public static void initResource() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                if (!isInitialed) {
                    ColorTexture colorTexture = new ColorTexture();
                    isInitialed = true;
                }
            }

            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID_USED, "color_texture");
            }
        });
    }
}
