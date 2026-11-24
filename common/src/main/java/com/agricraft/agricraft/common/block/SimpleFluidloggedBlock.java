package com.agricraft.agricraft.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface SimpleFluidloggedBlock extends BucketPickup, LiquidBlockContainer {
    BooleanProperty LAVALOGGED = BooleanProperty.create("agricraft_lavalogged");
    BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * Water, lava and the extra fluids from the {@code fluidloggable_fluids} config ({@link CropFluids}) can be
     * poured in, one fluid at a time.
     * <p>
     * False while a fluid is already in. The bucket ignores the result of {@link #placeLiquid}, so accepting here and
     * refusing there would empty the bucket into nothing. With false (and {@link CropBlock#canBeReplaced(BlockState, Fluid)}
     * keeping the crop from being replaced) the bucket pours next to the crop sticks instead.
     */
    @Override
    default boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !CropFluids.isFluidlogged(state) && CropFluids.withFluid(state, fluid) != null;
    }

    @Override
    default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (CropFluids.isFluidlogged(state) || !fluidState.isSource()) {
            return false;
        }
        BlockState submerged = CropFluids.withFluid(state, fluidState.getType());
        if (submerged == null) {
            return false;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, submerged, 3);
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
        }
        return true;
    }

    @Override
    default ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        FluidState fluid = CropFluids.getFluidState(state);
        if (fluid.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack bucket = new ItemStack(fluid.getType().getBucket());
        if (bucket.isEmpty()) {
            // a configured fluid without a bucket item can't be picked up
            return ItemStack.EMPTY;
        }
        CropFluids.drain(level, pos, state);
        return bucket;
    }

    @Override
    default Optional<SoundEvent> getPickupSound() {
        // TODO: @Ketheroth wrap CropBlock in loader specific block to have the blockstate passed in
//		if (state.getValue(LAVALOGGED)) {
//			return Fluids.LAVA.getPickupSound();
//		}
//		if (state.getValue(WATERLOGGED)) {
//			return Fluids.WATER.getPickupSound()
//		}
        return Fluids.WATER.getPickupSound();
    }
}
