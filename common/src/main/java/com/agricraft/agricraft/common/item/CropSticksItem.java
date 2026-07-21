package com.agricraft.agricraft.common.item;

import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropStickVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CropSticksItem extends BlockItem {

	private final CropStickVariant variant;

	public CropSticksItem(Block block, CropStickVariant variant) {
		super(block, variant == CropStickVariant.IRON || variant == CropStickVariant.OBSIDIAN
				? new Item.Properties().fireResistant()
				: new Item.Properties());
		this.variant = variant;
	}

	public CropStickVariant getVariant() {
		return this.variant;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		if (world.isClientSide()) {
			return InteractionResult.PASS;
		}
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof CropBlock) {
			// if there is already a crop in the target placement position, apply crop sticks there
			return this.applyToExisting(world, pos, state, context.getPlayer(), context.getHand());
		}
		// Delegate to default logic for placement on soil
		return super.useOn(context);
	}

	protected InteractionResult applyToExisting(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand) {
		InteractionResult result = CropBlock.applyCropSticks(world, pos, state, this.getVariant());
		if (result.consumesAction()) {
			if (!world.isClientSide() && player != null && !player.isCreative()) {
				ItemStack stack = player.getItemInHand(hand);
				stack.shrink(1);
			}
			return InteractionResult.sidedSuccess(world.isClientSide());
		}
		return result;
	}

	@Override
	public String getDescriptionId() {
		return "item.agricraft." + variant.getSerializedName() + "_crop_sticks";
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
		super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
		switch (this.variant) {
			case IRON -> tooltipComponents.add(Component.translatable("agricraft.tooltip.crop_sticks.weed_resistance", 30).withStyle(ChatFormatting.DARK_GRAY));
			case OBSIDIAN -> tooltipComponents.add(Component.translatable("agricraft.tooltip.crop_sticks.weed_resistance", 60).withStyle(ChatFormatting.DARK_GRAY));
			case WOODEN -> { /* no bonus */ }
		}
	}

}
