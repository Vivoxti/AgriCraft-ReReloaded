package seedbagprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.agricraft.agricraft.common.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** TEMPORARY dev-only probe for "seed bag deletes seeds". Not shipped. */
@EventBusSubscriber(modid = "agricraft")
public final class SeedBagProbe {
	private static final List<String> REPORT = new ArrayList<>();
	private static int failures = 0;
	private static final int SLOT = 9; // first main-inventory slot of the player inventory menu

	private static void check(boolean condition, String message) {
		REPORT.add((condition ? "PASS " : "FAIL ") + message);
		if (!condition) failures++;
	}

	private static ItemStack seed(String plant, int count) {
		ItemStack stack = AgriSeedItem.toStack(AgriApi.getPlant(ResourceLocation.parse(plant)).orElseThrow());
		stack.setCount(count);
		return stack;
	}

	private static void rightClick(FakePlayer player) {
		player.inventoryMenu.clicked(SLOT, 1, ClickType.PICKUP, player);
	}

	private static void verify(MinecraftServer server) {
		ServerLevel level = server.overworld();
		FakePlayer player = new FakePlayer(level, new GameProfile(UUID.fromString("0b9f3f8e-6b7d-4c55-a1d8-1d2b9f4e2c10"), "SeedBagProbe"));
		player.setGameMode(GameType.SURVIVAL);
		InventoryMenu menu = player.inventoryMenu;
		var slot = menu.getSlot(SLOT);

		// A: bag on cursor, right-click a slot holding 10 wheat seeds
		ItemStack bag = new ItemStack(ModItems.SEED_BAG.get());
		menu.setCarried(bag);
		slot.set(seed("minecraft:wheat", 10));
		rightClick(player);
		bag = menu.getCarried();
		check(slot.getItem().isEmpty(), "A slot seeds taken");
		check(SeedBagItem.size(bag) == 10, "A bag holds 10 (was " + SeedBagItem.size(bag) + ")");
		check(!SeedBagItem.isEmpty(bag), "A bag not empty");

		// B: bag in slot, right-click it with 5 wheat seeds on cursor
		slot.set(bag);
		menu.setCarried(seed("minecraft:wheat", 5));
		rightClick(player);
		bag = slot.getItem();
		check(menu.getCarried().isEmpty(), "B cursor seeds inserted");
		check(SeedBagItem.size(bag) == 15, "B bag holds 15 (was " + SeedBagItem.size(bag) + ")");

		// C: other species rejected and kept
		menu.setCarried(seed("minecraft:carrot", 3));
		rightClick(player);
		check(menu.getCarried().getCount() == 3, "C carrot seeds stay on cursor");
		check(SeedBagItem.size(bag) == 15, "C bag unchanged");

		// D: same genome merged into a single entry
		var data = bag.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
		check(data.getList("seeds", 10).size() == 1, "D identical seeds merged into one entry");

		// E: save / load round trip keeps contents
		var saved = bag.save(server.registryAccess());
		ItemStack loaded = ItemStack.parse(server.registryAccess(), saved).orElseThrow();
		check(SeedBagItem.size(loaded) == 15, "E contents survive save/load");

		// F: capacity limit leaves leftovers on the cursor
		menu.setCarried(seed("minecraft:wheat", 64));
		rightClick(player);
		check(SeedBagItem.size(bag) == CoreConfig.seedBagCapacity, "F bag filled to capacity " + CoreConfig.seedBagCapacity);
		check(menu.getCarried().getCount() == 64 - (CoreConfig.seedBagCapacity - 15), "F leftovers kept (" + menu.getCarried().getCount() + ")");
		menu.setCarried(ItemStack.EMPTY);

		// G: planting from the bag uses one seed
		BlockPos soil = new BlockPos(0, 120, 0);
		level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 3);
		level.setBlock(soil.above(), Blocks.AIR.defaultBlockState(), 3);
		int before = SeedBagItem.size(bag);
		player.setItemInHand(InteractionHand.MAIN_HAND, bag);
		var hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
		player.gameMode.useItemOn(player, level, bag, InteractionHand.MAIN_HAND, hit);
		check(AgriApi.getCrop(level, soil.above()).map(c -> c.hasPlant()).orElse(false), "G crop planted from bag");
		check(SeedBagItem.size(bag) == before - 1, "G one seed used (" + before + " -> " + SeedBagItem.size(bag) + ")");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

		// H: empty cursor right-click on the bag pulls out one stack, bag ends empty after enough pulls
		slot.set(bag);
		int total = SeedBagItem.size(bag);
		int pulled = 0;
		for (int i = 0; i < 5 && !SeedBagItem.isEmpty(slot.getItem()); i++) {
			menu.setCarried(ItemStack.EMPTY);
			rightClick(player);
			check(menu.getCarried().getItem() == ModItems.SEED.get(), "H pull " + i + " gives seeds");
			pulled += menu.getCarried().getCount();
		}
		bag = slot.getItem();
		check(pulled == total, "H pulled all " + total + " seeds (" + pulled + ")");
		check(SeedBagItem.isEmpty(bag) && SeedBagItem.size(bag) == 0, "H bag empty after extraction");
		check(ItemStack.isSameItemSameComponents(bag, new ItemStack(ModItems.SEED_BAG.get())), "H empty bag stacks like a fresh bag");

		// I: empty bag accepts another species afterwards
		menu.setCarried(seed("minecraft:carrot", 3));
		rightClick(player);
		check(menu.getCarried().isEmpty() && SeedBagItem.size(slot.getItem()) == 3, "I empty bag accepts a new species");
	}

	@SubscribeEvent
	public static void run(ServerStartedEvent event) {
		if (!Boolean.getBoolean("seedbag.probe")) return;
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
			Files.write(Path.of("seedbag-results.txt"), REPORT);
		} catch (Exception ignored) {
		}
		REPORT.forEach(line -> System.out.println("[SeedBagProbe] " + line));
		server.halt(false);
	}
}
