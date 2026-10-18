package seedcompostprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Standalone dedicated-server regression probe; never included in release jars. */
@Mod("seedcompostprobe")
public final class SeedCompostProbe {
    private static final List<String> REPORT = new ArrayList<>();
    private static final boolean BASELINE = Boolean.getBoolean("compost.baseline");
    private static final float EXPECTED = Float.parseFloat(System.getProperty("compost.value", "0.3"));

    public SeedCompostProbe() {
        NeoForge.EVENT_BUS.addListener(SeedCompostProbe::run);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        REPORT.add("PASS " + message);
    }

    private static void verify(MinecraftServer server, String phase) {
        check(CoreConfig.seedCompostValue == EXPECTED, phase + " loaded config = " + EXPECTED);
        var level = server.overworld();
        var pos = new BlockPos(0, 100, 0);
        var player = new FakePlayer(level, new GameProfile(UUID.fromString("2d3b138c-f058-4790-8dac-b0a7cfb07c41"), "CompostProbe"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(0.5, 100, 2);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var empty = Blocks.COMPOSTER.defaultBlockState();
        var seed = ModItems.SEED.get();
        if (BASELINE) {
            check(ComposterBlock.COMPOSTABLES.getFloat(seed) == EXPECTED, phase + " legacy map contains configured chance");
            check(ComposterBlock.getValue(new ItemStack(seed)) < 0, phase + " NeoForge ignores legacy entry (bug reproduced)");
        } else {
            check(ComposterBlock.getValue(new ItemStack(seed)) == EXPECTED, phase + " NeoForge data map contains configured chance");
        }
        check(ComposterBlock.getValue(new ItemStack(Items.WHEAT_SEEDS)) == 0.3F, phase + " vanilla wheat chance retained");
        boolean enabled = !BASELINE && EXPECTED > 0;
        for (boolean identified : new boolean[]{false, true}) {
            ItemStack stack = identified
                    ? AgriSeedItem.toStack(AgriApi.getPlant(ResourceLocation.parse("minecraft:wheat")).orElseThrow())
                    : new ItemStack(seed);
            stack.setCount(4);
            level.setBlock(pos, empty, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
            String name = phase + (identified ? " identified" : " unidentified");
            check(result.consumesAction() == enabled, name + " right-click result matches enabled state");
            check(stack.getCount() == (enabled ? 3 : 4), name + " correct seed consumption");
            check(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == (enabled ? 1 : 0), name + " empty composter fill");
        }

        level.setBlock(pos, empty, 3);
        var input = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        check(input != null, phase + " top hopper capability exists");
        var stack = new ItemStack(seed, 2);
        var simulated = input.insertItem(0, stack, true);
        check(simulated.getCount() == (enabled ? 1 : 2), phase + " hopper simulation acceptance");
        check(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == 0, phase + " simulation leaves composter unchanged");
        var remaining = input.insertItem(0, stack, false);
        check(remaining.getCount() == (enabled ? 1 : 2), phase + " hopper insertion consumption");
        check(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == (enabled ? 1 : 0), phase + " hopper insertion fill");

        level.setBlock(pos, empty.setValue(ComposterBlock.LEVEL, 1), 3);
        stack = new ItemStack(seed, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
        check(stack.getCount() == (enabled ? 1 : 2), phase + " partially filled composter consumption");
        if (!BASELINE && EXPECTED == 1) {
            check(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == 2, phase + " chance 1 guarantees fill");
        }
        level.setBlock(pos, empty.setValue(ComposterBlock.LEVEL, 7), 3);
        stack = new ItemStack(seed, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
        check(stack.getCount() == 2, phase + " ready-to-process composter does not consume seeds");
    }

    private static void finish(MinecraftServer server, Throwable failure) {
        if (failure != null) {
            REPORT.add("FAIL " + failure);
            failure.printStackTrace();
        } else {
            REPORT.add("RESULT " + REPORT.size() + " checks passed");
        }
        try {
            Files.write(Path.of("compost-results.txt"), REPORT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            REPORT.forEach(System.out::println);
            server.halt(false);
        }
    }

    private static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            verify(server, "startup");
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((unused, failure) ->
                    server.execute(() -> {
                        try {
                            if (failure != null) throw new RuntimeException(failure);
                            verify(server, "reload");
                            finish(server, null);
                        } catch (Throwable e) {
                            finish(server, e);
                        }
                    }));
        } catch (Throwable e) {
            finish(server, e);
        }
    }
}
