package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * Renders the rotating arms of the sprinkler head.
 */
public class SprinklerRenderer implements BlockEntityRenderer<SprinklerBlockEntity> {

	public static final ResourceLocation IRON_BLOCK = ResourceLocation.withDefaultNamespace("block/iron_block");

	public SprinklerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(SprinklerBlockEntity sprinkler, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		// interpolate using the current (eased) rotation speed, not a fixed rate, so the visual
		// motion between ticks matches the smooth accel/decel happening on the logical side
		float angle = sprinkler.angle + sprinkler.rotationSpeed * partialTick;
		poseStack.pushPose();
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
		poseStack.translate(-0.5, 0, -0.5);
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IRON_BLOCK);
		VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
		// two crossing spray arms (no central hub - the static sprinkler head model already
		// provides the body; a separate hub here just read as a stray cube)
		this.drawBox(poseStack, consumer, sprite, packedLight, packedOverlay, 1 / 16.0F, 4 / 16.0F, 7 / 16.0F, 15 / 16.0F, 6 / 16.0F, 9 / 16.0F);
		this.drawBox(poseStack, consumer, sprite, packedLight, packedOverlay, 7 / 16.0F, 4 / 16.0F, 1 / 16.0F, 9 / 16.0F, 6 / 16.0F, 15 / 16.0F);
		poseStack.popPose();
	}

	private void drawBox(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite, int light, int overlay,
	                     float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		Matrix4f m = poseStack.last().pose();
		float u0 = sprite.getU0();
		float u1 = sprite.getU1();
		float v0 = sprite.getV0();
		float v1 = sprite.getV1();
		// down
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0);
		// up
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 0, 1, 0);
		// north
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, 0, 0, -1);
		// south
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1);
		// west
		this.quad(consumer, m, light, overlay, u0, v0, u1, v1, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0);
		// east
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
