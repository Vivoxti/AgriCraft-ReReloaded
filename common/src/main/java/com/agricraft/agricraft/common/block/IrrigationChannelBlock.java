package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.common.block.entity.IrrigationChannelBlockEntity;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import com.agricraft.agricraft.common.registry.ModBlocks;
import com.agricraft.agricraft.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
 * Water channel: connects to tanks, other channels and sprinklers.
 * The normal variant is an open trough; the hollow variant is a full block with the channel inside.
 * A valve can be installed to block the flow: the normal channel valve is toggled by hand,
 * the hollow channel valve is controlled by redstone.
 */
public class IrrigationChannelBlock extends Block implements EntityBlock {

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	/** true when a sprinkler is attached directly below - renders a connecting post down to it. */
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final EnumProperty<Valve> VALVE = EnumProperty.create("valve", Valve.class);

	private static final VoxelShape SHAPE_BASE = Block.box(5, 5, 5, 11, 10, 11);
	private static final VoxelShape SHAPE_NORTH = Block.box(5, 5, 0, 11, 10, 5);
	private static final VoxelShape SHAPE_SOUTH = Block.box(5, 5, 11, 11, 10, 16);
	private static final VoxelShape SHAPE_WEST = Block.box(0, 5, 5, 5, 10, 11);
	private static final VoxelShape SHAPE_EAST = Block.box(11, 5, 5, 16, 10, 11);
	private static final VoxelShape SHAPE_VALVE = Block.box(6, 5, 6, 10, 16, 10);
	private static final VoxelShape SHAPE_DOWN = Block.box(5, 0, 5, 11, 5, 11);

	private final boolean hollow;

	public IrrigationChannelBlock(boolean hollow) {
		super(Properties.of().mapColor(MapColor.WOOD).strength(2, 3).noOcclusion());
		this.hollow = hollow;
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(NORTH, false)
				.setValue(EAST, false)
				.setValue(SOUTH, false)
				.setValue(WEST, false)
				.setValue(DOWN, false)
				.setValue(VALVE, Valve.NONE));
	}

	public boolean isHollow() {
		return this.hollow;
	}

	public static BooleanProperty connection(Direction direction) {
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
		builder.add(NORTH, EAST, SOUTH, WEST, DOWN, VALVE);
	}

	private static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction side) {
		BlockState neighbour = level.getBlockState(pos.relative(side));
		return neighbour.getBlock() instanceof IrrigationChannelBlock
				|| neighbour.is(ModBlocks.IRRIGATION_TANK.get());
	}

	private static boolean hasSprinklerBelow(LevelAccessor level, BlockPos pos) {
		return level.getBlockState(pos.below()).getBlock() instanceof SprinklerBlock;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = this.defaultBlockState();
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			state = state.setValue(connection(dir), connectsTo(level, pos, dir));
		}
		return state.setValue(DOWN, hasSprinklerBelow(level, pos));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (direction.getAxis().isHorizontal()) {
			return state.setValue(connection(direction), connectsTo(level, pos, direction));
		}
		if (direction == Direction.DOWN) {
			return state.setValue(DOWN, hasSprinklerBelow(level, pos));
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (this.hollow) {
			return Shapes.block();
		}
		VoxelShape shape = SHAPE_BASE;
		if (state.getValue(NORTH)) {
			shape = Shapes.or(shape, SHAPE_NORTH);
		}
		if (state.getValue(SOUTH)) {
			shape = Shapes.or(shape, SHAPE_SOUTH);
		}
		if (state.getValue(WEST)) {
			shape = Shapes.or(shape, SHAPE_WEST);
		}
		if (state.getValue(EAST)) {
			shape = Shapes.or(shape, SHAPE_EAST);
		}
		if (state.getValue(DOWN)) {
			shape = Shapes.or(shape, SHAPE_DOWN);
		}
		if (state.getValue(VALVE) != Valve.NONE) {
			shape = Shapes.or(shape, SHAPE_VALVE);
		}
		return shape;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		// toggle the valve by hand on normal channels
		if (!this.hollow && state.getValue(VALVE) != Valve.NONE) {
			boolean willClose = state.getValue(VALVE) == Valve.OPEN;
			if (level.isClientSide) {
				// kick off the client-side hand-wheel + stem animation
				if (level.getBlockEntity(pos) instanceof IrrigationChannelBlockEntity channel) {
					channel.startValveAnimation(willClose);
				}
			} else {
				level.setBlock(pos, state.setValue(VALVE, willClose ? Valve.CLOSED : Valve.OPEN), Block.UPDATE_ALL);
				level.playSound(null, pos, willClose ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.5F);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
		// hollow channel valves are controlled by redstone
		if (this.hollow && state.getValue(VALVE) != Valve.NONE && !level.isClientSide) {
			Valve valve = level.hasNeighborSignal(pos) ? Valve.CLOSED : Valve.OPEN;
			if (state.getValue(VALVE) != valve) {
				level.setBlock(pos, state.setValue(VALVE, valve), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		drops.add(new ItemStack(this));
		if (state.getValue(VALVE) != Valve.NONE) {
			drops.add(new ItemStack(ModItems.CHANNEL_VALVE.get()));
		}
		return drops;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new IrrigationChannelBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == ModBlockEntityTypes.IRRIGATION_CHANNEL.get()
				? (lvl, pos, st, be) -> IrrigationChannelBlockEntity.tick(lvl, pos, st, (IrrigationChannelBlockEntity) be)
				: null;
	}

	public enum Valve implements StringRepresentable {
		NONE("none", true),
		OPEN("open", true),
		CLOSED("closed", false);

		private final String name;
		private final boolean transfer;

		Valve(String name, boolean transfer) {
			this.name = name;
			this.transfer = transfer;
		}

		public boolean canTransfer() {
			return this.transfer;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

}
