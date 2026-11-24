package jeitransferprobe;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.registry.ModItems;
import com.agricraft.agricraft.compat.jei.CropProduceCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Separate test mod: checks that JEI's recipe transfer ("+" button) fills the AgriCraft Journal recipe
 * (writable book + #agricraft:seeds) from an inventory that only holds AgriCraft seeds with a plant, and that
 * per-plant recipe lookups for seeds are unchanged. Never included in a production source set.
 */
@Mod("jeitransferprobe")
public final class JeiTransferProbe {
	static IJeiRuntime runtime;
	private static final List<String> REPORT = new ArrayList<>();
	private static int ticks;
	private static int step;
	private static boolean failed;

	public JeiTransferProbe() {
		NeoForge.EVENT_BUS.addListener(JeiTransferProbe::tick);
	}

	@JeiPlugin
	public static final class Plugin implements IModPlugin {
		@Override
		public ResourceLocation getPluginUid() {
			return ResourceLocation.fromNamespaceAndPath("jeitransferprobe", "probe");
		}

		@Override
		public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
			runtime = jeiRuntime;
		}
	}

	private static void check(boolean condition, String message) {
		REPORT.add((condition ? "PASS " : "FAIL ") + message);
		failed |= !condition;
	}

	private static ItemStack seed(String plant, int count) {
		ItemStack stack = AgriSeedItem.toStack(AgriApi.getPlant(ResourceLocation.parse(plant)).orElseThrow());
		stack.setCount(count);
		return stack;
	}

	private static void give(Minecraft mc, ItemStack... stacks) {
		mc.getSingleplayerServer().execute(() -> {
			ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
			player.getInventory().clearContent();
			player.inventoryMenu.getCraftSlots().clearContent();
			for (ItemStack stack : stacks) {
				player.getInventory().add(stack.copy());
			}
			player.inventoryMenu.broadcastChanges();
		});
	}

	@SuppressWarnings("unchecked")
	private static IRecipeTransferError transfer(LocalPlayer player, boolean doTransfer) {
		RecipeHolder<CraftingRecipe> journal = (RecipeHolder<CraftingRecipe>) player.level().getRecipeManager()
				.byKey(ResourceLocation.fromNamespaceAndPath("agricraft", "journal")).orElseThrow();
		IRecipeCategory<RecipeHolder<CraftingRecipe>> category = runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
		IRecipeLayoutDrawable<RecipeHolder<CraftingRecipe>> layout = runtime.getRecipeManager()
				.createRecipeLayoutDrawable(category, journal, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup())
				.orElseThrow();
		IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> handler = runtime.getRecipeTransferManager()
				.getRecipeTransferHandler(player.inventoryMenu, category).orElseThrow();
		return handler.transferRecipe(player.inventoryMenu, journal, layout.getRecipeSlotsView(), player, false, doTransfer);
	}

	private static long produceRecipesFor(ItemStack seed) {
		IFocusFactory focuses = runtime.getJeiHelpers().getFocusFactory();
		return runtime.getRecipeManager().createRecipeLookup(CropProduceCategory.TYPE)
				.limitFocus(List.of(focuses.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, seed)))
				.get().count();
	}

	private static String describe(IRecipeTransferError error) {
		return error == null ? "none" : error.getType().toString();
	}

	private static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (step < 0 || player == null || runtime == null || mc.getSingleplayerServer() == null) {
			return;
		}
		ticks++;
		try {
			switch (step) {
				case 0 -> {
					if (ticks < 100) return;
					// Negative control: no seeds at all, JEI must still report the seed as missing.
					give(mc, new ItemStack(Items.WRITABLE_BOOK));
				}
				case 1 -> {
					if (ticks < 20) return;
					IRecipeTransferError error = transfer(player, false);
					check(error != null, "book only: JEI reports a missing ingredient (error=" + describe(error) + ")");
					give(mc, new ItemStack(Items.WRITABLE_BOOK), seed("minecraft:wheat", 5), seed("minecraft:carrot", 3));
				}
				case 2 -> {
					if (ticks < 20) return;
					IRecipeTransferError error = transfer(player, false);
					check(error == null, "book + AgriCraft wheat/carrot seeds: transfer allowed (error=" + describe(error) + ")");
					if (error == null) {
						transfer(player, true);
					}
				}
				case 3 -> {
					if (ticks < 20) return;
					InventoryMenu menu = player.inventoryMenu;
					List<String> grid = new ArrayList<>();
					boolean book = false, agriSeed = false;
					for (int slot = 1; slot <= 4; slot++) {
						ItemStack stack = menu.getSlot(slot).getItem();
						grid.add(stack.isEmpty() ? "-" : stack.getCount() + "x" + stack.getHoverName().getString());
						book |= stack.is(Items.WRITABLE_BOOK);
						agriSeed |= stack.is(ModItems.SEED.get());
					}
					ItemStack result = menu.getSlot(0).getItem();
					check(book && agriSeed, "crafting grid filled with book + AgriCraft seed " + grid);
					check(result.is(ModItems.JOURNAL.get()), "crafting result is the journal (" + result + ")");
					long wheat = produceRecipesFor(seed("minecraft:wheat", 1));
					long carrot = produceRecipesFor(seed("minecraft:carrot", 1));
					check(wheat == 1, "JEI lookup: wheat seed is used in " + wheat + " produce recipe(s), expected 1");
					check(carrot == 1, "JEI lookup: carrot seed is used in " + carrot + " produce recipe(s), expected 1");
					finish(mc);
					return;
				}
				default -> {
					return;
				}
			}
			step++;
			ticks = 0;
		} catch (Throwable t) {
			StringWriter trace = new StringWriter();
			t.printStackTrace(new PrintWriter(trace));
			REPORT.add("FAIL exception at step " + step + ": " + trace);
			failed = true;
			finish(mc);
		}
	}

	private static void finish(Minecraft mc) {
		step = -1;
		REPORT.add(failed ? "RESULT FAIL" : "RESULT PASS");
		try {
			Files.writeString(mc.gameDirectory.toPath().resolve("jei-transfer-results.txt"), String.join("\n", REPORT) + "\n");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
