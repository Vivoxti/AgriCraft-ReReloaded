package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.IrrigationTankBlock;
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
		// Draw the surface only in the block that is the actual top of the water column. In a
		// multi-block-high tank the lower blocks are full and would otherwise each draw their own
		// translucent surface, which shows through the blocks above. If the tank directly above
		// still holds water, the surface is up there, not here.
		if (tank.getLevel() != null
				&& tank.getLevel().getBlockEntity(tank.getBlockPos().above()) instanceof IrrigationTankBlockEntity above
				&& above.getContent() > 0) {
			return;
		}
		// once WATER is set the block reports a full vanilla water FluidState (see
		// IrrigationTankBlock.getFluidState / updateWaterState) so the engine already renders a real
		// water cube here for correct swim/overlay physics; drawing our own partial-height surface on
		// top of that would double up and look wrong, so let the engine handle it entirely.
		if (tank.getBlockState().getValue(IrrigationTankBlock.WATER)) {
			return;
		}
		double height = tank.getMinFluidHeight() + tank.getFillFraction() * (tank.getMaxFluidHeight() - tank.getMinFluidHeight());
		// avoid z-fighting with the block above at a completely full tank
		float y = (float) Math.min(height, 0.999);
		// full-block footprint (0..1) so adjacent tanks in a multiblock join with no gap; the tank
		// walls (opaque) cover the overhang on the outer edges
		this.drawWaterSurface(tank, poseStack, buffer, packedLight, packedOverlay, 0.0F, 0.0F, 1.0F, 1.0F, y);
	}

}
