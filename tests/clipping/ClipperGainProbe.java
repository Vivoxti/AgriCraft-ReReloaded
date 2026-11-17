package clipgainprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropState;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.common.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Separate test mod for issue #8: measures how often the Plant Clipper yields a poppy at Gain 1/4/7/10 with
 * core.clipping_uses_gain on, and at Gain 10 with it off. Never included in a production source set.
 */
@Mod("clipgainprobe")
public final class ClipperGainProbe {
    private static final BlockPos POS = new BlockPos(0, 101, 0);
    private static final int TRIALS = 400;
    private static final List<String> REPORT = new ArrayList<>();

    public ClipperGainProbe() {
        NeoForge.EVENT_BUS.addListener(ClipperGainProbe::run);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        REPORT.add("PASS " + message);
    }

    private static AgriGenome genome(int gain) throws Exception {
        StringBuilder genes = new StringBuilder("species:{dom:\"minecraft:poppy\",rec:\"minecraft:poppy\"}");
        for (String stat : List.of("gain", "growth", "strength", "resistance", "fertility", "mutativity")) {
            int value = stat.equals("gain") ? gain : 1;
            genes.append(',').append(stat).append(":{dom:").append(value).append(",rec:").append(value).append('}');
        }
        return AgriGenome.fromNBT(TagParser.parseTag("{genes:{" + genes + "}}"));
    }

    private static int drops(ServerLevel level) {
        var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(4));
        int count = items.stream().mapToInt(item -> item.getItem().getCount()).sum();
        items.forEach(ItemEntity::discard);
        return count;
    }

    /** Returns the share of clips that yielded at least one item. */
    private static double rate(ServerLevel level, FakePlayer player, int gain, boolean useGain) throws Exception {
        CoreConfig.clippingUsesGain = useGain;
        int yielded = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            drops(level);
            level.setBlock(POS, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(POS.below(), Blocks.FARMLAND.defaultBlockState(), 3);
            level.setBlock(POS, ModBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.SINGLE_STICKS), 3);
            AgriCrop crop = AgriApi.getCrop(level, POS).orElseThrow();
            crop.plantGenome(genome(gain));
            crop.setGrowthStage(new AgriGrowthStage(7, 8));
            if (crop.getGenome().getGain() != gain) throw new AssertionError("genome gain " + crop.getGenome().getGain() + " != " + gain);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CLIPPER.get()));
            var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false));
            if (!result.consumesAction() || crop.getGrowthStage().index() != 0) {
                throw new AssertionError("clip at gain " + gain + " did not succeed/reset: " + result + ", stage " + crop.getGrowthStage().index());
            }
            if (drops(level) > 0) yielded++;
        }
        double rate = yielded / (double) TRIALS;
        REPORT.add(String.format("RATE gain=%d clipping_uses_gain=%s yielded=%d/%d (%.1f%%)", gain, useGain, yielded, TRIALS, rate * 100));
        return rate;
    }

    private static void run(ServerStartedEvent event) {
        var server = event.getServer();
        boolean original = CoreConfig.clippingUsesGain;
        try {
            var level = server.overworld();
            level.getChunk(POS);
            var player = new FakePlayer(level, new GameProfile(UUID.nameUUIDFromBytes("clipgainprobe".getBytes()), "ClipGainProbe"));
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(0.5, 101, 2.5);
            check(original, "core.clipping_uses_gain defaults to true");
            // 400 trials: binomial sd <= 2.5%, so +-8% is > 3 sd.
            double g1 = rate(level, player, 1, true), g4 = rate(level, player, 4, true), g7 = rate(level, player, 7, true);
            double g10 = rate(level, player, 10, true), off10 = rate(level, player, 10, false);
            check(Math.abs(g1 - 0.25) < 0.08, "gain 1 keeps the plant's own ~25% chance");
            check(Math.abs(g4 - 0.50) < 0.08, "gain 4 yields ~50%");
            check(Math.abs(g7 - 0.75) < 0.08, "gain 7 yields ~75%");
            check(g10 == 1.0, "gain 10 always yields a flower");
            check(Math.abs(off10 - 0.25) < 0.08, "option off: gain 10 falls back to the original ~25%");
            REPORT.add("RESULT all checks passed");
        } catch (Throwable failure) {
            REPORT.add("FAIL " + failure);
            failure.printStackTrace();
        } finally {
            CoreConfig.clippingUsesGain = original;
            try {
                Files.write(Path.of("clip-gain-results.txt"), REPORT);
            } catch (Exception failure) {
                failure.printStackTrace();
            }
            REPORT.forEach(System.out::println);
            server.halt(false);
        }
    }
}
