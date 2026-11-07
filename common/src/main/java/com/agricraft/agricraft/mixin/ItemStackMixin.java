package com.agricraft.agricraft.mixin;

import com.agricraft.agricraft.common.handler.SeedFoodCompat;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	/**
	 * AgriCraft seeds count as their vanilla seed item for animal food tags
	 * (chickens, parrots, sniffers, pigs, rabbits, foxes, pandas, turtles, camels, striders, hoglins, bees...).
	 */
	@Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), cancellable = true)
	private void agricraft$seedAsFood(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && SeedFoodCompat.matchesFoodTag((ItemStack) (Object) this, tag)) {
			cir.setReturnValue(true);
		}
	}

}
