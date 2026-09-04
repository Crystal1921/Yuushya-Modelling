package com.yuushya.modelling.client.anvilcraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuushya.modelling.blockentity.renderstate.ItemBlockEntityRenderState;
import com.yuushya.modelling.blockentity.renderstate.SlotRenderState;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.utils.YuushyaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState.EMPTY_TINTS;

