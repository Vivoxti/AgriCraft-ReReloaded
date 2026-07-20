package com.agricraft.agricraft.api.requirement;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.AgriRegistrable;
import com.agricraft.agricraft.api.AgriRegistry;
import com.agricraft.agricraft.api.codecs.AgriBlockCondition;
import com.agricraft.agricraft.api.codecs.AgriFluidCondition;
import com.agricraft.agricraft.api.codecs.AgriListCondition;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.codecs.AgriRequirement;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.codecs.AgriSoilCondition;
import com.agricraft.agricraft.api.codecs.AgriSoilValue;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.common.util.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Encapsulate the growth conditions of a crop
 */
public class AgriGrowthConditionRegistry extends AgriRegistry<AgriGrowthConditionRegistry.BaseGrowthCondition<?>> {

	private static AgriGrowthConditionRegistry INSTANCE;
	private final BaseGrowthCondition<AgriSoilCondition.Humidity> humidity;
	private final BaseGrowthCondition<AgriSoilCondition.Acidity> acidity;
	private final BaseGrowthCondition<AgriSoilCondition.Nutrients> nutrients;
	private final BaseGrowthCondition<Integer> light;
	private final BaseGrowthCondition<BlockState> block;
	private final BaseGrowthCondition<Holder<Biome>> biome;
	private final BaseGrowthCondition<ResourceKey<Level>> dimension;
	private final BaseGrowthCondition<AgriSeason> season;
	private final BaseGrowthCondition<FluidState> fluid;


