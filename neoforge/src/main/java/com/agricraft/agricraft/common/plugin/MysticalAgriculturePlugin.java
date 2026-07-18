package com.agricraft.agricraft.common.plugin;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.common.registry.ModItems;
import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI;
import com.blakebr0.mysticalagriculture.api.crop.Crop;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Optional;

@EventBusSubscriber(modid = AgriApi.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MysticalAgriculturePlugin {

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		if (ModList.get().isLoaded("mysticalagriculture") && CompatConfig.enableMysticalAgriculture) {
			event.register((stack, tintIndex) -> {
				String species = AgriSeedItem.getSpecies(stack);
				if (species != null && !species.equals("agricraft:unknown")) {
					Crop crop = MysticalAgricultureAPI.getCropRegistry().getCropById(ResourceLocation.parse(species));
					if (crop != null && crop.isSeedColored()) {
						return crop.getSeedColor();
					}
				}
				return -1;
			}, ModItems.SEED.get());
		}
	}

	@SubscribeEvent
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		if (ModList.get().isLoaded("mysticalagriculture") && CompatConfig.enableMysticalAgriculture) {
			event.register((state, level, pos, tintIndex) -> {
				Optional<AgriCrop> optional = AgriApi.getCrop(level, pos);
				if (optional.isPresent() && optional.get().hasPlant()) {
					String species = optional.get().getGenome().getSpeciesGene().getTrait();
					Crop crop = MysticalAgricultureAPI.getCropRegistry().getCropById(ResourceLocation.parse(species));
					if (crop != null && crop.isFlowerColored()) {
						return crop.getFlowerColor();
					}
				}
				return -1;
			}, ModBlocks.CROP.get());
		}
	}

}
