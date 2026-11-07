package com.agricraft.agricraft.common.network;

import com.agricraft.agricraft.common.item.SeedBagItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: the player scrolled (while sneaking) with a seed bag in the main hand.
 * The server is authoritative for the bag contents, so the sorter is changed there and synced back.
 */
public record SeedBagSorterPayload(int delta) implements CustomPacketPayload {

	public static final Type<SeedBagSorterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("agricraft", "seed_bag_sorter"));
	public static final StreamCodec<ByteBuf, SeedBagSorterPayload> CODEC = ByteBufCodecs.VAR_INT.map(SeedBagSorterPayload::new, SeedBagSorterPayload::delta);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * @return true if a scroll by this player should change the bag sorter instead of the hotbar slot
	 */
	public static boolean shouldCapture(Player player) {
		return player != null && player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof SeedBagItem;
	}

	/**
	 * @param scroll the scroll amount, positive when scrolling up
	 * @return the sorter step to send, scrolling down moves to the next sorter
	 */
	public static int stepFromScroll(double scroll) {
		return scroll > 0 ? -1 : scroll < 0 ? 1 : 0;
	}

	/**
	 * Server-side handling, must run on the server thread.
	 */
	public static void handle(Player player, int delta) {
		ItemStack bag = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!(bag.getItem() instanceof SeedBagItem) || delta == 0) {
			return;
		}
		SeedBagItem.changeSorter(bag, Integer.signum(delta));
		player.displayClientMessage(SeedBagItem.sorterMessage(bag), true);
	}

}
