package com.yuushya.modelling.utils;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import com.yuushya.modelling.blockentity.showblock.ShowBlock;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
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
    public static void translateAfterScale(PoseStack arg, Vec3 pos, Vector3f scales){
        if (pos.x!=0.0||pos.y!=0.0||pos.z!=0.0){
            arg.translate(pos.x*scales.x()/16,pos.y*scales.y()/16,pos.z*scales.z()/16);
        }
    }

    public static void translate(PoseStack arg, Vec3 pos){
        if (pos.x!=0.0||pos.y!=0.0||pos.z!=0.0)
            arg.translate(pos.x/16,pos.y/16,pos.z/16);
    }
    public static void rotate(PoseStack arg,Vector3f rot) {
        float roll = rot.z(),yaw = rot.y(),pitch= rot.x();
        if(roll!=0.0F||yaw != 0.0F||pitch != 0.0F){
            arg.translate(0.5,0.5,0.5);
            if (roll != 0.0F)
                arg.mulPose(Vector3f.ZP.rotationDegrees(roll));
            if (yaw != 0.0F)
                arg.mulPose(Vector3f.YP.rotationDegrees(yaw));
            if (pitch != 0.0F)
                arg.mulPose(Vector3f.XP.rotationDegrees(pitch));
            arg.translate(-0.5,-0.5,-0.5);
        }
    }

    public static int encodeTintWithState(int tint, BlockState state) {
        // 最高位依然可以保留负数信息，但tint的有效位数很低了，原版够用，mod一般也不会用这个东西
        return Block.getId(state) << 8 | tint;
    }

    public static BlockState getBlockState(BlockState blockState, LevelAccessor world, BlockPos blockPos){
        if(blockState.getBlock() instanceof ShowBlock){
            ShowBlockEntity blockEntity = (ShowBlockEntity)world.getBlockEntity(blockPos);
            return blockEntity.getTransFormDataNow().blockState;
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
    public static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Map.Entry<Property<?>, Comparable<?>>, String>() {
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
        Arrays.stream(values).forEach((e)->{
            if(e instanceof Float)
                listTag.add(FloatTag.valueOf((Float) e));
            else if( e instanceof Double )
                listTag.add(DoubleTag.valueOf((Double) e));
        });
        return listTag;
    }

    public static Vec3 convertVec3(Vector3d vector3d){
        return new Vec3(vector3d.x,vector3d.y,vector3d.z);
    }
    public static Vector3d convertVec3(Vec3 vector3d){
        return new Vector3d(vector3d.x,vector3d.y,vector3d.z);
    }

    public static class Mth{
        public static double lerp(double delta, double start, double end) {
            return start + delta * (end - start);
        }

        public static double inverseLerp(double delta, double start, double end) {
            return (delta - start) / (end - start);
        }

        public static double map(double d, double e, double f, double g, double h) {
            return lerp(inverseLerp(d, e, f), g, h);
        }
    }
}
