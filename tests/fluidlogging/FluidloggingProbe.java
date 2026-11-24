package fluidprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.requirement.AgriGrowthConditionRegistry;
import com.agricraft.agricraft.api.requirement.AgriGrowthResponse;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropFluids;
import com.agricraft.agricraft.common.block.CropState;
import com.agricraft.agricraft.common.block.SimpleFluidloggedBlock;
import com.agricraft.agricraft.common.network.CropFluidsPayload;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Separate test mod for configurable crop fluidlogging (core.fluidloggable_fluids). Registers its own fluid,
 * fluidprobe:quicksilver (with bucket and liquid block), like any other mod would, and a datapack plant
 * fluidprobe:silverbloom that requires it. Never included in a production source set.
 * <p>
 * Stages, picked from the config and earlier result files:
 * main   - config lists fluidprobe:quicksilver: state property, buckets, growth condition, drops, payload check;
 *          leaves a submerged crop at PERSIST and saves the world
 * reload - same config, next launch: the crop at PERSIST is still submerged
 * empty  - config list emptied: no extra property, the crop at PERSIST loads as a dry crop
 */
@Mod("fluidprobe")
public final class FluidloggingProbe {
	private static final BlockPos POS = new BlockPos(0, 101, 0);
	private static final BlockPos PERSIST = new BlockPos(4, 101, 0);
	private static final List<String> REPORT = new ArrayList<>();

