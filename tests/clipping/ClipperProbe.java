package clippingprobe;

import com.agricraft.agricraft.api.AgriApi;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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

/** Separate test mod. Never included in a production source set. */
@Mod("clippingprobe")
public final class ClipperProbe {
    private static final BlockPos POS = new BlockPos(0, 101, 0);
    private static final List<String> REPORT = new ArrayList<>();

    private static final class TestPlayer extends FakePlayer {
        final List<String> messages = new ArrayList<>();

        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.nameUUIDFromBytes("clippingprobe".getBytes()), "ClipperProbe"));
            setGameMode(GameType.SURVIVAL);
            setPos(0.5, 101, 2.5);
        }

        @Override
        public void sendSystemMessage(Component message) {
            messages.add(message.getContents() instanceof TranslatableContents contents
                    ? contents.getKey() : message.getString());
        }
    }

    public ClipperProbe() {
        NeoForge.EVENT_BUS.addListener(ClipperProbe::run);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        REPORT.add("PASS " + message);
    }

    private static AgriCrop setup(ServerLevel level, String id, int stage) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(4)).forEach(ItemEntity::discard);
        level.setBlock(POS, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(POS.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        level.setBlock(POS, ModBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.SINGLE_STICKS), 3);
        var crop = AgriApi.getCrop(level, POS).orElseThrow();
        crop.plantGenome(new AgriGenome(AgriApi.getPlant(id, level.registryAccess()).orElseThrow()));
        crop.setGrowthStage(new AgriGrowthStage(stage, 8));
        return crop;
    }

    private static int drops(ServerLevel level) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(4)).stream()
                .mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static net.minecraft.world.InteractionResult clip(TestPlayer player, ServerLevel level) {
        player.messages.clear();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CLIPPER.get()));
        return player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false));
    }

    private static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var level = server.overworld();
            level.getChunk(POS);
            var player = new TestPlayer(level);
            for (String id : List.of("minecraft:wheat", "agricraft:ferranium", "mysticalagriculture:inferium",
                    "mysticalagriculture:iron", "mysticalagriculture:diamond")) {
                for (int stage : new int[]{3, 7}) {
                    var crop = setup(level, id, stage);
                    String genome = crop.getGenome().toString();
                    check(!clip(player, level).consumesAction(), id + " stage " + stage + " rejects clipping");
                    check(crop.getGrowthStage().index() == stage && crop.getGenome().toString().equals(genome)
                            && drops(level) == 0, id + " stage " + stage + " retains growth/genome and spawns no items");
                    String key = stage == 7 ? "agricraft.message.clipping_impossible" : "agricraft.message.clipping_immature";
                    check(player.messages.equals(List.of(key)), id + " stage " + stage + " message = " + key);
                }
            }
            var immatureFlower = setup(level, "minecraft:allium", 3);
            check(!clip(player, level).consumesAction() && immatureFlower.getGrowthStage().index() == 3
                    && player.messages.equals(List.of("agricraft.message.clipping_immature")), "immature flower preserves growth and asks player to wait");
            int yielded = 0, empty = 0;
            for (int trial = 0; trial < 64; trial++) {
                var crop = setup(level, "minecraft:allium", 7);
                check(clip(player, level).consumesAction() && crop.getGrowthStage().index() == 0
                        && player.messages.isEmpty(), "mature flower retains normal clipping/reset on trial " + trial);
                if (drops(level) > 0) yielded++; else empty++;
            }
            check(yielded > 0 && empty > 0, "flower chance rolls retained: yielded=" + yielded + ", empty=" + empty);
            REPORT.add("RESULT " + REPORT.size() + " checks passed");
        } catch (Throwable failure) {
            REPORT.add("FAIL " + failure);
            failure.printStackTrace();
        } finally {
            try {
                Files.write(Path.of("clipping-results.txt"), REPORT);
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            } finally {
                REPORT.forEach(System.out::println);
                server.halt(false);
            }
        }
    }
}
