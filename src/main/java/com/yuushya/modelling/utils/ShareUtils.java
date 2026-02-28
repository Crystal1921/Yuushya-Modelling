package com.yuushya.modelling.utils;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

public class ShareUtils {

    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();

    public static int getABGR(int rgb) {
        int a = (rgb >> 24) & 0xFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static String transfer(List<TransformBlockData> transformDataList) {
        ShareBlockInformation shareBlockInformation = ShareBlockInformation.from(transformDataList);
        return GSON.toJson(shareBlockInformation, ShareBlockInformation.class);
    }

    public static String transferItems(List<TransformItemData> transformDataList) {
        ShareItemInformation shareInformation = ShareItemInformation.from(transformDataList);
        return GSON.toJson(shareInformation, ShareItemInformation.class);
    }

    public static String transferText(List<TransformTextData> transformDataList) {
        SharedTextInformation sharedTextInformation = SharedTextInformation.from(transformDataList);
        return GSON.toJson(sharedTextInformation, SharedTextInformation.class);
    }

    public static ShareBlockInformation from(String json) {
        return GSON.fromJson(json, ShareBlockInformation.class);
    }

    public static ShareItemInformation fromItems(String json) {
        return GSON.fromJson(json, ShareItemInformation.class);
    }

    public static SharedTextInformation fromText(String json) {
        return GSON.fromJson(json, SharedTextInformation.class);
    }

    // JsonElement → CompoundTag
    public static CompoundTag jsonToCompoundTag(JsonElement json) {
        DataResult<CompoundTag> result = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json);
        return result.result().orElse(new CompoundTag());
    }

    public static class StringSerialization {
        public static CompoundTag transfer(String string) throws CommandSyntaxException {
            return TagParser.parseTag(string);
        }

        public static String from(CompoundTag tag) {
            return tag.getAsString();
        }
    }

