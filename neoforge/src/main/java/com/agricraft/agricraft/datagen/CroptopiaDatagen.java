package com.agricraft.agricraft.datagen;

import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.agricraft.agricraft.api.codecs.AgriRequirement;
import com.agricraft.agricraft.api.codecs.AgriSeed;
import com.agricraft.agricraft.api.plant.AgriPlant;
import net.minecraft.data.worldgen.BootstrapContext;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.List;

import static com.agricraft.agricraft.api.codecs.AgriSoilCondition.Acidity.SLIGHTLY_ACIDIC;
import static com.agricraft.agricraft.api.codecs.AgriSoilCondition.Humidity.WET;
import static com.agricraft.agricraft.api.codecs.AgriSoilCondition.Nutrients.HIGH;
import static com.agricraft.agricraft.api.codecs.AgriSoilCondition.Type.EQUAL;
import static com.agricraft.agricraft.api.codecs.AgriSoilCondition.Type.EQUAL_OR_HIGHER;

/** Croptopia 4.x farmland crops. Tree crops intentionally retain their native mechanics. */
public final class CroptopiaDatagen {

	private record Crop(String name, String product, String plantName, String seedName) {
		String seed() {
			return name + (name.equals("vanilla") ? "_seeds" : "_seed");
		}
	}

	private static final List<Crop> CROPS = List.of(
			new Crop("artichoke", "artichoke", "Artichoke", "Artichoke Seeds"),
			new Crop("asparagus", "asparagus", "Asparagus", "Asparagus Seeds"),
			new Crop("barley", "barley", "Barley", "Barley Seeds"),
			new Crop("basil", "basil", "Basil", "Basil Seeds"),
			new Crop("bellpepper", "bellpepper", "Bell Pepper", "Bell Pepper Seeds"),
			new Crop("blackbean", "blackbean", "Black Bean", "Black Bean Seeds"),
			new Crop("blackberry", "blackberry", "Blackberry", "Blackberry Seeds"),
			new Crop("blueberry", "blueberry", "Blueberry", "Blueberry Seeds"),
			new Crop("broccoli", "broccoli", "Broccoli", "Broccoli Seeds"),
			new Crop("cabbage", "cabbage", "Cabbage", "Cabbage Seeds"),
			new Crop("cantaloupe", "cantaloupe", "Cantaloupe", "Cantaloupe Seeds"),
			new Crop("cauliflower", "cauliflower", "Cauliflower", "Cauliflower Seeds"),
			new Crop("celery", "celery", "Celery", "Celery Seeds"),
			new Crop("chile_pepper", "chile_pepper", "Chile Pepper", "Chile Pepper Seeds"),
			new Crop("coffee", "coffee_beans", "Coffee Beans", "Coffee Seeds"),
			new Crop("corn", "corn", "Corn", "Corn Seeds"),
			new Crop("cranberry", "cranberry", "Cranberry", "Cranberry Seeds"),
			new Crop("cucumber", "cucumber", "Cucumber", "Cucumber Seeds"),
			new Crop("currant", "currant", "Currant", "Currant Seeds"),
			new Crop("eggplant", "eggplant", "Eggplant", "Eggplant Seeds"),
			new Crop("elderberry", "elderberry", "Elderberry", "Elderberry Seeds"),
			new Crop("garlic", "garlic", "Garlic", "Garlic Seeds"),
			new Crop("ginger", "ginger", "Ginger", "Ginger Seeds"),
			new Crop("grape", "grape", "Grape", "Grape Seeds"),
			new Crop("greenbean", "greenbean", "Green Bean", "Green Bean Seeds"),
			new Crop("greenonion", "greenonion", "Green Onion", "Green Onion Seeds"),
			new Crop("honeydew", "honeydew", "Honeydew", "Honeydew Seeds"),
			new Crop("hops", "hops", "Hops", "Hops Seeds"),
			new Crop("kale", "kale", "Kale", "Kale Seeds"),
			new Crop("kiwi", "kiwi", "Kiwi", "Kiwi Seeds"),
			new Crop("leek", "leek", "Leek", "Leek Seeds"),
			new Crop("lettuce", "lettuce", "Lettuce", "Lettuce Seeds"),
			new Crop("mustard", "mustard", "Mustard", "Mustard Seeds"),
			new Crop("oat", "oat", "Oats", "Oats Seeds"),
			new Crop("olive", "olive", "Olive", "Olive Seeds"),
			new Crop("onion", "onion", "Onion", "Onion Seeds"),
			new Crop("peanut", "peanut", "Peanut", "Peanut Seeds"),
			new Crop("pepper", "pepper", "Pepper", "Pepper Seeds"),
			new Crop("pineapple", "pineapple", "Pineapple", "Pineapple Seeds"),
			new Crop("radish", "radish", "Radish", "Radish Seeds"),
			new Crop("raspberry", "raspberry", "Raspberry", "Raspberry Seeds"),
			new Crop("rhubarb", "rhubarb", "Rhubarb", "Rhubarb Seeds"),
			new Crop("rice", "rice", "Rice", "Rice Seeds"),
			new Crop("rutabaga", "rutabaga", "Rutabaga", "Rutabaga Seeds"),
			new Crop("saguaro", "saguaro", "Saguaro", "Saguaro Seeds"),
			new Crop("soybean", "soybean", "Soybeans", "Soybean Seeds"),
			new Crop("spinach", "spinach", "Spinach", "Spinach Seeds"),
			new Crop("squash", "squash", "Squash", "Squash Seeds"),
			new Crop("strawberry", "strawberry", "Strawberry", "Strawberry Seeds"),
			new Crop("sweetpotato", "sweetpotato", "Sweet Potato", "Sweet Potato Seeds"),
			new Crop("tea", "tea_leaves", "Tea Leaves", "Tea Seeds"),
			new Crop("tomatillo", "tomatillo", "Tomatillo", "Tomatillo Seeds"),
			new Crop("tomato", "tomato", "Tomato", "Tomato Seeds"),
			new Crop("turmeric", "turmeric", "Turmeric", "Turmeric Seeds"),
			new Crop("turnip", "turnip", "Turnip", "Turnip Seeds"),
			new Crop("vanilla", "vanilla", "Vanilla", "Vanilla Seeds"),
			new Crop("yam", "yam", "Yam", "Yam Seeds"),
			new Crop("zucchini", "zucchini", "Zucchini", "Zucchini Seeds")
	);

