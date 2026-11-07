package com.agricraft.agricraft.common.handler;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriSeed;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.util.Platform;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Lets AgriCraft seeds be used as animal food wherever their vanilla seed item would be.
 * <p>
 * All AgriCraft seeds share one item ({@code agricraft:seed}), so vanilla food checks such as
 * {@code stack.is(ItemTags.CHICKEN_FOOD)} never match them. Every vanilla animal (breeding, taming,
 * tempting, feeding) checks its food through an item tag named {@code *_food} or {@code *_tempt_items},
 * so for those tags only, an AgriCraft seed is treated as the seed item(s) of its plant.
 * Other tags are untouched on purpose: villager farming tags would make villagers place
 * empty crop blocks, and recipe or mod tags must keep seeing the real item.
 */
public final class SeedFoodCompat {

	private SeedFoodCompat() {
	}

	public static boolean isFoodTag(TagKey<Item> tag) {
		String path = tag.location().getPath();
		return path.endsWith("_food") || path.endsWith("_tempt_items");
	}

	/**
	 * @return true if the stack is an AgriCraft seed whose plant's seed item is in the given food tag
	 */
	public static boolean matchesFoodTag(ItemStack stack, TagKey<Item> tag) {
		if (!(stack.getItem() instanceof AgriSeedItem) || !isFoodTag(tag)) {
			return false;
		}
		return AgriApi.getPlant(AgriSeedItem.getSpecies(stack)).map(plant -> {
			for (AgriSeed seed : plant.getSeeds()) {
				for (Item item : Platform.get().getItemsFromLocation(seed.item())) {
					// holder check, not ItemStack#is, to avoid recursing into the mixin
					if (!(item instanceof AgriSeedItem) && item.builtInRegistryHolder().is(tag)) {
						return true;
					}
				}
			}
			return false;
		}).orElse(false);
	}

}