    public record ShareBlockInformation(
            Set<String> mods,
            List<ShareData> blocks
    ) {
        public static ShareBlockInformation from(List<TransformBlockData> transformDataList) {
            Set<String> modIds = new HashSet<>();
            List<ShareData> shareDataList = new ArrayList<>();
            for (TransformBlockData data : transformDataList) {
                String namespace = BuiltInRegistries.BLOCK.getKey(data.blockState.getBlock()).getNamespace();
                if (!"minecraft".equals(namespace)) modIds.add(namespace);
                shareDataList.add(ShareData.from(data));
            }
            return new ShareBlockInformation(modIds, shareDataList);
        }

        public void transfer(List<TransformBlockData> transformDataList) {
            if (!transformDataList.isEmpty()) transformDataList.clear();
            for (ShareData data : blocks) {
                transformDataList.add(data.transfer());
            }
        }

        public record ShareData(
                List<Double> pos,
                List<Float> rot,
                List<Float> scales,
                ShareBlockState blockState,
                boolean isShown
        ) {
            public static ShareData from(TransformBlockData data) {
                return new ShareData(
                        List.of(data.pos.x, data.pos.y, data.pos.z),
                        List.of(data.rot.x, data.rot.y, data.rot.z),
                        List.of(data.scales.x, data.scales.y, data.scales.z),
                        ShareBlockState.from(data.blockState),
                        data.isShown
                );
            }

            public TransformBlockData transfer() {
                pos.add(0d);
                pos.add(0d);
                pos.add(0d);
                rot.add(0f);
                rot.add(0f);
                rot.add(0f);
                scales.add(1f);
                scales.add(1f);
                scales.add(1f);
                return new TransformBlockData(
                        new Vector3d(pos.get(0), pos.get(1), pos.get(2)),
                        new Vector3f(rot.get(0), rot.get(1), rot.get(2)),
                        new Vector3f(scales.get(0), scales.get(1), scales.get(2)),
                        blockState.transfer(),
                        isShown
                );
            }

            public record ShareBlockState(
                    String name,
                    Map<String, String> properties
            ) {
                public static ShareBlockState from(BlockState state) {
                    String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    Map<Property<?>, Comparable<?>> map = state.getValues();
                    Map<String, String> properties = new HashMap<>();
                    for (Map.Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                        Property<?> property = entry.getKey();
                        Comparable<?> value = entry.getValue();
                        properties.put(property.getName(), getName(property, (Comparable) value));
                    }
                    return new ShareBlockState(name, properties);
                }

                private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S stateHolder, Property<T> property, String value) {
                    return property.getValue(value).map(t -> stateHolder.setValue(property, t)).orElse(stateHolder);
                }

                private static <T extends Comparable<T>> String getName(Property<T> property, Comparable<T> value) {
                    return property.getName((T) value);
                }

                public BlockState transfer() {
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(this.name));
                    BlockState blockState = block.defaultBlockState();
                    StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
                    for (String string : this.properties.keySet()) {
                        Property<?> property = stateDefinition.getProperty(string);
                        if (property != null) {
                            blockState = setValueHelper(blockState, property, this.properties.get(string));
                        }
                    }
                    return blockState;
                }
            }

        }

    }

    // Item sharing support
    public record ShareItemInformation(
            Set<String> mods,
            List<ItemShareData> items
    ) {
        public static ShareItemInformation from(List<TransformItemData> transformDataList) {
            Set<String> modIds = new HashSet<>();
            List<ItemShareData> shareDataList = new ArrayList<>();
            for (TransformItemData data : transformDataList) {
                String namespace = BuiltInRegistries.ITEM.getKey(data.itemStack.getItem()).getNamespace();
                if (!"minecraft".equals(namespace)) modIds.add(namespace);
                shareDataList.add(ItemShareData.from(data));
            }
            return new ShareItemInformation(modIds, shareDataList);
        }

        public void transferItems(List<TransformItemData> transformDataList) {
            if (!transformDataList.isEmpty()) transformDataList.clear();
            for (ItemShareData data : items) {
                transformDataList.add(data.transfer());
            }
        }

        public record ItemShareData(
                List<Double> pos,
                List<Float> rot,
                List<Float> scales,
                ShareItemStack itemStack,
                int color,
                boolean isShown,
                boolean enableBlock
        ) {
            public static ItemShareData from(TransformItemData data) {
                return new ItemShareData(
                        List.of(data.pos.x(), data.pos.y(), data.pos.z()),
                        List.of(data.rot.x(), data.rot.y(), data.rot.z()),
                        List.of(data.scales.x(), data.scales.y(), data.scales.z()),
                        ShareItemStack.from(data.itemStack),
                        data.color,
                        data.isShown,
                        data.enableBlock
                );
            }

            public TransformItemData transfer() {
                List<Double> posList = new ArrayList<>(pos);
                List<Float> rotList = new ArrayList<>(rot);
                List<Float> scalesList = new ArrayList<>(scales);

                posList.add(0d);
                posList.add(0d);
                posList.add(0d);
                rotList.add(0f);
                rotList.add(0f);
                rotList.add(0f);
                scalesList.add(1f);
                scalesList.add(1f);
                scalesList.add(1f);

                return new TransformItemData(
                        new Vector3d(posList.get(0), posList.get(1), posList.get(2)),
                        new Vector3f(rotList.get(0), rotList.get(1), rotList.get(2)),
                        new Vector3f(scalesList.get(0), scalesList.get(1), scalesList.get(2)),
                        itemStack.transfer(),
                        color,
                        isShown,
                        enableBlock
                );
            }

            public record ShareItemStack(
                    String name,
                    JsonElement jsonElement
            ) {
                public static ShareItemStack from(ItemStack stack) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    CompoundTag tag = stack.save(new CompoundTag());

                    String asString = tag.getAsString();
                    JsonPrimitive jsonPrimitive = new JsonPrimitive(asString);

                    return new ShareItemStack(itemId.toString(), jsonPrimitive);
                }


                public ItemStack transfer() {
                    String snbtFromJson = jsonElement.getAsJsonPrimitive().getAsString();
                    CompoundTag compoundTag;
                    try {
                        compoundTag = TagParser.parseTag(snbtFromJson);
                    } catch (Exception e) {
                        compoundTag = new CompoundTag();
                    }

                    return ItemStack.of(compoundTag);
                }
            }
        }
    }

    public record SharedTextInformation(
            Set<String> mods,
            List<TextShareData> texts
    ) {
        public static SharedTextInformation from(List<TransformTextData> transformDataList) {
            Set<String> modIds = new HashSet<>();
            List<TextShareData> shareDataList = new ArrayList<>();
            for (TransformTextData data : transformDataList) {
                shareDataList.add(TextShareData.from(data));
            }
            return new SharedTextInformation(modIds, shareDataList);
        }

        public void transferTexts(List<TransformTextData> transformDataList) {
            if (!transformDataList.isEmpty()) transformDataList.clear();
            for (TextShareData data : texts) {
                transformDataList.add(data.transfer());
            }
        }

        public record TextShareData(
                List<Double> pos,
                List<Float> rot,
                List<Float> scales,
                List<String> textLines,
                boolean isCulled,
                boolean isMirror,
                boolean isShown
        ) {
            public static TextShareData from(TransformTextData data) {
                return new TextShareData(
                        List.of(data.pos.x, data.pos.y, data.pos.z),
                        List.of(data.rot.x(), data.rot.y(), data.rot.z()),
                        List.of(data.scales.x(), data.scales.y(), data.scales.z()),
                        new ArrayList<>(data.textLines),
                        data.isCulled,
                        data.isMirror,
                        data.isShown
                );
            }

            public TransformTextData transfer() {
                List<Double> posList = new ArrayList<>(pos);
                List<Float> rotList = new ArrayList<>(rot);
                List<Float> scalesList = new ArrayList<>(scales);

                posList.add(0d);
                posList.add(0d);
                posList.add(0d);
                rotList.add(0f);
                rotList.add(0f);
                rotList.add(0f);
                scalesList.add(1f);
                scalesList.add(1f);
                scalesList.add(1f);

                return new TransformTextData(
                        new Vector3d(posList.get(0), posList.get(1), posList.get(2)),
                        new Vector3f(rotList.get(0), rotList.get(1), rotList.get(2)),
                        new Vector3f(scalesList.get(0), scalesList.get(1), scalesList.get(2)),
                        new ArrayList<>(textLines),
                        isCulled,
                        isMirror,
                        isShown
                );
            }
        }
    }
}
