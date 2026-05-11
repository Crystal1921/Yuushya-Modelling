package com.yuushya.modelling.blockentity.transformData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yuushya.modelling.utils.CodecUtils;
import com.yuushya.modelling.utils.DeprecatedMethod;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class TransformItemData implements ITransformDataProvider {
    public Vector3d pos;
    public Vector3f rot;
    public Vector3f scales;
    public int color;
    public ItemStack itemStack;
    public boolean isShown;
    public boolean enableBlock;
    public static final Codec<TransformItemData> TRANSFORM_ITEM_DATA_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            CodecUtils.VECTOR_3D_CODEC.fieldOf("pos").forGetter(data -> data.pos),
            CodecUtils.VECTOR_3F_CODEC.fieldOf("rot").forGetter(data -> data.rot),
            CodecUtils.VECTOR_3F_CODEC.fieldOf("scales").forGetter(data -> data.scales),
            ItemStack.CODEC.fieldOf("itemStack").forGetter(data -> data.itemStack),
            Codec.INT.fieldOf("color").forGetter(data -> data.color),
            Codec.BOOL.fieldOf("isShown").forGetter(data -> data.isShown),
            Codec.BOOL.fieldOf("enableBlock").forGetter(data -> data.enableBlock)
    ).apply(instance, TransformItemData::new));



    public TransformItemData() {
        this.pos = new Vector3d(0, 0, 0);
        this.rot = new Vector3f(0, 0, 0);
        this.scales = new Vector3f(1, 1, 1);
        this.color = ARGB.color(255, 255, 255, 255);
        this.itemStack = Items.AIR.getDefaultInstance();
        this.isShown = false;
    }

    public TransformItemData(Vector3d pos, Vector3f rot, Vector3f scales, ItemStack ItemStack, int color, boolean isShown, boolean enableBlock) {
        this();
        this.pos.set(pos);
        this.rot.set(rot.x(), rot.y(), rot.z());
        this.scales.set(scales.x(), scales.y(), scales.z());
        this.itemStack = ItemStack;
        this.color = color;
        this.isShown = isShown;
        this.enableBlock = enableBlock;
    }

    public void set(Vector3d pos, Vector3f rot, Vector3f scales, ItemStack ItemStack, int color, boolean isShown, boolean enableBlock) {
        this.pos.set(pos);
        this.rot.set(rot.x(), rot.y(), rot.z());
        this.scales.set(scales.x(), scales.y(), scales.z());
        this.itemStack = ItemStack;
        this.color = color;
        this.isShown = isShown;
        this.enableBlock = enableBlock;
    }

    public void set(TransformItemData old) {
        set(old.pos, old.rot, old.scales, old.itemStack, old.color, old.isShown, old.enableBlock);
    }

    public void set() {
        this.pos.set(0, 0, 0);
        this.rot.set(0, 0, 0);
        this.scales.set(1, 1, 1);
        this.itemStack = Items.AIR.getDefaultInstance();
        this.isShown = false;
        this.enableBlock = false;
    }

    //readNbt from compoundTag
    public void load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ListTag listTagPos = compoundTag.getList("ShowPos").orElse(new ListTag());
        ListTag listTagRot = compoundTag.getList("ShowRotation").orElse(new ListTag());
        ListTag listTagScales = compoundTag.getList("ShowScales").orElse(new ListTag());
        this.pos.set(listTagPos.getDouble(0).orElse(0D), listTagPos.getDouble(1).orElse(0D), listTagPos.getDouble(2).orElse(0D));
        this.rot.set(listTagRot.getDouble(0).orElse(0D), listTagRot.getDouble(1).orElse(0D), listTagRot.getDouble(2).orElse(0D));
        this.scales.set(listTagScales.getDouble(0).orElse(0D), listTagScales.getDouble(1).orElse(0D), listTagScales.getDouble(2).orElse(0D));
        this.itemStack = DeprecatedMethod.parseOptional(registries, compoundTag.getCompound("ItemStack").orElse(new CompoundTag()));
        this.color = (compoundTag.getInt("Color").orElse(0));
        this.isShown = compoundTag.getBoolean("isShown").orElse(false);
        this.enableBlock = compoundTag.getBoolean("enableBlock").orElse(false);
    }

    //writeNbt to compoundTag
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put("ShowPos", YuushyaUtils.toListTag(pos.x, pos.y, pos.z));
        compoundTag.put("ShowRotation", YuushyaUtils.toDoubleListTag(rot));
        compoundTag.put("ShowScales", YuushyaUtils.toDoubleListTag(scales));
        compoundTag.put("ItemStack", YuushyaUtils.itemStackTag(itemStack, registries));
        compoundTag.putInt("Color", color);
        compoundTag.putBoolean("isShown", isShown);
        compoundTag.putBoolean("enableBlock", enableBlock);
    }

    // ITransformDataProvider interface methods
    @Override
    public Vector3d getPosition() {
        return pos;
    }

    @Override
    public Vector3f getRotation() {
        return rot;
    }

    @Override
    public Vector3f getScale() {
        return scales;
    }

    @Override
    public boolean isShown() {
        return isShown;
    }

    @Override
    public void setShown(boolean shown) {
        this.isShown = shown;
    }
}
