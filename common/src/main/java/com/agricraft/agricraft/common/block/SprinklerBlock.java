package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity;
import com.agricraft.agricraft.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sprinkler: must be placed below an irrigation channel; drains water from it and irrigates the
 * area below.
 */
public class SprinklerBlock extends Block implements EntityBlock {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(7, 8, 7, 9, 16, 9),
			Block.box(5, 1, 5, 11, 8, 11)
	);

	public SprinklerBlock() {
		super(Properties.of().mapColor(MapColor.METAL).strength(2, 3).noOcclusion());
		this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.above()).getBlock() instanceof IrrigationChannelBlock;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.canSurvive(this.defaultBlockState(), context.getLevel(), context.getClickedPos()) ? this.defaultBlockState() : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.UP && !(neighbourState.getBlock() instanceof IrrigationChannelBlock)) {
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		drops.add(new ItemStack(this));
		return drops;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SprinklerBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == ModBlockEntityTypes.SPRINKLER.get()
				? (lvl, pos, st, be) -> SprinklerBlockEntity.tick(lvl, pos, st, (SprinklerBlockEntity) be)
				: null;
	}

}
