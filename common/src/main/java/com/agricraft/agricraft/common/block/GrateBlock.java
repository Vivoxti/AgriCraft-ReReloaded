package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.common.item.ClipperItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Decorative wooden grate. The panel can sit at three positions inside the block (near, mid, far)
 * along its normal axis, can be waterlogged, decorated with vines, and climbed like a ladder.
 */
public class GrateBlock extends Block implements SimpleWaterloggedBlock {

	/** The axis perpendicular to the grate panel. */
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	public static final EnumProperty<Offset> OFFSET = EnumProperty.create("offset", Offset.class);
	public static final EnumProperty<Vines> VINES = EnumProperty.create("vines", Vines.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public GrateBlock() {
		super(Properties.of().mapColor(MapColor.WOOD).strength(1, 1).noOcclusion());
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(AXIS, Direction.Axis.Z)
				.setValue(OFFSET, Offset.MID)
				.setValue(VINES, Vines.NONE)
				.setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, OFFSET, VINES, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double min = state.getValue(OFFSET).getMin();
		double max = state.getValue(OFFSET).getMax();
		return switch (state.getValue(AXIS)) {
			case X -> Block.box(min, 0, 0, max, 16, 16);
			case Y -> Block.box(0, min, 0, 16, max, 16);
			case Z -> Block.box(0, 0, min, 16, 16, max);
		};
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = this.defaultBlockState()
				.setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
		// clicking the side of an existing grate extends it with the same orientation
		BlockState clicked = level.getBlockState(pos.relative(context.getClickedFace().getOpposite()));
		if (clicked.is(this) && context.getClickedFace().getAxis() != clicked.getValue(AXIS)) {
			return state.setValue(AXIS, clicked.getValue(AXIS)).setValue(OFFSET, clicked.getValue(OFFSET));
		}
		Direction.Axis axis;
		if (context.getClickedFace().getAxis().isVertical()) {
			// vertical panel, facing the player
			axis = context.getHorizontalDirection().getAxis();
		} else {
			// horizontal panel
			axis = Direction.Axis.Y;
		}
		Vec3 hit = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		double along = switch (axis) {
			case X -> hit.x;
			case Y -> hit.y;
			case Z -> hit.z;
		};
		Offset offset = along >= 11.0 / 16.0 ? Offset.FAR : along <= 5.0 / 16.0 ? Offset.NEAR : Offset.MID;
		return state.setValue(AXIS, axis).setValue(OFFSET, offset);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return state;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		Vines vines = state.getValue(VINES);
		if (stack.is(Items.VINE)) {
			Vines added = vines.addVines(this.isFrontHit(state, pos, hit));
			if (added != vines) {
				if (!level.isClientSide) {
					level.setBlock(pos, state.setValue(VINES, added), Block.UPDATE_ALL);
					if (!player.getAbilities().instabuild) {
						stack.shrink(1);
					}
					level.playSound(null, pos, SoundEvents.VINE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
			return ItemInteractionResult.FAIL;
		}
		if (stack.getItem() instanceof ClipperItem && vines != Vines.NONE) {
			Vines removed = vines.removeVines(this.isFrontHit(state, pos, hit));
			if (removed != vines) {
				if (!level.isClientSide) {
					level.setBlock(pos, state.setValue(VINES, removed), Block.UPDATE_ALL);
					if (!player.addItem(new ItemStack(Items.VINE))) {
						player.drop(new ItemStack(Items.VINE), false);
					}
					level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** @return true when the hit location is on the lower-coordinate side of the panel */
	private boolean isFrontHit(BlockState state, BlockPos pos, BlockHitResult hit) {
		Vec3 loc = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		double along = switch (state.getValue(AXIS)) {
			case X -> loc.x;
			case Y -> loc.y;
			case Z -> loc.z;
		};
		return along <= (state.getValue(OFFSET).getMin() + state.getValue(OFFSET).getMax()) / 32.0;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		drops.add(new ItemStack(this));
		int vines = state.getValue(VINES).count();
		if (vines > 0) {
			drops.add(new ItemStack(Items.VINE, vines));
		}
		return drops;
	}

	public enum Offset implements StringRepresentable {
		NEAR("near", 0, 2),
		MID("mid", 7, 9),
		FAR("far", 14, 16);

		private final String name;
		private final double min;
		private final double max;

		Offset(String name, double min, double max) {
			this.name = name;
			this.min = min;
			this.max = max;
		}

		public double getMin() {
			return this.min;
		}

		public double getMax() {
			return this.max;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public enum Vines implements StringRepresentable {
		NONE("none"),
		FRONT("front"),
		BACK("back"),
		BOTH("both");

		private final String name;

		Vines(String name) {
			this.name = name;
		}

		public Vines addVines(boolean front) {
			return switch (this) {
				case NONE -> front ? FRONT : BACK;
				case FRONT -> front ? FRONT : BOTH;
				case BACK -> front ? BOTH : BACK;
				case BOTH -> BOTH;
			};
		}

		public Vines removeVines(boolean front) {
			return switch (this) {
				case NONE -> NONE;
				case FRONT -> front ? NONE : FRONT;
				case BACK -> front ? BACK : NONE;
				case BOTH -> front ? BACK : FRONT;
			};
		}

		public int count() {
			return switch (this) {
				case NONE -> 0;
				case FRONT, BACK -> 1;
				case BOTH -> 2;
			};
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

}
