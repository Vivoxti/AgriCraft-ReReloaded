# Changelog

## 1.21.1-4.0.9 (unreleased)

- ADDED: Irrigation system ported to 1.21.1 (tanks, channels, hollow channels, channel valves, sprinklers), with fluid capability support on NeoForge
- ADDED: Greenhouses ported to 1.21.1 (greenhouse monitor, season bypass and configurable growth bonus inside greenhouses)
- ADDED: Decorative grates ported to 1.21.1 (three positions per block, waterlogging, vines, climbable)
- ADDED: Grass loot modifier ported from the official 1.21 branch (grass has a chance to drop agricraft seeds, NeoForge)
- ADDED: Re-enabled Mystical Agriculture, Industrial Foregoing and PneumaticCraft: Repressurized compat plugins against their 1.21.1 builds
- ADDED: Irrigation and greenhouse config categories
- FIXED: datagen aborting on missing textures of optional compat mods, which left the built jar without plants/soils/mutations data
- FIXED: maintains_farmland block tag was in the pre-1.21 `tags/blocks` folder and was silently ignored

## 1.20.1-4.0.6

- ADDED: PNC:R Harvesting Drones support (PiotrO15)
- ADDED: Industrial Foregoing support (PiotrO15)
- FIXED: annotate emi plugin with EmiEntrypoint (unilock)
- FIXED: reduce particles when breaking crop sticks (PiotrO15)
- FIXED: crop blocks not being horn harvestable in forge (PiotrO15)
- FIXED: typos in mutations jsons (Wyrdix)
- FIXED: Crash when using the clipper via automation (joakime)
- FIXED: typos in pamhc2crops mutations jsons (Minerofmillions)

## 1.20.1-4.0.5

- FIXED: crash when mystical agriculture is not present

## 1.20.1-4.0.4

- FIXED: server crash because client only code is called
- FIXED: crash when fertilizing empty crop sticks

## 1.20.1-4.0.3

- ADDED: Config option to toggle enabling Agricraft's mod compatibility datapacks/resourcepacks by default (unilock)
- ADDED: Allow lavalogging of crops (unilock)
- ADDED: allow marking plants as fireproof (unilock)
- ADDED: Botania plants datapack (PiotrO15)
- ADDED: Mystical Agriculture plants compatibility (PiotrO15, Ketheroth)
- ADDED: Farmer's Delight plants compatibility (PiotrO15)
- ADDED: Botania compat (unilock, Ketheroth)
- CHANGED: iron and obsidian crop sticks are now fireproof and wooden crop sticks brun in lava (unilock)
- CHANGED: renamed clipProducts to clip_products in plant json
- FIXED: wrong light level used (unilock)
- FIXED: unmet growth conditions are not displayed in the tooltips
- FIXED: the vanilla seed conversion doesn't respect the seed.override_planting field in the plant json
- FIXED: plant modifiers are not used
- FIXED: seed analyzer interactions bugs (oitsjustjose)

## 1.20.1-4.0.2-beta

- ADDED: WTHIT plugin (unilock)
- CHANGED: allow harvesting crops with items in hand (unilock)
- FIXED: crash bonemealing empty crops (unilock)
- FIXED: only add nbt to item produces when it's not empty (unilock)
- FIXED: item tags for recipes (unilock)
- FIXED: crash when accessing server side registry from client (unilock)
- FIXED: crop sticks block translation key (unilock)

## 1.20.1-4.0.1-beta

- backport 1.20.4-4.0.1-beta to minecraft 1.20.1 (unilock)

