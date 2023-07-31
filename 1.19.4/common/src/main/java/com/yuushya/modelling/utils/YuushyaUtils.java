package com.yuushya.modelling.utils;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class YuushyaUtils {
    public static int vertexSize() { return DefaultVertexFormat.BLOCK.getVertexSize() / 4;} // 一个顶点用多少位int表示，原版和开了光影的OptiFine不同所以得在这算出来

    public static void scale(PoseStack arg, Vector3f scales){
        if(scales.x()!=1||scales.y()!=1||scales.z()!=1){
            arg.translate(0.5,0.5,0.5);
            arg.scale(scales.x(),scales.y(),scales.z());
            arg.translate(-0.5,-0.5,-0.5);
        }
    }
    public static void translate(PoseStack arg, Vector3d pos){
        if (pos.x!=0.0||pos.y!=0.0||pos.z!=0.0)
            arg.translate(pos.x/16,pos.y/16,pos.z/16);
    }
    public static void rotate(PoseStack arg,Vector3f rot) {
        float roll = rot.z(),yaw = rot.y(),pitch= rot.x();
        if(roll!=0.0F||yaw != 0.0F||pitch != 0.0F){
            arg.translate(0.5,0.5,0.5);
            if (roll != 0.0F)
                arg.mulPose(Axis.ZP.rotationDegrees(roll));
            if (yaw != 0.0F)
                arg.mulPose(Axis.YP.rotationDegrees(yaw));
            if (pitch != 0.0F)
                arg.mulPose(Axis.XP.rotationDegrees(pitch));
            arg.translate(-0.5,-0.5,-0.5);
        }
    }

    public static int encodeTintWithState(int tint, BlockState state) {
        // 最高位依然可以保留负数信息，但tint的有效位数很低了，原版够用，mod一般也不会用这个东西
        return Block.getId(state) << 8 | tint;
    }

    public static BlockState getBlockState(BlockState blockState, LevelAccessor world, BlockPos blockPos){
        if(blockState.getBlock() instanceof ShowBlock){
            return ((ShowBlockEntity)world.getBlockEntity(blockPos)).getTransformData(0).blockState;
        }
        else return blockState;
    }
    public static String getBlockStateProperties(BlockState blockState){
        StringBuilder stringBuilder = new StringBuilder();
        if (!blockState.getValues().isEmpty()) {
            stringBuilder.append('[');
            stringBuilder.append(blockState.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
            stringBuilder.append(']');
        }
        return stringBuilder.toString();
    }
    private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<>() {
        @Override
        public String apply(@Nullable Map.Entry<Property<?>, Comparable<?>> propertyValueMap) {
            if (propertyValueMap == null) {
                return "<NULL>";
            }
            Property<?> property = propertyValueMap.getKey();
            return property.getName() + "=" + this.getName(property, propertyValueMap.getValue());
        }

        private <T extends Comparable<T>> String getName(Property<T> property, Comparable<?> value) {
            return property.getName((T) value);
        }

    };
    public static <T> ListTag toListTag(T... values){
        ListTag listTag=new ListTag();
        Arrays.stream(values).toList().forEach((e)->{
            if(e instanceof Float e1)
                listTag.add(FloatTag.valueOf(e1));
            else if( e instanceof Double e1)
                listTag.add(DoubleTag.valueOf(e1));
        });
        return listTag;
    }

    public static BlockState readBlockState(CompoundTag tag) {
        if (!tag.contains("Name", 8)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            Block block = (Block) BuiltInRegistries.BLOCK.get(new ResourceLocation(tag.getString("Name")));
            BlockState blockState = block.defaultBlockState();
            if (tag.contains("Properties", 10)) {
                CompoundTag compoundTag = tag.getCompound("Properties");
                StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
                Iterator var5 = compoundTag.getAllKeys().iterator();

                while(var5.hasNext()) {
                    String string = (String)var5.next();
                    net.minecraft.world.level.block.state.properties.Property<?> property = stateDefinition.getProperty(string);
                    if (property != null) {
                        blockState = (BlockState)setValueHelper(blockState, property, string, compoundTag, tag);
                    }
                }
            }

            return blockState;
        }
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S stateHolder, net.minecraft.world.level.block.state.properties.Property<T> property, String propertyName, CompoundTag propertiesTag, CompoundTag blockStateTag) {
        Optional<T> optional = property.getValue(propertiesTag.getString(propertyName));
        if (optional.isPresent()) {
            return stateHolder.setValue(property, optional.get());
        } else {
            return stateHolder;
        }
    }

}
