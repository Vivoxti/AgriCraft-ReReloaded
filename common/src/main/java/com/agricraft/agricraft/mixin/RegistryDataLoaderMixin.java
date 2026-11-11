package com.agricraft.agricraft.mixin;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Skips AgriCraft's built-in plants and mutations when {@link CoreConfig#loadDefaultPlants} is disabled.
 * Only entries whose winning resource comes from the AgriCraft mod jar itself are skipped, so datapack
 * definitions (including overrides of built-in ids) and the compatibility packs keep loading.
 */
@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {

	/** Pack id of the AgriCraft jar: "agricraft" on Fabric, "mod/agricraft" on NeoForge. */
	private static final Set<String> AGRICRAFT_MOD_PACK_IDS = Set.of(AgriApi.MOD_ID, "mod/" + AgriApi.MOD_ID);

	@Inject(method = "loadElementFromResource", at = @At("HEAD"), cancellable = true)
	private static <E> void agricraft$skipDefaultPlants(WritableRegistry<E> registry, Decoder<E> decoder, RegistryOps<JsonElement> ops,
	                                                    ResourceKey<E> key, Resource resource, RegistrationInfo info, CallbackInfo ci) {
		if (CoreConfig.loadDefaultPlants) {
			return;
		}
		ResourceKey<?> registryKey = registry.key();
		if ((registryKey.equals(AgriApi.AGRIPLANTS) || registryKey.equals(AgriApi.AGRIMUTATIONS))
				&& AGRICRAFT_MOD_PACK_IDS.contains(resource.sourcePackId())) {
			ci.cancel();
		}
	}

}
