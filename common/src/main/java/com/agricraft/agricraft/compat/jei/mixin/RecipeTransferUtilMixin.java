package com.agricraft.agricraft.compat.jei.mixin;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.registry.ModItems;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Lets JEI's recipe transfer ("+" button) use AgriCraft seeds for recipe slots that accept any AgriCraft seed.
 * <p>
 * JEI tells AgriCraft seeds apart by plant (see the subtype interpreter in {@code AgriCraftJeiPlugin}), which keeps
 * recipe lookups per plant. A tag ingredient such as {@code #c:seeds} or {@code #agricraft:seeds} is shown as a bare
 * seed without a genome, and the transfer only matches inventory stacks with the same subtype, so seeds that carry a
 * plant never matched and JEI reported them as missing. When a recipe slot accepts the bare seed, every plant's seed
 * is accepted for the transfer as well; the subtype used for recipe lookups is unchanged.
 */
@Mixin(targets = "mezz.jei.common.transfer.RecipeTransferUtil", remap = false)
public abstract class RecipeTransferUtilMixin {

	@Inject(method = "calculateUids", at = @At("RETURN"), remap = false)
	private static void agricraft$acceptAnyPlantSeed(IRecipeSlotView slotView, IStackHelper stackHelper,
	                                                 CallbackInfoReturnable<Set<Object>> cir) {
		Set<Object> uids = cir.getReturnValue();
		if (uids.isEmpty() || !uids.contains(stackHelper.getUidForStack(new ItemStack(ModItems.SEED.get()), UidContext.Recipe))) {
			return;
		}
		AgriApi.getPlantRegistry().ifPresent(registry -> registry.forEach(plant ->
				uids.add(stackHelper.getUidForStack(AgriSeedItem.toStack(plant), UidContext.Recipe))));
	}
}