	public static void registerPlants(BootstrapContext<AgriPlant> context) {
		for (Crop crop : CROPS) {
			AgriPlant.Builder plant = new AgriPlant.Builder().mods("croptopia")
					.seeds(AgriSeed.builder().item("croptopia:" + crop.seed()).chances(0, 1, 0).build())
					.harvest(0).chances(0.75, 0.025, 0.1)
					.products(AgriProduct.builder().item("croptopia:" + crop.product()).count(1, 3, 1).build())
					.requirement(AgriRequirement.builder().humidity(WET, EQUAL, 0.15)
							.acidity(SLIGHTLY_ACIDIC, EQUAL, 0.2).nutrients(HIGH, EQUAL_OR_HIGHER, 0.1)
							.light(9, 16, 0.5).build());
			if (crop.name().equals("corn")) {
				plant.stages(4, 8, 12, 16, 20, 24, 28, 32);
			} else {
				plant.stages16();
			}
			PlantsDatagen.r(context, "croptopia", crop.name(), plant.build());
		}
	}

	public static void registerPlantModels(ModelProvider<BlockModelBuilder> models) {
		// Match Croptopia's native age-to-model mapping, preserving its model geometry.
		int[] textures = {0, 1, 1, 1, 2, 2, 2, 3};
		for (Crop crop : CROPS) {
			for (int stage = 0; stage < textures.length; stage++) {
				models.withExistingParent(crop.name() + "_stage" + stage,
						"croptopia:block/" + crop.name() + "_crop_stage" + textures[stage]);
			}
		}
	}

	public static void registerSeedModels(ModelProvider<ItemModelBuilder> models) {
		for (Crop crop : CROPS) {
			models.withExistingParent(crop.name(), "croptopia:item/" + crop.seed());
		}
	}

	public static void registerTranslations(LanguageProvider lang) {
		for (Crop crop : CROPS) {
			lang.add("plant.agricraft.croptopia." + crop.name(), crop.plantName());
			lang.add("seed.agricraft.croptopia." + crop.name(), crop.seedName());
		}
	}
}
