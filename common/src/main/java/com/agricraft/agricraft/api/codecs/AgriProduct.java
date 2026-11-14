package com.agricraft.agricraft.api.codecs;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.common.util.Platform;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record AgriProduct(ExtraCodecs.TagOrElementLocation item, CompoundTag nbt, int min, int max, double chance,
                          boolean required) {

	public static final Codec<AgriProduct> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("item").forGetter(product -> product.item),
			CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(product -> product.nbt.isEmpty() ? Optional.empty() : Optional.of(product.nbt)),
			Codec.INT.fieldOf("min").forGetter(product -> product.min),
			Codec.INT.fieldOf("max").forGetter(product -> product.max),
			Codec.DOUBLE.fieldOf("chance").forGetter(product -> product.chance),
			Codec.BOOL.fieldOf("required").forGetter(product -> product.required)
	).apply(instance, AgriProduct::new));

	public AgriProduct(ExtraCodecs.TagOrElementLocation item, Optional<CompoundTag> nbt, int min, int max, double chance, boolean required) {
		// codec use
		this(item, nbt.orElse(new CompoundTag()), min, max, chance, required);
	}

	public boolean shouldDrop(RandomSource random) {
		return this.chance > random.nextDouble();
	}

	public static Builder builder() {
		return new Builder();
	}

	public int getAmount(RandomSource random) {
		return random.nextIntBetweenInclusive(this.min, this.max);
	}

	private static final Set<String> WARNED_EMPTY = ConcurrentHashMap.newKeySet();

	/**
	 * Picks the item this product drops: the item itself, or a random member of the tag.
	 *
	 * @return empty when the item or tag resolves to nothing (for example a tag a modpack left empty), in which case
	 * this product is skipped instead of crashing the harvest
	 */
	public Optional<Item> pickItem(RandomSource random) {
		List<Item> possible = Platform.get().getItemsFromLocation(this.item).stream()
				.filter(item -> item != null && item != Items.AIR)
				.toList();
		if (possible.isEmpty()) {
			if (WARNED_EMPTY.add(this.item.toString())) {
				AgriCraft.LOGGER.warn("AgriCraft product '{}' resolves to no items (empty tag or unknown item); it is skipped when harvesting. Check the modpack's tags/datapacks.", this.item);
			}
			return Optional.empty();
		}
		return Optional.of(possible.get(random.nextInt(possible.size())));
	}

	public static class Builder {
		ExtraCodecs.TagOrElementLocation item = new ExtraCodecs.TagOrElementLocation(ResourceLocation.fromNamespaceAndPath("minecraft", "air"), false);
		CompoundTag nbt = new CompoundTag();
		int min = 1;
		int max = 3;
		double chance = 0.95;
		boolean required = true;

		public AgriProduct build() {
			return new AgriProduct(item, nbt, min, max, chance, required);
		}

		public Builder item(String location) {
			this.item = new ExtraCodecs.TagOrElementLocation(ResourceLocation.parse(location), false);
			return this;
		}
		public Builder item(String namespace, String path) {
			this.item = new ExtraCodecs.TagOrElementLocation(ResourceLocation.fromNamespaceAndPath(namespace, path), false);
			return this;
		}
		public Builder tag(String location) {
			this.item = new ExtraCodecs.TagOrElementLocation(ResourceLocation.parse(location), true);
			return this;
		}
		public Builder tag(String namespace, String path) {
			this.item = new ExtraCodecs.TagOrElementLocation(ResourceLocation.fromNamespaceAndPath(namespace, path), true);
			return this;
		}
		public Builder nbt(CompoundTag nbt) {
			this.nbt = nbt;
			return this;
		}
		public Builder count(int min, int max, double chance) {
			this.min = min;
			this.max = max;
			this.chance = chance;
			return this;
		}
		public Builder required(boolean required) {
			this.required = required;
			return this;
		}

	}
}
