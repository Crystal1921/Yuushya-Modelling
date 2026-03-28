package com.yuushya.modelling.blockentity.textblock;

import com.yuushya.modelling.Yuushya;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.BlockShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.yuushya.modelling.block.blockstate.YuushyaBlockStates.SHAPES;

public class TextBlock extends AbstractTransformBlock {
    public static final IClientItemExtensions ITEM_EXTENSIONS = FMLEnvironment.dist == Dist.CLIENT ? new IClientItemExtensions() {
        @Override
        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
            Minecraft minecraft = Minecraft.getInstance();
            return new TextBlockSpecialRender(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        }
    } : null;

    public TextBlock(Properties properties, Integer tipLines) {
        super(properties, tipLines);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof TextBlockEntity textBlockEntity) {

            if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "gui_item")))) {
                textBlockEntity.setShowFrame();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "rot_trans_item")))) {
                textBlockEntity.setShowRotAxis();
                textBlockEntity.setShowText();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "pos_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "micro_pos_trans_item")))
            ) {
                textBlockEntity.setShowPosAxis();
                textBlockEntity.setShowText();
            } else if (context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "slot_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "get_showblock_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "move_transformdata_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "get_blockstate_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "scale_trans_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "debug_stick_item")))
                    || context.isHoldingItem(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Yuushya.MOD_ID, "destroy_item")))
            ) {
                textBlockEntity.setShowText();
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TextBlockEntity(blockPos, blockState);
    }

    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(level, player, pos, state);
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack itemStack = new ItemStack(this);
        BlockItemStateProperties stateProperties = itemStack.get(DataComponents.BLOCK_STATE);
        if (stateProperties == null) stateProperties = BlockItemStateProperties.EMPTY;
        BlockShape value = stateProperties.get(SHAPES);
        if (value == null) value = BlockShape.BLOCK;
        itemStack.set(DataComponents.BLOCK_STATE, stateProperties.with(SHAPES, value));
        return itemStack;
    }
}
