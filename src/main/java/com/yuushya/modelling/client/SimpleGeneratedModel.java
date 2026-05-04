package com.yuushya.modelling.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

import java.util.function.Function;

public class SimpleGeneratedModel extends Model<Unit> {
    public SimpleGeneratedModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
    }
}