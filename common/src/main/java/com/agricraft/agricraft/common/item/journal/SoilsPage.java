package com.agricraft.agricraft.common.item.journal;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.tools.journal.JournalPage;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Journal page listing every registered soil and the fixed humidity/acidity/nutrients it provides.
 * Built dynamically from the soil registry, so modded soils (compat datapacks) show up automatically.
 */
public class SoilsPage implements JournalPage {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "soils_page");
	/** Max soils per two-page spread before overflowing onto another SoilsPage. */
	public static final int LIMIT = 32;

	private final List<SoilEntry> soilsLeft;
	private final List<SoilEntry> soilsRight;

	public SoilsPage(List<SoilEntry> soils) {
		int mid = Math.min(soils.size(), (soils.size() + 1) / 2);
		this.soilsLeft = soils.subList(0, mid);
		this.soilsRight = soils.subList(mid, soils.size());
	}

	@Override
	public ResourceLocation getDrawerId() {
		return ID;
	}

	public List<SoilEntry> getSoilsLeft() {
		return this.soilsLeft;
	}

	public List<SoilEntry> getSoilsRight() {
		return this.soilsRight;
	}

	public record SoilEntry(ResourceLocation id, AgriSoil soil) {
	}

}
