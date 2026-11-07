package seedfoodprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** TEMPORARY dev-only probe for the "cannot breed chickens with AgriCraft seeds" bug. Not shipped. */
@EventBusSubscriber(modid = "agricraft")
public final class SeedFoodProbe {
	private static final List<String> REPORT = new ArrayList<>();
	private static int failures = 0;

	private static void check(boolean condition, String message) {
		REPORT.add((condition ? "PASS " : "FAIL ") + message);
		if (!condition) failures++;
	}

	private static ItemStack seed(String plant, int count) {
		ItemStack stack = AgriSeedItem.toStack(AgriApi.getPlant(ResourceLocation.parse(plant)).orElseThrow(() -> new IllegalStateException("no plant " + plant)));
		stack.setCount(count);
		return stack;
	}

	private static void tag(String plant, TagKey<Item> tag, boolean expected) {
		check(seed(plant, 1).is(tag) == expected, plant + " seed is(" + tag.location() + ") == " + expected);
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, BlockPos pos) {
		T mob = type.create(level);
		mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		mob.setNoAi(true);
		level.addFreshEntity(mob);
		return mob;
	}

	private static void breed(ServerLevel level, FakePlayer player, EntityType<? extends Animal> type, String plant, BlockPos pos) {
		Animal animal = spawn(level, type, pos);
		ItemStack stack = seed(plant, 4);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		var result = player.interactOn(animal, InteractionHand.MAIN_HAND);
		check(animal.isInLove(), type.toShortString() + " enters love mode with AgriCraft " + plant + " seed (result " + result + ")");
		check(stack.getCount() == 3, type.toShortString() + " consumes exactly one " + plant + " seed");
		animal.discard();
	}

	private static void verify(MinecraftServer server) {
		ServerLevel level = server.overworld();
		BlockPos spawn = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getSharedSpawnPos());
		FakePlayer player = new FakePlayer(level, new GameProfile(UUID.fromString("6c1f0d8e-5a8a-4a26-9a3e-1f6b3b2f7a11"), "SeedFoodProbe"));
		player.setGameMode(GameType.SURVIVAL);
		player.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 2.5);

		// tag-level coverage
		for (String p : List.of("minecraft:wheat", "minecraft:beetroot", "minecraft:melon", "minecraft:pumpkin", "minecraft:torchflower")) {
			tag(p, ItemTags.CHICKEN_FOOD, true);
			tag(p, ItemTags.PARROT_FOOD, true);
		}
		tag("minecraft:torchflower", ItemTags.SNIFFER_FOOD, true);
		tag("minecraft:carrot", ItemTags.PIG_FOOD, true);
		tag("minecraft:potato", ItemTags.PIG_FOOD, true);
		tag("minecraft:carrot", ItemTags.RABBIT_FOOD, true);
		tag("minecraft:dandelion", ItemTags.RABBIT_FOOD, false); // flower plants have no vanilla seed item
		tag("minecraft:sweet_berries", ItemTags.FOX_FOOD, true);
		tag("minecraft:bamboo", ItemTags.PANDA_FOOD, true);
		tag("minecraft:seagrass", ItemTags.TURTLE_FOOD, true);
		tag("minecraft:cactus", ItemTags.CAMEL_FOOD, true);
		tag("minecraft:warped_fungus", ItemTags.STRIDER_FOOD, true);
		tag("minecraft:warped_fungus", ItemTags.STRIDER_TEMPT_ITEMS, true);
		tag("minecraft:crimson_fungus", ItemTags.HOGLIN_FOOD, true);
		tag("minecraft:poppy", ItemTags.BEE_FOOD, false);
		// negatives: wrong animal, and non-food tags stay untouched
		tag("minecraft:wheat", ItemTags.PIG_FOOD, false);
		tag("minecraft:carrot", ItemTags.CHICKEN_FOOD, false);
		tag("minecraft:wheat", ItemTags.VILLAGER_PLANTABLE_SEEDS, false);
		tag("minecraft:wheat", ItemTags.COW_FOOD, false);
		check(!new ItemStack(Items.WHEAT).is(ItemTags.CHICKEN_FOOD), "vanilla wheat still not chicken food");
		check(new ItemStack(Items.WHEAT_SEEDS).is(ItemTags.CHICKEN_FOOD), "vanilla wheat seeds still chicken food");

		// real interactions
		BlockPos pos = spawn.above();
		breed(level, player, EntityType.CHICKEN, "minecraft:wheat", pos);
		breed(level, player, EntityType.CHICKEN, "minecraft:beetroot", pos);
		breed(level, player, EntityType.CHICKEN, "minecraft:pumpkin", pos);
		breed(level, player, EntityType.CHICKEN, "minecraft:melon", pos);
		breed(level, player, EntityType.CHICKEN, "minecraft:torchflower", pos);
		breed(level, player, EntityType.SNIFFER, "minecraft:torchflower", pos);
		breed(level, player, EntityType.PIG, "minecraft:carrot", pos);
		breed(level, player, EntityType.PIG, "minecraft:potato", pos);
		breed(level, player, EntityType.RABBIT, "minecraft:carrot", pos);

		Parrot parrot = spawn(level, EntityType.PARROT, pos);
		ItemStack parrotSeeds = seed("minecraft:wheat", 64);
		player.setItemInHand(InteractionHand.MAIN_HAND, parrotSeeds);
		for (int i = 0; i < 64 && !parrot.isTame(); i++) player.interactOn(parrot, InteractionHand.MAIN_HAND);
		check(parrot.isTame(), "parrot tamed with AgriCraft wheat seeds (used " + (64 - parrotSeeds.getCount()) + ")");
		parrot.discard();

		// a wheat seed must not make a cow breed
		Animal cow = spawn(level, EntityType.COW, pos);
		ItemStack cowSeeds = seed("minecraft:wheat", 4);
		player.setItemInHand(InteractionHand.MAIN_HAND, cowSeeds);
		player.interactOn(cow, InteractionHand.MAIN_HAND);
		check(!cow.isInLove() && cowSeeds.getCount() == 4, "cow ignores AgriCraft wheat seed");
		cow.discard();
	}

	@SubscribeEvent
	public static void run(ServerStartedEvent event) {
		if (!Boolean.getBoolean("seedfood.probe")) return;
		MinecraftServer server = event.getServer();
		try {
			verify(server);
		} catch (Throwable t) {
			failures++;
			REPORT.add("FAIL exception " + t);
			t.printStackTrace();
		}
		REPORT.add(failures == 0 ? "RESULT all " + REPORT.size() + " checks passed" : "RESULT " + failures + " FAILURES");
		try {
			Files.write(Path.of("seedfood-results.txt"), REPORT);
		} catch (Exception ignored) {
		}
		REPORT.forEach(line -> System.out.println("[SeedFoodProbe] " + line));
		server.halt(false);
	}
}
