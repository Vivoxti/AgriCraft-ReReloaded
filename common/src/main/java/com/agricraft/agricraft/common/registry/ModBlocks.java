package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.GrateBlock;
import com.agricraft.agricraft.common.block.GreenhouseAirBlock;
import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import com.agricraft.agricraft.common.block.SeedAnalyzerBlock;
import com.agricraft.agricraft.common.block.SprinklerBlock;
import com.agricraft.agricraft.common.util.Platform;
import com.agricraft.agricraft.common.util.PlatformRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class ModBlocks {
	public static final PlatformRegistry<Block> BLOCKS = Platform.get().createRegistry(BuiltInRegistries.BLOCK, AgriApi.MOD_ID);

	public static final PlatformRegistry.Entry<Block> CROP = BLOCKS.register("crop", CropBlock::new);
	public static final PlatformRegistry.Entry<Block> SEED_ANALYZER = BLOCKS.register("seed_analyzer", SeedAnalyzerBlock::new);
	public static final PlatformRegistry.Entry<Block> IRRIGATION_TANK = BLOCKS.register("irrigation_tank", IrrigationTankBlock::new);
	public static final PlatformRegistry.Entry<Block> IRRIGATION_CHANNEL = BLOCKS.register("irrigation_channel", () -> new IrrigationChannelBlock(false));
	public static final PlatformRegistry.Entry<Block> IRRIGATION_CHANNEL_HOLLOW = BLOCKS.register("irrigation_channel_hollow", () -> new IrrigationChannelBlock(true));
	public static final PlatformRegistry.Entry<Block> SPRINKLER = BLOCKS.register("sprinkler", SprinklerBlock::new);
	public static final PlatformRegistry.Entry<Block> GRATE = BLOCKS.register("grate", GrateBlock::new);
	public static final PlatformRegistry.Entry<Block> GREENHOUSE_AIR = BLOCKS.register("greenhouse_air", GreenhouseAirBlock::new);

}
