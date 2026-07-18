package com.agricraft.agricraft.common.plugin.industrialforegoing;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.CompatConfig;
import com.buuz135.industrial.registry.IFRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AgriApi.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class IndustrialForegoingPlugin {
    @SubscribeEvent
    public static void registerAgriCraftPlantRecollectable(RegisterEvent event) {
        if (ModList.get().isLoaded("industrialforegoing") && CompatConfig.enableIndustrialForegoing) {
            event.register(IFRegistries.PLANT_RECOLLECTABLES_REGISTRY_KEY,
                    helper -> helper.register(ResourceLocation.fromNamespaceAndPath(AgriApi.MOD_ID, "agricraft"), new AgriCraftPlantRecollectable())
            );
        }
    }
}
