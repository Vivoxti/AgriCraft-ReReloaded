package com.agricraft.agricraft.common.neoforge.lootmodifier;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.genetic.AgriAllele;
import com.agricraft.agricraft.api.genetic.AgriGenePair;
import com.agricraft.agricraft.api.genetic.AgriGeneRegistry;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.genetic.GeneSpecies;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.Optional;

/**
 * Global loot modifier that injects AgriCraft seeds into grass drops.
 */
public class GrassLootModifier extends LootModifier {

	public static final MapCodec<GrassLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
			LootModifier.codecStart(instance).and(instance.group(
					Codec.BOOL.fieldOf("reset").forGetter(e -> e.reset),
					Codec.DOUBLE.fieldOf("chance").forGetter(e -> e.chance),
					Entry.CODEC.listOf().fieldOf("seeds").forGetter(e -> e.entries)
			)).apply(instance, GrassLootModifier::new)
	);

	private final boolean reset;
	private final double chance;
	private final List<Entry> entries;
	private final int totalWeight;

	public GrassLootModifier(LootItemCondition[] conditionsIn, boolean reset, double chance, List<Entry> entries) {
		super(conditionsIn);
		this.reset = reset;
		this.chance = chance;
		this.entries = entries;
		this.totalWeight = entries.stream().mapToInt(Entry::weight).sum();
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		if (CoreConfig.allowGrassDropResets && this.reset) {
			generatedLoot.clear();
		}
		if (generatedLoot.isEmpty() && this.roll(context.getRandom()) && !this.entries.isEmpty()) {
			RegistryAccess registryAccess = context.getLevel().registryAccess();
			Entry entry = this.selectRandomEntry(context.getRandom(), registryAccess);
			if (entry != null) {
				ItemStack stack = entry.generateSeed(context.getRandom(), registryAccess);
				if (!stack.isEmpty()) {
					generatedLoot.add(stack);
				}
			}
		}
		return generatedLoot;
	}

	protected boolean roll(RandomSource random) {
		return random.nextDouble() < this.chance;
	}

	protected Entry selectRandomEntry(RandomSource random, RegistryAccess registryAccess) {
		int i = random.nextInt(this.totalWeight);
		for (Entry entry : this.entries) {
			if (entry.weight() >= i && entry.getPlant(registryAccess).isPresent()) {
				return entry;
			}
			i -= entry.weight();
		}
		return null;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}

	public record Entry(ResourceLocation plantId, int minStat, int maxStat, int weight) {

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("plant").forGetter(Entry::plantId),
				Codec.INT.fieldOf("min_stat").forGetter(Entry::minStat),
				Codec.INT.fieldOf("max_stat").forGetter(Entry::maxStat),
				Codec.INT.fieldOf("weight").forGetter(Entry::weight)
		).apply(instance, Entry::new));

		public Optional<AgriPlant> getPlant(RegistryAccess registryAccess) {
			return AgriApi.getPlant(this.plantId, registryAccess);
		}

		public ItemStack generateSeed(RandomSource random, RegistryAccess registryAccess) {
			if (this.getPlant(registryAccess).isEmpty()) {
				return ItemStack.EMPTY;
			}
			GeneSpecies species = AgriGeneRegistry.getInstance().getGeneSpecies();
			AgriAllele<String> allele = species.getAllele(this.plantId.toString());
			List<AgriGenePair<Integer>> statPairs = AgriStatRegistry.getInstance().stream()
					.map(stat -> AgriGeneRegistry.getInstance().getGeneStat(stat))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.map(gene -> new AgriGenePair<>(gene, gene.getAllele(random.nextIntBetweenInclusive(this.minStat, this.maxStat))))
					.toList();
			return AgriSeedItem.toStack(new AgriGenome(new AgriGenePair<>(species, allele), statPairs));
		}

	}

}
