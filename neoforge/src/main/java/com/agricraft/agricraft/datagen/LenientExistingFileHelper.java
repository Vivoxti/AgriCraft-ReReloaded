package com.agricraft.agricraft.datagen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

/**
 * ExistingFileHelper wrapper that skips validation for resources belonging to optional compat mods.
 * Their textures/models live inside the third-party mod jars, which are not present in the datagen
 * runtime, so validating them would abort the whole data run.
 */
public class LenientExistingFileHelper extends ExistingFileHelper {

	private static final Set<String> EXTERNAL_NAMESPACES = Set.of(
			"biomesoplenty", "botania", "farmersdelight", "immersiveengineering", "mysticalagriculture", "pamhc2crops"
	);

	private final ExistingFileHelper delegate;

	public LenientExistingFileHelper(ExistingFileHelper delegate) {
		super(List.of(), Set.of(), false, null, null);
		this.delegate = delegate;
	}

	@Override
	public boolean exists(ResourceLocation loc, PackType packType) {
		return EXTERNAL_NAMESPACES.contains(loc.getNamespace()) || this.delegate.exists(loc, packType);
	}

	@Override
	public boolean exists(ResourceLocation loc, IResourceType type) {
		return EXTERNAL_NAMESPACES.contains(loc.getNamespace()) || this.delegate.exists(loc, type);
	}

	@Override
	public boolean exists(ResourceLocation loc, PackType packType, String pathSuffix, String pathPrefix) {
		return EXTERNAL_NAMESPACES.contains(loc.getNamespace()) || this.delegate.exists(loc, packType, pathSuffix, pathPrefix);
	}

	@Override
	public void trackGenerated(ResourceLocation loc, IResourceType type) {
		this.delegate.trackGenerated(loc, type);
	}

	@Override
	public void trackGenerated(ResourceLocation loc, PackType packType, String pathSuffix, String pathPrefix) {
		this.delegate.trackGenerated(loc, packType, pathSuffix, pathPrefix);
	}

	@Override
	public Resource getResource(ResourceLocation loc, PackType packType, String pathSuffix, String pathPrefix) throws FileNotFoundException {
		return this.delegate.getResource(loc, packType, pathSuffix, pathPrefix);
	}

	@Override
	public Resource getResource(ResourceLocation loc, PackType packType) throws FileNotFoundException {
		return this.delegate.getResource(loc, packType);
	}

	@Override
	public List<Resource> getResourceStack(ResourceLocation loc, PackType packType) {
		return this.delegate.getResourceStack(loc, packType);
	}

	@Override
	public boolean isEnabled() {
		return this.delegate.isEnabled();
	}

}
