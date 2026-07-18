package com.agricraft.agricraft.client.tools.journal.drawers;

import com.agricraft.agricraft.api.tools.journal.JournalData;
import com.agricraft.agricraft.api.tools.journal.JournalPageDrawer;
import com.agricraft.agricraft.common.item.journal.SoilsPage;
import com.agricraft.agricraft.common.util.LangUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SoilsPageDrawer implements JournalPageDrawer<SoilsPage> {

	private static final Component TITLE = Component.translatable("agricraft.journal.soils");
	private static final Component INTRO = Component.translatable("agricraft.journal.soils.desc");

	@Override
	public void drawLeftSheet(GuiGraphics guiGraphics, SoilsPage page, int pageX, int pageY, JournalData journalData) {
		Font font = Minecraft.getInstance().font;
		float dx = pageX + 6;
		float dy = pageY + 10;

		guiGraphics.drawString(font, TITLE, (int) dx, (int) dy, 0, false);
		dy += font.lineHeight + 4;
		dy += this.drawScaledText(guiGraphics, INTRO, dx, dy, 0.6F);
		dy += 4;

		this.drawSoils(guiGraphics, page.getSoilsLeft(), dx, dy);
	}

	@Override
	public void drawRightSheet(GuiGraphics guiGraphics, SoilsPage page, int pageX, int pageY, JournalData journalData) {
		float dx = pageX + 6;
		float dy = pageY + 10;

		this.drawSoils(guiGraphics, page.getSoilsRight(), dx, dy);
	}

	private void drawSoils(GuiGraphics guiGraphics, List<SoilsPage.SoilEntry> soils, float dx, float dy) {
		for (SoilsPage.SoilEntry entry : soils) {
			Component line = Component.empty()
					.append(LangUtils.soilName(entry.id().toString()))
					.append(Component.literal(": "))
					.append(LangUtils.soilPropertyName("humidity", entry.soil().humidity()))
					.append(Component.literal(", "))
					.append(LangUtils.soilPropertyName("acidity", entry.soil().acidity()))
					.append(Component.literal(", "))
					.append(LangUtils.soilPropertyName("nutrients", entry.soil().nutrients()))
					.append(Component.literal(" (×" + entry.soil().growthModifier() + ")"));
			dy += this.drawScaledText(guiGraphics, line, dx, dy, 0.5F);
			dy += 2;
		}
	}

}
