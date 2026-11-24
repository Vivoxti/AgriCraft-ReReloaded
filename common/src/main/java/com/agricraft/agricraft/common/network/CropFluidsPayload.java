package com.agricraft.agricraft.common.network;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.common.block.CropFluids;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Server to client, sent when a player joins: the server's {@code fluidloggable_fluids} list ({@link CropFluids#names()}).
 * <p>
 * The list adds block states to the crop block, and block states are sent by numeric id, so a client with a different
 * list would read crops (and every block registered after them) wrongly. Such a client disconnects with an explanation.
 */
public record CropFluidsPayload(List<String> names) implements CustomPacketPayload {

	public static final Type<CropFluidsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("agricraft", "crop_fluids"));
	public static final StreamCodec<ByteBuf, CropFluidsPayload> CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
			.map(CropFluidsPayload::new, CropFluidsPayload::names);

	public static CropFluidsPayload fromConfig() {
		return new CropFluidsPayload(CropFluids.names());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * Client-side handling, must run on the client thread.
	 *
	 * @param disconnect closes the connection with the given reason
	 */
	public static void handleOnClient(List<String> serverNames, Consumer<Component> disconnect) {
		List<String> clientNames = CropFluids.names();
		if (clientNames.equals(serverNames)) {
			return;
		}
		AgriCraft.LOGGER.error("fluidloggable_fluids mismatch, server: {}, client: {}", serverNames, clientNames);
		disconnect.accept(Component.translatable("agricraft.disconnect.fluidloggable_fluids_mismatch", serverNames.toString(), clientNames.toString()));
	}

}
