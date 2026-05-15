package com.yuushya.modelling.blockentity.transformData;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yuushya.modelling.utils.CodecUtils;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class TransformTextData implements ITransformDataProvider {
    public static final Codec<TransformTextData> TRANSFORM_TEXT_DATA_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            CodecUtils.VECTOR_3D_CODEC.fieldOf("pos").forGetter(data -> data.pos),
            CodecUtils.VECTOR_3F_CODEC.fieldOf("rot").forGetter(data -> data.rot),
            CodecUtils.VECTOR_3F_CODEC.fieldOf("scales").forGetter(data -> data.scales),
            Codec.STRING.listOf().fieldOf("textLines").forGetter(data -> data.textLines),
            Codec.BOOL.fieldOf("isCulled").forGetter(data -> data.isCulled),
            Codec.BOOL.fieldOf("isMirror").forGetter(data -> data.isMirror),
            Codec.BOOL.fieldOf("isShown").forGetter(data -> data.isShown)
    ).apply(instance, TransformTextData::new));
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
        CompoundTag pos = compoundTag.getCompoundOrEmpty("pos");
        CompoundTag rot = compoundTag.getCompoundOrEmpty("rot");
        CompoundTag scales = compoundTag.getCompoundOrEmpty("scales");
        this.pos.set(pos.getDoubleOr("x",0), pos.getDoubleOr("y",0), pos.getDoubleOr("z",0));
        this.rot.set(rot.getFloatOr("x",0), rot.getFloatOr("y",0), rot.getFloatOr("z",0));
        this.scales.set(scales.getFloatOr("x",1), scales.getFloatOr("y",1), scales.getFloatOr("z",1));

        this.isCulled = compoundTag.getBoolean("isCulled").orElse(false);
        this.isMirror = compoundTag.getBoolean("isMirror").orElse(false);
        this.isShown = compoundTag.getBoolean("isShown").orElse(false);

        // Load text lines
        this.textLines.clear();
        ListTag textListTag = compoundTag.getList("textLines").orElse(new ListTag());
        for (int i = 0; i < textListTag.size(); i++) {
            this.textLines.add(textListTag.getString(i).orElse(""));
        }
    }

    //writeNbt to compoundTag
    public void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
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
