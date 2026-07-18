package com.agricraft.agricraft.datagen;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.neoforge.lootmodifier.GrassLootModifier;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {

	public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, AgriApi.MOD_ID);
	}

	@Override
	protected void start() {
		add("short_grass_drops", new GrassLootModifier(new LootItemCondition[]{
				InvertedLootItemCondition.invert(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Tags.Items.TOOLS_SHEAR))).build(),
				LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SHORT_GRASS).build()
		}, true, 0.2, List.of(
				new GrassLootModifier.Entry(ResourceLocation.fromNamespaceAndPath("minecraft", "wheat"), 1, 1, 10),
				new GrassLootModifier.Entry(ResourceLocation.fromNamespaceAndPath("minecraft", "carrot"), 1, 1, 10),
				new GrassLootModifier.Entry(ResourceLocation.fromNamespaceAndPath("minecraft", "potato"), 1, 1, 10)
		)));
		add("tall_grass_drops", new GrassLootModifier(new LootItemCondition[]{
				InvertedLootItemCondition.invert(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Tags.Items.TOOLS_SHEAR))).build(),
				LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS).build()
		}, true, 0.2, List.of(
				new GrassLootModifier.Entry(ResourceLocation.fromNamespaceAndPath("minecraft", "wheat"), 1, 1, 10),
				new GrassLootModifier.Entry(ResourceLocation.fromNamespaceAndPath("minecraft", "carrot"), 1, 1, 10),
				new GrassLootModifier.Entry(ResourceLocation.fromNamespaceAndPath("minecraft", "potato"), 1, 1, 10)
		)));
	}

}
