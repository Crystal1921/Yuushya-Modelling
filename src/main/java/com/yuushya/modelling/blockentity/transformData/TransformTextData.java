package com.yuushya.modelling.blockentity.transformData;

import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class TransformTextData implements ITransformDataProvider {
    public Vector3d pos;
    public Vector3f rot;
    public Vector3f scales;
    public List<String> textLines;
    public boolean isCulled;
    public boolean isMirror;
    public boolean isShown;

    public TransformTextData() {
        this.pos = new Vector3d(0, 0, 0);
        this.rot = new Vector3f(0, 0, 0);
        this.scales = new Vector3f(1, 1, 1);
        this.textLines = new ArrayList<>();
        this.isCulled = false;
        this.isMirror = false;
        this.isShown = false;
    }

    public TransformTextData(Vector3d pos, Vector3f rot, Vector3f scales, List<String> textLines, boolean isCulled, boolean isMirror, boolean isShown) {
        this();
        this.pos.set(pos);
        this.rot.set(rot.x(), rot.y(), rot.z());
        this.scales.set(scales.x(), scales.y(), scales.z());
        this.textLines = new ArrayList<>(textLines);
        this.isCulled = isCulled;
        this.isMirror = isMirror;
        this.isShown = isShown;
    }

    public void set(Vector3d pos, Vector3f rot, Vector3f scales, List<String> textLines, boolean isCulled, boolean isMirror, boolean isShown) {
        this.pos.set(pos);
        this.rot.set(rot.x(), rot.y(), rot.z());
        this.scales.set(scales.x(), scales.y(), scales.z());
        this.textLines = new ArrayList<>(textLines);
        this.isCulled = isCulled;
        this.isMirror = isMirror;
        this.isShown = isShown;
    }

    public void set(TransformTextData old) {
        set(old.pos, old.rot, old.scales, old.textLines, old.isCulled, old.isMirror, old.isShown);
    }

    public void set() {
        this.pos.set(0, 0, 0);
        this.rot.set(0, 0, 0);
        this.scales.set(1, 1, 1);
        this.textLines.clear();
        this.isCulled = false;
        this.isMirror = false;
        this.isShown = false;
    }

    //readNbt from compoundTag
    public void load(CompoundTag compoundTag) {
        ListTag listTagPos = compoundTag.getList("ShowPos", 6);//6 means Double
        ListTag listTagRot = compoundTag.getList("ShowRotation", 5);//5 means Float
        ListTag listTagScales = compoundTag.getList("ShowScales", 5);//5 means Float
        this.pos.set(listTagPos.getDouble(0), listTagPos.getDouble(1), listTagPos.getDouble(2));
        this.rot.set(listTagRot.getFloat(0), listTagRot.getFloat(1), listTagRot.getFloat(2));
        this.scales.set(listTagScales.getFloat(0), listTagScales.getFloat(1), listTagScales.getFloat(2));
        this.isCulled = compoundTag.getBoolean("Culled");
        this.isMirror = compoundTag.getBoolean("Mirrored");
        this.isShown = compoundTag.getBoolean("isShown");

        // Load text lines
        this.textLines.clear();
        ListTag textListTag = compoundTag.getList("TextLines", 8); // 8 means String
        for (int i = 0; i < textListTag.size(); i++) {
            this.textLines.add(textListTag.getString(i));
        }
    }

    //writeNbt to compoundTag
    public void saveAdditional(CompoundTag compoundTag) {
        compoundTag.put("ShowPos", YuushyaUtils.toListTag(pos.x, pos.y, pos.z));
        compoundTag.put("ShowRotation", YuushyaUtils.toListTag(rot.x(), rot.y(), rot.z()));
        compoundTag.put("ShowScales", YuushyaUtils.toListTag(scales.x(), scales.y(), scales.z()));
        compoundTag.putBoolean("Culled", isCulled);
        compoundTag.putBoolean("Mirrored", isMirror);
        compoundTag.putBoolean("isShown", isShown);

        // Save text lines
        ListTag textListTag = new ListTag();
        for (String line : textLines) {
            textListTag.add(StringTag.valueOf(line));
        }
        compoundTag.put("TextLines", textListTag);
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
