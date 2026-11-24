# Crops in water, lava and other fluids

Crop sticks can stand in a fluid. Most plants must stay dry, but some need a fluid around them: kelp, seagrass and sea pickles grow in water, Nethereed grows in lava, and Farmer's Delight rice grows in water. Since **4.0.30** a modpack can add fluids from other mods too, so a magical crop can, say, grow only in quicksilver.

## Submerging crop sticks

- **Pour a bucket** of the fluid onto the crop sticks. They keep their plant and hold the fluid.
- **Place crop sticks inside** a fluid source block. They are submerged right away.
- **Pick the fluid up** again with an empty bucket. The sticks and the plant stay.
- Flowing fluid never fills crop sticks; only a full bucket or a source block does.
- Sticks that already hold a fluid don't take a second one. The second bucket pours above them, so nothing is lost. The same happens with a fluid the sticks can't hold.
- When the plant or its soil is removed, the fluid stays behind as a normal fluid block.
- A Create open-ended pipe can drain submerged crop sticks. It pulls out the fluid once, and the sticks are dry afterwards.

## Which fluid a plant tolerates

| The plant's requirement | Dry sticks | The required fluid | Lava | Any other fluid |
|---|---|---|---|---|
| No fluid (most plants) | Grows | — | Burns | Withers |
| A fluid, e.g. water | Withers | Grows | Burns | Withers |
| Lava (Nethereed) | Withers | Grows | Grows | Withers |

**Burns** means the plant is destroyed on its next growth tick. **Withers** means it loses one growth stage on each growth tick instead of growing. Check a plant's needs in [JEI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/JEI) or [EMI](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/EMI) under Growth requirements. The soil, light and season rules from [Soil, irrigation and greenhouses](https://github.com/Vivoxti/AgriCraft-ReReloaded/wiki/Soil-Irrigation-and-Greenhouses) still apply.

## Adding a fluid from another mod (modpacks)

Out of the box, crop sticks hold only water and lava. To make a crop that grows in another mod's fluid:

1. **List the fluid in the config.** In `config/agricraft.jsonc`, add the fluid's id to `core.fluidloggable_fluids`:

   ```jsonc
   "core": {
       "fluidloggable_fluids": ["somemod:quicksilver"],
       // ...
   }
   ```

   Use the fluid's id, which a recipe viewer shows for the fluid and which is usually the same as its fluid block's id. Water and lava don't need to be listed. The list is read at game start, so restart the game after changing it.

2. **Give the server and every player the same list.** A client whose list differs from the server's is disconnected with a message that shows both lists. Ship the config with the modpack.

3. **Require the fluid in a plant.** Add a `fluid_condition` to the plant's `requirement` in its datapack file (`data/<namespace>/agricraft/plants/<plant>.json`):

   ```json
   "requirement": {
     "fluid_condition": { "fluid": "somemod:quicksilver" },
     "min_light": 0,
     "max_light": 16,
     "light_tolerance_factor": 0.5,
     "soil_humidity": { "type": "equal", "value": "wet", "tolerance_factor": 0.2 },
     "soil_acidity": { "type": "equal", "value": "slightly_acidic", "tolerance_factor": 0.2 },
     "soil_nutrients": { "type": "equal_or_higher", "value": "high", "tolerance_factor": 0.2 },
     "seasons": ["spring", "summer", "autumn", "winter"]
   }
   ```

   `fluid` also accepts a fluid tag, written with `#`, such as `"#c:quicksilver"`. Every fluid in the tag must still be listed in the config for crop sticks to hold it.

4. **Plant and pour.** Plant the seed on crop sticks, pour a bucket of the fluid onto them, and the crop grows.

### Things to keep in mind

- Each listed fluid adds 816 block states to the crop block. A few fluids are fine; keep the list short.
- If you remove a fluid from the list later, crops that held it load dry. Plants that need that fluid then wither.
- With the list empty (the default), the crop block is exactly the same as before 4.0.30.
- A malformed id is ignored with a warning in the game log. The log line `Crop sticks can be submerged in N extra fluid(s)` shows which fluids were accepted.

## Basis of this guide

Behaviour refers to AgriCraft ReReloaded **4.0.30**, Minecraft **1.21.1**. Modpacks can override data and settings. [Growth condition for fluids](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/4.0.30/common/src/main/java/com/agricraft/agricraft/api/requirement/AgriGrowthConditionRegistry.java) · [Extra crop-stick fluids](https://github.com/Vivoxti/AgriCraft-ReReloaded/blob/4.0.30/common/src/main/java/com/agricraft/agricraft/common/block/CropFluids.java).
