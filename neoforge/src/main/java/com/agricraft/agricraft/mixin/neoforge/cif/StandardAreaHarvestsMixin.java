package com.agricraft.agricraft.mixin.neoforge.cif;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.compat.ExternalHarvestSupport;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plus.dragons.createintegratedfarming.api.harvester.AreaHarvestContext;
import plus.dragons.createintegratedfarming.common.farming.harvest.StandardAreaHarvests;

/**
 * Lets the Create: Integrated Farming Vacuum Harvester (stationary and on contraptions) harvest AgriCraft crops.
 * <p>
 * The Vacuum Harvester runs every block in its area through {@code StandardAreaHarvests.harvest}, which only knows
 * vanilla-style crops; an AgriCraft crop block (with or without crop sticks) matches none of its rules, so nothing
 * was harvested. AgriCraft crops are handled here with AgriCraft's own harvest logic instead: mature plants drop
 * their genetics driven products and drop back to their after-harvest stage, the crop sticks and seeds stay in
 * place, and unripe plants are left untouched.
 */
@Mixin(StandardAreaHarvests.class)
public abstract class StandardAreaHarvestsMixin {

	@Inject(method = "harvest", at = @At("HEAD"), cancellable = true)
	private static void agricraft$harvestAgriCraftCrops(AreaHarvestContext context, BlockPos pos, BlockState state,
	                                                    CallbackInfoReturnable<Boolean> cir) {
		Level level = context.level();
		if (level.isClientSide()) {
			return;
		}

		if (ExternalHarvestSupport.inspect(level, pos) != ExternalHarvestSupport.Availability.UNHANDLED) {
			cir.setReturnValue(AgriApi.harvest(level, pos, context::collect));
			return;
		}

		if (!(state.getBlock() instanceof CropBlock)) {
			return;
		}
		// AgriCraft crop blocks are handled here exclusively, the generic area harvest must never break them.
		boolean harvested = false;
		if (level.getBlockEntity(pos) instanceof CropBlockEntity crop && crop.hasPlant()
				&& crop.harvest(context::collect, null)) {
			if (crop.hasWeeds()) {
				crop.setWeedGrowthStage(crop.getWeed().getInitialGrowthStage());
			}
			harvested = true;
		}
		cir.setReturnValue(harvested);
	}
}
