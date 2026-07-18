package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationChannelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the water inside an irrigation channel, extending towards connected sides.
 */
public class IrrigationChannelRenderer extends IrrigationComponentRenderer<IrrigationChannelBlockEntity> {

	public IrrigationChannelRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderWater(IrrigationChannelBlockEntity channel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		BlockState state = channel.getBlockState();
		double height = channel.getMinFluidHeight() + channel.getFillFraction() * (channel.getMaxFluidHeight() - channel.getMinFluidHeight());
		float y = (float) Math.min(height, 10.0 / 16.0 - 0.001);
		// central trough
		this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, 6 / 16.0F, 10 / 16.0F, 10 / 16.0F, y);
		// connections
		if (state.getValue(IrrigationChannelBlock.connection(Direction.NORTH))) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, 0.0F, 10 / 16.0F, 6 / 16.0F, y);
		}
		if (state.getValue(IrrigationChannelBlock.connection(Direction.SOUTH))) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 6 / 16.0F, 10 / 16.0F, 10 / 16.0F, 1.0F, y);
		}
		if (state.getValue(IrrigationChannelBlock.connection(Direction.WEST))) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 0.0F, 6 / 16.0F, 6 / 16.0F, 10 / 16.0F, y);
		}
		if (state.getValue(IrrigationChannelBlock.connection(Direction.EAST))) {
			this.drawWaterSurface(channel, poseStack, buffer, packedLight, packedOverlay, 10 / 16.0F, 6 / 16.0F, 1.0F, 10 / 16.0F, y);
		}
	}

}
