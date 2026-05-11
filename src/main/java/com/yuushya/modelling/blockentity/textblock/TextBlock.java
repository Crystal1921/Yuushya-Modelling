package com.yuushya.modelling.blockentity.textblock;

import com.yuushya.modelling.YuushyaNeoForge;
import com.yuushya.modelling.blockentity.AbstractTransformBlock;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.registries.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
//    public static final IClientItemExtensions ITEM_EXTENSIONS = FMLEnvironment.getDist() == Dist.CLIENT ? new IClientItemExtensions() {
//        @Override
//        public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
//            Minecraft minecraft = Minecraft.getInstance();
//            return new TextBlockSpecialRender(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
//        }
//    } : null;

    //TODO : 这里要新model，getCustomRenderer已被移除

    public TextBlock(Properties properties, Integer tipLines) {
        super(properties.setId(ResourceKey.create(Registries.BLOCK, YuushyaNeoForge.id("text_block"))), tipLines);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockState(pos).is(state.getBlock()) && level.getBlockEntity(pos) instanceof TextBlockEntity textBlockEntity) {

            if (context.isHoldingItem(ItemRegistry.GUI_ITEM.get())) {
                textBlockEntity.setShowFrame();
            } else if (context.isHoldingItem(ItemRegistry.ROT_TRANS_ITEM.get())) {
                textBlockEntity.setShowRotAxis();
                textBlockEntity.setShowText();
            } else if (context.isHoldingItem(ItemRegistry.POS_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MICRO_POS_TRANS_ITEM.get())
            ) {
                textBlockEntity.setShowPosAxis();
                textBlockEntity.setShowText();
            } else if (context.isHoldingItem(ItemRegistry.SLOT_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_SHOWBLOCK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.MOVE_TRANSFORMDATA_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.GET_BLOCKSTATE_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.SCALE_TRANS_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DEBUG_STICK_ITEM.get())
                    || context.isHoldingItem(ItemRegistry.DESTROY_ITEM.get())
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

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack itemStack = new ItemStack(this);
        BlockItemStateProperties stateProperties = itemStack.get(DataComponents.BLOCK_STATE);
        if (stateProperties == null) stateProperties = BlockItemStateProperties.EMPTY;
        BlockShape value = stateProperties.get(SHAPES);
        if (value == null) value = BlockShape.BLOCK;
        itemStack.set(DataComponents.BLOCK_STATE, stateProperties.with(SHAPES, value));
        return itemStack;
    }
}
