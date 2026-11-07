package com.agricraft.agricraft.common.item;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.genetic.AgriGenomeProviderItem;
import com.agricraft.agricraft.api.stat.AgriStat;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropState;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.common.util.LangUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SeedBagItem extends Item {

	public static final List<BagSorter> SORTERS = new ArrayList<>();
	public static final BagSorter DEFAULT_SORTER = new BagSorter() {
		@Override
		public ResourceLocation getId() {
			return ResourceLocation.fromNamespaceAndPath("agricraft", "default");
		}

		@Override
		public int compare(BagEntry entry1, BagEntry entry2) {
			int s1 = AgriStatRegistry.getInstance().stream().mapToInt(stat -> entry1.genome.getStatGene(stat).getTrait()).sum();
			int s2 = AgriStatRegistry.getInstance().stream().mapToInt(stat -> entry2.genome.getStatGene(stat).getTrait()).sum();
			if (s1 != s2) {
				return s2 - s1;
			}
			return AgriStatRegistry.getInstance().stream().mapToInt(stat -> {
				Integer d1 = entry1.genome.getStatGene(stat).getDominant().trait();
				Integer r1 = entry1.genome.getStatGene(stat).getRecessive().trait();
				Integer d2 = entry2.genome.getStatGene(stat).getDominant().trait();
				Integer r2 = entry2.genome.getStatGene(stat).getRecessive().trait();
				return (d2 + r2) - (d1 + r1);
			}).sum();
		}
	};

	static {
		SORTERS.add(DEFAULT_SORTER);
		// the stat registry is lazy and registers one sorter per stat; make sure they exist before the bag is used
		AgriStatRegistry.getInstance();
	}

	public SeedBagItem(Properties properties) {
		super(properties);
	}

	public static void addStatSorter(AgriStat stat) {
		SORTERS.add(new StatSorter(stat));
	}

	private static boolean plantOnCrop(AgriCrop crop, ItemStack seed) {
		if (crop.hasPlant() || crop.isCrossCropSticks()) {
			return false;
		}
		crop.plantGenome(AgriGenome.fromNBT(seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()));
		return true;
	}

	/**
	 * @return a mutable copy of the bag's data; call {@link #setData} to save changes
	 */
	private static CompoundTag getData(ItemStack seedBag) {
		return seedBag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	private static void setData(ItemStack seedBag, CompoundTag tag) {
		if (tag.isEmpty()) {
			seedBag.remove(DataComponents.CUSTOM_DATA);
		} else {
			seedBag.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	private static void clearIfEmpty(CompoundTag tag) {
		if (tag.getList("seeds", Tag.TAG_COMPOUND).isEmpty()) {
			tag.remove("seeds");
			tag.remove("species");
		}
	}

	public static int add(ItemStack seedBag, ItemStack insertedStack) {
		if (insertedStack.isEmpty() || !(insertedStack.getItem() instanceof AgriGenomeProviderItem seed)) {
			return 0;
		}
		Optional<AgriGenome> opt = seed.getGenome(insertedStack);
		if (opt.isEmpty()) {
			return 0;
		}
		AgriGenome genome = opt.get();
		String species = genome.getSpeciesGene().getTrait();
		CompoundTag tag = getData(seedBag);
		// a bag holds a single species
		if (tag.contains("species") && !species.equals(tag.getString("species"))) {
			return 0;
		}
		int size = size(seedBag);
		if (size >= CoreConfig.seedBagCapacity) {
			return 0;
		}
		int insertedCount = Math.min(CoreConfig.seedBagCapacity - size, insertedStack.getCount());
		ListTag seeds = tag.getList("seeds", Tag.TAG_COMPOUND);
		CompoundTag newGenome = new CompoundTag();
		genome.writeToNBT(newGenome);
		// merge into an existing entry with the exact same genome
		boolean merged = false;
		for (int i = 0; i < seeds.size(); i++) {
			CompoundTag entry = seeds.getCompound(i);
			CompoundTag entryGenome = entry.copy();
			entryGenome.remove("count");
			if (entryGenome.equals(newGenome)) {
				entry.putInt("count", entry.getInt("count") + insertedCount);
				merged = true;
				break;
			}
		}
		if (!merged) {
			newGenome.putInt("count", insertedCount);
			seeds.add(newGenome);
		}
		tag.put("seeds", seeds);
		tag.putString("species", species);
		sort(tag);
		setData(seedBag, tag);
		return insertedCount;
	}

	public static ItemStack extractFirstStack(ItemStack seedBag) {
		CompoundTag tag = getData(seedBag);
		ListTag seeds = tag.getList("seeds", Tag.TAG_COMPOUND);
		if (seeds.isEmpty()) {
			return ItemStack.EMPTY;
		}
		BagEntry entry = BagEntry.fromNBT(seeds.getCompound(0));
		ItemStack seed = AgriSeedItem.toStack(entry.genome);
		int count = Math.min(entry.count, seed.getMaxStackSize());
		seed.setCount(count);
		if (count >= entry.count) {
			seeds.remove(0);
		} else {
			seeds.getCompound(0).putInt("count", entry.count - count);
		}
		clearIfEmpty(tag);
		setData(seedBag, tag);
		return seed;
	}

	public static ItemStack extractFirstItem(ItemStack seedBag, boolean simulate) {
		CompoundTag tag = getData(seedBag);
		ListTag seeds = tag.getList("seeds", Tag.TAG_COMPOUND);
		if (seeds.isEmpty()) {
			return ItemStack.EMPTY;
		}
		CompoundTag seedTag = seeds.getCompound(0);
		ItemStack seed = AgriSeedItem.toStack(AgriGenome.fromNBT(seedTag));
		if (!simulate) {
			int count = seedTag.getInt("count") - 1;
			if (count <= 0) {
				seeds.remove(0);
			} else {
				seedTag.putInt("count", count);
			}
			clearIfEmpty(tag);
			setData(seedBag, tag);
		}
		return seed;
	}

	public static void changeSorter(ItemStack seedBag, int delta) {
		if (delta == 0) {
			return;
		}
		CompoundTag tag = getData(seedBag);
		int sorterIndex = Math.floorMod(tag.getInt("sorter") + delta, SORTERS.size());
		tag.putInt("sorter", sorterIndex);
		sort(tag);
		setData(seedBag, tag);
	}

	private static void sort(CompoundTag tag) {
		if (!tag.contains("seeds")) {
			return;
		}
		int sorterIndex = Math.floorMod(tag.getInt("sorter"), SORTERS.size());
		ListTag listTag = tag.getList("seeds", Tag.TAG_COMPOUND);
		List<BagEntry> entries = new ArrayList<>();
		for (int i = 0; i < listTag.size(); i++) {
			entries.add(BagEntry.fromNBT(listTag.getCompound(i)));
		}
		entries.sort(SORTERS.get(sorterIndex));
		listTag.clear();
		for (BagEntry entry : entries) {
			CompoundTag t = new CompoundTag();
			entry.writeToNBT(t);
			listTag.add(t);
		}
	}

	public static int size(ItemStack seedBag) {
		CompoundTag tag = seedBag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (tag == null || !tag.contains("seeds")) {
			return 0;
		}
		ListTag seeds = tag.getList("seeds", Tag.TAG_COMPOUND);
		int count = 0;
		for (int i = 0; i < seeds.size(); i++) {
			count += seeds.getCompound(i).getInt("count");
		}
		return count;
	}

	public static boolean isEmpty(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag == null || !tag.contains("species");
	}

	public static boolean isFilled(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag != null && tag.contains("seeds") && size(stack) == CoreConfig.seedBagCapacity;
	}

	private static void playRemoveOneSound(Entity entity) {
		entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}

	private static void playInsertSound(Entity entity) {
		entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getHand() == InteractionHand.OFF_HAND) {
			return super.useOn(context);
		}
		ItemStack seedBag = context.getItemInHand();
		if (isEmpty(seedBag)) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		ItemStack seed = extractFirstItem(seedBag, true);
		BlockPos pos = context.getClickedPos();
		Optional<AgriCrop> optional = AgriApi.getCrop(level, pos);
		// if we clicked on a crop stick
		if (optional.isPresent()) {
			if (plantOnCrop(optional.get(), seed)) {
				extractFirstItem(seedBag, false);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		// if we clicked on a soil
		optional = AgriApi.getCrop(level, pos.above());
		if (optional.isPresent()) {
			// if there's a crop above
			if (plantOnCrop(optional.get(), seed)) {
				extractFirstItem(seedBag, false);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		} else if (CoreConfig.plantOffCropSticks) {
			// if there is nothing above
			if (AgriApi.getSoil(level, pos).isPresent() && level.getBlockState(pos.above()).isAir()) {
				level.setBlock(pos.above(), ModBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.PLANT), Block.UPDATE_ALL_IMMEDIATE);
				optional = AgriApi.getCrop(level, pos.above());
				if (optional.isPresent()) {
					optional.get().plantGenome(AgriGenome.fromNBT(seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()), context.getPlayer());
					extractFirstItem(seedBag, false);
					return InteractionResult.SUCCESS;
				}
				return InteractionResult.PASS;
			}
		}

		return InteractionResult.PASS;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack seedBag, Slot slot, ClickAction action, Player player) {
		// when you right-click on another stack with the bag
		if (seedBag.getCount() == 1 && action == ClickAction.SECONDARY) {
			ItemStack itemStack = slot.getItem();
			if (itemStack.isEmpty()) {
				if (!isEmpty(seedBag)) {
					playRemoveOneSound(player);
					ItemStack seed = extractFirstStack(seedBag);
					slot.safeInsert(seed);
				}
			} else if (itemStack.getItem().canFitInsideContainerItems()) {
				int inserted = add(seedBag, itemStack);
				slot.safeTake(itemStack.getCount(), inserted, player);
				if (inserted > 0) {
					playInsertSound(player);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack seedBag, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
		// when you right-click on the bag with another stack
		if (seedBag.getCount() == 1 && action == ClickAction.SECONDARY && slot.allowModification(player)) {
			if (other.isEmpty()) {
				if (!isEmpty(seedBag)) {
					playRemoveOneSound(player);
					ItemStack seed = extractFirstStack(seedBag);
					access.set(seed);
				}
			} else if (other.getItem().canFitInsideContainerItems()) {
				int inserted = add(seedBag, other);
				if (inserted > 0) {
					playInsertSound(player);
					other.shrink(inserted);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
		if (isEmpty(stack)) {
			tooltipComponents.add(Component.translatable("agricraft.tooltip.bag.empty").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			tooltipComponents.add(Component.translatable("agricraft.tooltip.bag.content", size(stack)).append(LangUtils.seedName(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("species"))).withStyle(ChatFormatting.DARK_GRAY));
		}
		tooltipComponents.add(sorterMessage(stack).withStyle(ChatFormatting.DARK_GRAY));
	}

	public static BagSorter getSorter(ItemStack seedBag) {
		return SORTERS.get(Math.floorMod(getData(seedBag).getInt("sorter"), SORTERS.size()));
	}

	/**
	 * @return "The bag is currently sorting by ..." for the bag's current sorter
	 */
	public static MutableComponent sorterMessage(ItemStack seedBag) {
		String id = getSorter(seedBag).getId().toString().replace(":", ".");
		return Component.translatable("agricraft.tooltip.bag.sorter")
				.append(Component.translatable("agricraft.tooltip.bag.sorter." + id));
	}

	/**
	 * Sorts its entries by having the best in first, and the worst in last
	 */
	public interface BagSorter extends Comparator<BagEntry> {

		ResourceLocation getId();

	}

	public record BagEntry(int count, AgriGenome genome) {

		public static BagEntry fromNBT(CompoundTag tag) {
			return new BagEntry(tag.getInt("count"), AgriGenome.fromNBT(tag));
		}

		public void writeToNBT(CompoundTag tag) {
			tag.putInt("count", this.count);
			this.genome.writeToNBT(tag);
		}

	}

	public static class StatSorter implements BagSorter {

		private final AgriStat stat;
		private final ResourceLocation id;

		public StatSorter(AgriStat stat) {
			this.stat = stat;
			this.id = ResourceLocation.fromNamespaceAndPath("agricraft", stat.getId());
		}

		@Override
		public ResourceLocation getId() {
			return this.id;
		}

		@Override
		public int compare(BagEntry entry1, BagEntry entry2) {
			int s1 = entry1.genome.getStatGene(this.stat).getTrait();
			int s2 = entry2.genome.getStatGene(this.stat).getTrait();
			if (s1 == s2) {
				return DEFAULT_SORTER.compare(entry1, entry2);
			}
			return s2 - s1;
		}

	}

}
