package com.agricraft.agricraft.common.plugin;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.requirement.AgriGrowthConditionRegistry;
import com.agricraft.agricraft.api.requirement.AgriGrowthResponse;
import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI;
import com.blakebr0.mysticalagriculture.api.crop.Crop;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Server-side Mystical Agriculture compatibility.
 */
public final class MysticalAgricultureCompat {

	private static final AgriGrowthConditionRegistry.BaseGrowthCondition<Block> EFFECTIVE_FARMLAND =
		new AgriGrowthConditionRegistry.BaseGrowthCondition<>(
			"mystical_agriculture_effective_farmland",
			MysticalAgricultureCompat::checkEffectiveFarmland,
			(level, pos) -> level.getBlockState(pos.below()).getBlock()
		).setDescriber((plant, strength, farmland, consumer) -> {
			Component farmlandName = getMysticalCrop(plant)
				.map(Crop::getTier)
				.map(tier -> tier.getFarmland())
				.map(Block::getName)
				.orElse(Component.translatable("agricraft.tooltip.condition.mystical_agriculture_effective_farmland.unknown"));
			consumer.accept(Component.translatable(
				"agricraft.tooltip.condition.mystical_agriculture_effective_farmland",
				farmlandName
			));
		});

	private MysticalAgricultureCompat() {
	}

	public static void init() {
		AgriApi.getGrowthConditionRegistry().add(EFFECTIVE_FARMLAND);
	}

	private static Optional<Crop> getMysticalCrop(AgriPlant plant) {
		if (!CompatConfig.enableMysticalAgriculture || MysticalAgricultureAPI.getCropRegistry() == null) {
			return Optional.empty();
		}
		return AgriApi.getPlantId(plant)
				.filter(id -> CompatConfig.MYSTICAL_AGRICULTURE_MOD_ID.equals(id.getNamespace()))
				.map(MysticalAgricultureAPI.getCropRegistry()::getCropById);
	}

	private static boolean requiresEffectiveFarmland(Crop crop) {
		var config = MysticalAgricultureAPI.getConfigValues();
		return config != null
				&& config.isRequiresEffectiveFarmlandEnabled()
				&& !"inferium".equals(crop.getId().getPath());
	}

	private static AgriGrowthResponse checkEffectiveFarmland(AgriPlant plant, int strength, Block farmland) {
		return getMysticalCrop(plant)
			.filter(MysticalAgricultureCompat::requiresEffectiveFarmland)
			.map(crop -> crop.getTier().isEffectiveFarmland(farmland)
				? AgriGrowthResponse.FERTILE
				: AgriGrowthResponse.INFERTILE)
			.orElse(AgriGrowthResponse.FERTILE);
	}
}
