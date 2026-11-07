package com.agricraft.agricraft.client.neoforge;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.network.SeedBagSorterPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/**
 * NeoForge client event handler in the neoforge event bus
 */
@EventBusSubscriber(modid = AgriApi.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class NeoForgeBusClientEvents {

	/**
	 * Sneak + scroll with a seed bag in the main hand changes the bag sorter instead of the hotbar slot.
	 */
	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		if (Minecraft.getInstance().screen != null || !SeedBagSorterPayload.shouldCapture(Minecraft.getInstance().player)) {
			return;
		}
		event.setCanceled(true);
		int step = SeedBagSorterPayload.stepFromScroll(event.getScrollDeltaY());
		if (step != 0) {
			PacketDistributor.sendToServer(new SeedBagSorterPayload(step));
		}
	}

	@SubscribeEvent
	public static void onTooltipRender(RenderTooltipEvent.GatherComponents event) {
		if (event.getItemStack().has(DataComponents.CUSTOM_DATA) && event.getItemStack().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("magnifying")) {
			event.getTooltipElements().add(1, Either.left(Component.translatable("agricraft.tooltip.magnifying").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC)));
		}
	}

}
