package com.agricraft.agricraft.fabric.mixin;

import com.agricraft.agricraft.common.network.SeedBagSorterPayload;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

	/**
	 * Sneak + scroll with a seed bag in the main hand changes the bag sorter instead of the hotbar slot.
	 * The scroll amount is the argument vanilla passes to {@code swapPaint}.
	 */
	@WrapWithCondition(method = "onScroll(JDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"))
	private boolean agricraft$scrollSeedBag(Inventory inventory, double scroll) {
		if (!SeedBagSorterPayload.shouldCapture(Minecraft.getInstance().player)) {
			return true;
		}
		int step = SeedBagSorterPayload.stepFromScroll(scroll);
		if (step != 0) {
			ClientPlayNetworking.send(new SeedBagSorterPayload(step));
		}
		return false;
	}

}
