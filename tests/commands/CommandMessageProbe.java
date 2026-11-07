package cmdprobe;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** TEMPORARY dev-only probe: /agricraft_seed replies must be encodable as system_chat packets. Not shipped. */
@EventBusSubscriber(modid = "agricraft")
public final class CommandMessageProbe {
	private static final List<String> REPORT = new ArrayList<>();
	private static int failures = 0;

	private static void check(boolean condition, String message) {
		REPORT.add((condition ? "PASS " : "FAIL ") + message);
		if (!condition) failures++;
	}

	private static String encode(MinecraftServer server, java.util.function.Supplier<Component> message) {
		try {
			Component component = message.get();
			var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
			ClientboundSystemChatPacket.STREAM_CODEC.encode(buf, new ClientboundSystemChatPacket(component, false));
			return null;
		} catch (Throwable t) {
			return t.toString();
		}
	}

	private static void verify(MinecraftServer server) {
		// control: the old message shape must fail, otherwise this probe proves nothing
		String control = encode(server, () -> Component.translatable("agricraft.command.seed_default", ResourceLocation.parse("minecraft:wheat")));
		check(control != null, "control: ResourceLocation argument fails to encode (" + control + ")");

		FakePlayer player = new FakePlayer(server.overworld(), new GameProfile(UUID.fromString("7d2e9c11-3f4a-4b8e-9a6c-5e1d2f3a4b6c"), "CmdProbe"));
		List<Component> replies = new ArrayList<>();
		CommandSource capture = new CommandSource() {
			@Override public void sendSystemMessage(Component component) { replies.add(component); }
			@Override public boolean acceptsSuccess() { return true; }
			@Override public boolean acceptsFailure() { return true; }
			@Override public boolean shouldInformAdmins() { return false; }
		};
		var source = player.createCommandSourceStack().withSource(capture).withPermission(4);
		String[] commands = {
				"agricraft_seed minecraft:wheat",
				"agricraft_seed minecraft:wheat all 7",
				"agricraft_seed minecraft:wheat distinct \"10-3-3-3-3-3\"",
		};
		for (String command : commands) {
			replies.clear();
			int before = player.getInventory().countItem(com.agricraft.agricraft.common.registry.ModItems.SEED.get());
			server.getCommands().performPrefixedCommand(source, command);
			int after = player.getInventory().countItem(com.agricraft.agricraft.common.registry.ModItems.SEED.get());
			check(after == before + 1, "/" + command + " gives one seed");
			check(replies.size() == 1, "/" + command + " sends one reply (" + replies.size() + ")");
			for (Component reply : replies) {
				String error = encode(server, () -> reply);
				check(error == null, "/" + command + " reply encodes as system_chat" + (error == null ? "" : ": " + error));
			}
		}
	}

	@SubscribeEvent
	public static void run(ServerStartedEvent event) {
		if (!Boolean.getBoolean("cmd.probe")) return;
		MinecraftServer server = event.getServer();
		try {
			verify(server);
		} catch (Throwable t) {
			failures++;
			REPORT.add("FAIL exception " + t);
			t.printStackTrace();
		}
		REPORT.add(failures == 0 ? "RESULT all " + REPORT.size() + " checks passed" : "RESULT " + failures + " FAILURES");
		REPORT.forEach(line -> System.out.println("[CmdProbe] " + line));
		server.halt(false);
	}
}