	private AgriGrowthConditionRegistry() {
		super();
		humidity = new BaseGrowthCondition<>("humidity",
				(plant, strength, value) -> AgriGrowthConditionRegistry.handleSoilCriterion(strength, value, plant.getGrowthRequirements().soilHumidity()),
				(level, blockPos) -> AgriApi.getCrop(level, blockPos).flatMap(AgriCrop::getSoil).map(AgriSoil::humidity).orElse(AgriSoilCondition.Humidity.INVALID))
				.setDescriber((plant, strength, value, consumer) -> AgriGrowthConditionRegistry.describeSoilCriterion("humidity", strength, value, plant.getGrowthRequirements().soilHumidity(), consumer));
		acidity = new BaseGrowthCondition<>("acidity",
				(plant, strength, value) -> AgriGrowthConditionRegistry.handleSoilCriterion(strength, value, plant.getGrowthRequirements().soilAcidity()),
				(level, blockPos) -> AgriApi.getCrop(level, blockPos).flatMap(AgriCrop::getSoil).map(AgriSoil::acidity).orElse(AgriSoilCondition.Acidity.INVALID))
				.setDescriber((plant, strength, value, consumer) -> AgriGrowthConditionRegistry.describeSoilCriterion("acidity", strength, value, plant.getGrowthRequirements().soilAcidity(), consumer));
		nutrients = new BaseGrowthCondition<>("nutrients",
				(plant, strength, value) -> AgriGrowthConditionRegistry.handleSoilCriterion(strength, value, plant.getGrowthRequirements().soilNutrients()),
				(level, blockPos) -> AgriApi.getCrop(level, blockPos).flatMap(AgriCrop::getSoil).map(AgriSoil::nutrients).orElse(AgriSoilCondition.Nutrients.INVALID))
				.setDescriber((plant, strength, value, consumer) -> AgriGrowthConditionRegistry.describeSoilCriterion("nutrients", strength, value, plant.getGrowthRequirements().soilNutrients(), consumer));
		light = new BaseGrowthCondition<>("light", (plant, strength, value) -> {
			AgriRequirement requirement = plant.getGrowthRequirements();
			int lower = requirement.minLight() - (int) (requirement.lightToleranceFactor() * strength);
			int upper = requirement.maxLight() + (int) (requirement.lightToleranceFactor() * strength);
			return lower <= value && value <= upper ? AgriGrowthResponse.FERTILE : AgriGrowthResponse.INFERTILE;
		}, LevelReader::getMaxLocalRawBrightness);
		block = new BaseGrowthCondition<>("block", (plant, strength, blockstate) -> {
			List<AgriBlockCondition> blockConditions = plant.getGrowthRequirements().blockConditions();
			if (blockConditions.isEmpty()) {
				return AgriGrowthResponse.FERTILE;
			}
			for (AgriBlockCondition blockCondition : blockConditions) {
				if (strength >= blockCondition.strength()) {
					continue;
				}
				List<Block> requiredBlocks = Platform.get().getBlocksFromLocation(blockCondition.block());
				// regular block state requirement
				if (requiredBlocks.contains(blockstate.getBlock())) {
					if (blockCondition.states().isEmpty()) {
						return AgriGrowthResponse.FERTILE;
					}
					Set<String> list = blockstate.getValues().entrySet().stream().map(StateHolder.PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.toSet());
					if (list.containsAll(blockCondition.states())) {
						return AgriGrowthResponse.FERTILE;
					}
				}
				return AgriGrowthResponse.INFERTILE;
			}
			return AgriGrowthResponse.FERTILE;
		}, (level, blockPos) -> level.getBlockState(blockPos.below().below()))
				.setDescriber((plant, strength, value, consumer) -> AgriGrowthConditionRegistry.describeBlockCriterion(plant.getGrowthRequirements().blockConditions(), strength, consumer));
		biome = new BaseGrowthCondition<>("biome", (plant, strength, biome) -> {
			AgriListCondition listCondition = plant.getGrowthRequirements().biomes();
			if (strength >= listCondition.ignoreFromStrength() || (listCondition.blacklist() && listCondition.isEmpty())) {
				return AgriGrowthResponse.FERTILE;
			}
			if (listCondition.blacklist()) {
				if (listCondition.values().stream().anyMatch(biome::is)) {
					return AgriGrowthResponse.INFERTILE;
				}
			} else {
				if (listCondition.values().stream().noneMatch(biome::is)) {
					return AgriGrowthResponse.INFERTILE;
				}
			}
			return AgriGrowthResponse.FERTILE;
		}, LevelReader::getBiome);
		dimension = new BaseGrowthCondition<>("dimension", (plant, strength, dimension) -> {
			AgriListCondition listCondition = plant.getGrowthRequirements().dimensions();
			if (strength >= listCondition.ignoreFromStrength() || listCondition.blacklist() && listCondition.isEmpty()) {
				return AgriGrowthResponse.FERTILE;
			}
			if (listCondition.blacklist()) {
				if (listCondition.values().stream().anyMatch(dimension.location()::equals)) {
					return AgriGrowthResponse.INFERTILE;
				}
			} else {
				if (listCondition.values().stream().noneMatch(dimension.location()::equals)) {
					return AgriGrowthResponse.INFERTILE;
				}
			}
			return AgriGrowthResponse.FERTILE;
		}, (level, blockPos) -> level.dimension());
		season = new BaseGrowthCondition<>("season", (plant, strength, season) -> {
			List<AgriSeason> seasons = plant.getGrowthRequirements().seasons();
			if (!AgriApi.getSeasonLogic().isActive()
					|| seasons.isEmpty()
					|| strength >= AgriApi.getStatRegistry().strengthStat().getMax()
					|| seasons.stream().anyMatch(season::matches)) {
				return AgriGrowthResponse.FERTILE;
			}
			return AgriGrowthResponse.INFERTILE;
		}, (level, blockPos) -> AgriApi.getSeasonLogic().getSeason(level, blockPos));
		fluid = new BaseGrowthCondition<>("fluid", (plant, strength, fluid) -> {
			AgriFluidCondition fluidCondition = plant.getGrowthRequirements().fluidCondition();
			List<Fluid> requiredFluids = Platform.get().getFluidsFromLocation(fluidCondition.fluid());
			if (requiredFluids.isEmpty()) {
				if (fluid.is(Fluids.LAVA)) {
					return AgriGrowthResponse.KILL_IT_WITH_FIRE;
				}
				return fluid.is(Fluids.EMPTY) ? AgriGrowthResponse.FERTILE : AgriGrowthResponse.LETHAL;
			} else {
				if (requiredFluids.contains(fluid.getType())) {
					if (fluidCondition.states().isEmpty()) {
						return AgriGrowthResponse.FERTILE;
					}
					Set<String> list = fluid.getValues().entrySet().stream().map(StateHolder.PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.toSet());
					return list.containsAll(fluidCondition.states()) ? AgriGrowthResponse.FERTILE : AgriGrowthResponse.LETHAL;
				}
				return fluid.is(Fluids.LAVA) ? AgriGrowthResponse.FERTILE : AgriGrowthResponse.LETHAL;
			}
		}, Level::getFluidState);


		// TODO: @Ketheroth warn if no soil matches this requirement
	}

