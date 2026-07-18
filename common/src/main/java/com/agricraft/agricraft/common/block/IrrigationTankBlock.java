package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.common.block.entity.IrrigationTankBlockEntity;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import com.agricraft.agricraft.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wooden water tank; adjacent tanks connect and behave like one larger tank.
 * Collects rain water, can be filled and drained with buckets (or fluid pipes on NeoForge).
 */
public class IrrigationTankBlock extends Block implements EntityBlock {

	public static final EnumProperty<Connection> NORTH = EnumProperty.create("north", Connection.class);
	public static final EnumProperty<Connection> EAST = EnumProperty.create("east", Connection.class);
	public static final EnumProperty<Connection> SOUTH = EnumProperty.create("south", Connection.class);
	public static final EnumProperty<Connection> WEST = EnumProperty.create("west", Connection.class);
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final BooleanProperty LADDER = BooleanProperty.create("ladder");

	private static final VoxelShape SHAPE_BOTTOM = Block.box(0, 0, 0, 16, 2, 16);
	private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 0, 16, 16, 2);
	private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 14, 16, 16, 16);
	private static final VoxelShape SHAPE_WEST = Block.box(0, 0, 0, 2, 16, 16);
	private static final VoxelShape SHAPE_EAST = Block.box(14, 0, 0, 16, 16, 16);

	public IrrigationTankBlock() {
		super(Properties.of().mapColor(MapColor.WOOD).strength(2, 3).noOcclusion());
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(NORTH, Connection.NONE)
				.setValue(EAST, Connection.NONE)
				.setValue(SOUTH, Connection.NONE)
				.setValue(WEST, Connection.NONE)
				.setValue(DOWN, false)
				.setValue(LADDER, false));
	}

	public static EnumProperty<Connection> connection(Direction direction) {
		return switch (direction) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> throw new IllegalArgumentException("no connection property for direction " + direction);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, DOWN, LADDER);
	}

	private static Connection connectionTo(LevelAccessor level, BlockPos pos, Direction side) {
		BlockState neighbour = level.getBlockState(pos.relative(side));
		if (neighbour.is(ModBlocks.IRRIGATION_TANK.get())) {
			return Connection.TANK;
		}
		if (neighbour.getBlock() instanceof IrrigationChannelBlock && neighbour.getValue(IrrigationChannelBlock.connection(side.getOpposite()))) {
			return Connection.CHANNEL;
		}
		return Connection.NONE;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = this.defaultBlockState();
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			state = state.setValue(connection(dir), connectionTo(level, pos, dir));
		}
		return state.setValue(DOWN, !level.getBlockState(pos.below()).is(ModBlocks.IRRIGATION_TANK.get()));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.DOWN) {
			return state.setValue(DOWN, !neighbourState.is(ModBlocks.IRRIGATION_TANK.get()));
		}
		if (direction.getAxis().isHorizontal()) {
			return state.setValue(connection(direction), connectionTo(level, pos, direction));
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = state.getValue(DOWN) ? SHAPE_BOTTOM : Shapes.empty();
		if (state.getValue(NORTH) != Connection.TANK) {
			shape = Shapes.or(shape, SHAPE_NORTH);
		}
		if (state.getValue(SOUTH) != Connection.TANK) {
			shape = Shapes.or(shape, SHAPE_SOUTH);
		}
		if (state.getValue(WEST) != Connection.TANK) {
			shape = Shapes.or(shape, SHAPE_WEST);
		}
		if (state.getValue(EAST) != Connection.TANK) {
			shape = Shapes.or(shape, SHAPE_EAST);
		}
		return shape;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof IrrigationTankBlockEntity tank)) {
			return ItemInteractionResult.FAIL;
		}
		if (stack.is(Items.WATER_BUCKET)) {
			if (tank.getCapacity() - tank.getContent() >= 1000) {
				if (!level.isClientSide) {
					tank.pushWater(1000, true);
					player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
					level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
			return ItemInteractionResult.FAIL;
		}
		if (stack.is(Items.BUCKET)) {
			if (tank.getContent() >= 1000) {
				if (!level.isClientSide) {
					tank.drainWater(1000, true);
					player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
					level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
			return ItemInteractionResult.FAIL;
		}
		if (stack.is(Items.LADDER) && !state.getValue(LADDER)) {
			if (!level.isClientSide) {
				level.setBlock(pos, state.setValue(LADDER, true), Block.UPDATE_ALL);
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				level.playSound(null, pos, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
		if (precipitation == Biome.Precipitation.RAIN && level.getBlockEntity(pos) instanceof IrrigationTankBlockEntity tank) {
			tank.collectRain();
		}
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		drops.add(new ItemStack(this));
		if (state.getValue(LADDER)) {
			drops.add(new ItemStack(Items.LADDER));
		}
		return drops;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new IrrigationTankBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == ModBlockEntityTypes.IRRIGATION_TANK.get() && !level.isClientSide
				? (lvl, pos, st, be) -> IrrigationTankBlockEntity.tick(lvl, pos, st, (IrrigationTankBlockEntity) be)
				: null;
	}

	public enum Connection implements StringRepresentable {
		NONE("none"),
		TANK("tank"),
		CHANNEL("channel");

		private final String name;

		Connection(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

}
