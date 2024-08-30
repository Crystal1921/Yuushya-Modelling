package com.yuushya.modelling.utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ShareUtils {
    public static String asString(CompoundTag tag){
//        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//        buf.writeNbt(tag);
//        byte[] bytes = buf.array();
        return tag.getAsString();
    }
    public static CompoundTag asCompoundTag(String string) throws CommandSyntaxException {
//        byte[] array = string.getBytes(StandardCharsets.US_ASCII);
//        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(array));
//        CompoundTag tag = buf.readNbt();
        return TagParser.parseTag(string);
    }
}
