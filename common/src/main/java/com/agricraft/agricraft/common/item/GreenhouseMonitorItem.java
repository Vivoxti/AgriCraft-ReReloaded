package com.agricraft.agricraft.common.item;

import com.agricraft.agricraft.common.greenhouse.Greenhouses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Tool to create a greenhouse (sneak + use) and check whether the player is standing in one (use).
 */
public class GreenhouseMonitorItem extends Item {

	public GreenhouseMonitorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel) {
			BlockPos pos = player.blockPosition().above();
			if (player.isShiftKeyDown()) {
				Greenhouses.Result result = Greenhouses.createGreenhouse(serverLevel, pos);
				player.displayClientMessage(Component.translatable(result.translationKey()), true);
			} else {
				boolean inside = Greenhouses.isInGreenhouse(serverLevel, pos);
				player.displayClientMessage(Component.translatable(inside ? "agricraft.message.greenhouse.inside" : "agricraft.message.greenhouse.outside"), true);
			}
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("agricraft.tooltip.greenhouse_monitor").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
	}

}
