package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.entity.IrrigationComponentBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * Renders the water surface inside an irrigation component (tank or channel).
 */
public abstract class IrrigationComponentRenderer<T extends IrrigationComponentBlockEntity> implements BlockEntityRenderer<T> {

	public static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");

	@Override
	public void render(T component, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if (component.getContent() <= 0 || component.getLevel() == null) {
			return;
		}
		this.renderWater(component, partialTick, poseStack, buffer, packedLight, packedOverlay);
	}

	protected abstract void renderWater(T component, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);

	/**
	 * Draws a horizontal water quad between the given in-block bounds at the given in-block height.
	 */
	protected void drawWaterSurface(IrrigationComponentBlockEntity component, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
	                                float minX, float minZ, float maxX, float maxZ, float y) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_STILL);
		BlockPos pos = component.getBlockPos();
		int color = BiomeColors.getAverageWaterColor(component.getLevel(), pos);
		float r = FastColor.ARGB32.red(color) / 255.0F;
		float g = FastColor.ARGB32.green(color) / 255.0F;
		float b = FastColor.ARGB32.blue(color) / 255.0F;
		VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
		Matrix4f matrix = poseStack.last().pose();
		float u0 = sprite.getU(minX);
		float u1 = sprite.getU(maxX);
		float v0 = sprite.getV(minZ);
		float v1 = sprite.getV(maxZ);
		consumer.addVertex(matrix, minX, y, minZ).setColor(r, g, b, 0.85F).setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
		consumer.addVertex(matrix, minX, y, maxZ).setColor(r, g, b, 0.85F).setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
		consumer.addVertex(matrix, maxX, y, maxZ).setColor(r, g, b, 0.85F).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
		consumer.addVertex(matrix, maxX, y, minZ).setColor(r, g, b, 0.85F).setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
	}

}
