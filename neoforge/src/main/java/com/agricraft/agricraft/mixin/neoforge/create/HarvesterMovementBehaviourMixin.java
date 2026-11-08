package com.agricraft.agricraft.mixin.neoforge.create;

import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets Create's harvester actor harvest AgriCraft crops using AgriCraft's own harvesting
 * mechanics (weed/growth progression, genetics driven drops).
 * <p>
 * Originally the standalone "AgriCraft Create Compat" mod by Kuki2008 (MIT); integrated here so it
 * builds against this fork's NeoForge version. Only applied when Create is present
 * (see {@code agricraft-neoforge.mixins.json}, {@code required=false}).
 */
@Mixin(HarvesterMovementBehaviour.class)
public class HarvesterMovementBehaviourMixin {

	@Inject(
			method = "visitNewPosition(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
					ordinal = 0
			),
			cancellable = true
	)
	private void agricraft$harvest(MovementContext context, BlockPos pos, CallbackInfo ci) {
		Level world = context.world;

		if (world.isClientSide()) {
			return;
		}

		var nativeCrop = com.agricraft.agricraft.api.compat.ExternalHarvestSupport.inspect(world, pos);
		if (nativeCrop != com.agricraft.agricraft.api.compat.ExternalHarvestSupport.Availability.UNHANDLED) {
			com.agricraft.agricraft.api.AgriApi.harvest(world, pos,
					stack -> ((MovementBehaviour) (Object) this).collectOrDropItem(context, stack));
			ci.cancel();
			return;
		}

		if (!(world.getBlockState(pos).getBlock() instanceof CropBlock)) {
			return;
		}
		// AgriCraft crop blocks are handled here exclusively: isValidOther() reports them as cuttable
		// (so harvesters pass through colliding crop sticks), which would otherwise make Create break them.
		ci.cancel();

		if (!(world.getBlockEntity(pos) instanceof CropBlockEntity crop) || !crop.hasPlant()) {
			return;
		}

		if (crop.harvest(stack -> {
			if (!stack.isEmpty()) {
				((MovementBehaviour) (Object) this).collectOrDropItem(context, stack);
			}
		}, null)) {
			if (crop.hasWeeds()) {
				crop.setWeedGrowthStage(crop.getWeed().getInitialGrowthStage());
			}
		} else if (CompatConfig.createHarvesterRevertsGrowth) {
			crop.setGrowthStage(crop.getGrowthStage().getPrevious(crop, world.random));
			if (crop.hasWeeds()) {
				crop.setWeedGrowthStage(crop.getWeedGrowthStage().getPrevious(crop, world.random));
			}
		}
	}

	/**
	 * Create's contraption collider stops a moving contraption when a harvester runs into a block with a collision
	 * shape that it does not consider harvestable. AgriCraft crop sticks collide (config {@code crop_sticks_collide}),
	 * so report every AgriCraft crop block as cuttable: harvesters move through it and
	 * {@link #agricraft$harvest} decides what actually happens to the crop.
	 */
	@Inject(
			method = "isValidOther(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private void agricraft$passThroughCropSticks(Level world, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (state.getBlock() instanceof CropBlock) {
			cir.setReturnValue(true);
		}
	}
}
