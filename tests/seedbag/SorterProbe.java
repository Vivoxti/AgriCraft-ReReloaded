package seedbagprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.agricraft.agricraft.common.network.SeedBagSorterPayload;
import com.agricraft.agricraft.common.registry.ModItems;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** TEMPORARY dev-only probe for seed bag sorter switching. Not shipped. */
@EventBusSubscriber(modid = "agricraft")
public final class SorterProbe {
	private static final List<String> REPORT = new ArrayList<>();
	private static int failures = 0;

	private static void check(boolean condition, String message) {
		REPORT.add((condition ? "PASS " : "FAIL ") + message);
		if (!condition) failures++;
	}

	private static void verify(MinecraftServer server) {
		FakePlayer player = new FakePlayer(server.overworld(), new GameProfile(UUID.fromString("3a1c2b4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d"), "SorterProbe"));
		int n = SeedBagItem.SORTERS.size();
		check(n >= 7, "sorters registered: " + n);

		// codec round trip
		var buf = Unpooled.buffer();
		SeedBagSorterPayload.CODEC.encode(buf, new SeedBagSorterPayload(-1));
		check(SeedBagSorterPayload.CODEC.decode(buf).delta() == -1, "payload codec round trip");
		check(SeedBagSorterPayload.stepFromScroll(1.0) == -1 && SeedBagSorterPayload.stepFromScroll(-0.3) == 1 && SeedBagSorterPayload.stepFromScroll(0) == 0, "scroll direction mapping");

		// bag with mixed-stat seeds
		ItemStack bag = new ItemStack(ModItems.SEED_BAG.get());
		ItemStack seeds = AgriSeedItem.toStack(AgriApi.getPlant(ResourceLocation.parse("minecraft:wheat")).orElseThrow());
		seeds.setCount(20);
		SeedBagItem.add(bag, seeds);
		player.setItemInHand(InteractionHand.MAIN_HAND, bag);
		ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
		check(SeedBagItem.getSorter(held) == SeedBagItem.DEFAULT_SORTER, "starts on default sorter");

		List<String> seen = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			SeedBagSorterPayload.handle(player, 1);
			seen.add(SeedBagItem.getSorter(held).getId().toString());
		}
		check(seen.stream().distinct().count() == n, "scrolling forward visits all " + n + " sorters: " + seen);
		check(SeedBagItem.getSorter(held) == SeedBagItem.DEFAULT_SORTER, "wraps back to default after " + n + " steps");
		SeedBagSorterPayload.handle(player, -1);
		check(SeedBagItem.getSorter(held) == SeedBagItem.SORTERS.get(n - 1), "scrolling back from default wraps to last");
		check(SeedBagItem.size(held) == 20, "contents kept while switching (" + SeedBagItem.size(held) + ")");

		// empty bag can switch too, and sorter survives taking seeds out
		ItemStack empty = new ItemStack(ModItems.SEED_BAG.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, empty);
		SeedBagSorterPayload.handle(player, 1);
		check(SeedBagItem.getSorter(player.getMainHandItem()) == SeedBagItem.SORTERS.get(1), "empty bag switches sorter");
		check(SeedBagItem.isEmpty(player.getMainHandItem()), "empty bag still empty after switch");

		// not a bag: nothing happens
		ItemStack wheat = seeds.copy();
		player.setItemInHand(InteractionHand.MAIN_HAND, wheat);
		SeedBagSorterPayload.handle(player, 1);
		check(ItemStack.isSameItemSameComponents(player.getMainHandItem(), seeds), "non-bag item untouched");

		check(SeedBagItem.sorterMessage(held).getString().contains("agricraft.tooltip.bag") || !SeedBagItem.sorterMessage(held).getString().isEmpty(), "sorter message builds");
	}

	@SubscribeEvent
	public static void run(ServerStartedEvent event) {
		if (!Boolean.getBoolean("sorter.probe")) return;
		MinecraftServer server = event.getServer();
		try {
			verify(server);
		} catch (Throwable t) {
			failures++;
			REPORT.add("FAIL exception " + t);
			t.printStackTrace();
		}
		REPORT.add(failures == 0 ? "RESULT all " + REPORT.size() + " checks passed" : "RESULT " + failures + " FAILURES");
		REPORT.forEach(line -> System.out.println("[SorterProbe] " + line));
		server.halt(false);
	}
}
