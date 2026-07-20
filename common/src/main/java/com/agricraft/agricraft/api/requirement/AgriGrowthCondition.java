package com.agricraft.agricraft.api.requirement;

import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.crop.AgriCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public interface AgriGrowthCondition<T> {

	/**
	 * Checks this value at the given position in the given world.
	 *
	 * @param crop     the crop for which to check this value
	 * @param level    the level the crop is in
	 * @param pos      the position in the world
	 * @param strength the strength stat of the crop
	 * @return the response.
	 */
	AgriGrowthResponse check(AgriCrop crop, Level level, BlockPos pos, int strength);

	/**
	 * Check this condition for a specific strength and value
	 * @param plant the plant specifying the condition
	 * @param strength the strength to check the condition at
	 * @param value the value to check
	 * @return the response
	 */
	AgriGrowthResponse apply(AgriPlant plant, int strength, T value);

	/**
	 * Adds a detailed description of the condition when it is not met to the list.
	 * The crop and strength are provided so the description can explain <em>why</em> the condition
	 * is not met (e.g. whether the soil needs more or less of a property, or which block is missing).
	 *
	 * @param crop     the crop for which the condition is not met
	 * @param strength the strength stat of the crop
	 * @param consumer a consumer accepting the lines of text for a tooltip when the condition is not met.
	 */
	void notMetDescription(AgriCrop crop, int strength, Consumer<Component> consumer);

}
