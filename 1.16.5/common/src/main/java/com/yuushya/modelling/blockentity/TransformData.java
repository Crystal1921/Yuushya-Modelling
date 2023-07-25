package com.yuushya.modelling.blockentity;

import com.mojang.math.Vector3f;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TransformData{
    public Vec3 pos;
    public Vector3f rot;
    public Vector3f scales;
    public BlockState blockState;
    public boolean isShown;
    public TransformData(){
        this.pos=new Vec3(0,0,0);
        this.rot=new Vector3f(0,0,0);
        this.scales=new Vector3f(1,1,1);
        this.blockState= Blocks.AIR.defaultBlockState();
        this.isShown=false;
    }
    public TransformData(Vec3 pos, Vector3f rot, Vector3f scales, BlockState blockState,boolean isShown){
        this();
        this.pos = new Vec3(pos.x, pos.y, pos.z);
        this.rot.set(rot.x(),rot.y(),rot.z());
        this.scales.set(scales.x(),scales.y(),scales.z());
        this.blockState= blockState;
        this.isShown=isShown;
    }
    public void set(Vec3 pos, Vector3f rot, Vector3f scales, BlockState blockState,boolean isShown){
        this.pos= new Vec3(pos.x, pos.y, pos.z);
        this.rot.set(rot.x(),rot.y(),rot.z());
        this.scales.set(scales.x(),scales.y(),scales.z());
        this.blockState= blockState;
        this.isShown=isShown;
    }
    public void set(TransformData old){
        set(old.pos,old.rot,old.scales,old.blockState,old.isShown);
    }
    public void set(){
        this.pos= new Vec3(0, 0, 0);
        this.rot.set(0,0,0);
        this.scales.set(1,1,1);
        this.blockState=Blocks.AIR.defaultBlockState();
        this.isShown=false;
    }
    //readNbt from compoundTag
    public void load(CompoundTag compoundTag) {
        ListTag listTagPos = compoundTag.getList("ShowPos",6);//6 means Double
        ListTag listTagRot = compoundTag.getList("ShowRotation",5);//5 means Float
        ListTag listTagScales = compoundTag.getList("ShowScales",5);//5 means Float
        this.pos = new Vec3(listTagPos.getDouble(0), listTagPos.getDouble(1), listTagPos.getDouble(2));
        this.rot.set(listTagRot.getFloat(0), listTagRot.getFloat(1), listTagRot.getFloat(2));
        this.scales.set(listTagScales.getFloat(0), listTagScales.getFloat(1), listTagScales.getFloat(2));
        this.isShown = compoundTag.getBoolean("isShown");
        this.blockState = NbtUtils.readBlockState(compoundTag.getCompound("BlockState"));
    }

    //writeNbt to compoundTag
    public void saveAdditional(CompoundTag compoundTag) {
        compoundTag.put("ShowPos", YuushyaUtils.toListTag(pos.x,pos.y,pos.z));
        compoundTag.put("ShowRotation", YuushyaUtils.toListTag(rot.x(),rot.y(),rot.z()));
        compoundTag.put("ShowScales", YuushyaUtils.toListTag(scales.x(),scales.y(),scales.z()));
        compoundTag.put("BlockState", NbtUtils.writeBlockState(blockState));
        compoundTag.put("isShown",ByteTag.valueOf(isShown));
    }

}
