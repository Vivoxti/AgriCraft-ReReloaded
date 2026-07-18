package com.agricraft.agricraft.common.neoforge.registry;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.neoforge.lootmodifier.GrassLootModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NeoForgeGlobalLootModifiers {

	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AgriApi.MOD_ID);

	public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<GrassLootModifier>> GRASS_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIERS.register("grass", () -> GrassLootModifier.CODEC);

	private NeoForgeGlobalLootModifiers() {
	}

	public static void register(IEventBus bus) {
		GLOBAL_LOOT_MODIFIERS.register(bus);
	}

}
