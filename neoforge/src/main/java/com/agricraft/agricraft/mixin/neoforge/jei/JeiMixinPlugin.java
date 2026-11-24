package com.agricraft.agricraft.mixin.neoforge.jei;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies the JEI compat mixins only when JEI is installed. */
public final class JeiMixinPlugin implements IMixinConfigPlugin {
	private static final String MOD_ID = "jei";

	@Override
	public boolean shouldApplyMixin(String target, String mixin) {
		return LoadingModList.get().getModFileById(MOD_ID) != null;
	}

	@Override public void onLoad(String mixinPackage) {}
	@Override public String getRefMapperConfig() { return null; }
	@Override public void acceptTargets(Set<String> mine, Set<String> others) {}
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
	@Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
