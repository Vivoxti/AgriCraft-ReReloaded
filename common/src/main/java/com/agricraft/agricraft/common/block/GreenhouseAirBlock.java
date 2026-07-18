package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.common.greenhouse.Greenhouses;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Invisible air-like block marking the interior of a greenhouse.
 */
public class GreenhouseAirBlock extends Block {

	public GreenhouseAirBlock() {
		super(Properties.of().air().noCollission().noLootTable().noOcclusion().replaceable());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
		if (level instanceof ServerLevel serverLevel) {
			Greenhouses.onBlockChanged(serverLevel, neighbourPos, level.getBlockState(neighbourPos));
		}
	}

}
