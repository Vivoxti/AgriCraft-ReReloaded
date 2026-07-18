package com.agricraft.agricraft.common.greenhouse;

import com.agricraft.agricraft.api.config.GreenhouseConfig;
import com.agricraft.agricraft.common.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks all greenhouses of a level.
 * <p>
 * A greenhouse is created explicitly (with the greenhouse monitor) by flood-filling the enclosed
 * space around the player. Interior air blocks are replaced by invisible greenhouse air blocks,
 * which shield crops from seasonal requirements and speed up their growth. Breaking a block of the
 * greenhouse hull dissolves the greenhouse.
 */
public class Greenhouses extends SavedData {

	public static final TagKey<Block> GREENHOUSE_GLASS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("agricraft", "greenhouse_glass"));

	private static final String DATA_NAME = "agricraft_greenhouses";

	private final Map<Integer, Greenhouse> greenhouses = new HashMap<>();
	private final Long2IntMap interiorIndex = new Long2IntOpenHashMap();
	private final Long2IntMap boundaryIndex = new Long2IntOpenHashMap();
	private int nextId = 1;

	public Greenhouses() {
		this.interiorIndex.defaultReturnValue(-1);
		this.boundaryIndex.defaultReturnValue(-1);
	}

	public static Greenhouses get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(new Factory<>(Greenhouses::new, Greenhouses::load, DataFixTypes.LEVEL), DATA_NAME);
	}

	/**
	 * @return whether the given position is inside a greenhouse
	 */
	public static boolean isInGreenhouse(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			return get(serverLevel).interiorIndex.containsKey(pos.asLong());
		}
		// client side fallback: check the block itself or the one above (crops replace the greenhouse air block)
		return level.getBlockState(pos).is(ModBlocks.GREENHOUSE_AIR.get())
				|| level.getBlockState(pos.above()).is(ModBlocks.GREENHOUSE_AIR.get());
	}

	/**
	 * Attempts to create a greenhouse by flood-filling from the given starting position.
	 *
	 * @return the result of the attempt
	 */
	public static Result createGreenhouse(ServerLevel level, BlockPos start) {
		if (!level.getBlockState(start).isAir()) {
			return Result.NOT_AIR;
		}
		if (level.getBlockState(start).is(ModBlocks.GREENHOUSE_AIR.get())) {
			return Result.ALREADY_EXISTS;
		}
		int limit = GreenhouseConfig.blockLimit;
		LongSet interior = new LongOpenHashSet();
		LongSet boundary = new LongOpenHashSet();
		LongSet glass = new LongOpenHashSet();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start.immutable());
		interior.add(start.asLong());
		while (!queue.isEmpty()) {
			if (interior.size() > limit) {
				return Result.TOO_BIG;
			}
			BlockPos pos = queue.poll();
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				long key = next.asLong();
				if (interior.contains(key) || boundary.contains(key)) {
					continue;
				}
				if (level.isOutsideBuildHeight(next)) {
					return Result.NOT_ENCLOSED;
				}
				BlockState state = level.getBlockState(next);
				if (state.is(ModBlocks.GREENHOUSE_AIR.get())) {
					return Result.ALREADY_EXISTS;
				}
				if (state.is(GREENHOUSE_GLASS)) {
					boundary.add(key);
					glass.add(key);
				} else if (isSolidBoundary(level, next, state)) {
					boundary.add(key);
				} else {
					// air and non-solid blocks are part of the interior
					interior.add(key);
					queue.add(next.immutable());
				}
			}
		}
		// count ceiling blocks: boundary blocks right above an interior block
		int ceiling = 0;
		int ceilingGlass = 0;
		for (long key : boundary) {
			BlockPos pos = BlockPos.of(key);
			if (interior.contains(pos.below().asLong())) {
				ceiling++;
				if (glass.contains(key)) {
					ceilingGlass++;
				}
			}
		}
		if (ceiling == 0 || (double) ceilingGlass / ceiling < GreenhouseConfig.ceilingGlassFraction) {
			return Result.INSUFFICIENT_GLASS;
		}
		// register the greenhouse and replace the interior air blocks
		Greenhouses data = get(level);
		int id = data.nextId++;
		Greenhouse greenhouse = new Greenhouse(id, new LongArrayList(interior), new LongArrayList(boundary));
		data.greenhouses.put(id, greenhouse);
		greenhouse.interior().forEach(key -> data.interiorIndex.put(key, id));
		greenhouse.boundary().forEach(key -> data.boundaryIndex.put(key, id));
		data.setDirty();
		BlockState air = ModBlocks.GREENHOUSE_AIR.get().defaultBlockState();
		greenhouse.interior().forEach(key -> {
			BlockPos pos = BlockPos.of(key);
			if (level.getBlockState(pos).isAir()) {
				level.setBlock(pos, air, Block.UPDATE_CLIENTS);
			}
		});
		return Result.CREATED;
	}

	private static boolean isSolidBoundary(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) {
			return true;
		}
		for (Direction dir : Direction.values()) {
			if (state.isFaceSturdy(level, pos, dir)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Called when a block changed on the server; dissolves the greenhouse when its hull is broken
	 * and restores greenhouse air inside.
	 */
	public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState newState) {
		Greenhouses data = get(level);
		long key = pos.asLong();
		int boundaryId = data.boundaryIndex.get(key);
		if (boundaryId >= 0) {
			if (!newState.is(GREENHOUSE_GLASS) && !isSolidBoundary(level, pos, newState)) {
				data.dissolve(level, boundaryId);
			}
			return;
		}
		int interiorId = data.interiorIndex.get(key);
		if (interiorId >= 0 && newState.isAir() && !newState.is(ModBlocks.GREENHOUSE_AIR.get())) {
			level.setBlock(pos, ModBlocks.GREENHOUSE_AIR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Removes the greenhouse with the given id and reverts its interior to plain air.
	 */
	public void dissolve(ServerLevel level, int id) {
		Greenhouse greenhouse = this.greenhouses.remove(id);
		if (greenhouse == null) {
			return;
		}
		greenhouse.interior().forEach(key -> this.interiorIndex.remove(key));
		greenhouse.boundary().forEach(key -> this.boundaryIndex.remove(key));
		this.setDirty();
		greenhouse.interior().forEach(key -> {
			BlockPos pos = BlockPos.of(key);
			if (level.getBlockState(pos).is(ModBlocks.GREENHOUSE_AIR.get())) {
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		});
	}

	public Optional<Greenhouse> getGreenhouseAt(BlockPos pos) {
		int id = this.interiorIndex.get(pos.asLong());
		return id >= 0 ? Optional.ofNullable(this.greenhouses.get(id)) : Optional.empty();
	}

	public static Greenhouses load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		Greenhouses data = new Greenhouses();
		data.nextId = tag.getInt("next_id");
		ListTag list = tag.getList("greenhouses", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			int id = entry.getInt("id");
			LongList interior = new LongArrayList(entry.getLongArray("interior"));
			LongList boundary = new LongArrayList(entry.getLongArray("boundary"));
			Greenhouse greenhouse = new Greenhouse(id, interior, boundary);
			data.greenhouses.put(id, greenhouse);
			interior.forEach(key -> data.interiorIndex.put(key, id));
			boundary.forEach(key -> data.boundaryIndex.put(key, id));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		tag.putInt("next_id", this.nextId);
		ListTag list = new ListTag();
		for (Greenhouse greenhouse : this.greenhouses.values()) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("id", greenhouse.id());
			entry.putLongArray("interior", greenhouse.interior().toLongArray());
			entry.putLongArray("boundary", greenhouse.boundary().toLongArray());
			list.add(entry);
		}
		tag.put("greenhouses", list);
		return tag;
	}

	public record Greenhouse(int id, LongList interior, LongList boundary) {
	}

	public enum Result {
		CREATED,
		NOT_AIR,
		ALREADY_EXISTS,
		TOO_BIG,
		NOT_ENCLOSED,
		INSUFFICIENT_GLASS;

		public String translationKey() {
			return "agricraft.message.greenhouse." + this.name().toLowerCase();
		}
	}

}
