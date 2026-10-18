package com.agricraft.agricraft.plugin.minecraft;

import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.common.registry.ModItems;
import net.minecraft.world.level.block.ComposterBlock;

public class MinecraftPlugin {

	public static void init() {
		// Fabric uses the vanilla map; NeoForge registers its data map on reload.
		float compostValue = CoreConfig.seedCompostValue;
		if (compostValue > 0) {
			ComposterBlock.COMPOSTABLES.put(ModItems.SEED.get(), compostValue);
		}
		// In 1.20.5+, chicken food items are controlled by the minecraft:chicken_food item tag
		// Add agricraft seeds to this tag via datapack instead of runtime modification
		MinecraftPlantModifiers.register();
	}

}
