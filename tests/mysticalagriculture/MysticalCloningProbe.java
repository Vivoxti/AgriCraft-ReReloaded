package mysticalcloningprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.genetic.AgriCrossBreedEngine;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.plant.AgriPlant;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Dedicated-server engine regression probe, outside production source sets. */
@Mod("mysticalcloningprobe")
public final class MysticalCloningProbe {
    private static final List<String> REPORT = new ArrayList<>();

    public MysticalCloningProbe() {
        NeoForge.EVENT_BUS.addListener(MysticalCloningProbe::run);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        REPORT.add("PASS " + message);
    }

    private static AgriCrop crop(String id, AgriPlant plant, AgriGenome genome, boolean mature, int[] planted) {
        return (AgriCrop) Proxy.newProxyInstance(AgriCrop.class.getClassLoader(), new Class<?>[]{AgriCrop.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPlantId" -> id;
                    case "getPlant" -> plant;
                    case "getGenome" -> genome;
                    case "getGrowthStage" -> new AgriGrowthStage(mature ? 7 : 3, 8);
                    case "hasPlant" -> true;
                    case "canBeHarvested", "isFullyGrown" -> mature;
                    case "isFertile" -> true;
                    case "plantGenome" -> { planted[0]++; yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void attempt(AgriGenome genome, String id, boolean cloneable, double spread,
                                boolean mature, int parents, boolean expected, String message) {
        var plant = new AgriPlant.Builder().stages16().cloneable(cloneable).chances(1, 0, spread).build();
        int[] planted = {0};
        var parent = crop(id, plant, genome, mature, new int[]{0});
        var target = crop(id, plant, genome, false, planted);
        var random = RandomSource.create(42);
        var engine = new AgriCrossBreedEngine();
        // Exercise production maturity/Fertility selection and clone/combine execution.
        var candidates = engine.selectParents(Stream.generate(() -> parent).limit(parents), random);
        boolean result = engine.breedSelected(target, candidates, random);
        check(result == expected && planted[0] == (expected ? 1 : 0), message);
    }

    private static void run(ServerStartedEvent event) {
        var server = event.getServer();
        boolean cloning = CompatConfig.allowMysticalAgricultureCloning;
        boolean compatibility = CompatConfig.enableMysticalAgriculture;
        double multiplier = CoreConfig.mutationChanceMultiplier;
        boolean cloneMutations = CoreConfig.cloneMutations;
        try {
            check(cloning == Boolean.getBoolean("cloning.expected"), "configured cloning flag loaded correctly");
            var genome = new AgriGenome(AgriApi.getPlant("minecraft:wheat", server.registryAccess()).orElseThrow());
            CompatConfig.enableMysticalAgriculture = true;
            CoreConfig.cloneMutations = false;
            // Guarantee selection independently of the starter genome's Fertility value.
            CoreConfig.mutationChanceMultiplier = 100;
            CompatConfig.allowMysticalAgricultureCloning = false;
            attempt(genome, "mysticalagriculture:iron", false, 1, true, 1, false, "default refuses one-parent MA cloning");
            attempt(genome, "mysticalagriculture:iron", false, 1, true, 2, true, "default retains two-parent breeding");
            attempt(genome, "mysticalagriculture:iron", true, 1, true, 1, true, "default respects datapack-enabled cloning");
            CompatConfig.allowMysticalAgricultureCloning = true;
            attempt(genome, "mysticalagriculture:iron", false, 1, true, 1, true, "enabled allows one-parent MA cloning");
            attempt(genome, "mysticalagriculture:iron", false, 0, true, 1, false, "enabled retains spread chance check");
            attempt(genome, "mysticalagriculture:iron", false, 1, false, 1, false, "enabled rejects immature parent");
            attempt(genome, "minecraft:wheat", false, 1, true, 1, false, "enabled does not override other namespaces");
            attempt(genome, "mysticalagriculture_extra:iron", false, 1, true, 1, false, "namespace prefix cannot match unrelated mod");
            attempt(genome, "minecraft:wheat", true, 1, true, 1, true, "ordinary cloneable crop still spreads");
            CoreConfig.mutationChanceMultiplier = 0;
            attempt(genome, "mysticalagriculture:iron", false, 1, true, 1, false, "enabled retains Fertility selection roll");
            CoreConfig.mutationChanceMultiplier = 100;
            CompatConfig.enableMysticalAgriculture = false;
            attempt(genome, "mysticalagriculture:iron", false, 1, true, 1, false, "disabled compatibility suppresses override");
            CompatConfig.enableMysticalAgriculture = true;
            CompatConfig.allowMysticalAgricultureCloning = false;
            attempt(genome, "mysticalagriculture:iron", false, 1, true, 1, false, "turning option back off restores restriction");
            REPORT.add("RESULT " + REPORT.size() + " checks passed");
        } catch (Throwable failure) {
            REPORT.add("FAIL " + failure);
            failure.printStackTrace();
        } finally {
            CompatConfig.allowMysticalAgricultureCloning = cloning;
            CompatConfig.enableMysticalAgriculture = compatibility;
            CoreConfig.mutationChanceMultiplier = multiplier;
            CoreConfig.cloneMutations = cloneMutations;
            try {
                Files.write(Path.of("mystical-cloning-results.txt"), REPORT);
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            } finally {
                REPORT.forEach(System.out::println);
                server.halt(false);
            }
        }
    }
}