	static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "fluidprobe");
	static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "fluidprobe");
	static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fluidprobe");
	static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("fluidprobe");

	static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("quicksilver",
			() -> new FluidType(FluidType.Properties.create().density(13500).viscosity(1500).descriptionId("block.fluidprobe.quicksilver")));
	static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE = FLUIDS.register("quicksilver", () -> new BaseFlowingFluid.Source(props()));
	static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING = FLUIDS.register("flowing_quicksilver", () -> new BaseFlowingFluid.Flowing(props()));
	static final DeferredBlock<LiquidBlock> BLOCK = BLOCKS.register("quicksilver",
			() -> new LiquidBlock(SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.METAL)));
	static final DeferredItem<BucketItem> BUCKET = ITEMS.register("quicksilver_bucket",
			() -> new BucketItem(SOURCE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

	private static BaseFlowingFluid.Properties props() {
		return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).bucket(BUCKET).block(BLOCK);
	}

	public FluidloggingProbe(IEventBus bus) {
		TYPES.register(bus);
		FLUIDS.register(bus);
		BLOCKS.register(bus);
		ITEMS.register(bus);
		if (FMLEnvironment.dist.isClient()) {
			bus.addListener(FluidloggingProbe::clientExtensions);
		}
		NeoForge.EVENT_BUS.addListener(FluidloggingProbe::run);
	}

	private static void clientExtensions(RegisterClientExtensionsEvent event) {
		event.registerFluidType(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return ResourceLocation.withDefaultNamespace("block/water_still");
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return ResourceLocation.withDefaultNamespace("block/water_flow");
			}

			@Override
			public int getTintColor() {
				return 0xFFC4C8D2;
			}
		}, TYPE.get());
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
		REPORT.add("PASS " + message);
	}

	private static AgriGenome genome(String species) throws Exception {
		StringBuilder genes = new StringBuilder("species:{dom:\"" + species + "\",rec:\"" + species + "\"}");
		for (String stat : List.of("gain", "growth", "strength", "resistance", "fertility", "mutativity")) {
			genes.append(',').append(stat).append(":{dom:1,rec:1}");
		}
		return AgriGenome.fromNBT(TagParser.parseTag("{genes:{" + genes + "}}"));
	}

	private static BlockState sticks() {
		return ModBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.SINGLE_STICKS);
	}

	private static void reset(ServerLevel level) {
		for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-2, 0, -2), POS.offset(2, 2, 2))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-2, -1, -2), POS.offset(2, -1, 2))) {
			level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
		}
	}

	private static AgriGrowthResponse fluidResponse(ServerLevel level, BlockPos pos) {
		AgriCrop crop = AgriApi.getCrop(level, pos).orElseThrow();
		return AgriGrowthConditionRegistry.getFluid().check(crop, level, pos, 1);
	}

	private static BlockHitResult hit(BlockPos pos) {
		return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
	}

	private static void main(ServerLevel level, FakePlayer player) throws Exception {
		Block crop = ModBlocks.CROP.get();
		Property<?> property = crop.getStateDefinition().getProperty(CropFluids.PROPERTY_NAME);
		check(property != null, "crop block has the agricraft_fluid property");
		check(List.copyOf(property.getPossibleValues()).equals(List.of("none", "fluidprobe_quicksilver")),
				"agricraft_fluid values are [none, fluidprobe_quicksilver]: " + property.getPossibleValues());
		check(crop.defaultBlockState().getFluidState().isEmpty(), "default crop state holds no fluid");
		check(crop.getStateDefinition().getPossibleStates().size() == 3 * 4 * 17 * 4 * 2,
				"crop has 3 sticks x 4 crop states x 17 light x 4 water/lava x (none + quicksilver) states: " + crop.getStateDefinition().getPossibleStates().size());

		// the cached fluid state of the block state (built after all registries) points at the other mod's fluid
		BlockState cached = CropFluids.withFluid(crop.defaultBlockState(), SOURCE.get());
		check(cached != null && cached.getFluidState().getType() == SOURCE.get() && cached.getFluidState().isSource(),
				"cached BlockState#getFluidState is the quicksilver source");
		check(CropFluids.withFluid(crop.defaultBlockState(), FLOWING.get()) == null, "flowing quicksilver can't fill the sticks");

		// bucket into crop sticks (the NeoForge BucketItem path through canBlockContainFluid)
		reset(level);
		level.setBlock(POS, sticks(), 3);
		boolean poured = BUCKET.get().emptyContents(player, level, POS, hit(POS), new ItemStack(BUCKET.get()));
		BlockState state = level.getBlockState(POS);
		check(poured && state.is(crop) && level.getFluidState(POS).getType() == SOURCE.get(),
				"quicksilver bucket submerges crop sticks: " + state);
		check(!state.getValue(SimpleFluidloggedBlock.WATERLOGGED) && !state.getValue(SimpleFluidloggedBlock.LAVALOGGED),
				"water/lava flags stay off");

		// a second fluid can't be poured into the same sticks: the bucket pours next to them (the hit face is UP)
		check(!((SimpleFluidloggedBlock) crop).canPlaceLiquid(player, level, POS, state, Fluids.WATER), "submerged sticks don't accept another fluid");
		boolean water = ((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, POS, hit(POS), new ItemStack(Items.WATER_BUCKET));
		check(level.getFluidState(POS).getType() == SOURCE.get(), "water bucket does not replace the quicksilver");
		check(water && level.getFluidState(POS.above()).getType() == Fluids.WATER,
				"the water is poured above the sticks, not lost (poured=" + water + ", above=" + level.getBlockState(POS.above()) + ")");
		level.setBlock(POS.above(), Blocks.AIR.defaultBlockState(), 3);
		boolean again = BUCKET.get().emptyContents(player, level, POS, hit(POS), new ItemStack(BUCKET.get()));
		check(again && level.getFluidState(POS.above()).getType() == SOURCE.get() && level.getBlockState(POS).is(crop),
				"a second quicksilver bucket is poured above the sticks, not lost (above=" + level.getBlockState(POS.above()) + ")");
		reset(level);

		// water and lava still go into dry sticks
		level.setBlock(POS, sticks(), 3);
		((BucketItem) Items.LAVA_BUCKET).emptyContents(player, level, POS, hit(POS), new ItemStack(Items.LAVA_BUCKET));
		check(level.getBlockState(POS).is(crop) && level.getBlockState(POS).getValue(SimpleFluidloggedBlock.LAVALOGGED),
				"lava bucket still fills dry sticks: " + level.getBlockState(POS));
		reset(level);
		level.setBlock(POS, sticks(), 3);
		((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, POS, hit(POS), new ItemStack(Items.WATER_BUCKET));
		check(level.getBlockState(POS).is(crop) && level.getBlockState(POS).getValue(SimpleFluidloggedBlock.WATERLOGGED),
				"water bucket still fills dry sticks: " + level.getBlockState(POS));
		reset(level);

		if (net.neoforged.fml.ModList.get().isLoaded("create")) {
			createPipe(level, crop);
		} else {
			REPORT.add("SKIP Create is not installed: open-ended pipe checks");
		}

		// placing crop sticks inside a quicksilver source keeps the fluid
		level.setBlock(POS, BLOCK.get().defaultBlockState(), 3);
		BlockState placed = crop.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player,
				net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.agricraft.agricraft.common.registry.ModItems.WOODEN_CROP_STICKS.get()), hit(POS)));
		check(placed != null && placed.getFluidState().getType() == SOURCE.get(), "crop sticks placed in quicksilver are submerged");
		reset(level);

		// growth condition: silverbloom needs quicksilver, poppy needs no fluid, kelp needs water
		level.setBlock(POS, sticks(), 3);
		AgriApi.getCrop(level, POS).orElseThrow().plantGenome(genome("fluidprobe:silverbloom"));
		check(AgriApi.getCrop(level, POS).orElseThrow().hasPlant(), "silverbloom (datapack plant) was planted");
		check(!fluidResponse(level, POS).isFertile(), "silverbloom out of quicksilver is not fertile: " + fluidResponse(level, POS));
		level.setBlock(POS, CropFluids.withFluid(level.getBlockState(POS), SOURCE.get()), 3);
		check(fluidResponse(level, POS) == AgriGrowthResponse.FERTILE, "silverbloom in quicksilver is fertile");
		check(AgriApi.getCrop(level, POS).orElseThrow().isFertile(), "silverbloom in quicksilver passes all growth conditions");
		AgriApi.getCrop(level, POS).orElseThrow().removeGenome();
		AgriApi.getCrop(level, POS).orElseThrow().plantGenome(genome("minecraft:poppy"));
		check(fluidResponse(level, POS) == AgriGrowthResponse.LETHAL, "poppy (no fluid requirement) in quicksilver is lethal");
		reset(level);
		level.setBlock(POS, CropFluids.withFluid(sticks(), Fluids.LAVA), 3);
		AgriApi.getCrop(level, POS).orElseThrow().plantGenome(genome("fluidprobe:silverbloom"));
		check(fluidResponse(level, POS) == AgriGrowthResponse.KILL_IT_WITH_FIRE, "silverbloom in lava burns (was FERTILE before the fix)");
		AgriApi.getCrop(level, POS).orElseThrow().removeGenome();
		AgriApi.getCrop(level, POS).orElseThrow().plantGenome(genome("minecraft:kelp"));
		check(fluidResponse(level, POS) == AgriGrowthResponse.KILL_IT_WITH_FIRE, "kelp in lava burns");
		reset(level);

		// removing a plant that has no sticks leaves the quicksilver behind
		level.setBlock(POS, CropFluids.withFluid(crop.defaultBlockState().setValue(CropBlock.CROP_STATE, CropState.PLANT), SOURCE.get()), 3);
		AgriApi.getCrop(level, POS).orElseThrow().plantGenome(genome("fluidprobe:silverbloom"));
		AgriApi.getCrop(level, POS).orElseThrow().removeGenome();
		check(level.getBlockState(POS).is(BLOCK.get()), "removing the plant leaves a quicksilver block: " + level.getBlockState(POS));
		reset(level);

		// bucket pickup
		level.setBlock(POS, CropFluids.withFluid(sticks(), SOURCE.get()), 3);
		ItemStack picked = ((SimpleFluidloggedBlock) crop).pickupBlock(player, level, POS, level.getBlockState(POS));
		check(picked.is(BUCKET.get()), "bucket pickup returns a quicksilver bucket: " + picked);
		check(level.getBlockState(POS).is(crop) && level.getFluidState(POS).isEmpty(), "crop sticks are dry after pickup");
		reset(level);

		// crop sticks that can't survive turn into the fluid they held
		level.setBlock(POS, CropFluids.withFluid(sticks(), SOURCE.get()), 3);
		level.setBlock(POS.below(), Blocks.AIR.defaultBlockState(), 3);
		check(level.getBlockState(POS).is(BLOCK.get()), "crop that lost its soil becomes quicksilver: " + level.getBlockState(POS));
		reset(level);

		// client / server list comparison
		AtomicReference<Component> reason = new AtomicReference<>();
		CropFluidsPayload.handleOnClient(List.of("fluidprobe_quicksilver"), reason::set);
		check(reason.get() == null, "matching server list keeps the client connected");
		CropFluidsPayload.handleOnClient(List.of(), reason::set);
		check(reason.get() != null, "different server list disconnects: " + reason.get().getString());

		// leave a submerged silverbloom for the reload stages
		level.setBlock(PERSIST.below(), Blocks.FARMLAND.defaultBlockState(), 3);
		level.setBlock(PERSIST, CropFluids.withFluid(sticks(), SOURCE.get()), 3);
		AgriApi.getCrop(level, PERSIST).orElseThrow().plantGenome(genome("fluidprobe:silverbloom"));
	}

	/**
	 * Create's open-ended pipe pulling from submerged sticks (OpenEndedPipeMixin). Reflection keeps the probe free of a
	 * compile-time Create dependency.
	 */
	private static void createPipe(ServerLevel level, Block crop) throws Exception {
		Class<?> faceClass = Class.forName("net.createmod.catnip.math.BlockFace");
		Class<?> pipeClass = Class.forName("com.simibubi.create.content.fluids.OpenEndedPipe");
		// the pipe sits below the sticks and faces up, so its output is POS
		Object pipe = pipeClass.getConstructor(faceClass)
				.newInstance(faceClass.getConstructor(BlockPos.class, Direction.class).newInstance(POS.below(), Direction.UP));
		pipeClass.getMethod("manageSource", net.minecraft.world.level.Level.class, net.minecraft.world.level.block.entity.BlockEntity.class)
				.invoke(pipe, level, null);
		var pull = pipeClass.getDeclaredMethod("removeFluidFromSpace", boolean.class);
		pull.setAccessible(true);

		for (Fluid fluid : List.of(SOURCE.get(), Fluids.LAVA, Fluids.WATER)) {
			String name = fluidName(fluid);
			reset(level);
			level.setBlock(POS, CropFluids.withFluid(sticks(), fluid), 3);
			var simulated = (net.neoforged.neoforge.fluids.FluidStack) pull.invoke(pipe, true);
			check(simulated.getFluid() == fluid && simulated.getAmount() == 1000 && level.getFluidState(POS).getType() == fluid,
					"Create pipe simulates 1000 mB of " + name + " without draining: " + simulated.getAmount());
			var first = (net.neoforged.neoforge.fluids.FluidStack) pull.invoke(pipe, false);
			check(first.getFluid() == fluid && first.getAmount() == 1000, "Create pipe pulls 1000 mB of " + name + " from the sticks");
			check(level.getBlockState(POS).is(crop) && level.getFluidState(POS).isEmpty(),
					"the sticks are dry after the pipe pulled " + name + ": " + level.getBlockState(POS));
			var second = (net.neoforged.neoforge.fluids.FluidStack) pull.invoke(pipe, false);
			check(second.isEmpty(), "a second pull gets nothing (no endless " + name + "): " + second.getAmount());
		}
		reset(level);
	}

	private static String fluidName(Fluid fluid) {
		return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString();
	}

	private static void reload(ServerLevel level) {
		BlockState state = level.getBlockState(PERSIST);
		check(state.is(ModBlocks.CROP.get()) && level.getFluidState(PERSIST).getType() == SOURCE.get(),
				"submerged crop is still submerged after a restart: " + state);
		check(AgriApi.getCrop(level, PERSIST).map(AgriCrop::hasPlant).orElse(false), "its silverbloom survived the restart");
	}

	private static void empty(ServerLevel level) {
		check(CropFluids.property().isEmpty(), "empty list: no agricraft_fluid property");
		check(ModBlocks.CROP.get().getStateDefinition().getPossibleStates().size() == 3 * 4 * 17 * 4,
				"empty list: crop state count is unchanged (" + ModBlocks.CROP.get().getStateDefinition().getPossibleStates().size() + ")");
		BlockState state = level.getBlockState(PERSIST);
		check(state.is(ModBlocks.CROP.get()) && level.getFluidState(PERSIST).isEmpty(),
				"crop saved with quicksilver loads as a dry crop after the fluid is removed from the list: " + state);
	}

	private static void run(ServerStartedEvent event) {
		var server = event.getServer();
		String stage = CoreConfig.fluidloggableFluids.length == 0 ? "empty" : Files.exists(Path.of("fluid-results-main.txt")) ? "reload" : "main";
		REPORT.add("STAGE " + stage);
		try {
			var level = server.overworld();
			level.getChunk(POS);
			var player = new FakePlayer(level, new GameProfile(UUID.nameUUIDFromBytes("fluidprobe".getBytes()), "FluidProbe"));
			player.setGameMode(GameType.SURVIVAL);
			player.setPos(0.5, 101, 3.5);
			switch (stage) {
				case "main" -> main(level, player);
				case "reload" -> reload(level);
				default -> empty(level);
			}
			REPORT.add("RESULT all checks passed");
		} catch (Throwable failure) {
			REPORT.add("FAIL " + failure);
			failure.printStackTrace();
		} finally {
			server.saveEverything(true, true, true);
			try {
				Files.write(Path.of("fluid-results-" + stage + ".txt"), REPORT);
			} catch (Exception failure) {
				failure.printStackTrace();
			}
			REPORT.forEach(System.out::println);
			server.halt(false);
		}
	}
}
