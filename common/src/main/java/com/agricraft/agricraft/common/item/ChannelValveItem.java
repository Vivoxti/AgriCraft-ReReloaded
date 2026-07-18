package com.agricraft.agricraft.common.item;

import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Valve that can be installed on an irrigation channel to control the water flow.
 */
public class ChannelValveItem extends Item {

	public ChannelValveItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof IrrigationChannelBlock && state.getValue(IrrigationChannelBlock.VALVE) == IrrigationChannelBlock.Valve.NONE) {
			if (!level.isClientSide) {
				level.setBlock(pos, state.setValue(IrrigationChannelBlock.VALVE, IrrigationChannelBlock.Valve.OPEN), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
				if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
					context.getItemInHand().shrink(1);
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

}
