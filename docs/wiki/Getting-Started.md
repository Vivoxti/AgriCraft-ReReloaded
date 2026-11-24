# Getting started with compatible crops

Install the AgriCraft build for your loader, its required dependencies, and the source mod for each crop you want to grow. A compatibility pack supplies crop definitions; it does not install the other mod. The supported release uses Minecraft 1.21.1 and Java 21. Starfield Pastoral requires the NeoForge build and the exact supported Starfield version.

## Two kinds of farm

**Converted crops** use AgriCraft crop blocks. This includes the six [built-in crop packs](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Crop-Packs-and-Seed-Conversion). Their original seeds become AgriCraft seeds, their harvest returns the source mod's items, and AgriCraft controls growth, weeds and soil requirements.

**Native Starfield crops** keep Starfield's blocks, seed items, calendar, water, seasons and quality. AgriCraft adds a genome and tools to that planting. Read [Starfield Pastoral](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Starfield-Pastoral) before building this kind of farm: its Gain and Growth rules differ from converted crops.

## Your first converted crop

1. Obtain a supported seed in the source mod's usual way. Install that mod's matching loader version and leave automatic compatibility packs enabled.
2. Place an empty seed analyzer. Sneak-right-click it with the source seed to convert one seed, then analyze the resulting AgriCraft seed. The analyzer interface also accepts supported seeds.
3. Prepare suitable soil and light. Place **single** crop sticks and plant the AgriCraft seed. Use the journal or [JEI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/JEI) / [EMI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/EMI) to check requirements.
4. Let it mature and harvest without breaking the crop. The species determines which stage it returns to. Croptopia resets to stage 0; most other compatible packs reset to stage 4.
5. Keep two mature parents beside empty **double** crop sticks to breed. Inspect offspring and retain the traits you need.

The magnifying glass reveals both alleles. A trowel moves a planting with its genome; a rake removes ordinary weeds. The analyzer and journal help you compare varieties before expanding the farm.

## Everyday tools and items

**Plant clipper.** Clip a mature flower or other plant with clipping products to collect them without harvesting; the crop goes back to its first growth stage after each clip. The clipper works from either hand: since 4.0.21 a clipper in the offhand clips the plant, while seeds, fertilizer or a rake in the main hand still take priority. A plant with nothing to clip says "There is nothing worth clipping here." and keeps its growth, and an immature plant says it needs to mature first (since 4.0.24). Since 4.0.29 the Gain stat raises the chance that a clip yields something; see [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance).

**Seed bag.** A seed bag holds up to 64 AgriCraft seeds by default. In your inventory, right-click seeds onto the bag (or the bag onto seeds) to store them, and right-click an empty slot with the bag to take one stack out. Right-click soil or crop sticks with the bag in your main hand to plant its first seed. Sneak and scroll with the bag in your main hand to change which seed comes first: by overall genetic quality or by the highest value of one stat. The choice is shown in the action bar. Identical seeds share one entry. Since 4.0.25 the bag keeps its contents and sorter reliably.

**Feeding animals.** Since 4.0.25 an AgriCraft seed counts as its vanilla seed for animals: AgriCraft wheat, beetroot, melon, pumpkin and torchflower seeds breed chickens and tame parrots, and other animals that eat or follow seeds (sniffers, pigs, rabbits, foxes, pandas, turtles, camels, striders, hoglins) accept the matching AgriCraft seeds.

**Composting.** AgriCraft seeds go into a composter, by hand or by hopper. Each seed fills a layer with the chance set by `core.seed_compost_value` (default `0.3`; `0` disables composting).

**Recipes.** The recipes for the clipper, both hand rakes, hollow irrigation channels, magnifying glass, seed analyzer, sprinkler and trowel were revised in 4.0.22. Hollow channels craft three at a time, and the magnifying glass and trowel fit the 2×2 inventory grid. Look the current recipes up in [JEI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/JEI) or [EMI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/EMI).

## Choose an improvement

Use [Genes and Balance](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Genes-and-Balance) to plan for higher yield, faster growth, wider soil tolerance or more reliable breeding. Good soil still matters; high Growth cannot make an unsuitable location fertile. For a large farm, add [irrigation](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses) and select a compatible harvesting machine from the [module index](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Compatibility-Overview).

Some decorative plants have no convertible seed item. Obtain them through the documented mutation recipes instead. Different species do not automatically cross into a third species: a matching recipe must exist.

## Basis of this guide

Defaults refer to AgriCraft ReReloaded **4.0.30**, Minecraft **1.21.1**. Modpacks can override data and settings. [Core settings](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/config/CoreConfig.java) · [Breeding rules](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/6d6935f09e749923566ff0de3d0c76e0da5b3c4c/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriCrossBreedEngine.java).
