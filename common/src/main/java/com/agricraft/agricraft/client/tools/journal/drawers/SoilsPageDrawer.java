package com.agricraft.agricraft.client.tools.journal.drawers;

import com.agricraft.agricraft.api.tools.journal.JournalData;
import com.agricraft.agricraft.api.tools.journal.JournalPageDrawer;
import com.agricraft.agricraft.common.item.journal.SoilsPage;
import com.agricraft.agricraft.common.util.LangUtils;
import com.agricraft.agricraft.common.util.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Draws each soil as one compact row: a small icon of a representative block, its name, small
 * icons re-used from the growth-requirements legend for humidity/acidity/nutrients, and the growth
 * modifier as text. The left/right split is computed here (not on the page) so the left column
 * fills up as far as it actually can below the title/intro before overflowing to the right.
 */
public class SoilsPageDrawer implements JournalPageDrawer<SoilsPage> {

	private static final Component TITLE = Component.translatable("agricraft.journal.soils");
	private static final Component INTRO = Component.translatable("agricraft.journal.soils.desc");
	private static final float INTRO_SCALE = 0.6F;
	private static final float NAME_SCALE = 0.7F;
	private static final float VALUE_SCALE = 0.6F;

	private static final int ICON_SIZE = 12;
	private static final int ROW_HEIGHT = 15;
	private static final int BOTTOM_MARGIN = 8;

	@Override
	public void drawLeftSheet(GuiGraphics guiGraphics, SoilsPage page, int pageX, int pageY, JournalData journalData) {
		Font font = Minecraft.getInstance().font;
		float dx = pageX + 6;
		float dy = pageY + 10;

		guiGraphics.drawString(font, TITLE, (int) dx, (int) dy, 0, false);
		dy += font.lineHeight + 4;
		dy += this.drawScaledText(guiGraphics, INTRO, dx, dy, INTRO_SCALE);
		dy += 4;

		List<SoilsPage.SoilEntry> soils = page.getSoils();
		int capacity = this.rowsAvailable(pageY, dy);
		this.drawSoils(guiGraphics, soils.subList(0, Math.min(capacity, soils.size())), dx, dy);
	}

	@Override
	public void drawRightSheet(GuiGraphics guiGraphics, SoilsPage page, int pageX, int pageY, JournalData journalData) {
		float dx = pageX + 6;
		float dy = pageY + 10;

		List<SoilsPage.SoilEntry> soils = page.getSoils();
		int leftCount = Math.min(this.computeLeftCapacity(pageY), soils.size());
		if (leftCount >= soils.size()) {
			return;
		}
		List<SoilsPage.SoilEntry> remaining = soils.subList(leftCount, soils.size());
		int capacity = this.rowsAvailable(pageY, dy);
		this.drawSoils(guiGraphics, remaining.subList(0, Math.min(capacity, remaining.size())), dx, dy);
	}

	/** How many rows fit between the given cursor position and the bottom of the page. */
	private int rowsAvailable(int pageY, float dy) {
		int available = (int) (pageY + PAGE_HEIGHT - BOTTOM_MARGIN - dy);
		return Math.max(0, available / ROW_HEIGHT);
	}

	/** Pure measurement (no drawing) mirroring the title+intro block drawn in drawLeftSheet. */
	private int computeLeftCapacity(int pageY) {
		Font font = Minecraft.getInstance().font;
		float dy = pageY + 10;
		dy += font.lineHeight + 4;
		dy += font.wordWrapHeight(INTRO, (int) (PAGE_WIDTH / INTRO_SCALE)) * INTRO_SCALE;
		dy += 4;
		return this.rowsAvailable(pageY, dy);
	}

	private void drawSoils(GuiGraphics guiGraphics, List<SoilsPage.SoilEntry> soils, float dx, float dy) {
		Font font = Minecraft.getInstance().font;
		int y = (int) dy;
		for (SoilsPage.SoilEntry entry : soils) {
			int x = (int) dx;

			this.drawBlockIcon(guiGraphics, entry, x, y);
			x += ICON_SIZE + 2;

			Component name = LangUtils.soilName(entry.id().toString());
			x += this.drawScaledString(guiGraphics, font, name, x, y + 2, NAME_SCALE) + 3;

			x = this.drawSoilProperty(guiGraphics, x, y, HUMIDITY_OFFSETS, 0, entry.soil().humidity().ordinal());
			x = this.drawSoilProperty(guiGraphics, x, y, ACIDITY_OFFSETS, 12, entry.soil().acidity().ordinal());
			x = this.drawSoilProperty(guiGraphics, x, y, NUTRIENTS_OFFSETS, 24, entry.soil().nutrients().ordinal());

			Component modifier = Component.literal("×" + entry.soil().growthModifier());
			this.drawScaledString(guiGraphics, font, modifier, x + 2, y + 2, VALUE_SCALE);

			y += ROW_HEIGHT;
		}
	}

	private int drawSoilProperty(GuiGraphics guiGraphics, int x, int y, int[] offsets, int textureOffsetY, int ordinal) {
		if (ordinal < 0 || ordinal >= offsets.length - 1) {
			return x;
		}
		int width = offsets[ordinal + 1] - offsets[ordinal];
		guiGraphics.blit(GUI_COMPONENTS, x, y, offsets[ordinal], textureOffsetY, width, ICON_SIZE, 128, 128);
		return x + width + 2;
	}

	private void drawBlockIcon(GuiGraphics guiGraphics, SoilsPage.SoilEntry entry, int x, int y) {
		entry.soil().variants().stream()
				.flatMap(variant -> Platform.get().getBlocksFromLocation(variant.block()).stream())
				.findFirst()
				.ifPresent(block -> {
					TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState()).getParticleIcon();
					guiGraphics.blit(x, y, 1, ICON_SIZE, ICON_SIZE, sprite);
				});
	}

	/** Draws text scaled down without word-wrapping (rows are single-line); returns the rendered width in real pixels. */
	private int drawScaledString(GuiGraphics guiGraphics, Font font, Component text, int x, int y, float scale) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0);
		guiGraphics.pose().scale(scale, scale, 1);
		guiGraphics.drawString(font, text, 0, 0, 0, false);
		guiGraphics.pose().popPose();
		return (int) (font.width(text) * scale);
	}

}
