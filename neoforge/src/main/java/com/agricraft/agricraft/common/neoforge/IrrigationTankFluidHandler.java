package com.agricraft.agricraft.common.neoforge;

import com.agricraft.agricraft.common.block.entity.IrrigationTankBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes an irrigation tank as a fluid handler so pipes and pumps from other mods can interact with it.
 */
public class IrrigationTankFluidHandler implements IFluidHandler {

	private final IrrigationTankBlockEntity tank;

	public IrrigationTankFluidHandler(IrrigationTankBlockEntity tank) {
		this.tank = tank;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@NotNull
	@Override
	public FluidStack getFluidInTank(int index) {
		return this.tank.getContent() <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, this.tank.getContent());
	}

	@Override
	public int getTankCapacity(int index) {
		return this.tank.getCapacity();
	}

	@Override
	public boolean isFluidValid(int index, @NotNull FluidStack stack) {
		return stack.getFluid() == Fluids.WATER;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !this.isFluidValid(0, resource)) {
			return 0;
		}
		return this.tank.pushWater(resource.getAmount(), action.execute());
	}

	@NotNull
	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !this.isFluidValid(0, resource)) {
			return FluidStack.EMPTY;
		}
		return this.drain(resource.getAmount(), action);
	}

	@NotNull
	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		int drained = this.tank.drainWater(maxDrain, action.execute());
		return drained <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, drained);
	}

}
