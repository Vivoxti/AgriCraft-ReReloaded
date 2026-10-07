# AgriCraft

Agricultural farming extended.

Original available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/agricraft) and [Modrinth](https://modrinth.com/mod/agricraft).

Fork available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/agricraft-rereloaded)

## Available versions

| Version | Support     |
|---------|-------------|
| 1.21.1  | Supported   |

Croptopia's 58 farmland crops are supported through a built-in compatibility pack on NeoForge and Fabric. The pack is enabled automatically when Croptopia is installed, unless automatic compatibility packs are disabled in AgriCraft's configuration. Native seeds can be converted through the seed analyzer (sneak-right-click), then planted and bred with AgriCraft genes. Harvesting returns Croptopia produce. This pack adds no species mutation recipes and does not convert fruit trees.

On NeoForge, The One Probe displays growth percentages for all AgriCraft plants, including datapack crops. Sneaking also displays species and visible gene stats. The One Probe is optional.

For source builds, generate the compatibility data with `gradlew :neoforge:runData` before running `gradlew build`. To check the Croptopia packs against an installed Croptopia 4.x jar, run `python tests/verify_croptopia_pack.py <Croptopia.jar> <AgriCraft-neoforge.jar> <AgriCraft-fabric.jar>`.

// Null particle texture still shows up when walking on the cropsticks or when trampling crops with cropsticks added.
// If the can't trample config is true then the farmland should also stay farmland if jumped on, at the moment it turns to dirt just the crop doesn't break.
