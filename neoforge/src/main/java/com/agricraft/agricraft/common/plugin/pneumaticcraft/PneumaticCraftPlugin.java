package com.agricraft.agricraft.common.plugin.pneumaticcraft;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CompatConfig;
import me.desht.pneumaticcraft.common.registry.ModHarvestHandlers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AgriApi.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PneumaticCraftPlugin {

    @SubscribeEvent
    public static void registerAgriCraftHarvestHandler(RegisterEvent event) {
        if (ModList.get().isLoaded("pneumaticcraft") && CompatConfig.enablePneumaticCraft) {
            event.register(ModHarvestHandlers.HARVEST_HANDLERS_DEFERRED.getRegistryKey(),
                    helper -> helper.register(ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "agricraft"), new AgriCraftHarvestHandler())
            );
        }
    }
}
