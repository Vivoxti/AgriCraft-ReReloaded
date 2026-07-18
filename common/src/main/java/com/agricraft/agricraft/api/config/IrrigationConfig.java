package com.agricraft.agricraft.api.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

/**
 * Agricraft irrigation configuration category.
 */
@Category("irrigation")
public final class IrrigationConfig {

	@ConfigEntry(id = "tank_capacity", type = EntryType.INTEGER, translation = "config.agricraft.irrigation.tank_capacity")
	@ConfigOption.Range(min = 1000, max = 40000)
	@Comment("The amount of water (in mB) one irrigation tank block can hold")
	public static int tankCapacity = 8000;

	@ConfigEntry(id = "channel_capacity", type = EntryType.INTEGER, translation = "config.agricraft.irrigation.channel_capacity")
	@ConfigOption.Range(min = 50, max = 2000)
	@Comment("The amount of water (in mB) one irrigation channel block can hold")
	public static int channelCapacity = 500;

	@ConfigEntry(id = "rain_fill_rate", type = EntryType.INTEGER, translation = "config.agricraft.irrigation.rain_fill_rate")
	@ConfigOption.Range(min = 0, max = 50)
	@Comment("The amount of water (in mB per tick) an irrigation tank collects while it is raining (set to 0 to disable)")
	public static int rainFillRate = 5;

	@ConfigEntry(id = "sprinkler_water_consumption", type = EntryType.INTEGER, translation = "config.agricraft.irrigation.sprinkler_water_consumption")
	@ConfigOption.Range(min = 0, max = 1000)
	@Comment("The amount of water (in mB per second) a sprinkler consumes while it is working")
	public static int sprinklerWaterConsumption = 10;

	@ConfigEntry(id = "sprinkler_growth_chance", type = EntryType.DOUBLE, translation = "config.agricraft.irrigation.sprinkler_growth_chance")
	@ConfigOption.Range(min = 0.0, max = 1.0)
	@Comment("The chance a sprinkler gives a growth tick to a plant in its working area on each pass")
	public static double sprinklerGrowthChance = 0.2;

	@ConfigEntry(id = "sprinkler_interval", type = EntryType.INTEGER, translation = "config.agricraft.irrigation.sprinkler_interval")
	@ConfigOption.Range(min = 1, max = 1200)
	@Comment("The minimum interval (in ticks) between two full irrigation cycles of a sprinkler")
	public static int sprinklerInterval = 40;

	@ConfigEntry(id = "sprinkler_particles", type = EntryType.BOOLEAN, translation = "config.agricraft.irrigation.sprinkler_particles")
	@Comment("Set to false to disable sprinkler water particles")
	public static boolean sprinklerParticles = true;

}
