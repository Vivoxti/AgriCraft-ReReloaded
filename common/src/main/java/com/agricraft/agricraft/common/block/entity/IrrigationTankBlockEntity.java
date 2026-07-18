package com.agricraft.agricraft.common.block.entity;

import com.agricraft.agricraft.api.config.IrrigationConfig;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the irrigation tank.
 * <p>
 * Each tank block holds its own volume of water; stacked and adjacent tanks behave like one body of
 * water: gravity pulls water into the lowest tanks first, and the horizontal balancing of
 * {@link IrrigationComponentBlockEntity} evens out the levels of adjacent columns.
 */
public class IrrigationTankBlockEntity extends IrrigationComponentBlockEntity {

	public static final double MIN_FLUID_HEIGHT = 2.0 / 16.0;
	public static final double MAX_FLUID_HEIGHT = 1.0;

	public IrrigationTankBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntityTypes.IRRIGATION_TANK.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IrrigationTankBlockEntity tank) {
		if (!level.isClientSide) {
			tank.gravityFlow();
			tank.serverTick();
		}
	}

	@Override
	public int getCapacity() {
		return IrrigationConfig.tankCapacity;
	}

	@Override
	public double getMinFluidHeight() {
		return MIN_FLUID_HEIGHT;
	}

	@Override
	public double getMaxFluidHeight() {
		return MAX_FLUID_HEIGHT;
	}

	@Override
	public boolean canConnectTo(Direction side, IrrigationComponentBlockEntity other) {
		if (other instanceof IrrigationTankBlockEntity) {
			return true;
		}
		// channels connect through the hole in the tank wall
		return other instanceof IrrigationChannelBlockEntity
				&& this.getBlockState().getValue(IrrigationTankBlock.connection(side)) == IrrigationTankBlock.Connection.CHANNEL;
	}

	/** Transfers as much water as possible to the tank below (if any). */
	protected void gravityFlow() {
		if (this.level == null || this.isEmpty()) {
			return;
		}
		BlockEntity below = this.level.getBlockEntity(this.getBlockPos().below());
		if (below instanceof IrrigationTankBlockEntity tankBelow && !tankBelow.isFull()) {
			int amount = tankBelow.pushWater(this.getContent(), true);
			this.drainWater(amount, true);
		}
	}

	/** Called by the block when it is raining on top of the tank. */
	public void collectRain() {
		int rate = IrrigationConfig.rainFillRate;
		if (rate > 0) {
			this.pushWater(rate, true);
		}
	}

}