	public static AgriGrowthConditionRegistry getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new AgriGrowthConditionRegistry();
			INSTANCE.add(INSTANCE.humidity);
			INSTANCE.add(INSTANCE.acidity);
			INSTANCE.add(INSTANCE.nutrients);
			INSTANCE.add(INSTANCE.light);
			INSTANCE.add(INSTANCE.block);
			INSTANCE.add(INSTANCE.biome);
			INSTANCE.add(INSTANCE.dimension);
			INSTANCE.add(INSTANCE.season);
			INSTANCE.add(INSTANCE.fluid);
		}
		return INSTANCE;
	}

	public static BaseGrowthCondition<AgriSoilCondition.Humidity> getHumidity() {
		return getInstance().humidity;
	}

	public static BaseGrowthCondition<AgriSoilCondition.Acidity> getAcidity() {
		return getInstance().acidity;
	}

	public static BaseGrowthCondition<AgriSoilCondition.Nutrients> getNutrients() {
		return getInstance().nutrients;
	}

	public static BaseGrowthCondition<Integer> getLight() {
		return getInstance().light;
	}

	public static BaseGrowthCondition<BlockState> getBlock() {
		return getInstance().block;
	}

	public static BaseGrowthCondition<Holder<Biome>> getBiome() {
		return getInstance().biome;
	}

	public static BaseGrowthCondition<ResourceKey<Level>> getDimension() {
		return getInstance().dimension;
	}

	public static BaseGrowthCondition<AgriSeason> getSeason() {
		return getInstance().season;
	}

	public static BaseGrowthCondition<FluidState> getFluid() {
		return getInstance().fluid;
	}

	private static AgriGrowthResponse handleSoilCriterion(int strength, AgriSoilValue value, AgriSoilCondition<?> condition) {
		int lower = condition.type().lowerLimit(condition.value().ordinal() - (int) (condition.toleranceFactor() * strength));
		int upper = condition.type().upperLimit(condition.value().ordinal() + (int) (condition.toleranceFactor() * strength));
		if (value.isValid() && lower <= value.ordinal() && value.ordinal() <= upper) {
			return AgriGrowthResponse.FERTILE;
		}
		return AgriGrowthResponse.INFERTILE;
	}

	/**
	 * Describes an unmet soil criterion, telling the player whether the soil property is too low
	 * ("more" needed) or too high ("less" needed) with respect to the plant's tolerated range,
	 * instead of a generic "this property doesn't fit" message.
	 */
	private static void describeSoilCriterion(String id, int strength, AgriSoilValue value, AgriSoilCondition<?> condition, Consumer<Component> consumer) {
		int lower = condition.type().lowerLimit(condition.value().ordinal() - (int) (condition.toleranceFactor() * strength));
		int upper = condition.type().upperLimit(condition.value().ordinal() + (int) (condition.toleranceFactor() * strength));
		String suffix = "";
		if (value.isValid()) {
			if (value.ordinal() < lower) {
				suffix = ".more";
			} else if (value.ordinal() > upper) {
				suffix = ".less";
			}
		}
		consumer.accept(Component.translatable("agricraft.tooltip.condition." + id + suffix));
	}

	/**
	 * Describes an unmet block criterion as a short riddle hinting at the block that must sit under
	 * the soil. Only the conditions that are actually unmet for the given strength are described, and
	 * an unknown block falls back to a generic hint.
	 */
	private static void describeBlockCriterion(List<AgriBlockCondition> blockConditions, int strength, Consumer<Component> consumer) {
		if (blockConditions.isEmpty()) {
			consumer.accept(Component.translatable("agricraft.tooltip.condition.block"));
			return;
		}
		for (AgriBlockCondition blockCondition : blockConditions) {
			if (strength >= blockCondition.strength()) {
				// a strong enough plant ignores this requirement, so it is not what is holding growth back
				continue;
			}
			ResourceLocation location = blockCondition.block().id();
			String path = location.getPath();
			int slash = path.lastIndexOf('/');
			String name = slash >= 0 ? path.substring(slash + 1) : path;
			// state the requirement plainly first, then let the riddle hint at which block it is
			consumer.accept(Component.translatable("agricraft.tooltip.condition.block"));
			consumer.accept(Component.translatableWithFallback(
					"agricraft.tooltip.condition.block.riddle." + name,
					"A certain block must lie hidden right beneath the soil."));
		}
	}

	public interface ResponseGetter<T> {

		AgriGrowthResponse apply(AgriPlant plant, int strength, T value);

	}

	/**
	 * Produces the tooltip line(s) shown when a condition is not met, with access to the plant, the
	 * crop strength and the actual value read from the world, so the message can be context-aware.
	 */
	public interface NotMetDescriber<T> {

		void describe(AgriPlant plant, int strength, T value, Consumer<Component> consumer);

	}

	public static class BaseGrowthCondition<T> implements AgriGrowthCondition<T>, AgriRegistrable {

		private final String id;
		private final ResponseGetter<T> response;
		private final BiFunction<Level, BlockPos, T> getter;
		private NotMetDescriber<T> describer;

		public BaseGrowthCondition(String id, ResponseGetter<T> response, BiFunction<Level, BlockPos, T> getter) {
			this.id = id;
			this.response = response;
			this.getter = getter;
			// default description: a generic per-condition message
			this.describer = (plant, strength, value, consumer) -> consumer.accept(Component.translatable("agricraft.tooltip.condition." + this.id));
		}

		public BaseGrowthCondition<T> setDescriber(NotMetDescriber<T> describer) {
			this.describer = describer;
			return this;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public AgriGrowthResponse check(AgriCrop crop, Level level, BlockPos pos, int strength) {
			return this.response.apply(crop.getPlant(), strength, this.getter.apply(level, pos));
		}

		@Override
		public AgriGrowthResponse apply(AgriPlant plant, int strength, T value) {
			return this.response.apply(plant, strength, value);
		}

		@Override
		public void notMetDescription(AgriCrop crop, int strength, Consumer<Component> consumer) {
			this.describer.describe(crop.getPlant(), strength, this.getter.apply(crop.getLevel(), crop.getBlockPos()), consumer);
		}

	}

}
