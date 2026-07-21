package com.agricraft.agricraft.api.genetic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface AgriGenomeProviderItem {

	/**
	 * Change the genome of the plant.
	 * @param genome the new genome of the crop
	 */
	default void setGenome(ItemStack stack, AgriGenome genome) {
		// copyTag() hands back a detached copy, so it must be written back onto the stack explicitly,
		// otherwise the mutation is silently discarded and the stack keeps its old (or no) genome.
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		genome.writeToNBT(tag);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	default Optional<AgriGenome> getGenome(ItemStack stack) {
		return Optional.ofNullable(AgriGenome.fromNBT(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()));
	}

}
