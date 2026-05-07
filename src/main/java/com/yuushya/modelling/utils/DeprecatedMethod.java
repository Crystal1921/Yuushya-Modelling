package com.yuushya.modelling.utils;

import com.mojang.serialization.Codec;
import com.yuushya.modelling.Yuushya;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DeprecatedMethod {
    public static Optional<ItemStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
        return ItemStack.CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial((p_330102_) -> Yuushya.LOGGER.error("Tried to load invalid item: '{}'", p_330102_));
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
}
