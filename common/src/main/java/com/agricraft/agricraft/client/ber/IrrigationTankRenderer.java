package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.entity.IrrigationTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Renders the water surface inside an irrigation tank.
 */
public class IrrigationTankRenderer extends IrrigationComponentRenderer<IrrigationTankBlockEntity> {

	public IrrigationTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderWater(IrrigationTankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		double height = tank.getMinFluidHeight() + tank.getFillFraction() * (tank.getMaxFluidHeight() - tank.getMinFluidHeight());
		// avoid z-fighting with the tank walls and the block above
		float y = (float) Math.min(height, 0.999);
		this.drawWaterSurface(tank, poseStack, buffer, packedLight, packedOverlay, 0.001F, 0.001F, 0.999F, 0.999F, y);
	}

}
