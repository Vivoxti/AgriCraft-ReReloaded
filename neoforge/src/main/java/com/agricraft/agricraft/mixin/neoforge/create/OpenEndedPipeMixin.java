package com.agricraft.agricraft.mixin.neoforge.create;

import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropFluids;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets an open-ended Create pipe drain submerged crop sticks properly.
 * <p>
 * Create drains any block with a {@code waterlogged} property by setting it to false. Crop sticks have that property,
 * but lava and the {@code fluidloggable_fluids} fluids are held in other properties, so the crop kept its fluid and the
 * pipe pulled 1000 mB out of it again and again: an endless source. Here the crop's actual fluid is drained.
 * Only applied when Create is present ({@code agricraft-neoforge.mixins.json}, {@code required=false}).
 */
@Mixin(OpenEndedPipe.class)
public class OpenEndedPipeMixin {

	@Shadow
	private Level world;

	@Shadow
	private BlockPos outputPos;

	@Inject(method = "removeFluidFromSpace", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
			ordinal = 0), cancellable = true)
	private void agricraft$drainCropSticks(boolean simulate, CallbackInfoReturnable<FluidStack> cir) {
		BlockState state = this.world.getBlockState(this.outputPos);
		if (!(state.getBlock() instanceof CropBlock)) {
			return;
		}
		FluidState fluid = CropFluids.getFluidState(state);
		if (fluid.isEmpty()) {
			cir.setReturnValue(FluidStack.EMPTY);
			return;
		}
		FluidStack stack = new FluidStack(fluid.getType(), 1000);
		if (!simulate) {
			CropFluids.drain(this.world, this.outputPos, state);
		}
		cir.setReturnValue(stack);
	}

}
