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

	/** Length of the valve open/close animation, in ticks (matches the original mod). */
	public static final int VALVE_ANIM_TICKS = 20;

	/** client-side valve animation counter, counts down from VALVE_ANIM_TICKS to 0 */
	private int valveAnimCounter;
	private boolean valveClosing;

	public IrrigationChannelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntityTypes.IRRIGATION_CHANNEL.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, IrrigationChannelBlockEntity channel) {
		if (level.isClientSide) {
			channel.clientTick();
		} else {
			channel.serverTick();
		}
	}

	protected void clientTick() {
		if (this.valveAnimCounter > 0) {
			this.valveAnimCounter--;
		}
	}

	/** Starts the client-side hand-wheel + stem animation. */
	public void startValveAnimation(boolean closing) {
		this.valveClosing = closing;
		this.valveAnimCounter = VALVE_ANIM_TICKS;
	}

	/**
	 * @return the valve open progress, 0 = fully closed, 1 = fully open, smoothly interpolated
	 *         during the open/close animation
	 */
	public float getValveProgress(float partialTick) {
		if (this.valveAnimCounter > 0) {
			float t = Math.max(0.0F, this.valveAnimCounter - partialTick) / VALVE_ANIM_TICKS;
			return this.valveClosing ? t : 1.0F - t;
		}
		return this.getBlockState().getValue(IrrigationChannelBlock.VALVE) == IrrigationChannelBlock.Valve.CLOSED ? 0.0F : 1.0F;
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
