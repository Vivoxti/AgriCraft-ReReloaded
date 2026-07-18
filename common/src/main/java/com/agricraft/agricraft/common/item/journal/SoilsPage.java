package com.agricraft.agricraft.common.item.journal;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.tools.journal.JournalPage;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Journal page listing every registered soil and the fixed humidity/acidity/nutrients it provides.
 * Built dynamically from the soil registry, so modded soils (compat datapacks) show up automatically.
 * <p>
 * The left/right column split is computed by the drawer at render time (based on how many rows
 * actually fit after the title/intro text), not pre-split here, so no vertical space is wasted.
 */
public class SoilsPage implements JournalPage {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "soils_page");
	/** Safety cap per two-page spread for the outer pagination loop (see JournalItem); the drawer
	 *  itself decides the real per-column capacity, this is just a conservative upper bound. */
	public static final int LIMIT = 18;

	private final List<SoilEntry> soils;

	public SoilsPage(List<SoilEntry> soils) {
		this.soils = soils;
	}

	@Override
	public ResourceLocation getDrawerId() {
		return ID;
	}

	public List<SoilEntry> getSoils() {
		return this.soils;
	}

	public record SoilEntry(ResourceLocation id, AgriSoil soil) {
	}

}
