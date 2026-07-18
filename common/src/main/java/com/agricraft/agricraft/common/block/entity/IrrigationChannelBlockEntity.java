package com.agricraft.agricraft.common.block.entity;

import com.agricraft.agricraft.api.config.IrrigationConfig;
import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the irrigation channel: a small water conduit that connects tanks with sprinklers.
 */
public class IrrigationChannelBlockEntity extends IrrigationComponentBlockEntity {

	public static final double MIN_FLUID_HEIGHT = 6.0 / 16.0;
	public static final double MAX_FLUID_HEIGHT = 10.0 / 16.0;

	public IrrigationChannelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntityTypes.IRRIGATION_CHANNEL.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IrrigationChannelBlockEntity channel) {
		if (!level.isClientSide) {
			channel.serverTick();
		}
	}

	@Override
	public int getCapacity() {
		return IrrigationConfig.channelCapacity;
	}

	@Override
	public double getMinFluidHeight() {
		return MIN_FLUID_HEIGHT;
	}

	@Override
	public double getMaxFluidHeight() {
		return MAX_FLUID_HEIGHT;
	}

	/** @return false when a closed valve blocks the water flow */
	public boolean canTransfer() {
		return this.getBlockState().getValue(IrrigationChannelBlock.VALVE).canTransfer();
	}

	@Override
	public boolean canConnectTo(Direction side, IrrigationComponentBlockEntity other) {
		if (!this.canTransfer()) {
			return false;
		}
		if (other instanceof IrrigationChannelBlockEntity || other instanceof IrrigationTankBlockEntity) {
			return this.getBlockState().getValue(IrrigationChannelBlock.connection(side));
		}
		return false;
	}

}
