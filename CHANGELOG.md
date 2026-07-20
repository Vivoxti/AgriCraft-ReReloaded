# Changelog

## 1.21.1-4.0.13 (unreleased)

- ADDED: `mutation_chance_multiplier` config option, scaling the fertility-based base chance for a crop to be picked as a cross-breeding/cloning parent (default 1.2, i.e. +20%)

## 1.21.1-4.0.12

- CHANGED: Seed Analyzer stat labels now keep a 5px safety margin from the panel edge once shrunk to fit

## 1.21.1-4.0.11

- CHANGED: Magnifying glass tooltip max width increased by 30% to fit longer condition messages
- CHANGED: The "needs a specific block" condition line now always renders last among unmet growth conditions, and its intro line is shown before the block riddle instead of the riddle alone
- FIXED: Seed Analyzer stat labels (e.g. "Мутативность") overflowing past the GUI panel on longer translations by shrinking them to fit instead
- FIXED: Journal introduction pages — "Newly Ported Features" moved from page 2 to page 3, to declutter page 2
- FIXED: Journal "Plant Breeding Genetics" title overflowing on a single line on longer translations; it now wraps
- FIXED: Mutation page showing a pink/black missing-texture icon for undiscovered parents/results on Fabric instead of the proper unknown-plant icon

## 1.21.1-4.0.10

- ADDED: Magnifying glass now tells you whether a soil property needs to be higher or lower (e.g. "too dry, needs more moisture") instead of a generic mismatch message
- ADDED: Ore-requiring plants now show a short riddle hinting at the required block instead of a generic "needs a specific block" message
- ADDED: Line wrapping for the magnifying glass overlay tooltip so longer condition messages no longer run off-screen
- ADDED: Full Russian (ru_ru) and Ukrainian (uk_ua) translations
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

