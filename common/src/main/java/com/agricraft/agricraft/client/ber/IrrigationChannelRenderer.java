package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.IrrigationChannelBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationChannelBlockEntity;
import com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Renders the water inside an irrigation channel, extending towards connected sides, plus a
 * connecting post down to a sprinkler attached below (the channel's own model is a shallow trough
 * that does not reach the bottom of its block, so without this there'd be a visible gap between
 * the channel and a sprinkler's thin attachment plate).
 */
public class IrrigationChannelRenderer extends IrrigationComponentRenderer<IrrigationChannelBlockEntity> {

	private static final ResourceLocation OAK_PLANKS = ResourceLocation.withDefaultNamespace("block/oak_planks");

	public IrrigationChannelRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(IrrigationChannelBlockEntity channel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		this.drawSprinklerConnector(channel, poseStack, buffer, packedLight, packedOverlay);
		super.render(channel, partialTick, poseStack, buffer, packedLight, packedOverlay);
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

	private void drawSprinklerConnector(IrrigationChannelBlockEntity channel, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		if (channel.getLevel() == null || !(channel.getLevel().getBlockEntity(channel.getBlockPos().below()) instanceof SprinklerBlockEntity)) {
			return;
		}
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(OAK_PLANKS);
		VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
		this.drawBox(poseStack, consumer, sprite, packedLight, packedOverlay, 5 / 16.0F, 0.0F, 5 / 16.0F, 11 / 16.0F, 6 / 16.0F, 11 / 16.0F);
	}

	private void drawBox(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite, int light, int overlay,
	                     float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		Matrix4f m = poseStack.last().pose();
		float u0 = sprite.getU0();
		float u1 = sprite.getU1();
		float v0 = sprite.getV0();
		float v1 = sprite.getV1();
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 0, 1, 0);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, 0, 0, -1);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0);
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, 1, 0, 0);
	}

	private void quad(VertexConsumer consumer, Matrix4f m, int light, int overlay, float u0, float v0, float u1, float v1,
	                  float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
	                  int nx, int ny, int nz) {
		consumer.addVertex(m, x1, y1, z1).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
		consumer.addVertex(m, x2, y2, z2).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
		consumer.addVertex(m, x3, y3, z3).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
		consumer.addVertex(m, x4, y4, z4).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
	}

}
