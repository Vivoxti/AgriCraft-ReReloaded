package com.agricraft.agricraft.api.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

/**
 * Agricraft greenhouse configuration category.
 */
@Category("greenhouse")
public final class GreenhouseConfig {

	@ConfigEntry(id = "block_limit", type = EntryType.INTEGER, translation = "config.agricraft.greenhouse.block_limit")
	@ConfigOption.Range(min = 64, max = 8192)
	@Comment("The maximum interior size (in blocks) of a greenhouse")
	public static int blockLimit = 1024;

	@ConfigEntry(id = "ceiling_glass_fraction", type = EntryType.DOUBLE, translation = "config.agricraft.greenhouse.ceiling_glass_fraction")
	@ConfigOption.Range(min = 0.0, max = 1.0)
	@Comment("The minimum fraction of the greenhouse ceiling that must be made of glass")
	public static double ceilingGlassFraction = 0.4;

	@ConfigEntry(id = "ignores_seasons", type = EntryType.BOOLEAN, translation = "config.agricraft.greenhouse.ignores_seasons")
	@Comment("Set to false to prevent greenhouses from bypassing seasonal growth requirements")
	public static boolean ignoresSeasons = true;

	@ConfigEntry(id = "growth_modifier", type = EntryType.DOUBLE, translation = "config.agricraft.greenhouse.growth_modifier")
	@ConfigOption.Range(min = 1.0, max = 3.0)
	@Comment("Growth rate multiplier for crops inside a greenhouse")
	public static double growthModifier = 1.05;

}
