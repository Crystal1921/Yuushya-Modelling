package com.yuushya.modelling.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yuushya.modelling.blockentity.TransformData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

public class ShareUtils {

    public static class StringSerialization{
        public static CompoundTag transfer(String string) throws CommandSyntaxException {
            return TagParser.parseTag(string);
        }

        public static String from(CompoundTag tag) {
            return tag.getAsString();
        }
    }


    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .setPrettyPrinting().create();
    public record ShareInformation(
            Set<String> mods,
            List<ShareData> blocks
    ){
        public static ShareInformation from(List<TransformData> transformDataList){
            Set<String> modIds = new HashSet<>();
            List<ShareData> shareDataList = new ArrayList<>();
            for(TransformData data: transformDataList){
                String namespace = BuiltInRegistries.BLOCK.getKey(data.blockState.getBlock()).getNamespace();
                if(!"minecraft".equals(namespace)) modIds.add(namespace);
                shareDataList.add(ShareData.from(data));
            }
            return new ShareInformation(modIds,shareDataList);
        }

        public void transfer(List<TransformData> transformDataList){
            if (!transformDataList.isEmpty()) transformDataList.clear();
            for(ShareData data:blocks){
                transformDataList.add(data.transfer());
            }
        }

        public record ShareData(
                List<Double> pos,
                List<Float> rot,
                List<Float> scales,
                ShareBlockState blockState,
                boolean isShown
        ){
            public static ShareData from(TransformData data){
                return new ShareData(
                        List.of(data.pos.x,data.pos.y,data.pos.z),
                        List.of(data.rot.x,data.rot.y,data.rot.z),
                        List.of(data.scales.x,data.scales.y,data.scales.z),
                        ShareBlockState.from(data.blockState),
                        data.isShown
                );
            }

            public TransformData transfer(){
                pos.add(0d);pos.add(0d);pos.add(0d);
                rot.add(0f);rot.add(0f);rot.add(0f);
                scales.add(1f);scales.add(1f);scales.add(1f);
                return new TransformData(
                        new Vector3d(pos.get(0),pos.get(1),pos.get(2)),
                        new Vector3f(rot.get(0),rot.get(1),rot.get(2)),
                        new Vector3f(scales.get(0),scales.get(1),scales.get(2)),
                        blockState.transfer(),
                        isShown
                );
            }

            public record ShareBlockState(
                    String name,
                    Map<String,String> properties
            ){
                public static ShareBlockState from(BlockState state){
                    String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    Map<Property<?>, Comparable<?>> map = state.getValues();
                    Map<String,String> properties = new HashMap<>();
                    for (Map.Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                        Property<?> property = entry.getKey();
                        Comparable<?> value = entry.getValue();
                        properties.put(property.getName(), getName(property, (Comparable) value));
                    }
                    return new ShareBlockState(name,properties);
                }

                public BlockState transfer(){
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
                private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S stateHolder, Property<T> property, String value ) {
                    return property.getValue(value).map(t -> stateHolder.setValue(property, t)).orElse(stateHolder);
                }
                private static <T extends Comparable<T>> String getName(Property<T> property, Comparable<T> value) {
                    return property.getName((T) value);
                }
            }

        }

    }


    public static String transfer(List<TransformData> transformDataList){
        ShareInformation shareInformation = ShareInformation.from(transformDataList);
        return GSON.toJson(shareInformation,ShareInformation.class);
    }
    public static ShareInformation from(String json) {
        return GSON.fromJson(json,ShareInformation.class);
    }
}
