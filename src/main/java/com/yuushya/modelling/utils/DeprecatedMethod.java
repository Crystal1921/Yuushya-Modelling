package com.yuushya.modelling.utils;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.yuushya.modelling.Yuushya;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DeprecatedMethod {
    private static final Gson GSON = (new GsonBuilder()).disableHtmlEscaping().create();

    public static Optional<ItemStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
        return ItemStack.CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial((p_330102_) -> Yuushya.LOG_LOGGER.error("Tried to load invalid item: '{}'", p_330102_));
    }

    public static ItemStack parseOptional(HolderLookup.Provider lookupProvider, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : parse(lookupProvider, tag).orElse(ItemStack.EMPTY);
    }

    public static Tag save(HolderLookup.Provider levelRegistryAccess, Tag outputTag, ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return wrapEncodingExceptions(stack, ItemStack.CODEC, levelRegistryAccess, outputTag);
        }
    }

    public static Tag save(HolderLookup.Provider levelRegistryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return wrapEncodingExceptions(stack, ItemStack.CODEC, levelRegistryAccess);
        }
    }

    /**
     * Wraps encoding exceptions and adds additional logging for a DataComponentHolder that failed to save.
     */
    public static <T extends DataComponentHolder> Tag wrapEncodingExceptions(T componentHolder, Codec<T> codec, HolderLookup.Provider provider) {
        try {
            return codec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), componentHolder).getOrThrow();
        } catch (Exception exception) {
            logDataComponentSaveError(componentHolder, exception, null);
            throw exception;
        }
    }

    /**
     * Wraps encoding exceptions and adds additional logging for a DataComponentHolder that failed to save.
     */
    public static <T extends DataComponentHolder> Tag wrapEncodingExceptions(T componentHolder, Codec<T> codec, HolderLookup.Provider provider, Tag tag) {
        try {
            return codec.encode(componentHolder, provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
        } catch (Exception exception) {
            logDataComponentSaveError(componentHolder, exception, tag);
            throw exception;
        }
    }

    /**
     * <pre>
     * Example:
     * Error saving [1 minecraft:dirt]. Original cause: java.lang.NullPointerException
     * With components:
     * {
     *    neoforge:test=>Test[s=null]
     *    minecraft:max_stack_size=>64
     *    minecraft:lore=>ItemLore[lines=[], styledLines=[]]
     *    minecraft:enchantments=>ItemEnchantments{enchantments={}, showInTooltip=true}
     *    minecraft:repair_cost=>0
     *    minecraft:attribute_modifiers=>ItemAttributeModifiers[modifiers=[], showInTooltip=true]
     *    minecraft:rarity=>COMMON
     * }
     * With tag: {}
     * </pre>
     */
    public static void logDataComponentSaveError(DataComponentHolder componentHolder, Exception original, @Nullable Tag tag) {
        StringBuilder cause = new StringBuilder("Error saving [" + componentHolder + "]. Original cause: " + original);

        cause.append("\nWith components:\n{");
        componentHolder.getComponents().forEach((component) -> cause.append("\n\t").append(component));
        cause.append("\n}");
        if (tag != null) {
            cause.append("\nWith tag: ").append(tag);
        }
        Util.logAndPauseIfInIde(cause.toString());
    }

    public static void blit(GuiGraphicsExtractor guiGraphics, Identifier resourceLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourceLocation, x, y, uOffset, vOffset, uWidth, vHeight, 256, 256);
    }

    public static void blit(GuiGraphicsExtractor guiGraphics, Identifier resourceLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourceLocation, x, y, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    public static void blitSprite(GuiGraphicsExtractor guiGraphics, Identifier resourceLocation, int x, int y, int width, int height) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourceLocation, x, y, width, height);
    }

    static MutableComponent deserialize(JsonElement json, HolderLookup.Provider provider) {
        return (MutableComponent) ComponentSerialization.CODEC.parse(provider.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new);
    }

    static JsonElement serialize(Component component, HolderLookup.Provider provider) {
        return ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), component).getOrThrow(JsonParseException::new);
    }

    public static String toJson(Component component, HolderLookup.Provider registries) {
        return GSON.toJson(serialize(component, registries));
    }

    @javax.annotation.Nullable
    public static MutableComponent fromJson(String json, HolderLookup.Provider registries) {
        JsonElement jsonelement = JsonParser.parseString(json);
        return jsonelement == null ? null : deserialize(jsonelement, registries);
    }

    public static void saveToItem(BlockEntity blockEntity, ItemStack stack, HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), Yuushya.SLF_LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            blockEntity.saveCustomOnly(output);
            blockEntity.saveCustomOnly(output);
            BlockItem.setBlockEntityData(stack, blockEntity.getType(), output);
            stack.applyComponents(blockEntity.collectComponents());
        }
    }
}
