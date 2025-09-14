package com.yuushya.modelling.blockentity;

import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class TransformItemData implements ITransformDataProvider {
    public Vector3d pos;
    public Vector3f rot;
    public Vector3f scales;
    public ItemStack itemStack;
    public boolean isShown;

    public TransformItemData() {
        this.pos = new Vector3d(0, 0, 0);
        this.rot = new Vector3f(0, 0, 0);
        this.scales = new Vector3f(1, 1, 1);
        this.itemStack = Items.AIR.getDefaultInstance();
        this.isShown = false;
    }

    public TransformItemData(Vector3d pos, Vector3f rot, Vector3f scales, ItemStack ItemStack, boolean isShown) {
        this();
        this.pos.set(pos);
        this.rot.set(rot.x(), rot.y(), rot.z());
        this.scales.set(scales.x(), scales.y(), scales.z());
        this.itemStack = ItemStack;
        this.isShown = isShown;
    }

    public void set(Vector3d pos, Vector3f rot, Vector3f scales, ItemStack ItemStack, boolean isShown) {
        this.pos.set(pos);
        this.rot.set(rot.x(), rot.y(), rot.z());
        this.scales.set(scales.x(), scales.y(), scales.z());
        this.itemStack = ItemStack;
        this.isShown = isShown;
    }

    public void set(TransformItemData old) {
        set(old.pos, old.rot, old.scales, old.itemStack, old.isShown);
    }

    public void set() {
        this.pos.set(0, 0, 0);
        this.rot.set(0, 0, 0);
        this.scales.set(1, 1, 1);
        this.itemStack = Items.AIR.getDefaultInstance();
        this.isShown = false;
    }

    //readNbt from compoundTag
    public void load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        ListTag listTagPos = compoundTag.getList("ShowPos", 6);//6 means Double
        ListTag listTagRot = compoundTag.getList("ShowRotation", 5);//5 means Float
        ListTag listTagScales = compoundTag.getList("ShowScales", 5);//5 means Float
        this.pos.set(listTagPos.getDouble(0), listTagPos.getDouble(1), listTagPos.getDouble(2));
        this.rot.set(listTagRot.getFloat(0), listTagRot.getFloat(1), listTagRot.getFloat(2));
        this.scales.set(listTagScales.getFloat(0), listTagScales.getFloat(1), listTagScales.getFloat(2));
        this.isShown = compoundTag.getBoolean("isShown");
        this.itemStack = ItemStack.parseOptional(registries, compoundTag);
    }

    //writeNbt to compoundTag
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
        compoundTag.put("ShowPos", YuushyaUtils.toListTag(pos.x, pos.y, pos.z));
        compoundTag.put("ShowRotation", YuushyaUtils.toListTag(rot.x(), rot.y(), rot.z()));
        compoundTag.put("ShowScales", YuushyaUtils.toListTag(scales.x(), scales.y(), scales.z()));
        if (!itemStack.isEmpty()) {
            itemStack.save(registries, compoundTag);
        }
        compoundTag.put("isShown", ByteTag.valueOf(isShown));
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
