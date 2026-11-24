package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.api.config.CoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Extra fluids, configured in {@link CoreConfig#fluidloggableFluids}, that crop sticks can be submerged in.
 * <p>
 * Water and lava keep their own boolean properties ({@link SimpleFluidloggedBlock#WATERLOGGED},
 * {@link SimpleFluidloggedBlock#LAVALOGGED}). Every other fluid is a value of the {@code agricraft_fluid} property,
 * which only exists when the config lists at least one fluid, so an empty list leaves the crop block states unchanged.
 * Block states are fixed when blocks are registered, so the list is read once at startup.
 */
public final class CropFluids {

	public static final String NONE = "none";
	public static final String PROPERTY_NAME = "agricraft_fluid";

	private static boolean initialized = false;
	@Nullable
	private static FluidProperty property = null;
	/** property value name to the configured fluid id, sorted by name */
	private static Map<String, ResourceLocation> fluids = Map.of();

	private CropFluids() {
	}

	private static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		Map<String, ResourceLocation> byName = new TreeMap<>();
		String[] configured = CoreConfig.fluidloggableFluids == null ? new String[0] : CoreConfig.fluidloggableFluids;
		for (String entry : configured) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			ResourceLocation id = ResourceLocation.tryParse(entry.trim());
			if (id == null) {
				AgriCraft.LOGGER.warn("Ignoring invalid fluid id \"{}\" in fluidloggable_fluids", entry);
				continue;
			}
			if (id.getNamespace().equals("minecraft") && (id.getPath().equals("water") || id.getPath().equals("flowing_water")
					|| id.getPath().equals("lava") || id.getPath().equals("flowing_lava") || id.getPath().equals("empty"))) {
				AgriCraft.LOGGER.info("Ignoring {} in fluidloggable_fluids: crop sticks always accept water and lava", id);
				continue;
			}
			String name = toPropertyName(id);
			ResourceLocation previous = byName.putIfAbsent(name, id);
			if (previous != null && !previous.equals(id)) {
				AgriCraft.LOGGER.warn("Ignoring {} in fluidloggable_fluids: its state name \"{}\" is already used by {}", id, name, previous);
			}
		}
		fluids = Collections.unmodifiableMap(byName);
		if (!fluids.isEmpty()) {
			List<String> values = new ArrayList<>();
			values.add(NONE);
			values.addAll(fluids.keySet());
			property = new FluidProperty(PROPERTY_NAME, values);
			AgriCraft.LOGGER.info("Crop sticks can be submerged in {} extra fluid(s): {}", fluids.size(), fluids.values());
		}
	}

	/**
	 * Block state property values may only use [a-z0-9_].
	 */
	static String toPropertyName(ResourceLocation id) {
		return (id.getNamespace() + "_" + id.getPath()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
	}

	/**
	 * @return the {@code agricraft_fluid} property, or empty when no extra fluid is configured
	 */
	public static Optional<Property<String>> property() {
		init();
		return Optional.ofNullable(property);
	}

	/**
	 * @return the property value names of the configured fluids, sorted. Used to compare the client and server lists.
	 */
	public static List<String> names() {
		init();
		return List.copyOf(fluids.keySet());
	}

	/**
	 * @return the source fluid a configured id refers to, {@link Fluids#EMPTY} when the fluid is not registered
	 */
	private static Fluid resolve(ResourceLocation id) {
		Fluid fluid = BuiltInRegistries.FLUID.get(id);
		if (fluid instanceof FlowingFluid flowing) {
			return flowing.getSource();
		}
		return fluid;
	}

	private static FluidState sourceState(Fluid fluid) {
		if (fluid instanceof FlowingFluid flowing) {
			return flowing.getSource(false);
		}
		return fluid.defaultFluidState();
	}

	/**
	 * @return the property value for this fluid, or null when crop sticks cannot be submerged in it
	 */
	@Nullable
	public static String nameOf(Fluid fluid) {
		init();
		if (fluid == Fluids.EMPTY) {
			return null;
		}
		for (Map.Entry<String, ResourceLocation> entry : fluids.entrySet()) {
			if (resolve(entry.getValue()) == fluid) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * @return whether this fluid is one of the configured extra fluids (any registered id, source or flowing variant)
	 */
	public static boolean isExtraFluid(Fluid fluid) {
		if (fluid instanceof FlowingFluid flowing) {
			fluid = flowing.getSource();
		}
		return nameOf(fluid) != null;
	}

	/**
	 * @return the extra fluid this state holds, {@link Fluids#EMPTY} when it holds none (or holds water / lava)
	 */
	public static Fluid getExtraFluid(BlockState state) {
		if (property == null || !state.hasProperty(property)) {
			return Fluids.EMPTY;
		}
		String name = state.getValue(property);
		if (NONE.equals(name)) {
			return Fluids.EMPTY;
		}
		ResourceLocation id = fluids.get(name);
		return id == null ? Fluids.EMPTY : resolve(id);
	}

	/**
	 * @return the source fluid state this crop state holds, including water and lava
	 */
	public static FluidState getFluidState(BlockState state) {
		if (state.hasProperty(SimpleFluidloggedBlock.LAVALOGGED) && state.getValue(SimpleFluidloggedBlock.LAVALOGGED)) {
			return Fluids.LAVA.getSource(false);
		}
		if (state.hasProperty(SimpleFluidloggedBlock.WATERLOGGED) && state.getValue(SimpleFluidloggedBlock.WATERLOGGED)) {
			return Fluids.WATER.getSource(false);
		}
		Fluid extra = getExtraFluid(state);
		return extra == Fluids.EMPTY ? Fluids.EMPTY.defaultFluidState() : sourceState(extra);
	}

	/**
	 * @return whether this crop state holds any fluid
	 */
	public static boolean isFluidlogged(BlockState state) {
		return !getFluidState(state).isEmpty();
	}

	/**
	 * @return the state submerged in the given source fluid (water, lava or a configured fluid), or null if it can't be
	 */
	@Nullable
	public static BlockState withFluid(BlockState state, Fluid fluid) {
		if (fluid == Fluids.WATER) {
			return state.setValue(SimpleFluidloggedBlock.WATERLOGGED, true);
		}
		if (fluid == Fluids.LAVA) {
			return state.setValue(SimpleFluidloggedBlock.LAVALOGGED, true);
		}
		String name = nameOf(fluid);
		if (name == null || property == null || !state.hasProperty(property)) {
			return null;
		}
		return state.setValue(property, name);
	}

	/**
	 * @return the state with any fluid removed
	 */
	public static BlockState withoutFluid(BlockState state) {
		state = state.setValue(SimpleFluidloggedBlock.WATERLOGGED, false).setValue(SimpleFluidloggedBlock.LAVALOGGED, false);
		if (property != null && state.hasProperty(property)) {
			state = state.setValue(property, NONE);
		}
		return state;
	}

	/**
	 * Removes the fluid from the crop sticks at {@code pos}, as a bucket or a pump does. Crop sticks that can't
	 * survive dry are broken.
	 */
	public static void drain(LevelAccessor level, BlockPos pos, BlockState state) {
		BlockState drained = withoutFluid(state);
		level.setBlock(pos, drained, 3);
		if (!drained.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
		}
	}

	/**
	 * A block state property whose values are known only at startup (from the config), unlike enum properties.
	 */
	public static final class FluidProperty extends Property<String> {

		private final List<String> values;

		FluidProperty(String name, List<String> values) {
			super(name, String.class);
			this.values = List.copyOf(values);
		}

		@Override
		public Collection<String> getPossibleValues() {
			return this.values;
		}

		@Override
		public String getName(String value) {
			return value;
		}

		@Override
		public Optional<String> getValue(String value) {
			return this.values.contains(value) ? Optional.of(value) : Optional.empty();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			return other instanceof FluidProperty that && super.equals(other) && this.values.equals(that.values);
		}

		@Override
		public int generateHashCode() {
			return 31 * super.generateHashCode() + this.values.hashCode();
		}

	}

}
