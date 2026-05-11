package com.yuushya.modelling.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class CodecUtils {
    public static final Codec<Vector3d> VECTOR_3D_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Vector3d::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Vector3d::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Vector3d::z)
    ).apply(instance, Vector3d::new));

    public static final Codec<Vector3f> VECTOR_3F_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(Vector3f::x),
            Codec.FLOAT.fieldOf("y").forGetter(Vector3f::y),
            Codec.FLOAT.fieldOf("z").forGetter(Vector3f::z)
    ).apply(instance, Vector3f::new));
}
